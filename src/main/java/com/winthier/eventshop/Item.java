package com.winthier.eventshop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Item {
    private final EventShopPlugin plugin;
    private final Shop shop;
    //
    private Currency currency;
    private int price;
    private ItemStack icon;
    private String name;
    private String priceFormat;
    private final List<ItemStack> items = new ArrayList<>(1);
    private final List<String> commands = new ArrayList<>(1);

    public Item(EventShopPlugin plugin, Shop shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    public void load(ConfigurationSection config) {
        this.name = config.getString(Config.NAME.key, "Item");
        this.priceFormat = config.getString(Config.PRICE_FORMAT.key);
        this.currency = Currency.fromString(config.getString(Config.CURRENCY.key));
        if (config.getBoolean(Config.BOUND.key)) currency.setBound(true);
        this.price = config.getInt(Config.PRICE.key);
        this.icon = plugin.getItem(config.getString(Config.ICON.key));
        {
            String description = config.getString(Config.DESCRIPTION.key);
            if (description != null) {
                description = Msg.format(description);
                List<String> list = new ArrayList<>();
                String priceLine;
                if (price == 1) {
                    priceLine = plugin.getConfig().getString("currencies." + currency.getName() + ".display_name.Singular", currency.getName()+" %d");
                } else {
                    priceLine = plugin.getConfig().getString("currencies." + currency.getName() + ".display_name.Plural", currency.getName()+" %d");
                }
                priceLine = Msg.format(priceLine, price);
                list.add(priceLine);
                for (String line : description.split("\n")) list.add(line);
                if (priceFormat != null) {
                    list.add(Msg.format(priceFormat, price));
                }
                ItemMeta meta = icon.getItemMeta();
                meta.setLore(list);
                icon.setItemMeta(meta);
            }
        }
        {
            String title = config.getString(Config.TITLE.key);
            if (title != null) {
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(Msg.format(title));
                icon.setItemMeta(meta);
            }
        }
        for (String itemKey : config.getStringList(Config.ITEMS.key)) {
            items.add(plugin.getItem(itemKey));
        }
        for (String command : config.getStringList(Config.COMMANDS.key)) {
            commands.add(command);
        }
    }

    public boolean buy(Player player) throws EventShopException {
        if (!currency.hasAmount(player, price)) throw new EventShopException("You cannot afford this item");
        currency.takeAmount(player, price);
        give(player);
        plugin.getLogger().info(String.format("[%s] %s bought %s for %dx%s", shop.getName(), player.getName(), name, price, currency.getName()));
        return true;
    }

    public void give(Player player) {
        for (ItemStack item : items) {
            Map<Integer, ItemStack> drops = player.getInventory().addItem(item.clone());
            for (ItemStack drop : drops.values()) {
                player.getWorld().dropItem(player.getEyeLocation(), drop);
            }
        }
        for (String command : commands) {
            String cmd = command;
            // Replace variables
            cmd = cmd.replace("%player%", player.getName());
            cmd = cmd.replace("%uuid%", player.getUniqueId().toString());
            // Log and run
            plugin.getLogger().info("Running command: " + cmd);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }
    }

    public ItemStack getIcon() {
        return icon;
    }

    private static enum Config {
        COMMANDS("Commands"),
        CURRENCY("Currency"),
        BOUND("Bound"),
        DESCRIPTION("Description"),
        TITLE("Title"),
        ICON("Icon"),
        ITEMS("Items"),
        NAME("Name"),
        PRICE("Price"),
        PRICE_FORMAT("PriceFormat"),
        ;
        public final String key;
        Config(String key) { this.key = key; }
    }
}
