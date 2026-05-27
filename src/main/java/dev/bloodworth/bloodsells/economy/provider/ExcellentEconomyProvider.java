package dev.bloodworth.bloodsells.economy.provider;

import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.TransactionResult;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class ExcellentEconomyProvider extends AbstractReflectiveProvider {
    public ExcellentEconomyProvider(JavaPlugin plugin, BloodConfig config) {
        super(plugin, config, "EXCELLENTECONOMY", "ExcellentEconomy");
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(OfflinePlayer player, double amount, String currency) {
        return CompletableFuture.completedFuture(depositSync(player, amount, currency));
    }

    private TransactionResult depositSync(OfflinePlayer player, double amount, String currency) {
        Plugin excellent = dependency();
        if (excellent == null) {
            return TransactionResult.fail("ExcellentEconomy is not available.");
        }
        try {
            for (String methodName : new String[]{"deposit", "addMoney", "give"}) {
                try {
                    Method method = excellent.getClass().getMethod(methodName, java.util.UUID.class, double.class);
                    method.invoke(excellent, player.getUniqueId(), amount);
                    return TransactionResult.ok();
                } catch (NoSuchMethodException ignored) {
                }
            }
            return TransactionResult.fail("ExcellentEconomy API method not found.");
        } catch (ReflectiveOperationException ex) {
            return TransactionResult.fail(ex.getMessage());
        }
    }
}
