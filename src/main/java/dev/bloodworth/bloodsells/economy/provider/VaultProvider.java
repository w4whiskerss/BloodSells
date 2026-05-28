package dev.bloodworth.bloodsells.economy.provider;

import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.TransactionResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class VaultProvider extends AbstractReflectiveProvider {
    private Object economy;
    private Class<?> economyClass;

    public VaultProvider(JavaPlugin plugin, BloodConfig config) {
        super(plugin, config, "VAULT", "Vault");
        resolve();
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (economy == null) {
            resolve();
        }
        return economy != null;
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(OfflinePlayer player, double amount, String currency) {
        return CompletableFuture.completedFuture(depositSync(player, amount));
    }

    @Override
    public String format(double amount, String currency) {
        return String.format(Locale.US, config.string("economy.format", "$%,.2f"), amount);
    }

    private TransactionResult depositSync(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return TransactionResult.fail("Vault is not available.");
        }
        try {
            Method method = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            Object response = method.invoke(economy, player, amount);
            Method success = response.getClass().getMethod("transactionSuccess");
            boolean ok = Boolean.TRUE.equals(success.invoke(response));
            String message = "";
            try {
                message = String.valueOf(response.getClass().getField("errorMessage").get(response));
            } catch (ReflectiveOperationException ignored) {
            }
            return ok ? TransactionResult.ok() : TransactionResult.fail(message);
        } catch (ReflectiveOperationException ex) {
            return TransactionResult.fail(ex.getMessage());
        }
    }

    private void resolve() {
        try {
            economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            economy = rsp == null ? null : rsp.getProvider();
        } catch (ClassNotFoundException ex) {
            economy = null;
            economyClass = null;
        }
    }
}
