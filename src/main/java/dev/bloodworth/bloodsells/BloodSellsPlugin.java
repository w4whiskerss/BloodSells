package dev.bloodworth.bloodsells;

import dev.bloodworth.bloodsells.command.SellCommands;
import dev.bloodworth.bloodsells.command.WorthCommand;
import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.EconomyRegistry;
import dev.bloodworth.bloodsells.gui.SellGuiListener;
import dev.bloodworth.bloodsells.listener.ItemWorthListener;
import dev.bloodworth.bloodsells.sell.SellService;
import dev.bloodworth.bloodsells.storage.TransactionLogger;
import dev.bloodworth.bloodsells.util.Messages;
import dev.bloodworth.bloodsells.worth.WorthEngine;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;

public final class BloodSellsPlugin extends JavaPlugin {
    private BloodConfig bloodConfig;
    private Messages messages;
    private EconomyRegistry economyRegistry;
    private WorthEngine worthEngine;
    private TransactionLogger transactionLogger;
    private SellService sellService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        reloadSystems();

        registerCommands();

        getServer().getPluginManager().registerEvents(new ItemWorthListener(this), this);
        getServer().getPluginManager().registerEvents(new SellGuiListener(this), this);
        registerPlaceholderApi();
        getLogger().info("BloodSells enabled with " + economyRegistry.availableProviderCount() + " economy provider(s).");
    }

    @Override
    public void onDisable() {
        if (transactionLogger != null) {
            transactionLogger.close();
        }
    }

    public void reloadSystems() {
        reloadConfig();
        bloodConfig = new BloodConfig(this);
        messages = new Messages(this);
        economyRegistry = new EconomyRegistry(this, bloodConfig);
        worthEngine = new WorthEngine(this, bloodConfig);
        if (transactionLogger != null) {
            transactionLogger.close();
        }
        transactionLogger = new TransactionLogger(this, bloodConfig);
        sellService = new SellService(this, bloodConfig, worthEngine, economyRegistry, transactionLogger);
    }

    private void registerCommands() {
        SellCommands sellCommands = new SellCommands(this);
        WorthCommand worthCommand = new WorthCommand(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("sellhand", "Sell the stack in your main hand.", List.of(), new PaperCommand("sellhand", sellCommands));
            event.registrar().register("sellgui", "Open the BloodSells sell GUI.", List.of(), new PaperCommand("sellgui", sellCommands));
            event.registrar().register("sellall", "Sell every matching material in your inventory.", List.of(), new PaperCommand("sellall", sellCommands));
            event.registrar().register("sellhandall", "Sell every inventory item matching your hand.", List.of(), new PaperCommand("sellhandall", sellCommands));
            event.registrar().register("worth", "BloodSells admin worth command.", List.of(), new PaperCommand("worth", worthCommand));
        });
    }

    private void registerPlaceholderApi() {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            Class<?> expansionClass = Class.forName("dev.bloodworth.bloodsells.hook.BloodPlaceholderExpansion");
            Object expansion = expansionClass.getConstructor(BloodSellsPlugin.class).newInstance(this);
            expansionClass.getMethod("register").invoke(expansion);
            getLogger().info("Registered PlaceholderAPI expansion.");
        } catch (ReflectiveOperationException | LinkageError ex) {
            getLogger().warning("PlaceholderAPI was found, but the BloodSells expansion could not be registered: " + ex.getMessage());
        }
    }

    public BloodConfig bloodConfig() {
        return bloodConfig;
    }

    public Messages messages() {
        return messages;
    }

    public EconomyRegistry economies() {
        return economyRegistry;
    }

    public WorthEngine worthEngine() {
        return worthEngine;
    }

    public SellService sellService() {
        return sellService;
    }

    private record PaperCommand(String name, CommandHandler handler) implements BasicCommand {
        @Override
        public void execute(CommandSourceStack source, String[] args) {
            handler.execute(source.getSender(), name, args);
        }

        @Override
        public Collection<String> suggest(CommandSourceStack source, String[] args) {
            return handler.suggest(source.getSender(), name, args);
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return true;
        }
    }

    public interface CommandHandler {
        boolean execute(CommandSender sender, String name, String[] args);

        Collection<String> suggest(CommandSender sender, String name, String[] args);
    }
}
