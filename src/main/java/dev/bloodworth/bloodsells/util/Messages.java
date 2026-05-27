package dev.bloodworth.bloodsells.util;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public final class Messages {
    private final BloodSellsPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final YamlConfiguration messages;

    public Messages(BloodSellsPlugin plugin) {
        this.plugin = plugin;
        this.messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    public Component component(String key, Map<String, String> placeholders) {
        String raw = messages.getString("prefix", "") + messages.getString(key, key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return mini.deserialize(raw);
    }

    public Component mini(String raw) {
        return mini.deserialize(raw);
    }

    public void send(Audience audience, String key, Map<String, String> placeholders) {
        audience.sendMessage(component(key, placeholders));
    }

    public String raw(String key, String fallback) {
        return messages.getString(key, fallback);
    }
}
