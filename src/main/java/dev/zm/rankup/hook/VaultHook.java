package dev.zm.rankup.hook;

import dev.zm.rankup.zMRankup;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private Economy economy;

    public boolean setup(zMRankup plugin) {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public double getBalance(Player player) {
        if (economy == null) return 0.0;
        return economy.getBalance(player);
    }

    public boolean has(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        EconomyResponse r = economy.withdrawPlayer(player, amount);
        return r.transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        EconomyResponse r = economy.depositPlayer(player, amount);
        return r.transactionSuccess();
    }

    public boolean isEnabled() {
        return economy != null;
    }
}
