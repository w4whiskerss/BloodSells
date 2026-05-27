package dev.bloodworth.bloodsells.sell;

import dev.bloodworth.bloodsells.BloodSellsPlugin;
import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.EconomyKey;
import dev.bloodworth.bloodsells.economy.EconomyProvider;
import dev.bloodworth.bloodsells.economy.EconomyRegistry;
import dev.bloodworth.bloodsells.storage.TransactionLogger;
import dev.bloodworth.bloodsells.worth.WorthEngine;
import dev.bloodworth.bloodsells.worth.WorthResult;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public final class SellService {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private final BloodSellsPlugin plugin;
    private final BloodConfig config;
    private final WorthEngine worthEngine;
    private final EconomyRegistry economies;
    private final TransactionLogger logger;

    public SellService(BloodSellsPlugin plugin, BloodConfig config, WorthEngine worthEngine, EconomyRegistry economies, TransactionLogger logger) {
        this.plugin = plugin;
        this.config = config;
        this.worthEngine = worthEngine;
        this.economies = economies;
        this.logger = logger;
    }

    public SellResult sellHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return new SellResult(0, List.of(), List.of("empty"));
        }
        stripWorthLore(hand);
        ItemStack copy = hand.clone();
        SaleQuote quote = quoteStack(copy, player);
        if (quote.itemsSold() <= 0) {
            return new SellResult(0, List.of(), List.of("no-worth"));
        }
        SellResult result = pay(player, quote, copy.getType().name());
        if (result.soldAnything()) {
            player.getInventory().setItemInMainHand(null);
        }
        return result;
    }

    public SellResult sellAllNamed(Player player, Material material) {
        return sellMatching(player, stack -> stack != null && stack.getType() == material);
    }

    public SellResult sellHandAll(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return new SellResult(0, List.of(), List.of("empty"));
        }
        stripWorthLore(hand);
        ItemStack template = hand.clone();
        template.setAmount(1);
        return sellMatching(player, stack -> {
            if (stack == null) {
                return false;
            }
            stripWorthLore(stack);
            return stack.isSimilar(template);
        });
    }

    public SellResult sellInventoryContents(Player player, Inventory inventory, List<Integer> inputSlots) {
        SaleQuote total = new SaleQuote();
        List<Integer> soldSlots = new ArrayList<>();
        Map<Integer, ItemStack> replacements = new HashMap<>();
        for (int slot : inputSlots) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            stripWorthLore(stack);
            SaleQuote quote = quoteStack(stack, player);
            if (quote.itemsSold() > 0) {
                total.merge(quote);
                soldSlots.add(slot);
                if (isFilledShulker(stack)) {
                    ItemStack empty = stack.clone();
                    empty.setAmount(1);
                    clearShulker(empty);
                    replacements.put(slot, empty);
                }
            }
        }
        SellResult result = pay(player, total, "GUI");
        if (result.soldAnything()) {
            for (int slot : soldSlots) {
                inventory.setItem(slot, replacements.get(slot));
            }
            player.updateInventory();
        }
        return result;
    }

    public SaleQuote quoteInventory(Inventory inventory, List<Integer> inputSlots, Player player) {
        SaleQuote total = new SaleQuote();
        for (int slot : inputSlots) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.getType().isAir()) {
                stripWorthLore(stack);
                total.merge(quoteStack(stack, player));
            }
        }
        return total;
    }

    public SaleQuote quoteStack(ItemStack stack, Player player) {
        SaleQuote quote = new SaleQuote();
        if (isFilledShulker(stack)) {
            BlockStateMeta meta = (BlockStateMeta) stack.getItemMeta();
            ShulkerBox box = (ShulkerBox) meta.getBlockState();
            for (ItemStack content : box.getInventory().getContents()) {
                addItemQuote(quote, content, player);
            }
            return quote;
        }
        addItemQuote(quote, stack, player);
        return quote;
    }

    public String formatPayouts(List<Payout> payouts) {
        List<String> parts = new ArrayList<>();
        for (Payout payout : payouts) {
            parts.add(economies.format(payout.economy(), payout.amount()));
        }
        return String.join(", ", parts);
    }

    private SellResult sellMatching(Player player, java.util.function.Predicate<ItemStack> predicate) {
        PlayerInventory inv = player.getInventory();
        SaleQuote total = new SaleQuote();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir() || !predicate.test(stack)) {
                continue;
            }
            SaleQuote quote = quoteStack(stack, player);
            if (quote.itemsSold() > 0) {
                total.merge(quote);
                slots.add(slot);
            }
        }
        SellResult result = pay(player, total, slots.isEmpty() ? "NONE" : "MATCH");
        if (result.soldAnything()) {
            for (int slot : slots) {
                inv.setItem(slot, null);
            }
        }
        return result;
    }

    private void addItemQuote(SaleQuote quote, ItemStack stack, Player player) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        Optional<WorthResult> worth = worthEngine.worth(stack);
        if (worth.isEmpty()) {
            return;
        }
        Optional<EconomyKey> economy = economies.resolve(worth.get().economy());
        if (economy.isEmpty()) {
            return;
        }
        double multiplier = config.globalBooster();
        for (Map.Entry<String, Double> entry : config.permissionMultipliers().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                multiplier = Math.max(multiplier, entry.getValue());
            }
        }
        double total = worth.get().total(stack.getAmount()) * multiplier;
        quote.add(economy.get(), total, stack.getAmount());
    }

    private SellResult pay(Player player, SaleQuote quote, String itemName) {
        if (quote.itemsSold() <= 0 || quote.payouts().isEmpty()) {
            return new SellResult(0, List.of(), List.of("nothing"));
        }
        List<Payout> successful = Collections.synchronizedList(new ArrayList<>());
        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Payout payout : quote.payouts()) {
            Optional<EconomyProvider> provider = economies.provider(payout.economy());
            if (provider.isEmpty()) {
                return new SellResult(0, List.of(), List.of("Missing provider: " + payout.economy().raw()));
            }
            futures.add(provider.get().deposit(player, payout.amount(), payout.economy().currency()).thenAccept(result -> {
                if (result.success()) {
                    successful.add(payout);
                    logger.log(player.getUniqueId(), itemName, quote.itemsSold(), payout.amount(), payout.economy());
                } else {
                    failures.add(result.message());
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        if (!successful.isEmpty()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.25F);
        }
        return new SellResult(quote.itemsSold(), successful, failures);
    }

    private boolean isFilledShulker(ItemStack stack) {
        if (stack == null || !(stack.getItemMeta() instanceof BlockStateMeta meta) || !(meta.getBlockState() instanceof ShulkerBox box)) {
            return false;
        }
        for (ItemStack content : box.getInventory().getContents()) {
            if (content != null && !content.getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    private void clearShulker(ItemStack stack) {
        if (!(stack.getItemMeta() instanceof BlockStateMeta meta) || !(meta.getBlockState() instanceof ShulkerBox box)) {
            return;
        }
        box.getInventory().clear();
        meta.setBlockState(box);
        stack.setItemMeta(meta);
    }

    private void stripWorthLore(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return;
        }
        List<Component> kept = new ArrayList<>();
        boolean changed = false;
        for (Component line : meta.lore()) {
            String plain = PLAIN.serialize(line).toLowerCase(java.util.Locale.ROOT).replace(" ", "");
            if (plain.startsWith("worth:")) {
                changed = true;
                continue;
            }
            kept.add(line);
        }
        if (changed) {
            meta.lore(kept.isEmpty() ? null : kept);
            item.setItemMeta(meta);
        }
    }

    public static final class SaleQuote {
        private final Map<EconomyKey, Double> payouts = new HashMap<>();
        private int itemsSold;

        public void add(EconomyKey economy, double amount, int items) {
            payouts.merge(economy, amount, Double::sum);
            itemsSold += items;
        }

        public void merge(SaleQuote other) {
            other.payouts.forEach((key, value) -> payouts.merge(key, value, Double::sum));
            itemsSold += other.itemsSold;
        }

        public int itemsSold() {
            return itemsSold;
        }

        public List<Payout> payouts() {
            return payouts.entrySet().stream().map(e -> new Payout(e.getKey(), e.getValue())).toList();
        }
    }
}
