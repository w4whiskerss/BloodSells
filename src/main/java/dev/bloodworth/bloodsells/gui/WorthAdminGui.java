package dev.bloodworth.bloodsells.gui;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.economy.EconomyKey;
import dev.bloodworth.bloodsells.util.ItemNames;
import dev.bloodworth.bloodsells.worth.WorthResult;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class WorthAdminGui implements InventoryHolder {
    public static final int SLOT_DECREASE_100 = 10;
    public static final int SLOT_DECREASE_10 = 11;
    public static final int SLOT_DECREASE_1 = 12;
    public static final int SLOT_INFO = 22;
    public static final int SLOT_INCREASE_1 = 14;
    public static final int SLOT_INCREASE_10 = 15;
    public static final int SLOT_INCREASE_100 = 16;
    public static final int SLOT_ECONOMY = 30;
    public static final int SLOT_RESET = 32;
    public static final int SLOT_RELOAD = 49;

    private final BloodSellsPlugin plugin;
    private final Player player;
    private final Material material;
    private final Inventory inventory;

    public WorthAdminGui(BloodSellsPlugin plugin, Player player, Material material) {
        this.plugin = plugin;
        this.player = player;
        this.material = material;
        this.inventory = Bukkit.createInventory(this, 54, plugin.messages().mini("<dark_red>Worth Editor"));
        refresh();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Material material() {
        return material;
    }

    public void refresh() {
        inventory.clear();
        WorthResult worth = plugin.worthEngine().worth(new ItemStack(material)).orElse(new WorthResult(0, plugin.bloodConfig().defaultEconomy(), "none"));
        fill();
        inventory.setItem(SLOT_DECREASE_100, button(Material.RED_STAINED_GLASS_PANE, "<red>-100", List.of()));
        inventory.setItem(SLOT_DECREASE_10, button(Material.RED_STAINED_GLASS_PANE, "<red>-10", List.of()));
        inventory.setItem(SLOT_DECREASE_1, button(Material.RED_STAINED_GLASS_PANE, "<red>-1", List.of()));
        inventory.setItem(SLOT_INCREASE_1, button(Material.LIME_STAINED_GLASS_PANE, "<green>+1", List.of()));
        inventory.setItem(SLOT_INCREASE_10, button(Material.LIME_STAINED_GLASS_PANE, "<green>+10", List.of()));
        inventory.setItem(SLOT_INCREASE_100, button(Material.LIME_STAINED_GLASS_PANE, "<green>+100", List.of()));
        inventory.setItem(SLOT_INFO, button(material, "<white>" + ItemNames.pretty(material), List.of(
                "<gray>Worth: <green>" + plainPrice(worth.unitWorth()),
                "<gray>Economy: <yellow>" + worth.economy().raw(),
                "<gray>Source: <white>" + worth.reason()
        )));
        inventory.setItem(SLOT_ECONOMY, button(Material.COMPASS, "<yellow>Cycle Economy", economyLore(worth.economy())));
        inventory.setItem(SLOT_RESET, button(Material.BARRIER, "<red>Reset To Auto", List.of("<gray>Removes this material from items.")));
        inventory.setItem(SLOT_RELOAD, button(Material.EMERALD, "<green>Save / Reload", List.of("<gray>Reloads BloodSells systems.")));
    }

    public String nextEconomy(EconomyKey current) {
        List<String> options = new ArrayList<>(plugin.economies().availableIds());
        if (options.isEmpty()) {
            options.add(current.provider());
        }
        int index = options.indexOf(current.provider());
        return options.get((index + 1 + options.size()) % options.size());
    }

    private List<String> economyLore(EconomyKey current) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Current: <yellow>" + current.raw());
        Collection<String> detected = plugin.economies().availableIds();
        lore.add("<gray>Detected: <white>" + (detected.isEmpty() ? "none" : String.join(", ", detected)));
        return lore;
    }

    private void fill() {
        ItemStack filler = button(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack button(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.messages().mini("<!i>" + name));
        List<Component> lore = loreLines.stream().map(line -> plugin.messages().mini("<!i>" + line)).toList();
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    private String plainPrice(double value) {
        return String.format(Locale.US, plugin.bloodConfig().displayPriceFormat(), value);
    }
}
