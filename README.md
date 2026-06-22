# zMRankup

> **Advanced Progression System for Paper 1.19+**
> Build unlimited, independent progression paths for your players — no coding required.

[![Paper 1.19+](https://img.shields.io/badge/Paper-1.19%2B-orange)](https://papermc.io)
[![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-supported-green)](https://github.com/PlaceholderAPI/PlaceholderAPI)
[![Vault](https://img.shields.io/badge/Vault-supported-blue)](https://github.com/MilkBowl/Vault)

---

## Table of Contents

- [Overview](#overview)
- [File Structure](#file-structure)
- [config.yml](#configyml)
- [Systems (systems/*.yml)](#systems-systemsyml)
  - [Menu Settings](#menu-settings)
  - [Templates](#templates)
  - [Items & Navigation](#items--navigation)
  - [Defining Ranks](#defining-ranks)
  - [Requirements](#requirements)
  - [Rewards](#rewards)
- [Permission Groups (permission.yml)](#permission-groups-permissionyml)
- [Placeholder Lists (placeholder-list.yml)](#placeholder-lists-placeholder-listyml)
- [PlaceholderAPI Placeholders](#placeholderapi-placeholders)
- [Admin Commands](#admin-commands)
- [Permissions](#permissions)

---

## Overview

zMRankup is a complete progression platform. Unlike traditional rankup plugins locked to a single path, zMRankup lets you create **multiple independent progression systems** — Mining ranks, Playtime rewards, PvP ladders, Prestiges, and more — all from simple YAML files.

**Key highlights:**

- Unlimited independent progression systems
- Each system has its own menu command, GUI, and ranks
- Requirements can mix economy, playtime, blocks mined, mob kills, player kills, deaths, and any PlaceholderAPI placeholder
- Rewards support console commands, permissions, titles, and sounds
- Auto-rankup with configurable intervals
- Async architecture for maximum server performance
- MiniMessage formatting (HEX colors, gradients)
- English and Spanish included

---

## File Structure

```
plugins/zMRankup/
├── config.yml              ← Global settings (language, storage, auto-rankup, sounds, etc.)
├── permission.yml          ← Permission groups assignable to ranks
├── placeholder-list.yml    ← Reusable value lists for placeholders
└── systems/
    ├── ranks.yml           ← Example: general rank progression
    ├── playtime.yml        ← Example: playtime-based progression
    └── yourSystem.yml      ← Create as many as you want
```

Each `.yml` file inside `systems/` becomes an independent progression system. You can create, duplicate, or delete them freely.

---

## config.yml

Global configuration for the entire plugin.

```yaml
settings:
  prefix: "<b><gradient:#FFDC12:#EAC800:#FFDC12>zMRankup</gradient></b> <dark_gray>|</dark_gray> "
  language: EN       # EN or ES
  debug: false       # Enable for troubleshooting
  storage: H2        # Storage provider (currently: H2)
```

### Auto Rankup

Automatically promotes players when they meet requirements, without them needing to click.

```yaml
auto-rankup:
  enabled: false
  interval: 2          # Check every N seconds
  max-per-check: 0     # Max promotions per cycle (0 = unlimited)
  require-permission: false  # If true, player needs zmrankup.autorankup permission
```

### Titles

Displays a title to the player when they rank up.

```yaml
titles:
  enabled: true
  rankup-success:
    title: "<b><gradient:#FFDC12:#EAC800:#FFDC12>Progression</gradient></b>"
    subtitle: "<green>You reached {rank}"
    fade-in: 10    # ticks
    stay: 40
    fade-out: 10
```

### Sounds

```yaml
sounds:
  enabled: true
  rankup-success: ENTITY_PLAYER_LEVELUP
  rankup-fail: ENTITY_VILLAGER_NO
  menu-open: BLOCK_CHEST_OPEN
  menu-click: UI_BUTTON_CLICK
  page-turn: ITEM_BOOK_PAGE_TURN
```

### Number Formatting

Controls how large numbers are displayed in menus.

```yaml
number-format:
  short: true   # 1000 → 1K, 1000000 → 1M
  suffixes:
    thousand: "K"
    million: "M"
    billion: "B"
    trillion: "T"
    quadrillion: "Q"
```

### Progress Bar

Customizes the visual progress bar shown in rank menus.

```yaml
progress-bar:
  length: 8
  filled-char: "■"
  empty-char: "□"
  filled-color: "<green>"
  empty-color: "<gray>"
```

### Cooldowns

Prevents players from spamming rankup attempts.

```yaml
cooldowns:
  enabled: false
  time: 5   # seconds
```

### Integrations

```yaml
integrations:
  vault:
    enabled: true       # Required for economy-based requirements
  placeholderapi:
    enabled: true       # Required for placeholder requirements and displays
```

---

## Systems (systems/*.yml)

Each file in the `systems/` folder defines one independent progression system. You can have as many as you want.

### Menu Settings

```yaml
menu_title: '&8&nMy System ({page}/{max_page})&r'
open_command:
  - myranks          # Command that opens this menu
register_command: true  # Registers it as a real Bukkit command
size: 54              # Inventory size, must be a multiple of 9
```

The `open_command` list can contain multiple aliases. Only the first one is registered as a Bukkit command when `register_command: true`.

---

### Templates

Templates define how items appear in the menu depending on the player's progress state. There are three states:

| Template | Condition |
|---|---|
| `locked` | Player hasn't reached the previous rank yet |
| `current` | This is the player's current rank |
| `available` | Player meets all requirements and can claim this rank |

Each template supports these fields:

```yaml
templates:
  color-system: "&#FF1616"   # Accent color used in {color_system} placeholder

  locked:
    material: RED_STAINED_GLASS_PANE
    display_name: "&fRank → {rank_display}"
    glow: false
    lore:
      - "&#FF1616⭐ Rewards:"
      - "{rewards}"
      - ""
      - "&#FF1616✎ Requirements:"
      - "{requirements}"
      - ""
      - "&#FE4224⌚ Progress: &7({progress}%)"
      - "{progress_bar}"
      - ""
      - "&#F86767■ You need the previous rank"

  current:
    material: LIME_STAINED_GLASS_PANE
    glow: true
    # ... same fields as locked

  available:
    material: YELLOW_STAINED_GLASS_PANE
    glow: true
    # ... same fields as locked, but lore should end with "Click to claim"
```

**Available placeholders in templates:**

| Placeholder | Description |
|---|---|
| `{rank_display}` | The rank's `display_rank` value |
| `{rewards}` | Rendered list of reward displays |
| `{requirements}` | Rendered list of requirements with progress |
| `{progress}` | Numeric completion percentage (0–100) |
| `{progress_bar}` | Visual progress bar |
| `{page}` | Current menu page |
| `{max_page}` | Total pages |

---

### Items & Navigation

The `items` section lets you add decorations and navigation buttons to the menu.

#### Decoration

Fills specific slots with a decorative item (commonly glass panes for borders).

```yaml
items:
  DECORATION:
    enabled: true
    material: GRAY_STAINED_GLASS_PANE
    display_name: " "
    slots:
      - 0-8       # Slots 0 through 8 (top row)
      - 44-53     # Slots 44 through 53 (bottom row)
```

#### Navigation Buttons

```yaml
items:
  navigation:
    previous-page:
      enabled: true
      slot: 45
      material: ARROW
      display_name: "&fNavigation &8→ &#FF1616&lBACK"
      lore:
        - " &8| &fGo to the previous page."

    next-page:
      enabled: true
      slot: 53
      material: ARROW
      display_name: "&fNavigation &8→ &#FF1616&lNEXT"

    close:
      enabled: true
      slot: 49
      material: BARRIER
      display_name: "&fNavigation &8→ &#FF1616&lCLOSE"
      actions:
        - "[sound] UI_BUTTON_CLICK"
        - "[message] <gray>Closing menu..."
        - "[player_command] menu"

    info:
      enabled: true
      slot: 4
      material: BOOK
      display_name: "&fServer &8→ &#FF1616&lMY PROFILE"
      lore:
        - " &8| &fCurrent rank: %zmrankup_rank%"
        - " &8| &fNext rank: %zmrankup_next_rank%"
        - " &8| &fProgress: &a%zmrankup_progress%"
        - " &8| &fPage: &e{page}/{max_page}"
```

Button `actions` support:

| Action | Example |
|---|---|
| `[sound] SOUND_NAME` | `[sound] UI_BUTTON_CLICK` |
| `[message] text` | `[message] <gray>Closing...` |
| `[player_command] cmd` | `[player_command] menu` |

---

### Defining Ranks

Each rank is a numbered entry under `ranks:`. The order in the file determines progression order.

```yaml
ranks:
  1:
    material: RED_DYE         # Item shown in the menu for this rank
    display_rank: "&#FF1616&l1"  # Display name used in {rank_display}
    permission-group: 1       # Optional: assigns a permission group on rankup
    page: 1                   # Which menu page this rank appears on
    slot: 10                  # Inventory slot (0-based)
    amount: 1                 # Item stack size (purely visual)
    list-position: 1          # Color index used in requirement lore (maps to color-system list in placeholder-list.yml)
    requirements:
      # ... see Requirements section
    rewards:
      # ... see Rewards section
```

> **Tip:** `material: DEFAULT` uses the template's material. Set a specific material to override it per rank.

> **Note:** Ranks are claimed in order. A player must have rank N before they can claim rank N+1. The `locked` template is shown for any rank that requires a previous unclaimed rank.

---

### Requirements

All requirements listed under a rank must be met before a player can claim it. You can mix any combination.

#### Economy (Vault)

```yaml
requirements:
  money:
    type: vault_balance
    amount: 1500
```

#### Playtime

```yaml
requirements:
  playtime:
    type: playtime_hours
    amount: 5

  # Or in minutes:
  time_minutes:
    type: playtime_minutes
    amount: 120
```

#### Blocks Mined

```yaml
requirements:
  # Any block:
  blocks:
    type: blocks_mined
    amount: 100

  # Specific block:
  diamonds:
    type: blocks_mined
    block: DIAMOND_ORE
    amount: 10
```

#### Mob Kills

```yaml
requirements:
  mobs:
    type: mob_kills
    amount: 50

  # Specific mob:
  zombies:
    type: mob_kills
    mob: ZOMBIE
    amount: 20
```

#### Player Kills & Deaths

```yaml
requirements:
  pvp:
    type: player_kills
    amount: 5

  deaths:
    type: deaths
    amount: 10
```

#### PlaceholderAPI Conditions

Use any PlaceholderAPI placeholder as a requirement with an expression.

```yaml
requirements:
  level:
    type: placeholder
    expression: "%player_level% >= 10"
    display: "&fExperience levels"

  # Check if player is in a specific world:
  nether:
    type: placeholder
    expression: "%player_world% == world_nether"
    display: "&fBe in the Nether"

  # PlayerPoints integration:
  points:
    type: placeholder
    expression: "%playerpoints_points% >= 100"
    display: "&fPlayerPoints"
```

---

### Rewards

Rewards are given to the player when they claim a rank. They have two parts: `displays` (shown in the menu lore) and `commands` (executed on claim).

```yaml
rewards:
  displays:
    - "&#FF1616• &fStarter Kit (stone sword, bread)"
    - "&#FF1616• &f150 welcome coins"
    - "&#FF1616• &fTemporary /fly access (1 hour)"

  commands:
    - "give {player} stone_sword 1"
    - "give {player} bread 16"
    - "eco give {player} 150"
    - "lp user {player} permission settemp aircore.command.fly true 1h"
```

`{player}` is replaced with the claiming player's name.

Commands are executed from the **console**, so use console-compatible syntax (no leading `/`).

#### Using Placeholder Lists in Displays

Instead of hardcoding reward text, you can reference entries from `placeholder-list.yml`:

```yaml
rewards:
  displays:
    - "&#FBCC23• &f%zmrankups_rewards-playtime_1%"
    - "&#FBCC23• &f%zmrankups_rewards-playtime_2%"
  commands:
    - "crate key give {player} comun 1"
```

The `list-position` field on each rank determines which list entries cycle through — useful when you have a repeating pattern of rewards across many ranks.

---

## Permission Groups (permission.yml)

Permission groups let you assign a set of permissions to a player when they reach a rank. Groups support inheritance — a higher group automatically includes all permissions from its parent.

```yaml
groups:
  1:
    permissions:
      - essentials.warp.mina1

  2:
    parent: 1           # Inherits all permissions from group 1
    permissions:
      - essentials.warp.mina2

  3:
    parent: 2
    permissions:
      - essentials.warp.mina3
      - -essentials.warp.mina1   # Removes an inherited permission with "-" prefix
```

To assign a group to a rank, add `permission-group` to the rank definition:

```yaml
ranks:
  1:
    permission-group: 1
    # ...
  2:
    permission-group: 2
    # ...
```

When a player reaches rank 2, they receive all permissions from group 2 (which includes group 1's permissions through inheritance).

**Permission negation** — prefix a permission with `-` to remove an inherited permission from that group. This is useful when higher groups should lose access to something from a lower group.

---

## Placeholder Lists (placeholder-list.yml)

Placeholder lists let you define reusable value sequences that can be inserted into rank displays using a placeholder.

### Format

```
%zmrankups_<list-id>_<position>%
```

- `<list-id>` matches the `id:` field of the list
- `<position>` starts at 1
- If the position doesn't exist, the `default` value is returned

### Configuration

```yaml
default: "&cN/A"   # Global fallback if a position is missing

lists:
  playtime-rewards:
    id: rewards-playtime        # The ID used in the placeholder
    default: "&cN/A"            # Fallback for this specific list
    values:
      - "&fKey &8→ &#00E40A&lCOMMON &fx1"   # Position 1
      - "&fKey &8→ &#2891FC&lVOTE &fx1"     # Position 2
      - "&fKey &8→ &#9528FC&lEPIC &fx1"     # Position 3

  color-system:
    id: color-system
    default: "&#FFFFFF"
    values:
      - "&#FB3838"   # Position 1 → used by rank with list-position: 1
      - "&#FF7817"
      - "&#FBEE45"
      - "&#31FF39"
      - "&#59D7FF"
      - "&#9B4DFF"
      - "&#FFB0E9"
```

### Usage in rank displays

`list-position` controls which color from the `color-system` list is injected into requirement display strings. In the language file, requirement types like `playtime_hours` use the `{position}` placeholder:

```yaml
# lang file (EN/ES)
playtime_hours: "&fPlayed for %zmrankups_color-system_{position}%{amount} hours ⌚"
```

At runtime, `{position}` is replaced with the rank's `list-position` value, which then resolves to a color in the `color-system` list. For example, a rank with `list-position: 3` will use `color-system` position 3 (`&#FBEE45`) to color its requirement text.

```yaml
ranks:
  1:
    list-position: 1    # Requirements shown with color at position 1 (e.g. &#FB3838)
    # ...
  2:
    list-position: 2    # Requirements shown with color at position 2 (e.g. &#FF7817)
    # ...
  8:
    list-position: 1    # If you have more ranks than colors, you can cycle back
    # ...
```

---

## PlaceholderAPI Placeholders

These placeholders can be used anywhere PlaceholderAPI is supported (scoreboards, chat, other plugins).

### Global (uses the player's primary/first system)

| Placeholder | Description |
|---|---|
| `%zmrankup_rank%` | Display name of the player's current rank |
| `%zmrankup_rank_id%` | Internal ID of the current rank |
| `%zmrankup_next_rank%` | Display name of the next rank |
| `%zmrankup_next_rank_id%` | Internal ID of the next rank |
| `%zmrankup_progress%` | Progress toward next rank (percentage) |
| `%zmrankup_progress_bar%` | Visual progress bar |
| `%zmrankup_total_rankups%` | Total number of times this player has ranked up |
| `%zmrankup_total_ranks%` | Total number of ranks in the system |

### System-Specific

Replace `[system]` with the system's filename (without `.yml`):

| Placeholder | Description |
|---|---|
| `%zmrankup_[system]_rank%` | Current rank in a specific system |
| `%zmrankup_[system]_rank_id%` | Current rank ID in a specific system |
| `%zmrankup_[system]_next_rank%` | Next rank in a specific system |
| `%zmrankup_[system]_next_rank_id%` | Next rank ID in a specific system |
| `%zmrankup_[system]_progress%` | Progress in a specific system |
| `%zmrankup_[system]_progress_bar%` | Progress bar for a specific system |

**Example:** For a system file named `playtime.yml`, use `%zmrankup_playtime_rank%`.

---

## Admin Commands

All admin commands require the `zmrankup.admin` permission. They work on both **online and offline players**.

| Command | Description |
|---|---|
| `/zmrankups help` | Show command help |
| `/zmrankups admin reload` | Reload all configuration files |
| `/zmrankups admin version` | Check for updates |
| `/zmrankups admin set <player> <system> <rank>` | Set a player's rank in a system |
| `/zmrankups admin add <player> <system> <rank>` | Give a player the next rank |
| `/zmrankups admin remove <player> <system>` | Remove a player's rank in a system |
| `/zmrankups admin reset <player> <system>` | Reset all progression data for a player in a system |

**Examples:**

```
/zmrankups admin set Steve ranks 5
/zmrankups admin reset Steve playtime
/zmrankups admin add Alex ranks 1
```

---

## Permissions

| Permission | Description |
|---|---|
| `zmrankup.admin` | Access to all administrative commands |
| `zmrankup.autorankup` | Allows a player to benefit from automatic progression (required if `require-permission: true` in config) |
