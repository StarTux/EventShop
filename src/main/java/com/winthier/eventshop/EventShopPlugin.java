package com.winthier.eventshop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class EventShopPlugin extends JavaPlugin implements Listener {
    // Configurations
    private YamlConfiguration itemConfig, shopConfig;
    // State
    private Map<String, Shop> shops = null;
    private Map<UUID, Shop> openShops = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (UUID uuid : openShops.keySet()) {
            Player player = getServer().getPlayer(uuid);
            if (player != null) player.closeInventory();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        try {
            if (args.length == 0) {
                return false;
            } else if ("ListShops".equalsIgnoreCase(args[0]) && args.length == 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("&eEvent Shops:");
                for (String key : getShops().keySet()) {
                    sb.append(" ").append(key);
                }
                Msg.send(sender, sb.toString());
            } else if ("StoreItem".equalsIgnoreCase(args[0]) && args.length == 2) {
                if (player == null) throw new CommandException("Player expected");
                final String key = args[1];
                final ItemStack item = player.getItemInHand();
                if (item == null || item.getType() == Material.AIR) throw new CommandException("No item in your hand");
                reloadItemConfig();
                getItemConfig().set(key, item.clone());
                saveItemConfig();
                Msg.send(sender, "&eItem stored: %s", key);
            } else if ("LoadItem".equalsIgnoreCase(args[0]) && args.length == 2) {
                if (player == null) throw new CommandException("Player expected");
                String key = args[1];
                reloadItemConfig();
                final ItemStack item = getItemConfig().getItemStack(key);
                if (item == null) throw new CommandException("Item not found: " + key);
                player.getInventory().addItem(item);
                Msg.send(sender, "&eItem given: %s", key);
            } else if ("OpenShop".equalsIgnoreCase(args[0]) && args.length == 3) {
                final String playerArg = args[1];
                final String shopArg = args[2];
                final Player target = getPlayer(playerArg);
                if (target == null) throw new CommandException("Player not found: " + playerArg);
                final Shop shop = getShop(shopArg);
                if (shop == null) throw new CommandException("Shop not found: " + shopArg);
                target.closeInventory();
                shop.open(target);
                openShops.put(target.getUniqueId(), shop);
            } else if ("Save".equalsIgnoreCase(args[0]) && args.length == 1) {
                saveConfig();
                getShopConfig();
                getItemConfig();
                saveShopConfig();
                saveItemConfig();
                Msg.send(sender, "&eConfiguration files saved.");
            } else if ("Reload".equalsIgnoreCase(args[0]) && args.length == 1) {
                reloadConfig();
                this.shops = null;
                this.itemConfig = null;
                this.shopConfig = null;
                Msg.send(sender, "&eConfiguration files reloaded.");
            } else {
                return false;
            }
        } catch (CommandException ce) {
            Msg.send(sender, "&c%s", ce.getMessage());
        }
        return true;
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // Configuration files
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    private YamlConfiguration loadConfig(String filename) {
        YamlConfiguration result = new YamlConfiguration();
        try {
            File file = new File(getDataFolder(), filename);
            if (file.exists()) result.load(file);
            InputStream in = getResource(filename);
            if (in != null) {
                YamlConfiguration def = YamlConfiguration.loadConfiguration(new BufferedReader(new InputStreamReader(in)));
                result.setDefaults(def);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void saveConfig(YamlConfiguration config, String filename) {
        if (config == null) return;
        config.options().copyDefaults(true);
        File file = new File(getDataFolder(), filename);
        try {
            config.save(file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public ConfigurationSection getItemConfig() {
        if (itemConfig == null) itemConfig = loadConfig(Config.ITEMS.key);
        return itemConfig;
    }

    public ConfigurationSection getShopConfig() {
        if (shopConfig == null) shopConfig = loadConfig(Config.SHOPS.key);
        return shopConfig;
    }

    public void saveItemConfig() {
        saveConfig(itemConfig, Config.ITEMS.key);
    }

    public void saveShopConfig() {
        saveConfig(shopConfig, Config.SHOPS.key);
    }

    public void reloadItemConfig() {
        itemConfig = null;
    }

    public ItemStack getItem(String key) {
        ItemStack result = getItemConfig().getItemStack(key);
        if (result == null) {
            getLogger().warning("Item not found: " + key);
            return null;
        }
        return result.clone();
    }

    public Map<String, Shop> getShops() {
        if (shops == null) {
            shops = new HashMap<>();
            for (String key : getShopConfig().getKeys(false)) {
                Shop shop = new Shop(this, key);
                shop.load(getShopConfig().getConfigurationSection(key));
                shops.put(key, shop);
            }
        }
        return shops;
    }

    public Shop getShop(String key) {
        return getShops().get(key);
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // Utility
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Get a player by UUID or name.
     */
    private Player getPlayer(String arg) {
        Player result;
        try {
            UUID uuid = UUID.fromString(arg);
            result = getServer().getPlayer(uuid);
        } catch (IllegalArgumentException iae) {
            result = getServer().getPlayer(arg);
        }
        return result;
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // Event Handlers
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        openShops.remove(uuid);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        final Shop shop = openShops.get(event.getWhoClicked().getUniqueId());
        if (shop == null) return;
        event.setCancelled(true);
        final Player player = (Player)event.getWhoClicked();
        if (event.isLeftClick()) {
            if (event.isShiftClick()) {
                shop.buy(player, event.getRawSlot());
            } else {
                shop.click(player, event.getRawSlot());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        final Shop shop = openShops.get(event.getWhoClicked().getUniqueId());
        if (shop == null) return;
        event.setCancelled(true);
    }
    
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // Configuration Constants
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    public static enum Config {
        ITEMS("items.yml"),
        SHOPS("shops.yml"),
        ;
        public final String key;
        Config(String key) { this.key = key; }
    }
}
