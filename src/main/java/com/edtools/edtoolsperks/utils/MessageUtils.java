package com.edtools.edtoolsperks.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageUtils {

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    public static void send(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    public static void sendConsole(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize("[EdToolsPerks] " + message));
    }

    public static void broadcast(String message) {
        Bukkit.broadcastMessage(colorize(message));
    }

    public static List<String> colorize(List<String> messages) {
        return messages.stream()
                .map(MessageUtils::colorize)
                .toList();
    }

    public static String formatPlaceholders(String message, Object... replacements) {
        String result = message;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = "{" + replacements[i] + "}";
                String value = String.valueOf(replacements[i + 1]);
                result = result.replace(placeholder, value);
            }
        }
        return result;
    }

    public static String createProgressBar(int current, int max, int length, String completedChar, String uncompletedChar) {
        if (max <= 0) return uncompletedChar.repeat(length);
        
        double percentage = (double) current / max;
        int completed = (int) (length * percentage);
        int uncompleted = length - completed;
        
        return completedChar.repeat(Math.max(0, completed)) + 
               uncompletedChar.repeat(Math.max(0, uncompleted));
    }
}