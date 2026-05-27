package dev.bloodworth.bloodsells.gui;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.sell.Payout;
import dev.bloodworth.bloodsells.sell.SellService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SellGui implements InventoryHolder {
    private final BloodSellsPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final Set<Integer> inputSlots;
    private BukkitRunnable animation;
    private int frame;

    public SellGui(BloodSellsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inputSlots = new HashSet<>(plugin.bloodConfig().guiInputSlots());
        int size = plugin.getConfig().getInt("gui.size", 54);
        Component title = plugin.messages().mini(plugin.bloodConfig().string("gui.title", "<dark_red>BloodSells"));
        this.inventory = Bukkit.createInventory(this, size, title);
        drawStatic();
    }

    public void open() {
        player.openInventory(inventory);
        startAnimation();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public boolean isInputSlot(int slot) {
        return inputSlots.contains(slot);
    }

    public List<Integer> inputSlots() {
        return new ArrayList<>(inputSlots);
    }

    public void refresh() {
        SellService.SaleQuote quote = plugin.sellService().quoteInventory(inventory, inputSlots(), player);
        int previewSlot = plugin.getConfig().getInt("gui.preview-slot", 49);
        ItemStack preview = new ItemStack(Material.PAPER);
        ItemMeta meta = preview.getItemMeta();
        meta.displayName(plugin.messages().mini("<green>Sell Preview"));
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.messages().mini("<gray>Items: <white>" + quote.itemsSold()));
        if (quote.payouts().isEmpty()) {
            lore.add(plugin.messages().mini("<red>No sellable items"));
        } else {
            for (Payout payout : quote.payouts()) {
                lore.add(plugin.messages().mini("<gray>" + plugin.economies().icon(payout.economy()) + " <white>" + plugin.sellService().formatPayouts(List.of(payout)) + " <dark_gray>(" + payout.economy().raw() + ")"));
            }
        }
        meta.lore(lore);
        preview.setItemMeta(meta);
        inventory.setItem(previewSlot, preview);
    }

    public void stop() {
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }

    private void drawStatic() {
        Material fillerMaterial = Material.matchMaterial(plugin.bloodConfig().string("gui.filler-material", "BLACK_STAINED_GLASS_PANE"));
        ItemStack filler = named(fillerMaterial == null ? Material.BLACK_STAINED_GLASS_PANE : fillerMaterial, Component.empty(), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!inputSlots.contains(slot)) {
                inventory.setItem(slot, filler);
            }
        }
        refresh();
    }

    private void startAnimation() {
        animation = new BukkitRunnable() {
            @Override
            public void run() {
                int slot = plugin.getConfig().getInt("gui.confirm-slot", 53);
                List<String> materials = plugin.getConfig().getStringList("gui.confirm-materials");
                Material material = Material.LIME_STAINED_GLASS_PANE;
                if (!materials.isEmpty()) {
                    Material parsed = Material.matchMaterial(materials.get(frame % materials.size()));
                    if (parsed != null) material = parsed;
                }
                ItemStack item = named(material, plugin.messages().mini("<green><bold>Confirm Sale"), List.of(
                        plugin.messages().mini("<gray>Click to sell inserted items."),
                        plugin.messages().mini("<dark_gray>Shulkers sell contents first.")
                ));
                inventory.setItem(slot, item);
                frame++;
            }
        };
        animation.runTaskTimer(plugin, 0L, 10L);
    }

    private ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}
