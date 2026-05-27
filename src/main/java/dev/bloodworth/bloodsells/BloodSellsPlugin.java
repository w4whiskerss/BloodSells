package dev.bloodworth.bloodsells;

import dev.bloodworth.bloodsells.command.SellCommands;
import dev.bloodworth.bloodsells.command.WorthCommand;
import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.EconomyRegistry;
import dev.bloodworth.bloodsells.gui.SellGuiListener;
import dev.bloodworth.bloodsells.hook.BloodPlaceholderExpansion;
import dev.bloodworth.bloodsells.listener.ItemWorthListener;
import dev.bloodworth.bloodsells.sell.SellService;
import dev.bloodworth.bloodsells.storage.TransactionLogger;
import dev.bloodworth.bloodsells.util.Messages;
import dev.bloodworth.bloodsells.worth.WorthEngine;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

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

        SellCommands sellCommands = new SellCommands(this);
        register("sellhand", sellCommands);
        register("sellgui", sellCommands);
        register("sellall", sellCommands);
        register("sellhandall", sellCommands);
        register("worth", new WorthCommand(this));

        getServer().getPluginManager().registerEvents(new ItemWorthListener(this), this);
        getServer().getPluginManager().registerEvents(new SellGuiListener(this), this);
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new BloodPlaceholderExpansion(this).register();
        }
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

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command missing from plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
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
}
