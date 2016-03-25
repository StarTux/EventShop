package com.winthier.eventshop;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Msg {
    public static String format(String msg, Object... args) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }

    public static void send(CommandSender sender, String msg, Object... args) {
        msg = format(msg, args);
        sender.sendMessage(msg);
    }
}
