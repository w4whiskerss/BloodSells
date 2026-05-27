package dev.bloodworth.bloodsells;

import dev.bloodworth.bloodsells.command.BloodSellsCommand;
import dev.bloodworth.bloodsells.command.SellCommands;
import dev.bloodworth.bloodsells.command.WorthCommand;
import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.EconomyRegistry;
import dev.bloodworth.bloodsells.gui.SellGuiListener;
import dev.bloodworth.bloodsells.gui.WorthAdminGuiListener;
import dev.bloodworth.bloodsells.listener.ItemWorthListener;
import dev.bloodworth.bloodsells.sell.SellService;
import dev.bloodworth.bloodsells.storage.TransactionLogger;
import dev.bloodworth.bloodsells.util.Messages;
import dev.bloodworth.bloodsells.worth.WorthEngine;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
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
        if (!new File(getDataFolder(), "messages.yml").exists()) {
            saveResource("messages.yml", false);
        }
        reloadSystems();

        registerCommands();

        getServer().getPluginManager().registerEvents(new ItemWorthListener(this), this);
        getServer().getPluginManager().registerEvents(new SellGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new WorthAdminGuiListener(this), this);
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
        migrateConfig();
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

    private void migrateConfig() {
        boolean changed = false;
        String worthLine = getConfig().getString("format.worth-line", "");
        if ("<dark_gray>WORTH: <green><price> <gray><economy_icon>".equals(worthLine)) {
            getConfig().set("format.worth-line", "<!i><white>Worth : <price>");
            changed = true;
        }
        if (!getConfig().contains("settings.display-price-format")) {
            getConfig().set("settings.display-price-format", "%,.2f");
            changed = true;
        }
        if (!getConfig().contains("settings.enchantment-pricing")) {
            getConfig().set("settings.enchantment-pricing", false);
            changed = true;
        }
        if (!getConfig().contains("settings.metadata-pricing")) {
            getConfig().set("settings.metadata-pricing", false);
            changed = true;
        }
        if (!getConfig().contains("settings.durability-pricing")) {
            getConfig().set("settings.durability-pricing", false);
            changed = true;
        }
        if (getConfig().getBoolean("settings.nbt-aware-pricing", false)) {
            getConfig().set("settings.nbt-aware-pricing", false);
            changed = true;
        }
        String defaultEconomy = getConfig().getString("settings.default-economy", "");
        if (defaultEconomy.contains(":")) {
            getConfig().set("settings.default-economy", defaultEconomy.split(":", 2)[0].trim().toUpperCase(java.util.Locale.ROOT));
            changed = true;
        }
        if ("PLAYERPOINTS:blood".equalsIgnoreCase(getConfig().getString("items.EMERALD.economy", ""))) {
            getConfig().set("items.EMERALD.economy", "VAULT");
            changed = true;
        }
        if (getConfig().getString("categories.nether.economy", "").contains(":")) {
            getConfig().set("categories.nether.economy", "VAULT");
            changed = true;
        }
        if (getConfig().getString("items.NETHER_STAR.economy", "").contains(":")) {
            getConfig().set("items.NETHER_STAR.economy", "VAULT");
            changed = true;
        }
        if ("EXCELLENTECONOMY".equalsIgnoreCase(getConfig().getString("items.SPAWNER.economy", ""))) {
            getConfig().set("items.SPAWNER.economy", "VAULT");
            changed = true;
        }
        changed |= stripCurrencySuffixes("items");
        changed |= stripCurrencySuffixes("categories");
        if (changed) {
            saveConfig();
        }
    }

    private boolean stripCurrencySuffixes(String sectionPath) {
        org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection(sectionPath);
        if (section == null) {
            return false;
        }
        boolean changed = false;
        for (String key : section.getKeys(false)) {
            String path = sectionPath + "." + key + ".economy";
            String value = getConfig().getString(path);
            if (value != null && value.contains(":")) {
                getConfig().set(path, value.split(":", 2)[0].trim().toUpperCase(java.util.Locale.ROOT));
                changed = true;
            }
        }
        return changed;
    }

    private void registerCommands() {
        SellCommands sellCommands = new SellCommands(this);
        WorthCommand worthCommand = new WorthCommand(this);
        BloodSellsCommand bloodSellsCommand = new BloodSellsCommand(this);
        registerCommand("bloodsells", "BloodSells main command.", List.of("bsells"), new PaperCommand("bloodsells", bloodSellsCommand));
        registerCommand("sellhand", "Sell the stack in your main hand.", List.of(), new PaperCommand("sellhand", sellCommands));
        registerCommand("sellgui", "Open the BloodSells sell GUI.", List.of(), new PaperCommand("sellgui", sellCommands));
        registerCommand("sellall", "Sell every matching material in your inventory.", List.of(), new PaperCommand("sellall", sellCommands));
        registerCommand("sellhandall", "Sell every inventory item matching your hand.", List.of(), new PaperCommand("sellhandall", sellCommands));
        registerCommand("worth", "BloodSells admin worth command.", List.of(), new PaperCommand("worth", worthCommand));
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
