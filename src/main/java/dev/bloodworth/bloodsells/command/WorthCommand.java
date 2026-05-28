package dev.bloodworth.bloodsells.command;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.util.ItemNames;
import dev.bloodworth.bloodsells.worth.WorthResult;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WorthCommand implements CommandExecutor, TabCompleter, BloodSellsPlugin.CommandHandler {
    private final BloodSellsPlugin plugin;

    public WorthCommand(BloodSellsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return execute(sender, command.getName(), args);
    }

    @Override
    public boolean execute(CommandSender sender, String name, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/worth set <item> <price>");
            sender.sendMessage("/worth info <item>");
            sender.sendMessage("/bloodsells reload");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> set(sender, args);
            case "info" -> info(sender, args);
            default -> false;
        };
    }

    private boolean set(CommandSender sender, String[] args) {
        if (!permission(sender, "bloodsells.worth.set")) return true;
        if (args.length < 3) {
            plugin.messages().send(sender, "usage-worth-set", Map.of());
            return true;
        }
        Material material = material(sender, args[1]);
        if (material == null) return true;
        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            plugin.messages().send(sender, "invalid-price", Map.of());
            return true;
        }
        if (price < 0) {
            plugin.messages().send(sender, "invalid-price", Map.of());
            return true;
        }
        plugin.worthEngine().setOverride(material, price);
        plugin.reloadSystems();
        plugin.messages().send(sender, "worth-set", Map.of("item", material.name(), "price", String.valueOf(price)));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (!permission(sender, "bloodsells.worth.info")) return true;
        if (args.length < 2) {
            plugin.messages().send(sender, "usage-worth-info", Map.of());
            return true;
        }
        Material material = material(sender, args[1]);
        if (material == null) return true;
        WorthResult result = plugin.worthEngine().worth(new ItemStack(material)).orElse(null);
        if (result == null) {
            plugin.messages().send(sender, "no-items", Map.of());
            return true;
        }
        plugin.messages().send(sender, "worth-info", Map.of(
                "item", ItemNames.pretty(material),
                "price", plugin.economies().format(result.economy(), result.unitWorth()),
                "economy", "VAULT"
        ));
        return true;
    }

    private Material material(CommandSender sender, String value) {
        Material material = Material.matchMaterial(value);
        if (material == null) {
            plugin.messages().send(sender, "invalid-material", Map.of("item", value));
        }
        return material;
    }

    private boolean permission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("bloodsells.admin")) {
            return true;
        }
        plugin.messages().send(sender, "permission-denied", Map.of());
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return suggest(sender, command.getName(), args).stream().toList();
    }

    @Override
    public List<String> suggest(CommandSender sender, String name, String[] args) {
        if (args.length == 1) {
            return List.of("set", "info").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && List.of("set", "info").contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toUpperCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.isItem() && material.name().startsWith(prefix)) matches.add(material.name());
            }
            return matches.stream().limit(50).toList();
        }
        return List.of();
    }
}
