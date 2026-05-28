package dev.bloodworth.bloodsells.gui;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SellGui implements InventoryHolder {
    private final BloodSellsPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final Set<Integer> inputSlots;

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
        inventory.setItem(plugin.getConfig().getInt("gui.cancel-slot", 45),
                named(material("gui.cancel-material", Material.RED_WOOL), plugin.messages().mini("<!i><red>Cancel"), List.of()));
        inventory.setItem(plugin.getConfig().getInt("gui.confirm-slot", 53),
                named(material("gui.confirm-material", Material.LIME_WOOL), plugin.messages().mini("<!i><green>Confirm"), List.of()));
    }

    public void stop() {
    }

    private void drawStatic() {
        refresh();
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

    private Material material(String path, Material fallback) {
        Material parsed = Material.matchMaterial(plugin.bloodConfig().string(path, fallback.name()));
        return parsed == null ? fallback : parsed;
    }
}
