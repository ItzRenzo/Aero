package me.itzrenzo.aero.listeners;

import me.itzrenzo.aero.Aero;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class VoucherListener implements Listener {

    private final Aero plugin;

    public VoucherListener(Aero plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseVoucher(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player is right-clicking with an item
        if (item == null || item.getType() != Material.FEATHER) {
            return;
        }

        // Check if the item is a trial fly voucher
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "tfly_voucher_time");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            return;
        }

        // Cancel the interaction to prevent other actions
        event.setCancelled(true);

        // Get the time value from the voucher
        int timeInSeconds = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        if (timeInSeconds <= 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("voucher.invalid"));
            return;
        }

        // Check if world is whitelisted for trial fly
        if (!isWorldAllowed(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("world-restriction.not-allowed"));
            return;
        }

        // Give trial fly time to the player
        plugin.giveTrialFly(player, timeInSeconds, player);

        // Remove one voucher from the stack
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Send success message
        player.sendMessage(plugin.getMessageManager().getMessage("voucher.used", "time", String.valueOf(timeInSeconds)));
    }

    private boolean isWorldAllowed(Player player) {
        // Check if world whitelist is enabled
        if (!plugin.getConfig().getBoolean("world_restrictions.enabled", false)) {
            return true; // If whitelist is disabled, allow all worlds
        }
        
        // Get the list of allowed worlds from config
        java.util.List<String> allowedWorlds = plugin.getConfig().getStringList("world_restrictions.whitelisted_worlds");
        String currentWorld = player.getWorld().getName();
        
        // Check if current world is in the whitelist
        return allowedWorlds.contains(currentWorld);
    }
}