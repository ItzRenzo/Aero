package me.itzrenzo.aero.listeners;

import me.itzrenzo.aero.Aero;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

public class WorldRestrictionListener implements Listener {

    private final Aero plugin;

    public WorldRestrictionListener(Aero plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        checkWorldRestrictions(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Check if the teleport is going to a different world
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            // Schedule the world check for next tick to ensure the player has moved
            plugin.getServer().getScheduler().runTask(plugin, () -> checkWorldRestrictions(player));
        }
    }

    private void checkWorldRestrictions(Player player) {
        // Only check if player has active trial fly and is currently flying
        if (!plugin.hasTrialFly(player) || !player.getAllowFlight()) {
            return;
        }

        // Check if world restrictions are enabled
        if (!plugin.getConfig().getBoolean("world_restrictions.enabled", false)) {
            return; // World restrictions are disabled
        }

        // Get the list of allowed worlds
        List<String> allowedWorlds = plugin.getConfig().getStringList("world_restrictions.whitelisted_worlds");
        String currentWorld = player.getWorld().getName();

        // If current world is not in the whitelist, disable flight
        if (!allowedWorlds.contains(currentWorld)) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(plugin.getMessageManager().getMessage("world-restrictions.not-allowed"));
            
            // Clear the actionbar
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
            
            // Mark flight as manually disabled to prevent paused actionbar from showing
            plugin.setFlightManuallyDisabled(player, true);
        }
    }
}