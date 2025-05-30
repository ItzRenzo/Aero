package me.itzrenzo.aero.gui;

import me.itzrenzo.aero.Aero;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TflyShopGUI {

    private final Aero plugin;
    private final int guiSize;

    public TflyShopGUI(Aero plugin) {
        this.plugin = plugin;
        this.guiSize = plugin.getConfig().getInt("shop.gui.size", 27);
    }

    public void openShop(Player player) {
        // Create inventory with configurable title and size
        Component title = plugin.getMessageManager().getMessage("shop.gui.title");
        Inventory gui = Bukkit.createInventory(null, guiSize, title);

        // Fill GUI with shop items from config
        List<String> shopItems = plugin.getConfig().getStringList("shop.items");
        
        for (String itemKey : shopItems) {
            String basePath = "shop.items-config." + itemKey;
            
            // Skip if item config doesn't exist
            if (!plugin.getConfig().contains(basePath)) {
                continue;
            }
            
            int slot = plugin.getConfig().getInt(basePath + ".slot", 0);
            String materialName = plugin.getConfig().getString(basePath + ".material", "DIAMOND");
            int time = plugin.getConfig().getInt(basePath + ".time", 300);
            double price = plugin.getConfig().getDouble(basePath + ".price", 100.0);
            
            // Create shop item
            ItemStack shopItem = createShopItem(materialName, time, price, itemKey);
            
            // Place item in GUI at specified slot
            if (slot >= 0 && slot < guiSize) {
                gui.setItem(slot, shopItem);
            }
        }

        // Fill empty slots with filler item if enabled
        if (plugin.getConfig().getBoolean("shop.gui.fill-empty", true)) {
            String fillerMaterial = plugin.getConfig().getString("shop.gui.filler-material", "GRAY_STAINED_GLASS_PANE");
            ItemStack filler = createFillerItem(fillerMaterial);
            
            for (int i = 0; i < guiSize; i++) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, filler);
                }
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createShopItem(String materialName, int time, double price, String itemKey) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.DIAMOND;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Format price based on currency type
            String formattedPrice = formatPrice(price);
            
            // Set display name
            meta.displayName(plugin.getMessageManager().getMessage("shop.item.name", 
                "time", String.valueOf(time), 
                "price", formattedPrice));

            // Set lore
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getMessageManager().getMessage("shop.item.lore.time", "time", String.valueOf(time)));
            lore.add(plugin.getMessageManager().getMessage("shop.item.lore.price", "price", formattedPrice));
            lore.add(Component.empty());
            lore.add(plugin.getMessageManager().getMessage("shop.item.lore.click"));
            meta.lore(lore);

            // Add custom data to identify this as a shop item
            meta.getPersistentDataContainer().set(
                plugin.createNamespacedKey("shop_item"), 
                org.bukkit.persistence.PersistentDataType.STRING, 
                itemKey
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    private String formatPrice(double price) {
        String currencyType = plugin.getConfig().getString("shop.currency.type", "vault");
        
        switch (currencyType.toLowerCase()) {
            case "vault":
                // Return raw number - formatting handled by messages.yml
                return String.valueOf(price);
            case "exp":
                return (int) price + " XP";
            case "levels":
                return (int) price + " Levels";
            default:
                return String.valueOf(price);
        }
    }

    private ItemStack createFillerItem(String materialName) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        }

        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }

        return filler;
    }

    public boolean isShopGUI(Inventory inventory) {
        return inventory.getSize() == guiSize && 
               inventory.getViewers().size() > 0;
    }
}