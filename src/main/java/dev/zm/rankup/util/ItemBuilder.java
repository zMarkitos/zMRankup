package dev.zm.rankup.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(String materialStr, org.bukkit.entity.Player player) {
        if (materialStr != null && (materialStr.toLowerCase().startsWith("basehead-") || materialStr.toLowerCase().startsWith("head-"))) {
            this.item = new ItemStack(Material.PLAYER_HEAD);
            this.meta = item.getItemMeta();
            applyCustomHead(materialStr, player);
        } else {
            Material mat = Material.matchMaterial(materialStr != null ? materialStr : "STONE");
            this.item = new ItemStack(mat != null ? mat : Material.STONE);
            this.meta = item.getItemMeta();
        }
    }

    private void applyCustomHead(String customMaterialData, org.bukkit.entity.Player player) {
        if (customMaterialData.toLowerCase().startsWith("basehead-")) {
            String base64 = customMaterialData.substring(9);
            applyBase64Skull(base64);
        } else if (customMaterialData.toLowerCase().startsWith("head-")) {
            String owner = customMaterialData.substring(5);
            if (owner.contains("%player%") && player != null) {
                owner = owner.replace("%player%", player.getName());
            }
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && player != null) {
                owner = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, owner);
            }
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
                skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(owner));
            }
        }
    }

    private void applyBase64Skull(String base64) {
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(base64));
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"").matcher(decoded);
                if (matcher.find()) {
                    String urlString = matcher.group(1);
                    org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(java.util.UUID.randomUUID());
                    org.bukkit.profile.PlayerTextures textures = profile.getTextures();
                    textures.setSkin(new java.net.URL(urlString));
                    profile.setTextures(textures);
                    skullMeta.setOwnerProfile(profile);
                }
            } catch (Exception e) {
                // Ignore invalid base64
            }
        }
    }

    public ItemBuilder name(String name) {
        if (meta != null && name != null) {
            meta.displayName(ColorUtil.parse(name).decoration(TextDecoration.ITALIC, false));
        }
        return this;
    }

    public ItemBuilder lore(List<String> loreLines) {
        if (meta != null && loreLines != null) {
            List<Component> components = new ArrayList<>();
            for (String line : loreLines) {
                components.add(ColorUtil.parse(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(components);
        }
        return this;
    }

    public ItemBuilder lore(String... loreLines) {
        return lore(Arrays.asList(loreLines));
    }

    public ItemBuilder addLore(String line) {
        if (meta != null && line != null) {
            List<Component> currentLore = meta.lore();
            if (currentLore == null) {
                currentLore = new ArrayList<>();
            }
            currentLore.add(ColorUtil.parse(line).decoration(TextDecoration.ITALIC, false));
            meta.lore(currentLore);
        }
        return this;
    }

    public ItemBuilder material(Material material) {
        item.setType(material);
        return this;
    }

    public ItemBuilder amount(int amount) {
        int max = Math.max(1, item.getMaxStackSize());
        item.setAmount(Math.max(1, Math.min(amount, max)));
        return this;
    }

    public ItemBuilder enchant(Enchantment ench, int level) {
        if (meta != null) {
            meta.addEnchant(ench, level, true);
        }
        return this;
    }

    public ItemBuilder flag(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow && meta != null) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder customModelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
