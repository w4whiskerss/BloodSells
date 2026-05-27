package dev.bloodworth.bloodsells.gui;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.economy.EconomyKey;
import dev.bloodworth.bloodsells.worth.WorthResult;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public final class WorthAdminGuiListener implements Listener {
    private final BloodSellsPlugin plugin;

    public WorthAdminGuiListener(BloodSellsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorthAdminGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.hasPermission("bloodsells.admingui") && !player.hasPermission("bloodsells.admin")) {
            plugin.messages().send(player, "permission-denied", Map.of());
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        WorthResult worth = plugin.worthEngine().worth(new org.bukkit.inventory.ItemStack(gui.material())).orElse(null);
        if (worth == null) {
            return;
        }
        if (slot == WorthAdminGui.SLOT_ECONOMY) {
            plugin.worthEngine().setEconomy(gui.material(), EconomyKey.parse(gui.nextEconomy(worth.economy())));
            plugin.reloadSystems();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
            gui.refresh();
            return;
        }
        if (slot == WorthAdminGui.SLOT_RESET) {
            plugin.worthEngine().clearOverride(gui.material());
            plugin.reloadSystems();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.4F, 1.8F);
            gui.refresh();
            return;
        }
        if (slot == WorthAdminGui.SLOT_RELOAD) {
            plugin.reloadSystems();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.4F);
            gui.refresh();
            return;
        }
        double delta = switch (slot) {
            case WorthAdminGui.SLOT_DECREASE_100 -> -100D;
            case WorthAdminGui.SLOT_DECREASE_10 -> -10D;
            case WorthAdminGui.SLOT_DECREASE_1 -> -1D;
            case WorthAdminGui.SLOT_INCREASE_1 -> 1D;
            case WorthAdminGui.SLOT_INCREASE_10 -> 10D;
            case WorthAdminGui.SLOT_INCREASE_100 -> 100D;
            default -> 0D;
        };
        if (delta != 0D) {
            plugin.worthEngine().setOverride(gui.material(), Math.max(0D, worth.unitWorth() + delta));
            plugin.reloadSystems();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, delta > 0 ? 1.5F : 0.8F);
            gui.refresh();
        }
    }
}
