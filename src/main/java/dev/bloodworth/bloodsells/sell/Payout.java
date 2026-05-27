package dev.bloodworth.bloodsells.sell;

import dev.bloodworth.bloodsells.economy.EconomyKey;

public record Payout(EconomyKey economy, double amount) {
}
