package me.itzrenzo.aero.utils;

import me.itzrenzo.aero.Aero;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultManager {
    
    private final Aero plugin;
    private Economy economy = null;
    private boolean vaultEnabled = false;

    public VaultManager(Aero plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found! Economy features disabled.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy plugin found! Vault economy features disabled.");
            return false;
        }

        economy = rsp.getProvider();
        vaultEnabled = economy != null;
        
        if (vaultEnabled) {
            plugin.getLogger().info("Vault economy integration enabled with " + economy.getName());
        } else {
            plugin.getLogger().warning("Failed to initialize Vault economy!");
        }
        
        return vaultEnabled;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public boolean hasEnoughMoney(Player player, double amount) {
        if (!vaultEnabled) return false;
        return economy.has(player, amount);
    }

    public boolean withdrawMoney(Player player, double amount) {
        if (!vaultEnabled) return false;
        
        if (!hasEnoughMoney(player, amount)) {
            return false;
        }
        
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public double getBalance(Player player) {
        if (!vaultEnabled) return 0.0;
        return economy.getBalance(player);
    }

    public String formatMoney(double amount) {
        // Return raw number - formatting will be handled by messages.yml
        return String.valueOf(amount);
    }

    public String getCurrencyName() {
        if (!vaultEnabled) return "Money";
        return economy.currencyNamePlural();
    }

    public String getCurrencyNameSingular() {
        if (!vaultEnabled) return "Money";
        return economy.currencyNameSingular();
    }
}