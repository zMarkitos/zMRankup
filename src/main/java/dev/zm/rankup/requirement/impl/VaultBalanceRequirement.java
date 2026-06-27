package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.util.FormatUtil;
import dev.zm.rankup.zMRankup;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class VaultBalanceRequirement extends Requirement {

    private final double amount;
    private final boolean withdraw;

    public VaultBalanceRequirement(ConfigurationSection config) {
        super("vault_balance", config.getString("display", config.getString("display_name")));
        this.amount = config.getDouble("amount", 0.0);
        this.withdraw = config.getBoolean("withdraw", true);
    }

    public boolean isWithdraw() {
        return withdraw;
    }

    @Override
    public boolean check(Player player) {
        if (!zMRankup.getInstance().getHookManager().isVaultEnabled()) return false;
        return zMRankup.getInstance().getHookManager().getVault().has(player, amount);
    }

    @Override
    public double getProgress(Player player) {
        if (!zMRankup.getInstance().getHookManager().isVaultEnabled()) return 0.0;
        return zMRankup.getInstance().getHookManager().getVault().getBalance(player);
    }

    @Override
    public double getRequired() {
        return amount;
    }

    @Override
    protected String getDefaultDisplay() {
        return lang("requirements.display.vault_balance", "amount", FormatUtil.formatShort(amount));
    }
}
