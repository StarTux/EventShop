package com.winthier.eventshop;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.simple.JSONValue;

public class Currency {
    private final String id;
    private boolean bound = false;
    private final static String MAGIC = "" + ChatColor.RESET + ChatColor.BLACK + ChatColor.MAGIC;

    public Currency(String id) {
        this.id = id;
    }

    public static Currency fromString(String id) {
        return new Currency(id);
    }

    public String getName() {
        return id;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    private Map<String, Object> getHiddenTag(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return null;
        String line = meta.getLore().get(0);
        if (line == null) return null;
        if (!line.contains(MAGIC)) return null;
        int index = line.indexOf(MAGIC) + MAGIC.length();
        return getHiddenTag(line.substring(index));
    }

    private Map<String, Object> getHiddenTag(String string) {
        if (string.length() % 2 != 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i += 2) {
            if (string.charAt(i) != ChatColor.COLOR_CHAR) return null;
            sb.append(string.charAt(i + 1));
        }
        try {
            @SuppressWarnings("unchecked")
            val result = (Map<String, Object>)JSONValue.parse(sb.toString());
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    boolean isCurrency(ItemStack item, Player player) {
        if (item == null) return false;
        if (item.getType() == Material.AIR) return false;
        Map<String, Object> section = getHiddenTag(item);
        if (section == null) return false;
        if (!id.equals(section.get("id"))) return false;
        if (bound) {
            Object tmp = section.get("owner");
            if (tmp == null) return false;
            if (!(tmp instanceof String)) return false;
            UUID uuid;
            try {
                uuid = UUID.fromString((String)tmp);
            } catch (IllegalArgumentException iae) {
                return false;
            }
            if (!player.getUniqueId().equals(uuid)) return false;
        }
        return true;
    }

    public boolean hasAmount(Player player, int price) {
        int need = price;
        if (need <= 0) return true;
        for (ItemStack item : player.getInventory()) {
            if (isCurrency(item, player)) {
                need -= item.getAmount();
                if (need <= 0) return true;
            }
        }
        return false;
    }

    public void takeAmount(Player player, int price) {
        int need = price;
        if (need <= 0) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); ++i) {
            final ItemStack item = inv.getItem(i);
            if (isCurrency(item, player)) {
                int takeAway = Math.min(item.getAmount(), need);
                int newAmount = item.getAmount() - takeAway;
                if (newAmount <= 0) {
                    inv.setItem(i, null);
                } else {
                    item.setAmount(newAmount);
                    inv.setItem(i, item);
                }
                need -= takeAway;
                if (need <= 0) return;
            }
        }
    }
}
