package com.edtools.edtoolsperks.gui;

import com.edtools.edtoolsperks.EdToolsPerks;
import com.edtools.edtoolsperks.utils.ItemUtils;
import com.edtools.edtoolsperks.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class GUIManager {

    private final EdToolsPerks plugin;
    public final Map<UUID, String> openGUIs = new HashMap<>();
    private final Map<UUID, Map<String, String>> guiPlaceholders = new HashMap<>();
    public final Map<String, GUIConfiguration> guiConfigurations = new HashMap<>();

    public GUIManager(EdToolsPerks plugin) {
        this.plugin = plugin;
    }

    public void loadGUIs() {
        guiConfigurations.clear();
        loadGUIConfiguration("main-gui");
        loadGUIConfiguration("perks-list-gui");
        loadGUIConfiguration("animation-gui");
        MessageUtils.sendConsole("&aGUI Manager initialized with " + guiConfigurations.size() + " GUI configurations!");
    }

    private void loadGUIConfiguration(String guiId) {
        FileConfiguration guisConfig = plugin.getConfigManager().getGUIsConfig();
        ConfigurationSection guiSection = guisConfig.getConfigurationSection(guiId);
        
        if (guiSection == null) {
            MessageUtils.sendConsole("&cGUI configuration '" + guiId + "' not found!");
            return;
        }
        
        String title = guiSection.getString("title", "&8GUI");
        int size = guiSection.getInt("size", 54);
        
        // Load settings
        Map<String, Object> settings = new HashMap<>();
        ConfigurationSection settingsSection = guiSection.getConfigurationSection("settings");
        if (settingsSection != null) {
            for (String key : settingsSection.getKeys(false)) {
                settings.put(key, settingsSection.get(key));
            }
        }
        
        // Load items
        Map<String, GUIItem> items = new HashMap<>();
        ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    GUIItem guiItem = loadGUIItem(itemSection);
                    if (guiItem != null) {
                        items.put(itemKey, guiItem);
                    }
                }
            }
        }
        
        guiConfigurations.put(guiId, new GUIConfiguration(title, size, items, settings));
    }

    private GUIItem loadGUIItem(ConfigurationSection section) {
        String material = section.getString("material", "STONE");
        String name = section.getString("name", "");
        List<String> lore = section.getStringList("lore");
        String slotsStr = section.getString("slots", "");
        
        // Parse slots
        List<Integer> slots = new ArrayList<>();
        if (!slotsStr.isEmpty()) {
            String[] slotParts = slotsStr.split(",");
            for (String slotPart : slotParts) {
                slotPart = slotPart.trim();
                if (slotPart.contains("-")) {
                    // Handle range (e.g., "0-8")
                    String[] range = slotPart.split("-");
                    if (range.length == 2) {
                        try {
                            int start = Integer.parseInt(range[0]);
                            int end = Integer.parseInt(range[1]);
                            for (int i = start; i <= end; i++) {
                                slots.add(i);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid ranges
                        }
                    }
                } else {
                    // Single slot
                    try {
                        slots.add(Integer.parseInt(slotPart));
                    } catch (NumberFormatException e) {
                        // Ignore invalid slots
                    }
                }
            }
        }
        
        // Load actions
        Map<String, List<String>> actions = new HashMap<>();
        ConfigurationSection actionsSection = section.getConfigurationSection("actions");
        if (actionsSection != null) {
            for (String actionType : actionsSection.getKeys(false)) {
                List<String> actionList = actionsSection.getStringList(actionType);
                actions.put(actionType, actionList);
            }
        }
        
        return new GUIItem(material, name, lore, slots, actions);
    }

    public void openMainGUI(Player player) {
        GUIConfiguration config = guiConfigurations.get("main-gui");
        if (config == null) {
            MessageUtils.send(player, "&cGUI configuration not found!");
            return;
        }

        // Create inventory
        Inventory inventory = Bukkit.createInventory(null, config.getSize(), 
            MessageUtils.colorize(config.getTitle()));

        // Set placeholders for this player
        Map<String, String> placeholders = new HashMap<>();
        
        // Get player data asynchronously and then update GUI
        plugin.getDatabaseManager().getPlayerRolls(player.getUniqueId()).thenAccept(rolls -> {
            plugin.getDatabaseManager().getTotalRolls(player.getUniqueId()).thenAccept(totalRolls -> {
                placeholders.put("player_rolls", String.valueOf(rolls));
                placeholders.put("total_rolls", String.valueOf(totalRolls));
                placeholders.put("pity_bar", createPityBar(totalRolls));
                
                // Add tool placeholders
                ItemStack tool = player.getInventory().getItemInMainHand();
                placeholders.put("tool_material", tool.getType().name());
                placeholders.put("tool_name", tool.hasItemMeta() && tool.getItemMeta().hasDisplayName() 
                    ? tool.getItemMeta().getDisplayName() : tool.getType().name());
                
                // Run on main thread to update inventory
                Bukkit.getScheduler().runTask(plugin, () -> {
                    populateGUIFromConfig(inventory, config, player, placeholders);
                    player.openInventory(inventory);
                    openGUIs.put(player.getUniqueId(), "main");
                    guiPlaceholders.put(player.getUniqueId(), placeholders);
                });
            });
        });
    }

    public void openPerksListGUI(Player player) {
        GUIConfiguration config = guiConfigurations.get("perks-list-gui");
        if (config == null) {
            MessageUtils.send(player, "&cGUI configuration not found!");
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, config.getSize(), 
            MessageUtils.colorize(config.getTitle()));

        Map<String, String> placeholders = new HashMap<>();
        populateGUIFromConfig(inventory, config, player, placeholders);
        
        // Add dynamic perks
        populatePerksInGUI(inventory, config);
        
        player.openInventory(inventory);
        openGUIs.put(player.getUniqueId(), "perks-list");
    }

    private void populateGUIFromConfig(Inventory inventory, GUIConfiguration config, Player player, Map<String, String> placeholders) {
        for (Map.Entry<String, GUIItem> entry : config.getItems().entrySet()) {
            String itemKey = entry.getKey();
            GUIItem guiItem = entry.getValue();
            
            // Special handling for tool-display
            ItemStack item;
            if (itemKey.equals("tool-display")) {
                item = getPlayerToolDisplay(player);
            } else {
                // Create item from configuration
                String material = replacePlaceholders(guiItem.getMaterial(), placeholders);
                String name = MessageUtils.colorize(replacePlaceholders(guiItem.getName(), placeholders));
                
                List<String> processedLore = guiItem.getLore().stream()
                    .map(line -> MessageUtils.colorize(replacePlaceholders(line, placeholders)))
                    .collect(Collectors.toList());
                
                if (material.startsWith("texture-")) {
                    String texture = material.substring(8); // Remove "texture-" prefix
                    item = ItemUtils.createSkullItem(texture, name, processedLore.toArray(new String[0]));
                } else {
                    item = ItemUtils.createItem(material, name, processedLore.toArray(new String[0]));
                }
            }
            
            // Place item in specified slots
            for (int slot : guiItem.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }

    private void populatePerksInGUI(Inventory inventory, GUIConfiguration config) {
        // Get perk slots from configuration
        String perkSlotsStr = config.getStringSetting("perk-slots");
        List<String> categoryOrder = config.getStringListSetting("category-order");
        
        if (perkSlotsStr.isEmpty()) {
            return;
        }
        
        // Parse perk slots
        List<Integer> perkSlots = Arrays.stream(perkSlotsStr.split(","))
            .map(String::trim)
            .map(Integer::parseInt)
            .collect(Collectors.toList());
        
        int slotIndex = 0;
        
        for (String categoryId : categoryOrder) {
            var perks = plugin.getPerkManager().getPerksByCategory(categoryId);
            for (var perk : perks) {
                if (slotIndex >= perkSlots.size()) break;
                
                ItemStack perkItem = ItemUtils.createItem(perk.getDisplayMaterial().name(),
                    plugin.getPerkManager().getCategory(categoryId).getColor() + perk.getDisplayName(),
                    perk.getLore().toArray(new String[0]));
                
                inventory.setItem(perkSlots.get(slotIndex), perkItem);
                slotIndex++;
            }
            if (slotIndex >= perkSlots.size()) break;
        }
    }

    private ItemStack getPlayerToolDisplay(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        // Check if it's an EdTool using the integration
        if (plugin.getEdToolsIntegration() != null && 
            plugin.getEdToolsIntegration().isEdTool(heldItem)) {
            
            return heldItem.clone();
        }
        
        // Default display if no tool
        return ItemUtils.createItem("BARRIER", 
            "&cNo EdTool", 
            new String[]{
                "&7Hold an EdTool to use perks!",
                "&7",
                "&cPlease equip a valid tool."
            });
    }

    private String createPityBar(int totalRolls) {
        int maxRolls = plugin.getConfigManager().getConfig().getInt("rolls.guaranteed-purple-at", 500);
        
        return MessageUtils.createProgressBar(
            Math.min(totalRolls, maxRolls), 
            maxRolls, 
            20, 
            "&d▌", 
            "&8▌"
        );
    }

    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public String getOpenGUI(UUID playerId) {
        return openGUIs.get(playerId);
    }

    public void closeGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
        guiPlaceholders.remove(player.getUniqueId());
        player.closeInventory();
    }

    public Map<String, String> getGUIPlaceholders(UUID playerId) {
        return guiPlaceholders.getOrDefault(playerId, new HashMap<>());
    }

    public GUIItem getGUIItem(String guiId, String itemKey) {
        GUIConfiguration config = guiConfigurations.get(guiId);
        return config != null ? config.getItem(itemKey) : null;
    }
}