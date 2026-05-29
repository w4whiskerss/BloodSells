package dev.bloodworth.bloodsells.display;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class WorthDisplayPreferences {
    private final JavaPlugin plugin;
    private final File file;
    private final Set<UUID> disabled = new HashSet<>();

    public WorthDisplayPreferences(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "display-disabled.yml");
        load();
    }

    public boolean enabled(UUID uuid) {
        return !disabled.contains(uuid);
    }

    public boolean setEnabled(UUID uuid, boolean enabled) {
        if (enabled) {
            disabled.remove(uuid);
        } else {
            disabled.add(uuid);
        }
        save();
        return enabled;
    }

    public boolean toggle(UUID uuid) {
        return setEnabled(uuid, !enabled(uuid));
    }

    private void load() {
        disabled.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String value : config.getStringList("disabled")) {
            try {
                disabled.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("disabled", disabled.stream().map(UUID::toString).sorted().toList());
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save worth display preferences.", ex);
        }
    }
}
