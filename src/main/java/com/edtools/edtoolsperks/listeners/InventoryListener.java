package com.edtools.edtoolsperks.listeners;

import com.edtools.edtoolsperks.EdToolsPerks;
import com.edtools.edtoolsperks.gui.RollAnimationManager;
import com.edtools.edtoolsperks.perks.Perk;
import com.edtools.edtoolsperks.utils.MessageUtils;
// No longer using Economy - using EdTools farm-coins currency
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final EdToolsPerks plugin;

    public InventoryListener(EdToolsPerks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String openGUI = plugin.getGuiManager().getOpenGUI(player.getUniqueId());
        if (openGUI == null) {
            return;
        }

        // ALWAYS cancel the event first, regardless of anything else
        event.setCancelled(true);
        
        // Also set the result to DENY to be extra sure
        event.setResult(org.bukkit.event.Event.Result.DENY);

        // Prevent all potentially problematic actions
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR ||
            event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
            event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD ||
            event.getAction() == InventoryAction.HOTBAR_SWAP ||
            event.getAction() == InventoryAction.CLONE_STACK ||
            event.getAction() == InventoryAction.UNKNOWN) {
            plugin.getLogger().info("Blocked problematic action: " + event.getAction());
            return;
        }

        // Only handle clicks in the top inventory (our GUI)  
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            plugin.getLogger().info("Click not in top inventory, ignoring");
            return;
        }

        int slot = event.getSlot();

        switch (openGUI) {
            case "main" -> handleMainGUIClick(player, slot, event);
            case "perks-list" -> handlePerksListClick(player, slot, event);
            case "animation" -> {
                // Don't allow clicking during animation
                plugin.getLogger().info("Click during animation, ignoring");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String openGUI = plugin.getGuiManager().getOpenGUI(player.getUniqueId());
        if (openGUI != null) {
            // Cancel all dragging in our GUIs
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            plugin.getLogger().info("Blocked drag event in GUI: " + openGUI);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // Check if any player has a GUI open that might be affected
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            String openGUI = plugin.getGuiManager().getOpenGUI(player.getUniqueId());
            if (openGUI != null && (event.getSource().equals(player.getOpenInventory().getTopInventory()) || 
                                   event.getDestination().equals(player.getOpenInventory().getTopInventory()))) {
                event.setCancelled(true);
                plugin.getLogger().info("Blocked item move event affecting GUI: " + openGUI);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        // Block any pickup events that might affect our GUIs
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            String openGUI = plugin.getGuiManager().getOpenGUI(player.getUniqueId());
            if (openGUI != null && event.getInventory().equals(player.getOpenInventory().getTopInventory())) {
                event.setCancelled(true);
                plugin.getLogger().info("Blocked pickup event affecting GUI: " + openGUI);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickHighest(InventoryClickEvent event) {
        // Additional blocking at highest priority as a fallback
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String openGUI = plugin.getGuiManager().getOpenGUI(player.getUniqueId());
        if (openGUI != null) {
            // Force cancel and deny at highest priority too
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            
            // Block ALL actions that could move items
            if (event.getAction().name().contains("MOVE") || 
                event.getAction().name().contains("COLLECT") ||
                event.getAction().name().contains("HOTBAR") ||
                event.getAction().name().contains("DROP") ||
                event.getAction() == InventoryAction.CLONE_STACK ||
                event.getAction() == InventoryAction.UNKNOWN) {
                plugin.getLogger().info("Highest priority block: " + event.getAction() + " in GUI: " + openGUI);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDragHighest(InventoryDragEvent event) {
        // Additional drag blocking at highest priority
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String openGUI = plugin.getGuiManager().getOpenGUI(player.getUniqueId());
        if (openGUI != null) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            plugin.getLogger().info("Highest priority drag block in GUI: " + openGUI);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            String openGUI = plugin.getGuiManager().getOpenGUI(player.getUniqueId());
            if (openGUI != null) {
                plugin.getLogger().info("Player " + player.getName() + " closed GUI: " + openGUI);
                // The GUIManager should handle cleanup
            }
        }
    }

    private void handleMainGUIClick(Player player, int slot, InventoryClickEvent event) {
        // Debug logging
        plugin.getLogger().info("Click detected - Slot: " + slot + ", Right click: " + event.isRightClick());
        
        // Get the item action from GUI configuration
        String action = getActionForSlot("main-gui", slot, event.isRightClick() ? "right-click" : "left-click");
        
        plugin.getLogger().info("Action found: " + action);
        
        if (action == null) {
            return;
        }

        if (action.startsWith("open-gui:")) {
            String targetGUI = action.substring(9);
            if (targetGUI.equals("perks-list")) {
                plugin.getGuiManager().openPerksListGUI(player);
            }
        } else if (action.startsWith("roll:")) {
            plugin.getLogger().info("Processing roll action: " + action);
            
            if (!plugin.getEdToolsIntegration().isHoldingEdTool(player)) {
                plugin.getLogger().info("Player not holding EdTool");
                MessageUtils.send(player, plugin.getConfigManager().getMessage("tool.not-edtool"));
                return;
            }

            int rollCount = Integer.parseInt(action.substring(5));
            plugin.getLogger().info("Roll count: " + rollCount);
            
            plugin.getDatabaseManager().getPlayerRolls(player.getUniqueId()).thenAccept(currentRolls -> {
                plugin.getLogger().info("Player has " + currentRolls + " rolls, needs " + rollCount);
                if (currentRolls < rollCount) {
                    MessageUtils.send(player, plugin.getConfigManager().getMessage("roll.insufficient-rolls", 
                        "required", rollCount, "current", currentRolls));
                    return;
                }

                plugin.getLogger().info("Starting roll sequence");
                // Start roll animation
                startRollSequence(player, rollCount);
            });
        } else if (action.startsWith("buy-rolls:")) {
            plugin.getLogger().info("Processing buy-rolls action: " + action);
            String[] parts = action.substring(10).split(":");
            plugin.getLogger().info("Split parts: " + java.util.Arrays.toString(parts));
            
            if (parts.length == 2) {
                try {
                    int amount = Integer.parseInt(parts[0]);
                    double cost = Double.parseDouble(parts[1]);
                    plugin.getLogger().info("Parsed buy-rolls: amount=" + amount + ", cost=" + cost);
                    buyRolls(player, amount, cost);
                } catch (NumberFormatException e) {
                    plugin.getLogger().severe("ERROR parsing buy-rolls parameters: " + e.getMessage());
                }
            } else {
                plugin.getLogger().severe("ERROR: buy-rolls action has wrong number of parts: " + parts.length);
            }
        }
    }

    private String getActionForSlot(String guiId, int slot, String clickType) {
        String itemKey = findItemKeyBySlot(guiId, slot);
        plugin.getLogger().info("Looking for item key: " + itemKey + " in GUI: " + guiId + " for slot: " + slot);
        
        var guiItem = plugin.getGuiManager().getGUIItem(guiId, itemKey);
        if (guiItem != null) {
            var actions = guiItem.getActions(clickType);
            plugin.getLogger().info("Found actions: " + actions + " for click type: " + clickType);
            return actions.isEmpty() ? null : actions.get(0);
        } else {
            plugin.getLogger().info("No GUI item found for key: " + itemKey);
        }
        return null;
    }

    private String findItemKeyBySlot(String guiId, int slot) {
        // This is a simplified implementation - you might want to cache this for performance
        var config = plugin.getGuiManager().guiConfigurations.get(guiId);
        if (config != null) {
            for (var entry : config.getItems().entrySet()) {
                if (entry.getValue().getSlots().contains(slot)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void handlePerksListClick(Player player, int slot, InventoryClickEvent event) {
        // Debug logging
        plugin.getLogger().info("Perks list click detected - Slot: " + slot + ", Right click: " + event.isRightClick());
        
        // Get the item action from GUI configuration
        String action = getActionForSlot("perks-list-gui", slot, event.isRightClick() ? "right-click" : "left-click");
        
        plugin.getLogger().info("Perks list action found: " + action);
        
        if (action == null) {
            return;
        }

        if (action.startsWith("open-gui:")) {
            String targetGUI = action.substring(9);
            plugin.getLogger().info("Opening GUI: " + targetGUI);
            if (targetGUI.equals("main")) {
                plugin.getGuiManager().openMainGUI(player);
            }
        }
    }

    private void startRollSequence(Player player, int rollCount) {
        plugin.getLogger().info("Starting roll sequence for " + player.getName() + " with " + rollCount + " rolls");
        
        // Deduct rolls first
        plugin.getDatabaseManager().addPlayerRolls(player.getUniqueId(), -rollCount);
        plugin.getDatabaseManager().incrementTotalRolls(player.getUniqueId(), rollCount);

        // Check if pity should trigger
        plugin.getDatabaseManager().getTotalRolls(player.getUniqueId()).thenAccept(totalRolls -> {
            plugin.getDatabaseManager().getPityCounter(player.getUniqueId()).thenAccept(pityCounter -> {
                try {
                    boolean guaranteePurple = (pityCounter + rollCount) >= 
                        plugin.getConfigManager().getConfig().getInt("rolls.guaranteed-purple-at", 500);

                    plugin.getLogger().info("Pity check - Total: " + totalRolls + ", Counter: " + pityCounter + ", Guarantee: " + guaranteePurple);

                    if (guaranteePurple) {
                        plugin.getDatabaseManager().setPityCounter(player.getUniqueId(), 0);
                    }

                    // Roll the perks
                    for (int i = 0; i < rollCount; i++) {
                        try {
                            boolean isLastRoll = (i == rollCount - 1);
                            boolean useGuarantee = guaranteePurple && isLastRoll;
                            
                            plugin.getLogger().info("Rolling perk " + (i+1) + "/" + rollCount + ", Last roll: " + isLastRoll);
                            
                            Perk rolledPerk = plugin.getPerkManager().rollRandomPerk(useGuarantee);
                            if (rolledPerk == null) {
                                plugin.getLogger().severe("ERROR: rollRandomPerk returned null!");
                                return;
                            }
                            
                            int perkLevel = plugin.getPerkManager().rollPerkLevel(rolledPerk);
                            plugin.getLogger().info("Rolled: " + rolledPerk.getDisplayName() + " Level " + perkLevel);

                            if (isLastRoll) {
                                // Show animation for last roll - must run on main thread
                                plugin.getLogger().info("Starting animation for last roll");
                                try {
                                    // Schedule animation on main thread
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        try {
                                            RollAnimationManager animationManager = new RollAnimationManager(plugin, player, rolledPerk, perkLevel, useGuarantee);
                                            animationManager.startAnimation();
                                        } catch (Exception e) {
                                            plugin.getLogger().severe("ERROR starting animation: " + e.getMessage());
                                            e.printStackTrace();
                                            // Fallback: apply perk without animation
                                            applyPerkToTool(player, rolledPerk, perkLevel, useGuarantee);
                                        }
                                    });
                                } catch (Exception e) {
                                    plugin.getLogger().severe("ERROR scheduling animation: " + e.getMessage());
                                    e.printStackTrace();
                                    // Fallback: apply perk without animation
                                    applyPerkToTool(player, rolledPerk, perkLevel, useGuarantee);
                                }
                            } else {
                                // Apply perk immediately for non-animated rolls
                                plugin.getLogger().info("Applying perk immediately (not last roll)");
                                applyPerkToTool(player, rolledPerk, perkLevel, useGuarantee);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("ERROR in roll loop iteration " + (i+1) + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("ERROR in startRollSequence: " + e.getMessage());
                    e.printStackTrace();
                }
            }).exceptionally(ex -> {
                plugin.getLogger().severe("ERROR getting pity counter: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("ERROR getting total rolls: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private void applyPerkToTool(Player player, Perk perk, int level, boolean wasGuaranteed) {
        try {
            plugin.getLogger().info("Applying perk to tool: " + perk.getDisplayName() + " Level " + level);
            
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool == null) {
                plugin.getLogger().severe("ERROR: Player has no item in main hand!");
                return;
            }
            
            plugin.getLogger().info("Tool in hand: " + tool.getType());
            
            // Apply perk via EdTools integration and NBT
            try {
                plugin.getEdToolsIntegration().applyPerkToTool(tool, perk, level);
                plugin.getLogger().info("Perk applied to tool successfully");
            } catch (Exception e) {
                plugin.getLogger().severe("ERROR applying perk to tool: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Save to database IMMEDIATELY and wait for completion
            String toolUUID = plugin.getEdToolsIntegration().getToolUUID(tool);
            plugin.getLogger().info("Tool UUID: " + toolUUID);
            
            if (toolUUID != null) {
                try {
                    // Save synchronously to ensure database is updated before continuing
                    plugin.getDatabaseManager().savePerkToTool(toolUUID, player.getUniqueId(), perk.getId(), level).join();
                    plugin.getDatabaseManager().saveRollHistory(player.getUniqueId(), perk.getId(), level, perk.getCategory(), wasGuaranteed);
                    plugin.getLogger().info("Perk saved to database successfully (synchronized)");
                } catch (Exception e) {
                    plugin.getLogger().severe("ERROR saving perk to database: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                plugin.getLogger().warning("Tool UUID is null - not saving to database");
            }

            // Send message
            try {
                String message = wasGuaranteed ? "roll.guaranteed-perk" : "roll.perk-applied";
                String categoryColor = plugin.getPerkManager().getCategory(perk.getCategory()).getColor();
                
                plugin.getLogger().info("Sending message to player: " + message);
                MessageUtils.send(player, plugin.getConfigManager().getMessage(message,
                    "category_color", categoryColor,
                    "perk_name", perk.getDisplayName(),
                    "level", level));
            } catch (Exception e) {
                plugin.getLogger().severe("ERROR sending message to player: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("ERROR in applyPerkToTool: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buyRolls(Player player, int amount, double cost) {
        plugin.getLogger().info("Player " + player.getName() + " attempting to buy " + amount + " rolls for " + cost + " farm-coins");
        
        // Check current balance
        double currentBalance = plugin.getEdToolsIntegration().getFarmCoinsBalance(player);
        plugin.getLogger().info("Player current farm-coins balance: " + currentBalance);
        
        if (!plugin.getEdToolsIntegration().hasFarmCoins(player, cost)) {
            plugin.getLogger().info("Player doesn't have enough farm-coins. Required: " + cost + ", Has: " + currentBalance);
            MessageUtils.send(player, plugin.getConfigManager().getMessage("roll.insufficient-money",
                "required", (int)cost, "current", (int)currentBalance));
            return;
        }

        plugin.getLogger().info("Player has enough farm-coins, attempting to remove " + cost);
        if (!plugin.getEdToolsIntegration().removeFarmCoins(player, cost)) {
            plugin.getLogger().severe("ERROR: Failed to remove farm-coins from player!");
            MessageUtils.send(player, "&cFailed to remove farm-coins!");
            return;
        }
        
        plugin.getLogger().info("Farm-coins removed successfully, adding " + amount + " rolls to player");
        plugin.getDatabaseManager().addPlayerRolls(player.getUniqueId(), amount);
        
        // Verify the rolls were added
        plugin.getDatabaseManager().getPlayerRolls(player.getUniqueId()).thenAccept(newRollCount -> {
            plugin.getLogger().info("Player now has " + newRollCount + " rolls after purchase");
        });
        
        MessageUtils.send(player, plugin.getConfigManager().getMessage("roll.rolls-purchased",
            "amount", amount, "cost", (int)cost));
            
        // Refresh the GUI
        plugin.getGuiManager().openMainGUI(player);
    }
}