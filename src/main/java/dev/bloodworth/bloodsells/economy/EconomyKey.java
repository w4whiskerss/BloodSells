package dev.bloodworth.bloodsells.economy;

import java.util.Locale;

public record EconomyKey(String provider, String currency) {
    public static EconomyKey parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new EconomyKey("VAULT", "");
        }
        String[] parts = raw.trim().split(":", 2);
        String provider = parts[0].trim().toUpperCase(Locale.ROOT);
        String currency = parts.length > 1 ? parts[1].trim() : "";
        return new EconomyKey(provider, currency);
    }

    public String raw() {
        return currency == null || currency.isBlank() ? provider : provider + ":" + currency;
    }
}
