# BloodSells

**BloodSells** is a production-minded Paper selling and worth plugin for modern SMP economies. It was built for **Paper 1.21.11** and **Java 21**, with a DonutSMP-inspired flow: fast item selling, visible worth, configurable balancing, multi-economy payouts, and a clean admin workflow.

BloodSells is designed for servers where value is not just one number. A diamond can pay Vault money, emeralds can pay PlayerPoints, a nether star can pay a CoinsEngine currency, and rare server items can route into ExcellentEconomy, all in the same sale.

## Highlights

- **Paper 1.21.11 ready**
- **Java 21**
- **Automatic worth generation** for obtainable Minecraft items
- **Manual item overrides** through `config.yml` or admin commands
- **Per-item and per-category economies**
- **Multiple economy providers at the same time**
- **Vault, CoinsEngine, PlayerPoints, ExcellentEconomy**
- **Sell GUI with animated confirmation**
- **Mixed-economy payout preview**
- **Filled shulker handling**
- **Worth tooltip/lore display**
- **Adventure + MiniMessage formatting**
- **SQLite transaction logging**
- **Optional MySQL configuration**
- **PlaceholderAPI expansion**
- **Soft integration posture** for ItemsAdder, Oraxen, MMOItems, and NBT-style custom items

## Why BloodSells Exists

Most sell plugins assume one economy and one value table. That works for simple survival servers, but it falls apart when an SMP has multiple currencies, custom items, seasonal points, crate rewards, progression currencies, or admin-balanced rare items.

BloodSells treats economy routing as part of the item definition.

```yaml
items:
  DIAMOND:
    worth: 500
    economy: VAULT

  EMERALD:
    worth: 250
    economy: VAULT

  NETHER_STAR:
    worth: 1000
    economy: VAULT

  SPAWNER:
    worth: 5000
    economy: VAULT
```

One player sale can pay several providers in one action. The GUI previews those payouts separately so players know exactly what they are receiving.

## Requirements

- Paper `1.21.11`
- Java `21`
- At least one supported economy plugin for real payouts

Supported economy providers:

| Provider | Economy Key | Notes |
| --- | --- | --- |
| Vault | `VAULT` | Works with Vault-backed economies such as EssentialsX Economy |
| PlayerPoints | `PLAYERPOINTS` | Uses the provider default |
| CoinsEngine | `COINSENGINE` | Uses the provider default |
| ExcellentEconomy | `EXCELLENTECONOMY` | Reflection-backed soft adapter |

Optional integrations:

| Plugin | Support |
| --- | --- |
| PlaceholderAPI | Registers BloodSells placeholders |
| ItemsAdder | Custom item metadata is preserved and can influence worth |
| Oraxen | Custom item metadata is preserved and can influence worth |
| MMOItems | Custom item metadata is preserved and can influence worth |
| Vault | Standard economy bridge |

## Installation

1. Build the jar:

   ```bash
   mvn package
   ```

2. Copy the shaded jar into your server:

   ```text
   target/bloodsells-1.0.0.jar
   ```

3. Restart the server.

4. Edit:

   ```text
   plugins/BloodSells/config.yml
   ```

5. Reload with:

   ```text
   /bloodsells reload
   ```

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/sellhand` | `bloodsells.sellhand` | Sells the stack in the player's main hand |
| `/sellgui` | `bloodsells.sellgui` | Opens the animated sell GUI |
| `/sellall <itemname>` | `bloodsells.sellall` | Sells every matching material in the player's inventory |
| `/sellhandall` | `bloodsells.sellhandall` | Sells every inventory item matching the item in hand |
| `/worth set <item> <price>` | `bloodsells.worth.set` | Sets an item worth override |
| `/worth economy <item> <economy>` | `bloodsells.worth.economy` | Sets the economy used by an item |
| `/bloodsells reload` | `bloodsells.reload` | Reloads config and systems |
| `/worth info <item>` | `bloodsells.worth.info` | Shows worth and economy routing for an item |
| `/worth gui` | `bloodsells.admingui` | Opens the in-game worth editor for the item in hand |
| `/worth economies` | `bloodsells.worth.info` | Shows detected economy providers |

Admin umbrella permission:

```text
bloodsells.admin
```

General use permission:

```text
bloodsells.use
```

## Economy Routing

BloodSells resolves the payout economy in this order:

1. Item override in `items`
2. Matching category rule in `categories`
3. Global default economy in `settings.default-economy`

Example:

```yaml
settings:
  default-economy: VAULT

categories:
  nether:
    economy: VAULT
    multiplier: 1.25
    materials:
      - NETHER_STAR
      - BLAZE_ROD
      - GHAST_TEAR

items:
  EMERALD:
    worth: 250
    economy: VAULT
```

This gives you broad category balancing while still allowing exact per-item control.

## Worth Engine

BloodSells calculates item worth using:

- Material rarity hints
- Progression difficulty
- Stack size
- Manual overrides
- Category multipliers
- Global boosters
- Permission multipliers
- Blacklists

Generated values are static per material by default. A diamond is always the diamond value unless you change `DIAMOND` in-game or in config. Enchantment, durability, lore, NBT, and custom metadata pricing are disabled by default so values do not drift.

## Worth Display

BloodSells can show item worth as a lore line:

```text
Worth : <price>
```

The display system is designed to avoid duplicate lines. By default it treats injected worth lore as transient and cleans it up when inventories close or players quit.

Config:

```yaml
settings:
  display-worth: true
  permanent-lore: false
```

Formatting uses MiniMessage:

```yaml
format:
  worth-line: "<!i><white>Worth : <price>"
```

## Sell GUI

The sell GUI supports:

- Insert slots
- Shift-click support
- Drag support
- Animated confirm button
- Live sell preview
- Total payout display
- Economy-specific icons
- Duplication prevention through restricted GUI slots
- Return of unsold items when the GUI closes

GUI config:

```yaml
gui:
  title: "<dark_red>BloodSells"
  size: 54
  input-slots: [10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34]
  preview-slot: 49
  confirm-slot: 53
```

## Shulker Box Rules

BloodSells handles shulkers carefully:

| Shulker State | Behavior |
| --- | --- |
| Filled shulker | Sells only the contents and returns the empty shulker |
| Empty shulker | Sells the shulker box itself if it has worth |

This prevents players from losing valuable containers while still supporting bulk selling.

## Multipliers And Boosters

Permission multiplier example:

```yaml
multipliers:
  permissions:
    bloodsells.multiplier.vip: 1.25
    bloodsells.multiplier.mvp: 1.5
  boosters:
    global: 1.0
```

BloodSells uses the best applicable permission multiplier and combines it with the global booster.

## Transaction Logging

BloodSells logs transactions when enabled:

```yaml
settings:
  transaction-log: true
  storage:
    type: SQLITE
```

SQLite is enabled by default and stores data in:

```text
plugins/BloodSells/transactions.db
```

Optional MySQL config:

```yaml
settings:
  storage:
    type: MYSQL
    mysql:
      jdbc-url: "jdbc:mysql://localhost:3306/bloodsells"
      username: "root"
      password: ""
```

## PlaceholderAPI

When PlaceholderAPI is installed, BloodSells registers:

| Placeholder | Description |
| --- | --- |
| `%bloodsells_hand_worth%` | Raw worth of the item in hand |
| `%bloodsells_hand_worth_formatted%` | Formatted worth of the item in hand |
| `%bloodsells_hand_economy%` | Economy key for the item in hand |

## Configuration Overview

Important sections:

| Section | Purpose |
| --- | --- |
| `settings` | Core behavior, default economy, storage, cache |
| `format` | MiniMessage output and value formatting |
| `economies` | Icons, display names, formatting, provider toggles |
| `categories` | Group-based economy and multiplier rules |
| `items` | Per-item worth and economy overrides |
| `blacklist` | Materials that cannot be sold |
| `multipliers` | Permission and global boosters |
| `gui` | Sell GUI layout and animation |

## Build

```bash
mvn clean package
```

Output:

```text
target/bloodsells-1.0.0.jar
```

## Developer Notes

The economy layer is intentionally modular:

```text
EconomyProvider
EconomyRegistry
EconomyKey
```

To add another provider:

1. Implement `EconomyProvider`
2. Register it in `EconomyRegistry`
3. Add config formatting under `economies`
4. Use an economy key such as `MYPROVIDER` or `MYPROVIDER:currency`

## Production Notes

Before deploying to a live SMP:

- Tune generated prices with manual overrides
- Confirm the exact versions of CoinsEngine, PlayerPoints, and ExcellentEconomy used by your server
- Test each provider with a small sale
- Review blacklisted materials
- Set permission multipliers intentionally
- Keep transaction logging enabled during economy rollout

Reflection-backed economy adapters let BloodSells stay soft-dependent and boot even when optional plugins are missing, but live testing against your exact provider versions is still recommended.

## License

Private server plugin unless you choose to add a public license.
