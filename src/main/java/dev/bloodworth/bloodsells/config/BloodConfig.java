package dev.bloodworth.bloodsells.config;

import dev.bloodworth.bloodsells.economy.EconomyKey;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BloodConfig {
    private final JavaPlugin plugin;
    private final Set<Material> blacklist = new HashSet<>();
    private final Map<Material, ItemOverride> overrides = new HashMap<>();
    private final Map<Material, CategoryRule> categories = new HashMap<>();
    private final Map<String, Double> permissionMultipliers = new HashMap<>();
    private final List<Integer> guiInputSlots;

    public BloodConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
        guiInputSlots = plugin.getConfig().getIntegerList("gui.input-slots");
    }

    private void load() {
        for (String value : plugin.getConfig().getStringList("blacklist")) {
            Material material = Material.matchMaterial(value);
            if (material != null) {
                blacklist.add(material);
            }
        }

        ConfigurationSection items = plugin.getConfig().getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null) {
                    continue;
                }
                double worth = items.getDouble(key + ".worth", -1D);
                EconomyKey economy = EconomyKey.parse(items.getString(key + ".economy", defaultEconomy().raw()));
                overrides.put(material, new ItemOverride(worth, economy));
            }
        }

        ConfigurationSection categorySection = plugin.getConfig().getConfigurationSection("categories");
        if (categorySection != null) {
            for (String key : categorySection.getKeys(false)) {
                EconomyKey economy = EconomyKey.parse(categorySection.getString(key + ".economy", defaultEconomy().raw()));
                double multiplier = categorySection.getDouble(key + ".multiplier", 1D);
                CategoryRule rule = new CategoryRule(key, economy, multiplier);
                for (String materialName : categorySection.getStringList(key + ".materials")) {
                    Material material = Material.matchMaterial(materialName);
                    if (material != null) {
                        categories.put(material, rule);
                    }
                }
            }
        }

        ConfigurationSection multipliers = plugin.getConfig().getConfigurationSection("multipliers.permissions");
        if (multipliers != null) {
            for (String node : multipliers.getKeys(false)) {
                permissionMultipliers.put(node, multipliers.getDouble(node, 1D));
            }
        }
    }

    public boolean isBlacklisted(Material material) {
        return blacklist.contains(material);
    }

    public EconomyKey defaultEconomy() {
        return EconomyKey.parse(plugin.getConfig().getString("settings.default-economy", "VAULT"));
    }

    public ItemOverride override(Material material) {
        return overrides.get(material);
    }

    public CategoryRule category(Material material) {
        return categories.get(material);
    }

    public boolean displayWorth() {
        return plugin.getConfig().getBoolean("settings.display-worth", true);
    }

    public boolean permanentLore() {
        return plugin.getConfig().getBoolean("settings.permanent-lore", false);
    }

    public boolean nbtAwarePricing() {
        return plugin.getConfig().getBoolean("settings.nbt-aware-pricing", false);
    }

    public boolean enchantmentPricing() {
        return plugin.getConfig().getBoolean("settings.enchantment-pricing", false);
    }

    public boolean metadataPricing() {
        return plugin.getConfig().getBoolean("settings.metadata-pricing", false);
    }

    public boolean durabilityPricing() {
        return plugin.getConfig().getBoolean("settings.durability-pricing", false);
    }

    public double enchantmentMultiplier() {
        return plugin.getConfig().getDouble("settings.enchantment-multiplier", 0.08D);
    }

    public double customModelDataMultiplier() {
        return plugin.getConfig().getDouble("settings.custom-model-data-multiplier", 1.15D);
    }

    public String displayPriceFormat() {
        return plugin.getConfig().getString("settings.display-price-format", "%,.2f");
    }

    public int cacheExpireSeconds() {
        return plugin.getConfig().getInt("settings.cache-expire-seconds", 300);
    }

    public boolean transactionLog() {
        return plugin.getConfig().getBoolean("settings.transaction-log", false);
    }

    public double globalBooster() {
        return plugin.getConfig().getDouble("multipliers.boosters.global", 1D);
    }

    public Map<String, Double> permissionMultipliers() {
        return permissionMultipliers;
    }

    public List<Integer> guiInputSlots() {
        List<Integer> slots = new ArrayList<>();
        int size = plugin.getConfig().getInt("gui.size", 54);
        int cancel = plugin.getConfig().getInt("gui.cancel-slot", 45);
        int confirm = plugin.getConfig().getInt("gui.confirm-slot", 53);
        for (int slot = 0; slot < size; slot++) {
            if (slot != cancel && slot != confirm) {
                slots.add(slot);
            }
        }
        return slots;
    }

    public String string(String path, String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }

    public record ItemOverride(double worth, EconomyKey economy) {
    }

    public record CategoryRule(String name, EconomyKey economy, double multiplier) {
    }
}
