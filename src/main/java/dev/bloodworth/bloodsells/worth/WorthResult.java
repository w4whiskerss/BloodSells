package dev.bloodworth.bloodsells.worth;

import dev.bloodworth.bloodsells.economy.EconomyKey;

public record WorthResult(double unitWorth, EconomyKey economy, String reason) {
    public double total(int amount) {
        return unitWorth * amount;
    }
}
