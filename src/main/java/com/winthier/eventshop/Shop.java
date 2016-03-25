package com.winthier.eventshop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class Shop {
    private final EventShopPlugin plugin;
    private final String name;
    //
    private String title;
    private String currency;
    private boolean bound;
    private String priceFormat;
    private final List<Item> items = new ArrayList<>();
    private Inventory inventory = null;

    public Shop(EventShopPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public void load(ConfigurationSection config) {
        this.title = Msg.format(config.getString(Config.TITLE.key));
        this.currency = config.getString(Config.CURRENCY.key, Config.CURRENCY_DEFAULT.key);
        this.bound = config.getBoolean(Config.BOUND.key, false);
        this.priceFormat = config.getString(Config.PRICE_FORMAT.key);
        for (Map<?,?> map : config.getMapList("items")) {
            ConfigurationSection section = new MemoryConfiguration().createSection("tmp", map);
            section.addDefault(Config.CURRENCY.key, currency);
            section.addDefault(Config.BOUND.key, bound);
            if (priceFormat != null) section.addDefault(Config.PRICE_FORMAT.key, priceFormat);
            try {
                Item item = new Item(plugin, this);
                item.load(section);
                items.add(item);
            } catch (Exception e) {
                System.err.println(section);
                e.printStackTrace();
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public Inventory getInventory() {
        if (inventory == null) {
            int size = ((items.size() - 1) / 9 + 1) * 9;
            inventory = plugin.getServer().createInventory(null, size, this.title);
            for (Item item : items) {
                inventory.addItem(item.getIcon());
            }
        }
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(getInventory());
    }

    public boolean buy(Player player, int slot) {
        if (slot < 0 || slot >= items.size()) return false;
        Item item = items.get(slot);
        if (item == null) return false;
        try {
            return item.buy(player);
        } catch (EventShopException ese) {
            Msg.send(player, "&c%s", ese.getMessage());
        }
        return false;
    }

    public boolean click(Player player, int slot) {
        if (slot < 0 || slot >= items.size()) return false;
        Item item = items.get(slot);
        if (item == null) return false;
        Msg.send(player, "&bShift click to buy this item");
        return false;
    }

    public static enum Config {
        TITLE("Title"),
        CURRENCY("Currency"),
        BOUND("Bound"),
        CURRENCY_DEFAULT("Default Currency"),
        PRICE_FORMAT("PriceFormat"),
        ;
        public final String key;
        Config(String key) { this.key = key; }
    }
}
