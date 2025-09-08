package com.edtools.edtoolsperks.listeners;

import com.edtools.edtoolsperks.EdToolsPerks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final EdToolsPerks plugin;

    public PlayerListener(EdToolsPerks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Create player in database if not exists
        plugin.getDatabaseManager().createPlayer(player.getUniqueId(), player.getName());
        
        // Give some initial rolls for new players
        if (!player.hasPlayedBefore()) {
            plugin.getDatabaseManager().setPlayerRolls(player.getUniqueId(), 5);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if the player right-clicked with an EdTool
        if (!event.getAction().isRightClick()) {
            return;
        }
        
        if (!plugin.getEdToolsIntegration().isEdTool(item)) {
            return;
        }
        
        // Check if the item has a perk button (slot 49 functionality)
        // This would normally be handled by EdTools GUI integration
        // For now, we'll open our GUI when they interact with the tool
        
        // Cancel the event to prevent normal EdTools GUI from opening
        // and open our perks GUI instead
        if (player.isSneaking()) {
            event.setCancelled(true);
            plugin.getGuiManager().openMainGUI(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        plugin.getLogger().info("Player " + player.getName() + " changed hotbar slot from " + event.getPreviousSlot() + " to " + event.getNewSlot());
        
        // Schedule multiple checks to catch EdTools' dynamic item replacement
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
                plugin.getLogger().info("Checking item in slot " + event.getNewSlot() + ": " + (newItem != null ? newItem.getType() : "null"));
                
                if (newItem != null && plugin.getEdToolsIntegration().isEdTool(newItem)) {
                    loadPerkForTool(player, newItem);
                }
            }
        }.runTaskLater(plugin, 1L);
        
        // Additional check after more ticks in case EdTools replaces the item later
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack laterItem = player.getInventory().getItem(event.getNewSlot());
                plugin.getLogger().info("Re-checking item in slot " + event.getNewSlot() + " after 5 ticks: " + (laterItem != null ? laterItem.getType() : "null"));
                
                if (laterItem != null && plugin.getEdToolsIntegration().isEdTool(laterItem)) {
                    loadPerkForTool(player, laterItem);
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack mainHandItem = event.getMainHandItem();
        
        // Schedule task to run after the swap is complete
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mainHandItem != null && plugin.getEdToolsIntegration().isEdTool(mainHandItem)) {
                    loadPerkForTool(player, mainHandItem);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
    
    // Check for tool changes periodically for the player holding an item
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractForPerks(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // When player interacts, check if the tool should have perks restored
        if (item != null && plugin.getEdToolsIntegration().isEdTool(item)) {
            plugin.getLogger().info("Player interacted with EdTool, checking for perks...");
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack currentItem = player.getInventory().getItemInMainHand();
                    if (currentItem != null && plugin.getEdToolsIntegration().isEdTool(currentItem)) {
                        loadPerkForTool(player, currentItem);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private void loadPerkForTool(Player player, ItemStack tool) {
        plugin.getLogger().info("=== PERK DETECTION START ===");
        plugin.getLogger().info("Player " + player.getName() + " switched to tool: " + tool.getType());
        
        // First, check if the tool already has perk data in NBT
        String existingPerkFromNBT = plugin.getEdToolsIntegration().getToolPerk(tool);
        plugin.getLogger().info("Existing perk in NBT: " + (existingPerkFromNBT != null ? existingPerkFromNBT : "NONE"));
        
        // Check if the tool has perk lore
        if (tool.hasItemMeta() && tool.getItemMeta().hasLore()) {
            boolean hasPerkLore = tool.getItemMeta().getLore().stream()
                .anyMatch(line -> line.contains("Perk |"));
            plugin.getLogger().info("Tool has perk lore: " + hasPerkLore);
            if (hasPerkLore) {
                tool.getItemMeta().getLore().stream()
                    .filter(line -> line.contains("Perk |"))
                    .forEach(line -> plugin.getLogger().info("Perk lore: " + line));
            }
        } else {
            plugin.getLogger().info("Tool has no lore");
        }
        
        // Show all NBT keys in the tool
        if (tool.hasItemMeta()) {
            var pdc = tool.getItemMeta().getPersistentDataContainer();
            plugin.getLogger().info("NBT Keys in tool:");
            for (var key : pdc.getKeys()) {
                try {
                    // Try different data types
                    String value = "unknown";
                    if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                        value = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                    } else if (pdc.has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                        value = String.valueOf(pdc.get(key, org.bukkit.persistence.PersistentDataType.INTEGER));
                    } else if (pdc.has(key, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
                        value = String.valueOf(pdc.get(key, org.bukkit.persistence.PersistentDataType.DOUBLE));
                    } else if (pdc.has(key, org.bukkit.persistence.PersistentDataType.BOOLEAN)) {
                        value = String.valueOf(pdc.get(key, org.bukkit.persistence.PersistentDataType.BOOLEAN));
                    }
                    plugin.getLogger().info("  - " + key.getNamespace() + ":" + key.getKey() + " = " + value);
                } catch (Exception e) {
                    plugin.getLogger().info("  - " + key.getNamespace() + ":" + key.getKey() + " = <error reading value>");
                }
            }
            if (pdc.getKeys().isEmpty()) {
                plugin.getLogger().info("  (No NBT keys found)");
            }
        }
        
        String toolUUID = plugin.getEdToolsIntegration().getToolUUID(tool);
        
        if (toolUUID == null) {
            plugin.getLogger().warning("Could not get UUID for tool: " + tool.getType());
            plugin.getLogger().info("=== PERK DETECTION END ===");
            return;
        }

        plugin.getLogger().info("Loading perk for tool UUID: " + toolUUID);
        
        // Get the perk from database
        plugin.getDatabaseManager().getToolPerk(toolUUID).thenAccept(perkData -> {
            if (perkData != null) {
                plugin.getLogger().info("Found saved perk data: " + perkData);
                
                // Check if NBT and database are synchronized
                if (existingPerkFromNBT != null && !existingPerkFromNBT.equals(perkData)) {
                    plugin.getLogger().warning("DESYNC DETECTED!");
                    plugin.getLogger().warning("NBT has: " + existingPerkFromNBT);
                    plugin.getLogger().warning("Database has: " + perkData);
                    plugin.getLogger().info("Updating database to match NBT (NBT takes priority)");
                    
                    // Parse NBT perk to update database
                    String[] nbtParts = existingPerkFromNBT.split(":");
                    if (nbtParts.length == 2) {
                        try {
                            String nbtPerkId = nbtParts[0];
                            int nbtLevel = Integer.parseInt(nbtParts[1]);
                            // Update database to match NBT (wait for completion)
                            plugin.getDatabaseManager().savePerkToTool(toolUUID, player.getUniqueId(), nbtPerkId, nbtLevel).join();
                            plugin.getLogger().info("Database updated to match NBT");
                        } catch (NumberFormatException e) {
                            plugin.getLogger().severe("Invalid NBT perk format: " + existingPerkFromNBT);
                        } catch (Exception e) {
                            plugin.getLogger().severe("Failed to update database to match NBT: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    plugin.getLogger().info("=== PERK DETECTION END ===");
                    return;
                }
                
                // Parse perk data (format: "perkId:level")
                String[] parts = perkData.split(":");
                if (parts.length == 2) {
                    String perkId = parts[0];
                    int level;
                    
                    try {
                        level = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().severe("Invalid perk level in database: " + parts[1]);
                        return;
                    }
                    
                    // Check if the tool already has this perk applied correctly
                    if (existingPerkFromNBT != null && existingPerkFromNBT.equals(perkData)) {
                        plugin.getLogger().info("Tool already has the correct perk applied: " + existingPerkFromNBT);
                        plugin.getLogger().info("No need to reapply perk");
                        plugin.getLogger().info("=== PERK DETECTION END ===");
                        return;
                    }
                    
                    // Get the perk object
                    var perk = plugin.getPerkManager().getPerk(perkId);
                    if (perk != null) {
                        plugin.getLogger().info("Restoring perk: " + perk.getDisplayName() + " Level " + level);
                        
                        // Apply the perk to the tool (this will restore NBT)
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.getLogger().info("Applying perk to tool: " + perk.getDisplayName() + " Level " + level);
                            plugin.getEdToolsIntegration().applyPerkToTool(tool, perk, level);
                            plugin.getLogger().info("Perk application completed");
                            plugin.getLogger().info("=== PERK DETECTION END ===");
                        });
                    } else {
                        plugin.getLogger().warning("Perk not found: " + perkId);
                        plugin.getLogger().info("=== PERK DETECTION END ===");
                    }
                } else {
                    plugin.getLogger().warning("Invalid perk data format: " + perkData);
                    plugin.getLogger().info("=== PERK DETECTION END ===");
                }
            } else {
                plugin.getLogger().info("No saved perk found for tool UUID: " + toolUUID);
                plugin.getLogger().info("Tool will remain without perks");
                plugin.getLogger().info("=== PERK DETECTION END ===");
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error loading perk for tool: " + ex.getMessage());
            ex.printStackTrace();
            plugin.getLogger().info("=== PERK DETECTION END ===");
            return null;
        });
    }
}