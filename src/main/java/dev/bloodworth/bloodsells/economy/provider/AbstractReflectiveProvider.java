package dev.bloodworth.bloodsells.economy.provider;

import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Locale;

abstract class AbstractReflectiveProvider implements EconomyProvider {
    protected final JavaPlugin plugin;
    protected final BloodConfig config;
    private final String id;
    private final String pluginName;

    AbstractReflectiveProvider(JavaPlugin plugin, BloodConfig config, String id, String pluginName) {
        this.plugin = plugin;
        this.config = config;
        this.id = id;
        this.pluginName = pluginName;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isAvailable() {
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
        return target != null && target.isEnabled() && config.string("economies." + id + ".enabled", "true").equalsIgnoreCase("true");
    }

    @Override
    public String format(double amount, String currency) {
        String pattern = config.string("economies." + id + ".format", "%,.2f");
        return String.format(Locale.US, pattern, amount);
    }

    @Override
    public String icon(String currency) {
        return config.string("economies." + id + ".icon", "");
    }

    protected Plugin dependency() {
        return Bukkit.getPluginManager().getPlugin(pluginName);
    }

    protected Object call(Object target, String method, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        Method reflected = target.getClass().getMethod(method, types);
        return reflected.invoke(target, args);
    }
}
