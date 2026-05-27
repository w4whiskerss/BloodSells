package dev.bloodworth.bloodsells.economy.provider;

import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.TransactionResult;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public final class PlayerPointsProvider extends AbstractReflectiveProvider {
    public PlayerPointsProvider(JavaPlugin plugin, BloodConfig config) {
        super(plugin, config, "PLAYERPOINTS", "PlayerPoints");
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(OfflinePlayer player, double amount, String currency) {
        return CompletableFuture.completedFuture(depositSync(player, amount));
    }

    private TransactionResult depositSync(OfflinePlayer player, double amount) {
        Plugin pp = dependency();
        if (pp == null) {
            return TransactionResult.fail("PlayerPoints is not available.");
        }
        try {
            Object api = call(pp, "getAPI", new Class<?>[0]);
            call(api, "give", new Class<?>[]{java.util.UUID.class, int.class}, player.getUniqueId(), (int) Math.round(amount));
            return TransactionResult.ok();
        } catch (ReflectiveOperationException ex) {
            return TransactionResult.fail(ex.getMessage());
        }
    }
}
