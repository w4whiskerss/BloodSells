package dev.bloodworth.bloodsells.economy;

public record TransactionResult(boolean success, String message) {
    public static TransactionResult ok() {
        return new TransactionResult(true, "");
    }

    public static TransactionResult fail(String message) {
        return new TransactionResult(false, message);
    }
}
