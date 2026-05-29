# BloodSells

Simple Paper `1.21.11` item selling plugin.

## Features

- Vault economy only
- Static item worth values
- Worth lore on items:

```text
Worth : $<value>
```

- Configurable item prices
- Configurable material blacklist, for items like spawners
- `/sellhand`
- `/sellhandall`
- `/sellall <item>`
- `/sellgui`
- `/bloodsells reload`
- `/worthdisplay [on|off|toggle]`
- Shulker support:
  - Filled shulker: sells contents only and returns the empty shulker
  - Empty shulker: sells the shulker itself if it is not blacklisted

## Requirements

- Paper `1.21.11`
- Java `21`
- Vault
- Any Vault-compatible economy plugin

## GUI

The sell GUI is intentionally plain:

- Empty slots for items
- Red wool in the bottom-left to cancel
- Lime wool in the bottom-right to confirm

## Config

```yaml
format:
  worth-line: "<!i><#d3d3d3>Worth : <#90ee90>$<price>"

blacklist:
  - SPAWNER
  - TRIAL_SPAWNER
  - VAULT

items:
  DIAMOND:
    worth: 500
  EMERALD:
    worth: 250
```

## Commands

| Command | Permission |
| --- | --- |
| `/sellhand` | `bloodsells.sellhand` |
| `/sellhandall` | `bloodsells.sellhandall` |
| `/sellall <item>` | `bloodsells.sellall` |
| `/sellgui` | `bloodsells.sellgui` |
| `/bloodsells reload` | `bloodsells.reload` |
| `/worth set <item> <price>` | `bloodsells.worth.set` |
| `/worth info <item>` | `bloodsells.worth.info` |
| `/worthdisplay [on\|off\|toggle]` | `bloodsells.worthdisplay` |

## Build

```bash
mvn package
```

Jar:

```text
target/bloodsells-1.0.0.jar
```
