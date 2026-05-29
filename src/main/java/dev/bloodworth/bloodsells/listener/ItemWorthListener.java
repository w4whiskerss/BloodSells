package dev.bloodworth.bloodsells.listener;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.worth.WorthResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ItemWorthListener implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final BloodSellsPlugin plugin;
    private final NamespacedKey marker;

    public ItemWorthListener(BloodSellsPlugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, "worth_lore");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refreshInventory(player, player.getInventory()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void beforePickup(EntityPickupItemEvent event) {
        strip(event.getItem().getItemStack());
        if (event.getEntity() instanceof Player player) {
            stripInventory(player.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            refreshInventory(player, event.getInventory());
            refreshInventory(player, player.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        strip(event.getCurrentItem());
        strip(event.getCursor());
        if (event.getWhoClicked() instanceof Player player) {
            stripInventory(player.getInventory());
        }
        stripInventory(event.getInventory());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getWhoClicked() instanceof Player player) {
                refreshInventory(player, event.getInventory());
                refreshInventory(player, player.getInventory());
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        strip(event.getOldCursor());
        stripInventory(event.getInventory());
        if (event.getWhoClicked() instanceof Player player) {
            stripInventory(player.getInventory());
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getWhoClicked() instanceof Player player) {
                refreshInventory(player, event.getInventory());
                refreshInventory(player, player.getInventory());
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.bloodConfig().permanentLore()) {
            strip(event.getItemDrop().getItemStack());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!plugin.bloodConfig().permanentLore()) {
            stripInventory(event.getInventory());
            if (event.getPlayer() instanceof Player player) {
                stripInventory(player.getInventory());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.bloodConfig().permanentLore()) {
            stripInventory(event.getPlayer().getInventory());
        }
    }

    private void refreshInventory(Player player, Inventory inventory) {
        if (!plugin.displayPreferences().enabled(player.getUniqueId())) {
            stripInventory(inventory);
            return;
        }
        injectInventory(inventory);
    }

    private void injectInventory(Inventory inventory) {
        if (!plugin.bloodConfig().displayWorth()) {
            return;
        }
        for (ItemStack item : inventory.getContents()) {
            inject(item);
        }
    }

    private void stripInventory(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            strip(item);
        }
    }

    private void inject(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        plugin.worthEngine().worth(item).ifPresent(worth -> {
            strip(item);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(worthLine(worth));
            meta.lore(lore);
            if (!plugin.bloodConfig().permanentLore()) {
                meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
            }
            item.setItemMeta(meta);
        });
    }

    private void strip(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return;
        }
        boolean marked = meta.getPersistentDataContainer().has(marker, PersistentDataType.BYTE);
        List<Component> lore = new ArrayList<>();
        boolean changed = false;
        for (Component line : meta.lore()) {
            if (isWorthLine(PLAIN.serialize(line))) {
                changed = true;
                continue;
            }
            lore.add(line);
        }
        if (changed || marked) {
            meta.lore(lore.isEmpty() ? null : lore);
            meta.getPersistentDataContainer().remove(marker);
            item.setItemMeta(meta);
        }
    }

    private Component worthLine(WorthResult worth) {
        String raw = plugin.bloodConfig().string("format.worth-line", "<!i><#d3d3d3>Worth : <#90ee90>$<price>");
        String price = String.format(java.util.Locale.US, plugin.bloodConfig().displayPriceFormat(), worth.unitWorth());
        raw = raw.replace("<price>", price)
                .replace("<economy>", worth.economy().raw())
                .replace("<economy_icon>", plugin.economies().icon(worth.economy()));
        return plugin.messages().mini(raw);
    }

    private boolean isWorthLine(String plain) {
        return plain.toLowerCase(java.util.Locale.ROOT).replace(" ", "").startsWith("worth:");
    }
}
