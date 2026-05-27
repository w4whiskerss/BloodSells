package dev.bloodworth.bloodsells.command;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.economy.EconomyKey;
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

public final class WorthCommand implements CommandExecutor, TabCompleter {
    private final BloodSellsPlugin plugin;

    public WorthCommand(BloodSellsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/worth set <item> <price>");
            sender.sendMessage("/worth economy <item> <economy>");
            sender.sendMessage("/worth reload");
            sender.sendMessage("/worth info <item>");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> set(sender, args);
            case "economy" -> economy(sender, args);
            case "reload" -> reload(sender);
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

    private boolean economy(CommandSender sender, String[] args) {
        if (!permission(sender, "bloodsells.worth.economy")) return true;
        if (args.length < 3) {
            plugin.messages().send(sender, "usage-worth-economy", Map.of());
            return true;
        }
        Material material = material(sender, args[1]);
        if (material == null) return true;
        EconomyKey economy = EconomyKey.parse(args[2]);
        plugin.worthEngine().setEconomy(material, economy);
        plugin.reloadSystems();
        plugin.messages().send(sender, "worth-economy", Map.of("item", material.name(), "economy", economy.raw()));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!permission(sender, "bloodsells.worth.reload")) return true;
        plugin.reloadSystems();
        sender.sendMessage(plugin.messages().mini(plugin.bloodConfig().string("format.reload", "<green>BloodSells reloaded.")));
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
                "economy", result.economy().raw()
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
        if (args.length == 1) {
            return List.of("set", "economy", "reload", "info").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if ((args.length == 2 && List.of("set", "economy", "info").contains(args[0].toLowerCase(Locale.ROOT)))) {
            String prefix = args[1].toUpperCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.isItem() && material.name().startsWith(prefix)) matches.add(material.name());
            }
            return matches.stream().limit(50).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("economy")) {
            return plugin.economies().ids().stream().map(String::toUpperCase).filter(id -> id.startsWith(args[2].toUpperCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
