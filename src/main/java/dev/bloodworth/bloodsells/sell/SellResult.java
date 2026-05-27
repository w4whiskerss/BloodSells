package dev.bloodworth.bloodsells.sell;

import java.util.List;

public record SellResult(int itemsSold, List<Payout> payouts, List<String> failures) {
    public boolean soldAnything() {
        return itemsSold > 0 && !payouts.isEmpty();
    }
}
