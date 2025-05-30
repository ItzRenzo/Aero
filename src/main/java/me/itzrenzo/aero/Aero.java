package me.itzrenzo.aero;

import me.itzrenzo.aero.commands.AeroCommand;
import me.itzrenzo.aero.commands.TflyCommand;
import me.itzrenzo.aero.database.DatabaseManager;
import me.itzrenzo.aero.gui.TflyShopGUI;
import me.itzrenzo.aero.listeners.ShopGUIListener;
import me.itzrenzo.aero.listeners.VoucherListener;
import me.itzrenzo.aero.listeners.WorldRestrictionListener;
import me.itzrenzo.aero.utils.MessageManager;
import me.itzrenzo.aero.utils.VaultManager;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public final class Aero extends JavaPlugin {

    private Map<UUID, BukkitRunnable> flyTasks = new HashMap<>();
    private Map<UUID, Integer> flyTimeRemaining = new HashMap<>();
    private Map<UUID, Boolean> playerActionbarPreference = new HashMap<>();
    private Map<UUID, Boolean> playerFlightDisabled = new HashMap<>();
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private VaultManager vaultManager;
    private TflyShopGUI shopGUI;
    private BukkitRunnable autoSaveTask;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        getLogger().info("Aero plugin has been enabled!");
        getLogger().info("Trial fly system loaded successfully.");
        
        // Initialize managers
        messageManager = new MessageManager(this);
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        vaultManager = new VaultManager(this);
        
        // Initialize shop GUI
        shopGUI = new TflyShopGUI(this);
        
        // Start auto-save task if enabled
        startAutoSaveTask();
        
        // Register command executors and tab completers
        AeroCommand aeroCommand = new AeroCommand(this);
        getCommand("aero").setExecutor(aeroCommand);
        getCommand("aero").setTabCompleter(aeroCommand);
        
        TflyCommand tflyCommand = new TflyCommand(this);
        getCommand("tfly").setExecutor(tflyCommand);
        getCommand("tfly").setTabCompleter(tflyCommand);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new VoucherListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldRestrictionListener(this), this);
    }

    @Override
    public void onDisable() {
        // Save all current session data to database before shutdown
        for (UUID playerId : flyTimeRemaining.keySet()) {
            Player player = getServer().getPlayer(playerId);
            if (player != null) {
                int sessionTime = flyTimeRemaining.get(playerId);
                databaseManager.savePlayerData(player, sessionTime);
                // Add session time to total fly time
                databaseManager.addToTotalFlyTime(playerId, sessionTime);
            }
        }
        
        // Cancel all ongoing fly tasks
        for (BukkitRunnable task : flyTasks.values()) {
            task.cancel();
        }
        flyTasks.clear();
        flyTimeRemaining.clear();
        
        // Cancel auto-save task
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("Aero plugin has been disabled!");
    }

    private void startAutoSaveTask() {
        int interval = getConfig().getInt("settings.auto_save_interval", 300);
        if (interval > 0) {
            autoSaveTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // Save all current session data
                    for (UUID playerId : flyTimeRemaining.keySet()) {
                        Player player = getServer().getPlayer(playerId);
                        if (player != null) {
                            int sessionTime = flyTimeRemaining.get(playerId);
                            databaseManager.savePlayerData(player, sessionTime);
                        }
                    }
                    
                    if (getConfig().getBoolean("settings.debug", false)) {
                        getLogger().info("Auto-saved flight data for " + flyTimeRemaining.size() + " players");
                    }
                }
            };
            autoSaveTask.runTaskTimerAsynchronously(this, interval * 20L, interval * 20L);
            getLogger().info("Auto-save task started (interval: " + interval + " seconds)");
        }
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void giveTrialFly(Player player, int timeInSeconds, CommandSender sender) {
        UUID playerId = player.getUniqueId();

        // Load existing session time from database if player doesn't have active trial fly
        if (!flyTasks.containsKey(playerId)) {
            databaseManager.loadPlayerSessionTime(playerId).thenAccept(savedTime -> {
                if (savedTime > 0) {
                    // Player has saved session time, add to it
                    int newTotalTime = savedTime + timeInSeconds;
                    flyTimeRemaining.put(playerId, newTotalTime);
                    
                    // Enable flight
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    
                    // Notify about combined time
                    player.sendMessage(messageManager.getMessage("trial-fly.given", "time", String.valueOf(newTotalTime)));
                    sender.sendMessage(messageManager.getMessage("trial-fly.given-sender", "player", player.getName(), "time", String.valueOf(newTotalTime)));
                    
                    startTrialFlyTask(player);
                } else {
                    // No saved time, start fresh
                    startNewTrialFly(player, timeInSeconds, sender);
                }
            });
            return;
        }

        // Stack time if player already has active trial fly
        int currentTime = flyTimeRemaining.get(playerId);
        int newTotalTime = currentTime + timeInSeconds;
        flyTimeRemaining.put(playerId, newTotalTime);
        
        // Notify about stacked time
        player.sendMessage(messageManager.getMessage("trial-fly.extended", "time", String.valueOf(newTotalTime)));
        sender.sendMessage(messageManager.getMessage("trial-fly.extended-sender", "player", player.getName(), "time", String.valueOf(newTotalTime)));
    }

    private void startNewTrialFly(Player player, int timeInSeconds, CommandSender sender) {
        UUID playerId = player.getUniqueId();
        
        // Enable flight for new trial fly
        player.setAllowFlight(true);
        player.setFlying(true);
        flyTimeRemaining.put(playerId, timeInSeconds);

        // Notify player and sender
        player.sendMessage(messageManager.getMessage("trial-fly.given", "time", String.valueOf(timeInSeconds)));
        sender.sendMessage(messageManager.getMessage("trial-fly.given-sender", "player", player.getName(), "time", String.valueOf(timeInSeconds)));

        startTrialFlyTask(player);
    }

    private void startTrialFlyTask(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Create countdown task that only runs when player is flying
        BukkitRunnable flyTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    flyTasks.remove(playerId);
                    // Save remaining time to database
                    if (flyTimeRemaining.containsKey(playerId)) {
                        int remainingTime = flyTimeRemaining.get(playerId);
                        databaseManager.savePlayerData(player, remainingTime);
                        flyTimeRemaining.remove(playerId);
                    }
                    return;
                }

                // Only countdown when player is actually flying
                if (player.isFlying()) {
                    int timeLeft = flyTimeRemaining.get(playerId);
                    timeLeft--;
                    flyTimeRemaining.put(playerId, timeLeft);

                    // Show actionbar countdown if enabled for this player
                    if (isActionbarEnabled(player)) {
                        player.sendActionBar(messageManager.getMessage("actionbar.trial-fly-countdown", "time", String.valueOf(timeLeft)));
                    }

                    // Send warnings at specific intervals
                    if (timeLeft == 60) {
                        player.sendMessage(messageManager.getMessage("trial-fly.warnings.sixty", "time", String.valueOf(timeLeft)));
                    } else if (timeLeft == 30) {
                        player.sendMessage(messageManager.getMessage("trial-fly.warnings.thirty", "time", String.valueOf(timeLeft)));
                    } else if (timeLeft == 10) {
                        player.sendMessage(messageManager.getMessage("trial-fly.warnings.ten", "time", String.valueOf(timeLeft)));
                    } else if (timeLeft == 5) {
                        player.sendMessage(messageManager.getMessage("trial-fly.warnings.five", "time", String.valueOf(timeLeft)));
                    } else if (timeLeft <= 3 && timeLeft > 0) {
                        String plural = timeLeft == 1 ? "" : "s";
                        player.sendMessage(messageManager.getMessage("trial-fly.warnings.countdown", "time", String.valueOf(timeLeft), "s", plural));
                    }

                    // Remove flight when time expires
                    if (timeLeft <= 0) {
                        removeTrialFly(player);
                        cancel();
                    }
                } else {
                    // Player is on ground - only show paused actionbar if flight wasn't manually disabled
                    int timeLeft = flyTimeRemaining.get(playerId);
                    if (isActionbarEnabled(player) && !isFlightManuallyDisabled(player)) {
                        player.sendActionBar(messageManager.getMessage("actionbar.trial-fly-paused", "time", String.valueOf(timeLeft)));
                    }
                }
            }
        };

        // Run task every second (20 ticks)
        flyTask.runTaskTimer(this, 20L, 20L);
        flyTasks.put(playerId, flyTask);
    }

    private void removeTrialFly(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Get the time that was used and add to total fly time
        int usedTime = flyTimeRemaining.getOrDefault(playerId, 0);
        if (usedTime > 0) {
            databaseManager.addToTotalFlyTime(playerId, usedTime);
        }
        
        // Remove flight abilities
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Clear the actionbar
        player.sendActionBar(Component.empty());
        
        // Clean up tracking
        flyTasks.remove(playerId);
        flyTimeRemaining.remove(playerId);
        playerFlightDisabled.remove(playerId); // Clear manually disabled state when trial fly expires
        
        // Clear session time in database
        databaseManager.savePlayerData(player, 0);
        
        // Notify player
        player.sendMessage(messageManager.getMessage("trial-fly.expired"));
        
        // Safe landing - teleport player to ground if they're in the air
        if (player.getLocation().getY() > player.getWorld().getHighestBlockYAt(player.getLocation())) {
            player.teleport(player.getWorld().getHighestBlockAt(player.getLocation()).getLocation().add(0, 1, 0));
            player.sendMessage(messageManager.getMessage("trial-fly.safe-landing"));
        }
    }

    // Utility method to get remaining fly time for a player
    public int getRemainingFlyTime(Player player) {
        return flyTimeRemaining.getOrDefault(player.getUniqueId(), 0);
    }

    // Utility method to check if player has active trial fly
    public boolean hasTrialFly(Player player) {
        return flyTasks.containsKey(player.getUniqueId());
    }

    // Utility methods for per-player actionbar preferences
    public boolean isActionbarEnabled(Player player) {
        UUID playerId = player.getUniqueId();
        // Default to global config if player hasn't set a preference
        return playerActionbarPreference.getOrDefault(playerId, getConfig().getBoolean("settings.actionbar_countdown", true));
    }

    public void setPlayerActionbarPreference(Player player, boolean enabled) {
        playerActionbarPreference.put(player.getUniqueId(), enabled);
    }

    // Utility methods for tracking manually disabled flight
    public void setFlightManuallyDisabled(Player player, boolean disabled) {
        UUID playerId = player.getUniqueId();
        if (disabled) {
            playerFlightDisabled.put(playerId, true);
        } else {
            playerFlightDisabled.remove(playerId);
        }
    }

    public boolean isFlightManuallyDisabled(Player player) {
        return playerFlightDisabled.getOrDefault(player.getUniqueId(), false);
    }

    // Shop GUI getter
    public TflyShopGUI getShopGUI() {
        return shopGUI;
    }

    // VaultManager getter
    public VaultManager getVaultManager() {
        return vaultManager;
    }

    // Helper method for creating NamespacedKeys
    public NamespacedKey createNamespacedKey(String key) {
        return new NamespacedKey(this, key);
    }
}
