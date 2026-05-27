package dev.bloodworth.bloodsells.hook;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.worth.WorthResult;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class BloodPlaceholderExpansion extends PlaceholderExpansion {
    private final BloodSellsPlugin plugin;

    public BloodPlaceholderExpansion(BloodSellsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bloodsells";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BloodWorth";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        WorthResult worth = plugin.worthEngine().worth(hand).orElse(null);
        if (worth == null) {
            return "0";
        }
        return switch (params.toLowerCase()) {
            case "hand_worth" -> String.valueOf(worth.unitWorth());
            case "hand_economy" -> worth.economy().raw();
            case "hand_worth_formatted" -> plugin.economies().format(worth.economy(), worth.unitWorth());
            default -> "";
        };
    }
}
