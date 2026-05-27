package dev.bloodworth.bloodsells.economy.provider;

import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.TransactionResult;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class CoinsEngineProvider extends AbstractReflectiveProvider {
    public CoinsEngineProvider(JavaPlugin plugin, BloodConfig config) {
        super(plugin, config, "COINSENGINE", "CoinsEngine");
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(OfflinePlayer player, double amount, String currency) {
        return CompletableFuture.completedFuture(depositSync(player, amount, currency));
    }

    private TransactionResult depositSync(OfflinePlayer player, double amount, String currency) {
        Plugin coins = dependency();
        if (coins == null) {
            return TransactionResult.fail("CoinsEngine is not available.");
        }
        String name = currency == null || currency.isBlank() ? "coins" : currency;
        try {
            Object currencyObj = invokeAny(coins, new String[]{"getCurrency", "currency"}, new Class<?>[]{String.class}, name);
            if (currencyObj == null) {
                Object manager = invokeAny(coins, new String[]{"getCurrencyManager", "getManager"}, new Class<?>[0]);
                if (manager != null) {
                    currencyObj = invokeAny(manager, new String[]{"getCurrency", "get"}, new Class<?>[]{String.class}, name);
                }
            }
            if (currencyObj == null) {
                return TransactionResult.fail("CoinsEngine currency not found: " + name);
            }
            invokeAny(currencyObj, new String[]{"add", "deposit", "give"}, new Class<?>[]{java.util.UUID.class, double.class}, player.getUniqueId(), amount);
            return TransactionResult.ok();
        } catch (ReflectiveOperationException ex) {
            return TransactionResult.fail(ex.getMessage());
        }
    }

    private Object invokeAny(Object target, String[] names, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name, types);
                return method.invoke(target, args);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
