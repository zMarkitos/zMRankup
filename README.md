# zMRankup

An advanced, highly customizable rank-up plugin for Minecraft servers (Paper 1.19+).
**zMRankup** provides a flexible system for server owners to create multiple, distinct rank progression paths (systems) with custom requirements, rewards, and dynamic GUI menus.

## Features

- **Multiple Rank Systems**: Create independent rank progression paths. Players can progress through a 'miner' system while simultaneously progressing through a 'pvp' system.
- **Dynamic GUI Menus**: Highly configurable menus with custom layouts, items, glass panes, heads (supports custom base64 textures and player heads), and interactive navigation buttons.
- **Extensive Requirements**:
  - Vault money (`vault_balance`)
  - Playtime (`playtime_hours`, `playtime_minutes`)
  - Permissions
  - Mobs killed (`mob_kills`)
  - Players killed (`player_kills`)
  - Deaths
  - Blocks mined (`blocks_mined`)
  - PlaceholderAPI conditions (`placeholder`)
- **Rewards**: Execute commands and display custom text in the menu when a player ranks up.
- **Auto-Rankup**: Optional background task that automatically ranks up players when they meet the requirements.
- **Multi-Language Support**: Built-in support for multiple languages (EN, ES), fully customizable.
- **MiniMessage & Legacy Colors**: Full support for Hex colors, MiniMessage (`<red>`, `<bold>`, `<#FFFFFF>`), and legacy formatting (`&c`, `&#FFFFFF`).
- **PlaceholderAPI Integration**: Exposes internal rank data as placeholders and evaluates external placeholders in menus and requirements.
- **Update Checker**: Notifies admins asynchronously on join when a new version is available on Spigot or Modrinth.
- **Dynamic Commands**: Open rank menus with fully customizable aliases and commands (e.g., `/ranks`, `/rankup`, `/playtime`).

## Commands

- `/ranks` or `/rankup` - Opens the default ranks menu.
- `/ranksadmin reload` - Reloads the configuration, language files, systems, and dynamic commands.
- `/ranksadmin forcerank <player> <rank> [system]` - Forces a player into a specific rank.
- `/ranksadmin reset <player> [system]` - Resets a player's rank and statistics.
- `/ranksadmin info <rank> [system]` - Displays info about a specific rank.
- `/ranksadmin list [system]` - Lists all loaded ranks.

## Requirements

- **Java 17+**
- **Paper 1.19+** (or forks)
- [Vault](https://www.spigotmc.org/resources/vault.34315/) (Optional, needed for economy requirements)
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (Optional, highly recommended)

## Installation

1. Download the latest `zMRankup.jar`.
2. Place the jar into your server's `plugins/` directory.
3. Start the server to generate the default configuration files.
4. Edit the files in `plugins/zMRankup/` to fit your server's needs.
5. Use `/ranksadmin reload` to apply the changes.

## Building from source

To compile the plugin from the source code using Maven:
```bash
mvn clean package
```
The compiled jar will be located in the `target/` directory.
