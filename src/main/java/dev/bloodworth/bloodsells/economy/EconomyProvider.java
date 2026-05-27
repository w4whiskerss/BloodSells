package dev.bloodworth.bloodsells.economy;

import org.bukkit.OfflinePlayer;

import java.util.concurrent.CompletableFuture;

public interface EconomyProvider {
    String id();

    boolean isAvailable();

    CompletableFuture<TransactionResult> deposit(OfflinePlayer player, double amount, String currency);

    String format(double amount, String currency);

    default String icon(String currency) {
        return "";
    }
}
