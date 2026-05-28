package dev.bloodworth.bloodsells.gui;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.sell.SellResult;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class SellGuiListener implements Listener {
    private final BloodSellsPlugin plugin;

    public SellGuiListener(BloodSellsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellGui gui)) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= 0 && raw < event.getInventory().getSize()) {
            if (raw == plugin.getConfig().getInt("gui.confirm-slot", 53)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    SellResult result = plugin.sellService().sellInventoryContents(player, event.getInventory(), gui.inputSlots());
                    gui.refresh();
                    if (result.soldAnything()) {
                        plugin.messages().send(player, "gui-sold", Map.of("payouts", plugin.sellService().formatPayouts(result.payouts())));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.6F);
                    } else {
                        plugin.messages().send(player, "no-items", Map.of());
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F, 0.8F);
                    }
                }
                return;
            }
            if (raw == plugin.getConfig().getInt("gui.cancel-slot", 45)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    player.closeInventory();
                }
                return;
            }
            if (!gui.isInputSlot(raw)) {
                event.setCancelled(true);
                return;
            }
        }
        plugin.getServer().getScheduler().runTask(plugin, gui::refresh);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellGui gui)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < event.getInventory().getSize() && !gui.isInputSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
        plugin.getServer().getScheduler().runTask(plugin, gui::refresh);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellGui gui)) {
            return;
        }
        gui.stop();
        returnItems((Player) event.getPlayer(), event.getInventory(), gui);
    }

    private void returnItems(Player player, Inventory inventory, SellGui gui) {
        for (int slot : gui.inputSlots()) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            inventory.setItem(slot, null);
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }
}
