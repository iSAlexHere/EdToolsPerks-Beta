package com.edtools.edtoolsperks.commands;

import com.edtools.edtoolsperks.EdToolsPerks;
import com.edtools.edtoolsperks.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final EdToolsPerks plugin;

    public MainCommand(EdToolsPerks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.player-only"));
                return true;
            }
            
            plugin.getGuiManager().openMainGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                if (!sender.hasPermission("edtoolsperks.admin")) {
                    MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                
                plugin.getConfigManager().reloadConfigs();
                plugin.getPerkManager().loadPerks();
                MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.reload-success"));
            }
            
            case "help" -> showHelp(sender);
            
            case "give" -> {
                if (!sender.hasPermission("edtoolsperks.admin")) {
                    MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                
                if (args.length < 3) {
                    MessageUtils.send(sender, "&cUsage: /edtoolsperks give <player> <amount>");
                    return true;
                }
                
                giveRollsCommand(sender, args[1], args[2]);
            }
            
            case "reset" -> {
                if (!sender.hasPermission("edtoolsperks.admin")) {
                    MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                
                if (args.length < 2) {
                    MessageUtils.send(sender, "&cUsage: /edtoolsperks reset <player>");
                    return true;
                }
                
                resetPityCommand(sender, args[1]);
            }
            
            case "sync" -> {
                if (!sender.hasPermission("edtoolsperks.admin")) {
                    MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                
                if (!(sender instanceof Player player)) {
                    MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.player-only"));
                    return true;
                }
                
                syncPerkCommand(player);
            }
            
            case "regen-uuid" -> {
                if (!sender.hasPermission("edtoolsperks.admin")) {
                    MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                
                if (!(sender instanceof Player player)) {
                    MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.player-only"));
                    return true;
                }
                
                regenerateUUIDCommand(player);
            }
            
            default -> MessageUtils.send(sender, plugin.getConfigManager().getMessage("general.invalid-command"));
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        List<String> helpMessages = Arrays.asList(
            plugin.getConfigManager().getMessage("help.header"),
            plugin.getConfigManager().getMessage("help.commands.0"),
            plugin.getConfigManager().getMessage("help.commands.1")
        );
        
        if (sender.hasPermission("edtoolsperks.admin")) {
            helpMessages.addAll(Arrays.asList(
                plugin.getConfigManager().getMessage("help.admin-commands.0"),
                plugin.getConfigManager().getMessage("help.admin-commands.1"),
                plugin.getConfigManager().getMessage("help.admin-commands.2")
            ));
        }
        
        helpMessages.add(plugin.getConfigManager().getMessage("help.footer"));
        
        helpMessages.forEach(msg -> MessageUtils.send(sender, msg));
    }

    private void giveRollsCommand(CommandSender sender, String playerName, String amountStr) {
        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID;
        String targetDisplayName;
        
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetDisplayName = target.getName();
        } else {
            // Player is offline, try to get UUID from name
            // This is a simple implementation - you might want to use a more robust method
            MessageUtils.send(sender, plugin.getConfigManager().getMessage("admin.player-not-found"));
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, "&cInvalid amount: " + amountStr);
            return;
        }
        
        if (amount <= 0) {
            MessageUtils.send(sender, "&cAmount must be positive!");
            return;
        }
        
        plugin.getDatabaseManager().addPlayerRolls(targetUUID, amount).thenRun(() -> {
            MessageUtils.send(sender, plugin.getConfigManager().getMessage("admin.gave-rolls",
                "amount", amount, "player", targetDisplayName));
                
            if (target != null) {
                MessageUtils.send(target, "&aYou received " + amount + " rolls from an administrator!");
            }
        });
    }

    private void resetPityCommand(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID;
        String targetDisplayName;
        
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetDisplayName = target.getName();
        } else {
            MessageUtils.send(sender, plugin.getConfigManager().getMessage("admin.player-not-found"));
            return;
        }
        
        plugin.getDatabaseManager().setPityCounter(targetUUID, 0).thenRun(() -> {
            MessageUtils.send(sender, plugin.getConfigManager().getMessage("admin.reset-pity",
                "player", targetDisplayName));
                
            if (target != null) {
                MessageUtils.send(target, "&aYour pity counter has been reset by an administrator!");
            }
        });
    }

    private void syncPerkCommand(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        // Check if player is holding an EdTool
        if (!plugin.getEdToolsIntegration().isEdTool(tool)) {
            MessageUtils.send(player, "&cYou must be holding an EdTool to use this command!");
            return;
        }
        
        MessageUtils.send(player, "&eForcing perk synchronization for your tool...");
        
        // Get tool UUID
        String toolUUID = plugin.getEdToolsIntegration().getToolUUID(tool);
        if (toolUUID == null) {
            MessageUtils.send(player, "&cCould not get UUID for this tool!");
            return;
        }
        
        // Get perk from NBT
        String nbtPerk = plugin.getEdToolsIntegration().getToolPerk(tool);
        
        if (nbtPerk == null) {
            MessageUtils.send(player, "&cNo perk found in NBT! Tool has no perk to sync.");
            return;
        }
        
        // Parse NBT perk
        String[] parts = nbtPerk.split(":");
        if (parts.length != 2) {
            MessageUtils.send(player, "&cInvalid perk format in NBT: " + nbtPerk);
            return;
        }
        
        try {
            String perkId = parts[0];
            int level = Integer.parseInt(parts[1]);
            
            // Force update database with NBT data
            plugin.getDatabaseManager().savePerkToTool(toolUUID, player.getUniqueId(), perkId, level).thenRun(() -> {
                MessageUtils.send(player, "&aPerk synchronization completed!");
                MessageUtils.send(player, "&7NBT Perk: &f" + nbtPerk);
                MessageUtils.send(player, "&7Database updated to match NBT data.");
                
                // Get the perk object and reapply it to refresh the lore
                var perk = plugin.getPerkManager().getPerk(perkId);
                if (perk != null) {
                    plugin.getEdToolsIntegration().applyPerkToTool(tool, perk, level);
                    MessageUtils.send(player, "&7Lore refreshed!");
                }
                
            }).exceptionally(ex -> {
                MessageUtils.send(player, "&cFailed to sync perk: " + ex.getMessage());
                plugin.getLogger().severe("Failed to sync perk for player " + player.getName() + ": " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
            
        } catch (NumberFormatException e) {
            MessageUtils.send(player, "&cInvalid perk level in NBT: " + parts[1]);
        }
    }

    private void regenerateUUIDCommand(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        // Check if player is holding an EdTool
        if (!plugin.getEdToolsIntegration().isEdTool(tool)) {
            MessageUtils.send(player, "&cYou must be holding an EdTool to use this command!");
            return;
        }
        
        MessageUtils.send(player, "&eRegenerating UUID for your tool...");
        
        // Get current perk before regenerating UUID
        String currentPerk = plugin.getEdToolsIntegration().getToolPerk(tool);
        
        // Remove the old UUID from NBT to force regeneration
        if (tool.hasItemMeta()) {
            var meta = tool.getItemMeta();
            var pdc = meta.getPersistentDataContainer();
            pdc.remove(plugin.getKey("tool_uuid"));
            tool.setItemMeta(meta);
        }
        
        // Get new UUID (this will generate a new one)
        String newUUID = plugin.getEdToolsIntegration().getToolUUID(tool);
        
        MessageUtils.send(player, "&aNew UUID generated: &f" + newUUID);
        
        // If the tool had a perk, reapply it with the new UUID
        if (currentPerk != null) {
            String[] parts = currentPerk.split(":");
            if (parts.length == 2) {
                try {
                    String perkId = parts[0];
                    int level = Integer.parseInt(parts[1]);
                    
                    // Save perk with new UUID
                    plugin.getDatabaseManager().savePerkToTool(newUUID, player.getUniqueId(), perkId, level).thenRun(() -> {
                        MessageUtils.send(player, "&aPerk restored with new UUID!");
                        MessageUtils.send(player, "&7Perk: &f" + currentPerk);
                        
                        // Reapply perk to refresh lore
                        var perk = plugin.getPerkManager().getPerk(perkId);
                        if (perk != null) {
                            plugin.getEdToolsIntegration().applyPerkToTool(tool, perk, level);
                        }
                        
                    }).exceptionally(ex -> {
                        MessageUtils.send(player, "&cFailed to restore perk: " + ex.getMessage());
                        return null;
                    });
                    
                } catch (NumberFormatException e) {
                    MessageUtils.send(player, "&cInvalid perk format, perk not restored: " + currentPerk);
                }
            }
        } else {
            MessageUtils.send(player, "&7Tool had no perk to restore.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("help", "reload"));
            
            if (sender.hasPermission("edtoolsperks.admin")) {
                subCommands.addAll(Arrays.asList("give", "reset", "sync", "regen-uuid"));
            }
            
            String input = args[0].toLowerCase();
            subCommands.stream()
                .filter(cmd -> cmd.startsWith(input))
                .forEach(completions::add);
        }
        
        else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset"))) {
            if (sender.hasPermission("edtoolsperks.admin")) {
                String input = args[1].toLowerCase();
                Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .forEach(completions::add);
            }
        }
        
        else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("edtoolsperks.admin")) {
                completions.addAll(Arrays.asList("1", "5", "10", "25", "50"));
            }
        }
        
        return completions;
    }
}