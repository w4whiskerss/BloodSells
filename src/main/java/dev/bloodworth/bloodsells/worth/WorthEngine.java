package dev.bloodworth.bloodsells.worth;

import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.EconomyKey;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WorthEngine {
    private final JavaPlugin plugin;
    private final BloodConfig config;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<Material, Double> baseCache = new EnumMap<>(Material.class);

    public WorthEngine(JavaPlugin plugin, BloodConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public Optional<WorthResult> worth(ItemStack item) {
        if (item == null || item.getType().isAir() || config.isBlacklisted(item.getType())) {
            return Optional.empty();
        }
        String key = cacheKey(item);
        CacheEntry cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt > now) {
            return Optional.of(cached.result);
        }
        WorthResult result = calculate(item);
        cache.put(key, new CacheEntry(result, now + config.cacheExpireSeconds() * 1000L));
        return Optional.of(result);
    }

    public void invalidate() {
        cache.clear();
        baseCache.clear();
    }

    public void setOverride(Material material, double worth) {
        plugin.getConfig().set("items." + material.name() + ".worth", worth);
        plugin.saveConfig();
        invalidate();
    }

    public void setEconomy(Material material, EconomyKey economy) {
        plugin.getConfig().set("items." + material.name() + ".economy", economy.raw());
        plugin.saveConfig();
        invalidate();
    }

    private WorthResult calculate(ItemStack item) {
        Material material = item.getType();
        BloodConfig.ItemOverride override = config.override(material);
        double base = override != null && override.worth() >= 0 ? override.worth() : generatedBase(material);
        EconomyKey economy = override != null ? override.economy() : config.defaultEconomy();

        BloodConfig.CategoryRule category = config.category(material);
        if (category != null) {
            economy = category.economy();
            base *= category.multiplier();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!meta.getEnchants().isEmpty()) {
                int levels = meta.getEnchants().entrySet().stream().mapToInt(entry -> enchantWeight(entry.getKey(), entry.getValue())).sum();
                base *= 1D + (levels * config.enchantmentMultiplier());
            }
            if (meta.hasCustomModelData() || meta.hasDisplayName() || !meta.getPersistentDataContainer().isEmpty()) {
                base *= config.customModelDataMultiplier();
            }
            if (meta instanceof Damageable damageable && material.getMaxDurability() > 0) {
                double remaining = Math.max(0D, material.getMaxDurability() - damageable.getDamage());
                base *= Math.max(0.1D, remaining / material.getMaxDurability());
            }
        }
        return new WorthResult(round(base), economy, override == null ? "generated" : "override");
    }

    private double generatedBase(Material material) {
        return baseCache.computeIfAbsent(material, this::scoreMaterial);
    }

    private double scoreMaterial(Material material) {
        String name = material.name();
        double score = 2D;
        if (material.isBlock()) score += 3D;
        if (material.getMaxStackSize() == 1) score += 25D;
        if (material.getMaxDurability() > 0) score += Math.min(80D, material.getMaxDurability() / 20D);
        if (name.contains("NETHERITE")) score += 900D;
        else if (name.contains("DIAMOND")) score += 450D;
        else if (name.contains("EMERALD")) score += 220D;
        else if (name.contains("GOLD")) score += 90D;
        else if (name.contains("IRON")) score += 55D;
        else if (name.contains("COPPER")) score += 20D;
        if (name.contains("SPAWNER")) score += 5000D;
        if (name.contains("ELYTRA")) score += 7500D;
        if (name.contains("TOTEM")) score += 2500D;
        if (name.contains("TRIDENT")) score += 1800D;
        if (name.contains("SHULKER_BOX")) score += 350D;
        if (name.contains("NETHER_STAR")) score += 1000D;
        if (name.contains("DRAGON") || name.contains("BEACON")) score += 1500D;
        if (name.contains("LOG") || name.contains("PLANKS") || name.contains("STEM") || name.contains("HYPHAE")) score += 6D;
        if (name.contains("WOOL") || name.contains("TERRACOTTA")) score += 8D;
        if (name.contains("MUSIC_DISC")) score += 200D;
        if (name.contains("ORE")) score *= 2.3D;
        if (name.contains("DEEPSLATE")) score *= 1.2D;
        if (name.contains("RAW_")) score *= 1.4D;
        if (name.contains("ENCHANTED")) score *= 3D;
        return round(Math.max(1D, score));
    }

    private int enchantWeight(Enchantment enchantment, int level) {
        String key = enchantment.getKey().getKey().toUpperCase(Locale.ROOT);
        int weight = key.contains("MENDING") || key.contains("SILK_TOUCH") ? 8 : 4;
        return weight * Math.max(1, level);
    }

    private String cacheKey(ItemStack item) {
        String base = item.getType().name();
        if (!config.nbtAwarePricing()) {
            return base;
        }
        return base + ":" + item.getItemMeta();
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record CacheEntry(WorthResult result, long expiresAt) {
    }
}
