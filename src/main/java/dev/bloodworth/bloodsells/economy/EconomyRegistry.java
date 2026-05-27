package dev.bloodworth.bloodsells.economy;

import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.provider.CoinsEngineProvider;
import dev.bloodworth.bloodsells.economy.provider.ExcellentEconomyProvider;
import dev.bloodworth.bloodsells.economy.provider.PlayerPointsProvider;
import dev.bloodworth.bloodsells.economy.provider.VaultProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EconomyRegistry {
    private final BloodConfig config;
    private final Map<String, EconomyProvider> providers = new HashMap<>();

    public EconomyRegistry(JavaPlugin plugin, BloodConfig config) {
        this.config = config;
        register(new VaultProvider(plugin, config));
        register(new PlayerPointsProvider(plugin, config));
        register(new CoinsEngineProvider(plugin, config));
        register(new ExcellentEconomyProvider(plugin, config));
    }

    public void register(EconomyProvider provider) {
        providers.put(provider.id().toUpperCase(Locale.ROOT), provider);
    }

    public Optional<EconomyProvider> provider(EconomyKey key) {
        EconomyProvider provider = providers.get(key.provider().toUpperCase(Locale.ROOT));
        if (provider == null || !provider.isAvailable()) {
            return Optional.empty();
        }
        return Optional.of(provider);
    }

    public Optional<EconomyKey> resolve(EconomyKey preferred) {
        if (provider(preferred).isPresent()) {
            return Optional.of(preferred);
        }
        EconomyKey fallback = config.defaultEconomy();
        if (provider(fallback).isPresent()) {
            return Optional.of(fallback);
        }
        for (EconomyProvider provider : providers.values()) {
            if (provider.isAvailable()) {
                return Optional.of(new EconomyKey(provider.id(), ""));
            }
        }
        return Optional.empty();
    }

    public String format(EconomyKey key, double amount) {
        return provider(key).map(p -> p.format(amount, key.currency())).orElse(String.format(Locale.US, "%,.2f %s", amount, key.raw()));
    }

    public String icon(EconomyKey key) {
        return provider(key).map(p -> p.icon(key.currency())).orElse(config.string("economies." + key.provider() + ".icon", key.provider()));
    }

    public int availableProviderCount() {
        int count = 0;
        for (EconomyProvider provider : providers.values()) {
            if (provider.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    public Collection<String> ids() {
        return providers.keySet();
    }

    public Collection<String> availableIds() {
        List<String> ids = new ArrayList<>();
        for (EconomyProvider provider : providers.values()) {
            if (provider.isAvailable()) {
                ids.add(provider.id());
            }
        }
        return ids;
    }
}
