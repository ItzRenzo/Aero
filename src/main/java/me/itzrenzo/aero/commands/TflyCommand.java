package me.itzrenzo.aero.commands;

import me.itzrenzo.aero.Aero;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TflyCommand implements CommandExecutor, TabCompleter {

    private final Aero plugin;

    public TflyCommand(Aero plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.only-players"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("aero.tfly.toggle")) {
            player.sendMessage(plugin.getMessageManager().getMessage("command.no-permission-toggle"));
            return true;
        }

        // Check if world is whitelisted for trial fly
        if (!isWorldAllowed(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("world-restrictions.not-allowed"));
            return true;
        }

        // Handle /tfly time command
        if (args.length == 1 && args[0].equalsIgnoreCase("time")) {
            if (!plugin.hasTrialFly(player)) {
                player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.timer.time.no-trial-fly"));
                return true;
            }

            int remainingTime = plugin.getRemainingFlyTime(player);
            player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.timer.time.remaining", "time", String.valueOf(remainingTime)));
            return true;
        }

        // Handle /tfly actionbar command
        if (args.length == 1 && args[0].equalsIgnoreCase("actionbar")) {
            boolean currentSetting = plugin.isActionbarEnabled(player);
            boolean newSetting = !currentSetting;
            plugin.setPlayerActionbarPreference(player, newSetting);
            
            if (newSetting) {
                player.sendMessage(plugin.getMessageManager().getMessage("actionbar.toggle-enabled"));
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("actionbar.toggle-disabled"));
            }
            return true;
        }

        // Handle /tfly stats command
        if (args.length == 1 && args[0].equalsIgnoreCase("stats")) {
            showPlayerStats(player, player);
            return true;
        }

        // Handle regular /tfly toggle
        if (args.length > 0 && !args[0].equalsIgnoreCase("time") && !args[0].equalsIgnoreCase("stats") && !args[0].equalsIgnoreCase("actionbar")) {
            player.sendMessage(plugin.getMessageManager().getMessage("command.usage.tfly"));
            return true;
        }

        // Check if player has an active trial fly before allowing toggle
        if (!plugin.hasTrialFly(player)) {
            // Also check database for saved trial fly time
            plugin.getDatabaseManager().loadPlayerSessionTime(player.getUniqueId()).thenAccept(savedTime -> {
                if (savedTime > 0) {
                    // Player has saved time, load it and start trial fly
                    plugin.getDatabaseManager().loadPlayerSessionTime(player.getUniqueId()).thenAccept(sessionTime -> {
                        if (sessionTime > 0) {
                            // Start trial fly with saved time
                            plugin.giveTrialFly(player, 0, player); // This will load and start with existing time
                            
                            // Now toggle flight - but check world restrictions first
                            if (player.getAllowFlight()) {
                                player.setAllowFlight(false);
                                player.setFlying(false);
                                player.sendMessage(plugin.getMessageManager().getMessage("fly.disabled"));
                                
                                // Clear the actionbar when flight is disabled
                                player.sendActionBar(net.kyori.adventure.text.Component.empty());
                                
                                // Mark flight as manually disabled to prevent paused actionbar from showing
                                plugin.setFlightManuallyDisabled(player, true);
                            } else {
                                // Check world restrictions before enabling flight
                                if (!isWorldAllowed(player)) {
                                    player.sendMessage(plugin.getMessageManager().getMessage("world-restrictions.not-allowed"));
                                    return;
                                }
                                
                                player.setAllowFlight(true);
                                player.sendMessage(plugin.getMessageManager().getMessage("fly.enabled"));
                                
                                // Clear the manually disabled flag when flight is re-enabled
                                plugin.setFlightManuallyDisabled(player, false);
                            }
                            
                            // Show remaining time
                            int remainingTime = plugin.getRemainingFlyTime(player);
                            if (remainingTime > 0) {
                                player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.timer.time.remaining", "time", String.valueOf(remainingTime)));
                            }
                        } else {
                            player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.timer.time.no-trial-fly"));
                        }
                    });
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.timer.time.no-trial-fly"));
                }
            });
            return true;
        }

        // Toggle flight (only allowed if player has active trial fly)
        if (player.getAllowFlight()) {
            // Disable flight
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(plugin.getMessageManager().getMessage("fly.disabled"));
            
            // Clear the actionbar when flight is disabled
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
            
            // Mark flight as manually disabled to prevent paused actionbar from showing
            plugin.setFlightManuallyDisabled(player, true);
            
            // If player has trial fly, inform them about remaining time
            if (plugin.hasTrialFly(player)) {
                int remainingTime = plugin.getRemainingFlyTime(player);
                player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.timer.time.remaining", "time", String.valueOf(remainingTime)));
            }
        } else {
            // Check world restrictions before enabling flight
            if (!isWorldAllowed(player)) {
                player.sendMessage(plugin.getMessageManager().getMessage("world-restrictions.not-allowed"));
                return true;
            }
            
            // Enable flight
            player.setAllowFlight(true);
            player.sendMessage(plugin.getMessageManager().getMessage("fly.enabled"));
            
            // Clear the manually disabled flag when flight is re-enabled
            plugin.setFlightManuallyDisabled(player, false);
            
            // If player has trial fly, inform them about remaining time
            if (plugin.hasTrialFly(player)) {
                int remainingTime = plugin.getRemainingFlyTime(player);
                player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.timer.time.remaining", "time", String.valueOf(remainingTime)));
            }
        }

        return true;
    }

    private void showPlayerStats(CommandSender sender, Player target) {
        sender.sendMessage(plugin.getMessageManager().getMessage("stats.loading"));
        
        // Get current session time
        int currentSession = plugin.getRemainingFlyTime(target);
        
        // Get total flight time from database
        plugin.getDatabaseManager().getTotalFlyTime(target.getUniqueId()).thenAccept(totalTime -> {
            // Send stats messages
            sender.sendMessage(plugin.getMessageManager().getMessage("stats.header", "player", target.getName()));
            sender.sendMessage(plugin.getMessageManager().getMessage("stats.total-time", "current", String.valueOf(currentSession)));
            
            // Format total time nicely
            long hours = totalTime / 3600;
            long minutes = (totalTime % 3600) / 60;
            long seconds = totalTime % 60;
            
            if (hours > 0) {
                sender.sendMessage(plugin.getMessageManager().getMessage("stats.formatted-total", 
                    "hours", String.valueOf(hours), 
                    "minutes", String.valueOf(minutes), 
                    "seconds", String.valueOf(seconds)));
            } else {
                sender.sendMessage(plugin.getMessageManager().getMessage("stats.session-time", "total", String.valueOf(totalTime)));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("aero.tfly.toggle")) {
            return completions;
        }

        switch (args.length) {
            case 1:
                // First argument: subcommands
                String partial = args[0].toLowerCase();
                if ("time".startsWith(partial)) {
                    completions.add("time");
                }
                if ("stats".startsWith(partial)) {
                    completions.add("stats");
                }
                if ("actionbar".startsWith(partial)) {
                    completions.add("actionbar");
                }
                break;

            default:
                // No additional arguments for tfly subcommands
                break;
        }

        return completions;
    }

    private boolean isWorldAllowed(Player player) {
        // Check if world whitelist is enabled
        if (!plugin.getConfig().getBoolean("world_restrictions.enabled", false)) {
            return true; // If whitelist is disabled, allow all worlds
        }
        
        // Get the list of allowed worlds from config
        List<String> allowedWorlds = plugin.getConfig().getStringList("world_restrictions.whitelisted_worlds");
        String currentWorld = player.getWorld().getName();
        
        // Check if current world is in the whitelist
        return allowedWorlds.contains(currentWorld);
    }
}