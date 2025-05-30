package me.itzrenzo.aero.utils;

import me.itzrenzo.aero.Aero;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class MessageManager {
    
    private final Aero plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    
    public MessageManager(Aero plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        // Create messages.yml if it doesn't exist
        if (!messagesFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream inputStream = plugin.getResource("messages.yml")) {
                if (inputStream != null) {
                    Files.copy(inputStream, messagesFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create messages.yml file!");
                e.printStackTrace();
            }
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public Component getMessage(String path) {
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    public Component getMessage(String path, String placeholder, String value) {
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        message = message.replace("{" + placeholder + "}", value);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    public Component getMessage(String path, String placeholder1, String value1, String placeholder2, String value2) {
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        message = message.replace("{" + placeholder1 + "}", value1);
        message = message.replace("{" + placeholder2 + "}", value2);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    public Component getMessage(String path, String placeholder1, String value1, String placeholder2, String value2, String placeholder3, String value3) {
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        message = message.replace("{" + placeholder1 + "}", value1);
        message = message.replace("{" + placeholder2 + "}", value2);
        message = message.replace("{" + placeholder3 + "}", value3);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    public Component getMessage(String key, String... replacements) {
        String message = messagesConfig.getString(key, "&cMessage not found: " + key);
        
        // Apply replacements in pairs (key, value)
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = "{" + replacements[i] + "}";
            String replacement = replacements[i + 1];
            message = message.replace(placeholder, replacement);
        }
        
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    public void reloadMessages() {
        // Reload the messages.yml file
        plugin.reloadConfig();
        
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.getLogger().info("Messages configuration reloaded successfully!");
    }
}