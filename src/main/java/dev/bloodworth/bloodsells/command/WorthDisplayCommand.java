package dev.bloodworth.bloodsells.command;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WorthDisplayCommand implements BloodSellsPlugin.CommandHandler {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final BloodSellsPlugin plugin;

    public WorthDisplayCommand(BloodSellsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String name, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only", Map.of());
            return true;
        }
        if (!player.hasPermission("bloodsells.worthdisplay")) {
            plugin.messages().send(player, "permission-denied", Map.of());
            return true;
        }

        boolean enabled;
        if (args.length > 0 && args[0].equalsIgnoreCase("on")) {
            enabled = plugin.displayPreferences().setEnabled(player.getUniqueId(), true);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
            enabled = plugin.displayPreferences().setEnabled(player.getUniqueId(), false);
        } else {
            enabled = plugin.displayPreferences().toggle(player.getUniqueId());
        }

        if (!enabled) {
            stripInventory(player.getInventory());
            if (player.getOpenInventory() != null) {
                stripInventory(player.getOpenInventory().getTopInventory());
            }
        }

        plugin.messages().send(player, enabled ? "worth-display-on" : "worth-display-off", Map.of());
        return true;
    }

    @Override
    public Collection<String> suggest(CommandSender sender, String name, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "toggle").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }

    private void stripInventory(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            strip(item);
        }
    }

    private void strip(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return;
        }
        List<Component> kept = new ArrayList<>();
        boolean changed = false;
        for (Component line : meta.lore()) {
            if (PLAIN.serialize(line).toLowerCase(Locale.ROOT).replace(" ", "").startsWith("worth:")) {
                changed = true;
                continue;
            }
            kept.add(line);
        }
        if (changed) {
            meta.lore(kept.isEmpty() ? null : kept);
            item.setItemMeta(meta);
        }
    }
}
