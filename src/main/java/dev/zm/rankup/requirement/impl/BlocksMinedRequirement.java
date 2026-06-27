package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.util.FormatUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class BlocksMinedRequirement extends Requirement {

    private final long amount;
    private final String blockType;

    public BlocksMinedRequirement(ConfigurationSection config) {
        super("blocks_mined", config.getString("display", config.getString("display_name")));
        this.amount = config.getLong("amount", 0);
        this.blockType = config.getString("block", null);
    }

    @Override
    public boolean check(Player player) {
        return getProgress(player) >= amount;
    }

    @Override
    public double getProgress(Player player) {
        if (blockType != null) {
            Material material = Material.matchMaterial(blockType);
            if (material == null) {
                return 0.0;
            }
            try {
                return player.getStatistic(Statistic.MINE_BLOCK, material);
            } catch (IllegalArgumentException ignored) {
                return 0.0;
            }
        }
        dev.zm.rankup.storage.PlayerData data = dev.zm.rankup.zMRankup.getInstance().getPlayerDataCache()
                .get(player.getUniqueId());
        return data != null ? data.getLiveTotalBlocksMined(player) : 0.0;
    }

    @Override
    public double getRequired() {
        return amount;
    }

    @Override
    protected String getDefaultDisplay() {
        String blockName = blockType != null ? translateNamedValue("blocks", blockType) : "";
        return lang("requirements.display.blocks_mined",
                "amount", FormatUtil.formatShort(amount),
                "block_suffix",
                blockType != null ? " (" + blockName + ")" : lang("requirements.display.generic_blocks", " bloques"));
    }
}
