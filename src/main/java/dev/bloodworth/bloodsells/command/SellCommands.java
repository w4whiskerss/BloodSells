package dev.bloodworth.bloodsells.command;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.gui.SellGui;
import dev.bloodworth.bloodsells.sell.SellResult;
import dev.bloodworth.bloodsells.util.ItemNames;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SellCommands implements CommandExecutor, TabCompleter, BloodSellsPlugin.CommandHandler {
    private final BloodSellsPlugin plugin;

    public SellCommands(BloodSellsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return execute(sender, command.getName(), args);
    }

    @Override
    public boolean execute(CommandSender sender, String name, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only", Map.of());
            return true;
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sellhand" -> sellHand(player);
            case "sellgui" -> sellGui(player);
            case "sellall" -> sellAll(player, args);
            case "sellhandall" -> sellHandAll(player);
            default -> false;
        };
    }

    private boolean sellHand(Player player) {
        if (!require(player, "bloodsells.sellhand")) return true;
        ItemStack hand = player.getInventory().getItemInMainHand();
        SellResult result = plugin.sellService().sellHand(player);
        announce(player, result, hand, hand.getAmount());
        return true;
    }

    private boolean sellGui(Player player) {
        if (!require(player, "bloodsells.sellgui")) return true;
        new SellGui(plugin, player).open();
        return true;
    }

    private boolean sellAll(Player player, String[] args) {
        if (!require(player, "bloodsells.sellall")) return true;
        if (args.length < 1) {
            plugin.messages().send(player, "usage-sellall", Map.of());
            return true;
        }
        Material material = Material.matchMaterial(args[0]);
        if (material == null) {
            plugin.messages().send(player, "invalid-material", Map.of("item", args[0]));
            return true;
        }
        SellResult result = plugin.sellService().sellAllNamed(player, material);
        announce(player, result, new ItemStack(material), result.itemsSold());
        return true;
    }

    private boolean sellHandAll(Player player) {
        if (!require(player, "bloodsells.sellhandall")) return true;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            plugin.messages().send(player, "empty-hand", Map.of());
            return true;
        }
        SellResult result = plugin.sellService().sellHandAll(player);
        announce(player, result, hand, result.itemsSold());
        return true;
    }

    private void announce(Player player, SellResult result, ItemStack item, int amount) {
        if (!result.soldAnything()) {
            plugin.messages().send(player, "no-items", Map.of());
            return;
        }
        String payouts = plugin.sellService().formatPayouts(result.payouts());
        plugin.messages().send(player, "sold", Map.of(
                "amount", String.valueOf(amount),
                "item", ItemNames.display(item),
                "payouts", payouts
        ));
        player.sendActionBar(Component.text("+" + payouts));
    }

    private boolean require(Player player, String permission) {
        if (player.hasPermission(permission) || player.hasPermission("bloodsells.use")) {
            return true;
        }
        plugin.messages().send(player, "permission-denied", Map.of());
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return suggest(sender, command.getName(), args).stream().toList();
    }

    @Override
    public List<String> suggest(CommandSender sender, String name, String[] args) {
        if (name.equalsIgnoreCase("sellall") && args.length == 1) {
            String prefix = args[0].toUpperCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.isItem() && material.name().startsWith(prefix)) {
                    matches.add(material.name());
                }
            }
            return matches.stream().limit(50).toList();
        }
        return List.of();
    }
}
