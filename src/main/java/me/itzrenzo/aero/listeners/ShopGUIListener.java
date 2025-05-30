package me.itzrenzo.aero.listeners;

import me.itzrenzo.aero.Aero;
import me.itzrenzo.aero.utils.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ShopGUIListener implements Listener {

    private final Aero plugin;

    public ShopGUIListener(Aero plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        
        // Check if this is our shop GUI
        if (!plugin.getShopGUI().isShopGUI(event.getInventory())) {
            return;
        }

        // Cancel the event to prevent item moving
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        // Check if this is a shop item
        if (!meta.getPersistentDataContainer().has(plugin.createNamespacedKey("shop_item"), PersistentDataType.STRING)) {
            return;
        }

        String itemKey = meta.getPersistentDataContainer().get(plugin.createNamespacedKey("shop_item"), PersistentDataType.STRING);
        if (itemKey == null) {
            return;
        }

        // Process purchase
        processPurchase(player, itemKey);
    }

    private void processPurchase(Player player, String itemKey) {
        String basePath = "shop.items-config." + itemKey;
        
        // Get item configuration
        int time = plugin.getConfig().getInt(basePath + ".time", 300);
        double price = plugin.getConfig().getDouble(basePath + ".price", 100.0);
        String currencyType = plugin.getConfig().getString("shop.currency.type", "vault");

        // Check if world is allowed for trial fly
        if (!isWorldAllowed(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("world-restriction.not-allowed"));
            return;
        }

        // Process payment based on currency type
        boolean paymentSuccess = false;
        if (currencyType.equalsIgnoreCase("vault")) {
            paymentSuccess = processVaultPayment(player, price);
        } else if (currencyType.equalsIgnoreCase("exp")) {
            paymentSuccess = processExpPayment(player, (int) price);
        } else if (currencyType.equalsIgnoreCase("levels")) {
            paymentSuccess = processLevelPayment(player, (int) price);
        }

        if (!paymentSuccess) {
            return; // Payment failed, error message already sent
        }

        // Give trial fly time
        plugin.giveTrialFly(player, time, player);
        
        // Send purchase confirmation
        player.sendMessage(plugin.getMessageManager().getMessage("shop.purchase.success", 
            "time", String.valueOf(time), 
            "price", String.valueOf(price)));

        // Close GUI if configured to do so
        if (plugin.getConfig().getBoolean("shop.gui.close-after-purchase", true)) {
            player.closeInventory();
        }
    }

    private boolean processVaultPayment(Player player, double price) {
        VaultManager vaultManager = plugin.getVaultManager();
        
        if (!vaultManager.isVaultEnabled()) {
            player.sendMessage(plugin.getMessageManager().getMessage("shop.currency.vault-not-supported"));
            return false;
        }

        if (!vaultManager.hasEnoughMoney(player, price)) {
            player.sendMessage(plugin.getMessageManager().getMessage("shop.currency.insufficient-money", 
                "required", vaultManager.formatMoney(price), 
                "current", vaultManager.formatMoney(vaultManager.getBalance(player))));
            return false;
        }

        // Withdraw money
        return vaultManager.withdrawMoney(player, price);
    }

    private boolean processExpPayment(Player player, int price) {
        int totalExp = getTotalExperience(player);
        
        if (totalExp < price) {
            player.sendMessage(plugin.getMessageManager().getMessage("shop.currency.insufficient-exp", 
                "required", String.valueOf(price), 
                "current", String.valueOf(totalExp)));
            return false;
        }

        // Deduct experience
        setTotalExperience(player, totalExp - price);
        return true;
    }

    private boolean processLevelPayment(Player player, int price) {
        int currentLevels = player.getLevel();
        
        if (currentLevels < price) {
            player.sendMessage(plugin.getMessageManager().getMessage("shop.currency.insufficient-levels", 
                "required", String.valueOf(price), 
                "current", String.valueOf(currentLevels)));
            return false;
        }

        // Deduct levels
        player.setLevel(currentLevels - price);
        return true;
    }

    private int getTotalExperience(Player player) {
        int level = player.getLevel();
        int totalExp = Math.round(player.getExp() * getExpForLevel(level));
        
        for (int i = 0; i < level; i++) {
            totalExp += getExpForLevel(i);
        }
        
        return totalExp;
    }

    private void setTotalExperience(Player player, int exp) {
        player.setExp(0);
        player.setLevel(0);
        
        int level = 0;
        while (exp > 0) {
            int expForLevel = getExpForLevel(level);
            if (exp >= expForLevel) {
                exp -= expForLevel;
                level++;
            } else {
                break;
            }
        }
        
        player.setLevel(level);
        if (level > 0) {
            player.setExp((float) exp / getExpForLevel(level));
        }
    }

    private int getExpForLevel(int level) {
        if (level <= 16) {
            return 2 * level + 7;
        } else if (level <= 31) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    private boolean isWorldAllowed(Player player) {
        // Check if world whitelist is enabled
        if (!plugin.getConfig().getBoolean("world-whitelist.enabled", false)) {
            return true; // If whitelist is disabled, allow all worlds
        }
        
        // Get the list of allowed worlds from config
        java.util.List<String> allowedWorlds = plugin.getConfig().getStringList("world-whitelist.worlds");
        String currentWorld = player.getWorld().getName();
        
        // Check if current world is in the whitelist
        return allowedWorlds.contains(currentWorld);
    }
}