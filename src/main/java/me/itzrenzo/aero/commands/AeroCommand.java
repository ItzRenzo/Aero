package me.itzrenzo.aero.commands;

import me.itzrenzo.aero.Aero;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AeroCommand implements CommandExecutor, TabCompleter {

    private final Aero plugin;

    public AeroCommand(Aero plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.usage.aero"));
            return true;
        }

        if (!args[0].equalsIgnoreCase("tfly")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.unknown-subcommand"));
            return true;
        }

        String action = args[1].toLowerCase();
        
        switch (action) {
            case "give":
                return handleGiveCommand(sender, args);
            case "time":
                return handleTimeCommand(sender, args);
            case "stats":
                return handleStatsCommand(sender, args);
            case "voucher":
                return handleVoucherCommand(sender, args);
            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("error.unknown-action"));
                return true;
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.usage.aero-give"));
            return true;
        }

        if (!sender.hasPermission("aero.tfly.give")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.no-permission-give"));
            return true;
        }

        String playerName = args[2];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player-not-found", "player", playerName));
            return true;
        }

        int timeInSeconds;
        try {
            timeInSeconds = Integer.parseInt(args[3]);
            if (timeInSeconds <= 0) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-time"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-time-format"));
            return true;
        }

        plugin.giveTrialFly(target, timeInSeconds, sender);
        return true;
    }

    private boolean handleTimeCommand(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Check own time - /aero tfly time
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.only-players"));
                return true;
            }

            Player player = (Player) sender;
            if (!plugin.hasTrialFly(player)) {
                player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.time.no-trial-fly"));
                return true;
            }

            int remainingTime = plugin.getRemainingFlyTime(player);
            player.sendMessage(plugin.getMessageManager().getMessage("trial-fly.time.remaining", "time", String.valueOf(remainingTime)));
            return true;

        } else if (args.length == 3) {
            // Check other player's time - /aero tfly time <player>
            if (!sender.hasPermission("aero.tfly.give")) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.no-permission-give"));
                return true;
            }

            String playerName = args[2];
            Player target = Bukkit.getPlayer(playerName);

            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.player-not-found", "player", playerName));
                return true;
            }

            if (!plugin.hasTrialFly(target)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("trial-fly.time.no-trial-fly-other", "player", target.getName()));
                return true;
            }

            int remainingTime = plugin.getRemainingFlyTime(target);
            sender.sendMessage(plugin.getMessageManager().getMessage("trial-fly.time.remaining-other", "player", target.getName(), "time", String.valueOf(remainingTime)));
            return true;

        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.usage.aero-time"));
            return true;
        }
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Check own stats - /aero tfly stats
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.only-players"));
                return true;
            }

            Player player = (Player) sender;
            showPlayerStats(sender, player);
            return true;

        } else if (args.length == 3) {
            // Check other player's stats - /aero tfly stats <player>
            if (!sender.hasPermission("aero.tfly.give")) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.no-permission-give"));
                return true;
            }

            String playerName = args[2];
            Player target = Bukkit.getPlayer(playerName);

            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.player-not-found", "player", playerName));
                return true;
            }

            showPlayerStats(sender, target);
            return true;

        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.usage.aero-stats"));
            return true;
        }
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

    private boolean handleVoucherCommand(CommandSender sender, String[] args) {
        // Usage: /aero tfly voucher <time> [amount] [player]
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.usage.aero-voucher"));
            return true;
        }

        if (!sender.hasPermission("aero.tfly.voucher")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.no-permission-voucher"));
            return true;
        }

        // Parse time parameter
        int timeInSeconds;
        try {
            timeInSeconds = Integer.parseInt(args[2]);
            if (timeInSeconds <= 0) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-time"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-time-format"));
            return true;
        }

        // Parse amount parameter (default: 1)
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount-format"));
                return true;
            }
        }

        // Determine target player
        Player target;
        if (args.length >= 5) {
            // Give to specified player
            String playerName = args[4];
            target = Bukkit.getPlayer(playerName);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.player-not-found", "player", playerName));
                return true;
            }
        } else {
            // Give to command sender (must be a player)
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.only-players"));
                return true;
            }
            target = (Player) sender;
        }

        // Create and give voucher items
        ItemStack voucher = createTflyVoucher(timeInSeconds);
        voucher.setAmount(amount);

        // Check if player has enough inventory space
        if (target.getInventory().firstEmpty() == -1 && amount > 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.inventory-full", "player", target.getName()));
            return true;
        }

        target.getInventory().addItem(voucher);
        
        // Send confirmation messages
        target.sendMessage(plugin.getMessageManager().getMessage("voucher.received", 
            "amount", String.valueOf(amount), 
            "time", String.valueOf(timeInSeconds)));
        
        if (!target.equals(sender)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("voucher.given", 
                "player", target.getName(), 
                "amount", String.valueOf(amount), 
                "time", String.valueOf(timeInSeconds)));
        }

        return true;
    }

    private ItemStack createTflyVoucher(int timeInSeconds) {
        ItemStack voucher = new ItemStack(Material.FEATHER);
        ItemMeta meta = voucher.getItemMeta();
        
        if (meta != null) {
            // Set display name using Adventure API
            meta.displayName(plugin.getMessageManager().getMessage("voucher.name", "time", String.valueOf(timeInSeconds)));
            
            // Set lore using Adventure API
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(plugin.getMessageManager().getMessage("voucher.lore.line1"));
            lore.add(plugin.getMessageManager().getMessage("voucher.lore.line2", "time", String.valueOf(timeInSeconds)));
            lore.add(plugin.getMessageManager().getMessage("voucher.lore.line3"));
            meta.lore(lore);
            
            // Add custom NBT data to identify this as a trial fly voucher
            NamespacedKey key = new NamespacedKey(plugin, "tfly_voucher_time");
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, timeInSeconds);
            
            voucher.setItemMeta(meta);
        }
        
        return voucher;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("aero.tfly.give")) {
            return completions;
        }

        switch (args.length) {
            case 1:
                // First argument: subcommands
                if ("tfly".startsWith(args[0].toLowerCase())) {
                    completions.add("tfly");
                }
                break;

            case 2:
                // Second argument: actions for tfly
                if (args[0].equalsIgnoreCase("tfly")) {
                    if ("give".startsWith(args[1].toLowerCase())) {
                        completions.add("give");
                    }
                    if ("time".startsWith(args[1].toLowerCase())) {
                        completions.add("time");
                    }
                    if ("stats".startsWith(args[1].toLowerCase())) {
                        completions.add("stats");
                    }
                    if ("voucher".startsWith(args[1].toLowerCase())) {
                        completions.add("voucher");
                    }
                }
                break;

            case 3:
                // Third argument: depends on the action
                if (args[0].equalsIgnoreCase("tfly")) {
                    if (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("time") || args[1].equalsIgnoreCase("stats")) {
                        // Player names for give/time/stats commands
                        String partial = args[2].toLowerCase();
                        completions.addAll(
                            Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList())
                        );
                    } else if (args[1].equalsIgnoreCase("voucher")) {
                        // Time suggestions for voucher command
                        List<String> timeSuggestions = Arrays.asList(
                            "30", "60", "120", "300", "600", "900", "1800", "3600"
                        );
                        String partial = args[2].toLowerCase();
                        completions.addAll(
                            timeSuggestions.stream()
                                .filter(time -> time.startsWith(partial))
                                .collect(Collectors.toList())
                        );
                    }
                }
                break;

            case 4:
                // Fourth argument: depends on the action
                if (args[0].equalsIgnoreCase("tfly")) {
                    if (args[1].equalsIgnoreCase("give")) {
                        // Time suggestions for give command
                        List<String> timeSuggestions = Arrays.asList(
                            "30", "60", "120", "300", "600", "900", "1800", "3600"
                        );
                        String partial = args[3].toLowerCase();
                        completions.addAll(
                            timeSuggestions.stream()
                                .filter(time -> time.startsWith(partial))
                                .collect(Collectors.toList())
                        );
                    } else if (args[1].equalsIgnoreCase("voucher")) {
                        // Amount suggestions for voucher command
                        List<String> amountSuggestions = Arrays.asList("1", "2", "3", "4", "5", "10", "16", "32", "64");
                        String partial = args[3].toLowerCase();
                        completions.addAll(
                            amountSuggestions.stream()
                                .filter(amount -> amount.startsWith(partial))
                                .collect(Collectors.toList())
                        );
                    }
                }
                break;

            case 5:
                // Fifth argument: player names for voucher command
                if (args[0].equalsIgnoreCase("tfly") && args[1].equalsIgnoreCase("voucher")) {
                    String partial = args[4].toLowerCase();
                    completions.addAll(
                        Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList())
                    );
                }
                break;
        }

        return completions;
    }
}