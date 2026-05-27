package dev.bloodworth.bloodsells.economy;

import java.util.Locale;

public record EconomyKey(String provider, String currency) {
    public static EconomyKey parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new EconomyKey("VAULT", "");
        }
        String[] parts = raw.trim().split(":", 2);
        String provider = parts[0].trim().toUpperCase(Locale.ROOT);
        return new EconomyKey(provider, "");
    }

    public String raw() {
        return provider;
    }
}
