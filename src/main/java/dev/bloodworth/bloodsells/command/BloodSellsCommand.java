package dev.bloodworth.bloodsells.command;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class BloodSellsCommand implements BloodSellsPlugin.CommandHandler {
    private final BloodSellsPlugin plugin;

    public BloodSellsCommand(BloodSellsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String name, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.messages().mini("<dark_red><bold>BloodSells</bold></dark_red> <gray>></gray> <white>Use <green>/bloodsells reload</green>."));
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("bloodsells.reload") && !sender.hasPermission("bloodsells.admin")) {
                plugin.messages().send(sender, "permission-denied", java.util.Map.of());
                return true;
            }
            plugin.reloadSystems();
            sender.sendMessage(plugin.messages().mini(plugin.bloodConfig().string("format.reload", "<green>BloodSells reloaded.")));
            return true;
        }
        return false;
    }

    @Override
    public Collection<String> suggest(CommandSender sender, String name, String[] args) {
        if (args.length == 1) {
            return List.of("reload").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
