package com.edtools.edtoolsperks.integration;

import com.edtools.edtoolsperks.EdToolsPerks;
import com.edtools.edtoolsperks.perks.Perk;
import com.edtools.edtoolsperks.utils.MessageUtils;
import es.edwardbelt.edgens.iapi.EdToolsAPI;
import es.edwardbelt.edgens.iapi.EdToolsOmniToolAPI;
import es.edwardbelt.edgens.iapi.EdToolsCurrencyAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;

public class EdToolsIntegration {

    private final EdToolsPerks plugin;
    private EdToolsAPI edToolsAPI;
    private EdToolsOmniToolAPI omniToolAPI;
    private EdToolsCurrencyAPI currencyAPI;

    public EdToolsIntegration(EdToolsPerks plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // Initialize EdTools API
        if (!initializeEdToolsAPI()) {
            MessageUtils.sendConsole("&cFailed to initialize EdTools API!");
            return;
        }

        // Initialize Currency API
        if (!initializeCurrencyAPI()) {
            MessageUtils.sendConsole("&cFailed to initialize EdTools Currency API! Some features may not work.");
        }

        MessageUtils.sendConsole("&aEdTools integration initialized successfully!");
    }

    private boolean initializeEdToolsAPI() {
        try {
            edToolsAPI = EdToolsAPI.getInstance();
            if (edToolsAPI == null) {
                return false;
            }
            
            omniToolAPI = edToolsAPI.getOmniToolAPI();
            MessageUtils.sendConsole("&aEdTools API initialized successfully");
            return omniToolAPI != null;
        } catch (Exception e) {
            MessageUtils.sendConsole("&cError initializing EdTools API: " + e.getMessage());
            return false;
        }
    }

    private boolean initializeCurrencyAPI() {
        try {
            if (edToolsAPI == null) {
                return false;
            }
            
            currencyAPI = edToolsAPI.getCurrencyAPI();
            MessageUtils.sendConsole("&aEdTools Currency API initialized successfully");
            return currencyAPI != null;
        } catch (Exception e) {
            MessageUtils.sendConsole("&cError initializing EdTools Currency API: " + e.getMessage());
            return false;
        }
    }

    public boolean isEdTool(ItemStack item) {
        if (item == null || omniToolAPI == null) {
            return false;
        }
        
        return omniToolAPI.isItemOmniTool(item);
    }

    public boolean isHoldingEdTool(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return isEdTool(item);
    }

    public String getToolUUID(ItemStack item) {
        if (!isEdTool(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // Priority 1: Check for EdTools UUID key
        if (pdc.has(plugin.getKey("edtools_omnitool_uuid"), PersistentDataType.STRING)) {
            String edToolsUUID = pdc.get(plugin.getKey("edtools_omnitool_uuid"), PersistentDataType.STRING);
            plugin.getLogger().info("Found EdTools UUID: " + edToolsUUID);
            return edToolsUUID;
        }
        
        // Priority 2: Check for our own saved UUID
        if (pdc.has(plugin.getKey("tool_uuid"), PersistentDataType.STRING)) {
            String savedUUID = pdc.get(plugin.getKey("tool_uuid"), PersistentDataType.STRING);
            plugin.getLogger().info("Found saved UUID: " + savedUUID);
            return savedUUID;
        }
        
        // Priority 3: Generate and save a new UUID
        String newUUID = generateToolUUID(item);
        pdc.set(plugin.getKey("tool_uuid"), PersistentDataType.STRING, newUUID);
        item.setItemMeta(meta);
        plugin.getLogger().info("Generated new UUID: " + newUUID);
        return newUUID;
    }

    private String generateToolUUID(ItemStack item) {
        // Generate a truly unique UUID using Java's UUID class
        // Add current timestamp to ensure uniqueness
        String baseUUID = java.util.UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        
        // Create unique hash combining UUID and timestamp
        String uniqueString = baseUUID + "-" + timestamp + "-" + item.getType().name();
        String hash = Integer.toHexString(uniqueString.hashCode());
        
        // Format as a UUID-like string
        if (hash.length() < 8) {
            hash = String.format("%8s", hash).replace(' ', '0');
        }
        
        plugin.getLogger().info("Generated truly unique UUID for tool: tool-" + hash);
        return "tool-" + hash;
    }

    public void applyPerkToTool(ItemStack tool, Perk perk, int level) {
        if (!isEdTool(tool)) {
            plugin.getLogger().warning("applyPerkToTool: Item is not an EdTool: " + tool.getType());
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            plugin.getLogger().warning("applyPerkToTool: Tool has no ItemMeta");
            return;
        }

        plugin.getLogger().info("=== APPLYING PERK TO TOOL ===");
        plugin.getLogger().info("Tool: " + tool.getType());
        plugin.getLogger().info("Perk: " + perk.getDisplayName() + " Level " + level);

        // Store perk information in NBT
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getKey("perk_name"), PersistentDataType.STRING, perk.getId());
        pdc.set(plugin.getKey("perk_level"), PersistentDataType.INTEGER, level);
        
        // Store additional metadata for EdTools integration
        pdc.set(plugin.getKey("edtools_perk_id"), PersistentDataType.STRING, perk.getId());
        pdc.set(plugin.getKey("edtools_perk_level"), PersistentDataType.INTEGER, level);
        
        plugin.getLogger().info("NBT data written to tool");

        // Add perk to lore since EdTools doesn't automatically handle it
        addPerkToLore(meta, perk, level);
        tool.setItemMeta(meta);
        
        plugin.getLogger().info("Lore updated and ItemMeta applied to tool");
        
        // Verify the NBT was saved correctly
        ItemMeta verifyMeta = tool.getItemMeta();
        if (verifyMeta != null) {
            PersistentDataContainer verifyPdc = verifyMeta.getPersistentDataContainer();
            String savedPerkName = verifyPdc.get(plugin.getKey("perk_name"), PersistentDataType.STRING);
            Integer savedPerkLevel = verifyPdc.get(plugin.getKey("perk_level"), PersistentDataType.INTEGER);
            plugin.getLogger().info("Verification - Saved perk: " + savedPerkName + " Level: " + savedPerkLevel);
        }

        // Apply boost effects to EdTools
        applyBoostEffects(tool, perk, level);
        
        plugin.getLogger().info("=== PERK APPLICATION COMPLETE ===");
    }

    private void applyBoostEffects(ItemStack tool, Perk perk, int level) {
        if (perk.getLevel(level) == null) {
            return;
        }

        Perk.PerkLevel perkLevel = perk.getLevel(level);
        
        // Apply boosts to EdTools enchantments/currencies
        // This would interact with EdTools' boost system
        for (var entry : perkLevel.getParsedBoosts().entrySet()) {
            String boostType = entry.getKey();
            Double boostAmount = entry.getValue();
            
            applyBoostToEdTools(tool, boostType, boostAmount);
        }
    }

    private void applyBoostToEdTools(ItemStack tool, String boostType, double amount) {
        // This method would interact with EdTools' boost system
        // Since we don't have direct access to EdTools' boost modification methods,
        // we'll store the boost information and apply it when the tool is used
        
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String key = "boost_" + boostType.toLowerCase();
        
        // Store or add to existing boost
        double currentBoost = pdc.getOrDefault(plugin.getKey(key), PersistentDataType.DOUBLE, 0.0);
        pdc.set(plugin.getKey(key), PersistentDataType.DOUBLE, currentBoost + amount);
        
        tool.setItemMeta(meta);
    }

    private void addPerkToLore(ItemMeta meta, Perk perk, int level) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // Remove any existing perk lore first
        lore.removeIf(line -> line.contains("Perk |"));
        
        // Create perk lore line
        String perkLore = "§6Perk | §f" + perk.getDisplayName() + " §7Level " + level;
        
        // Add the perk lore (usually near the end but before any other plugin lores)
        // Find a good position to insert the perk lore
        int insertIndex = lore.size();
        
        // Try to insert before any EdTools internal lore (if present)
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            // Insert before lines that look like EdTools stats or other technical info
            if (line.contains("§7") && (line.contains("UUID") || line.contains("ID") || line.contains("Type"))) {
                insertIndex = i;
                break;
            }
        }
        
        lore.add(insertIndex, perkLore);
        meta.setLore(lore);
        
        plugin.getLogger().info("Added perk lore: " + perkLore);
    }

    public String getToolPerk(ItemStack tool) {
        if (!isEdTool(tool)) {
            return null;
        }

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String perkName = pdc.get(plugin.getKey("perk_name"), PersistentDataType.STRING);
        Integer perkLevel = pdc.get(plugin.getKey("perk_level"), PersistentDataType.INTEGER);

        if (perkName != null && perkLevel != null) {
            return perkName + ":" + perkLevel;
        }

        return null;
    }

    public double getToolBoost(ItemStack tool, String boostType) {
        if (!isEdTool(tool)) {
            return 0.0;
        }

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return 0.0;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String key = "boost_" + boostType.toLowerCase();
        
        return pdc.getOrDefault(plugin.getKey(key), PersistentDataType.DOUBLE, 0.0);
    }

    public void removeToolPerk(ItemStack tool) {
        if (!isEdTool(tool)) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // Remove perk data
        pdc.remove(plugin.getKey("perk_name"));
        pdc.remove(plugin.getKey("perk_level"));

        // Remove boost data
        String[] boostTypes = {"coin", "orb", "money", "pass", "level", "enchant", "crops"};
        for (String type : boostTypes) {
            pdc.remove(plugin.getKey("boost_" + type));
        }

        // Remove perk from lore
        if (meta.hasLore()) {
            List<String> lore = new ArrayList<>(meta.getLore());
            lore.removeIf(line -> line.contains("Perk |"));
            meta.setLore(lore);
        }

        tool.setItemMeta(meta);
    }

    // Getters
    // public EdToolsAPI getEdToolsAPI() {
    //     return edToolsAPI;
    // }

    public EdToolsOmniToolAPI getOmniToolAPI() {
        return omniToolAPI;
    }

    public EdToolsCurrencyAPI getCurrencyAPI() {
        return currencyAPI;
    }

    // Farm-coins currency management methods
    public boolean hasFarmCoins(Player player, double amount) {
        if (currencyAPI == null) {
            plugin.getLogger().severe("CurrencyAPI is null!");
            return false;
        }
        try {
            double balance = currencyAPI.getCurrency(player.getUniqueId(), "farm-coins");
            plugin.getLogger().info("Player " + player.getName() + " farm-coins balance: " + balance + ", required: " + amount);
            return balance >= amount;
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking farm-coins balance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public double getFarmCoinsBalance(Player player) {
        if (currencyAPI == null) {
            plugin.getLogger().severe("CurrencyAPI is null when getting balance!");
            return 0.0;
        }
        try {
            double balance = currencyAPI.getCurrency(player.getUniqueId(), "farm-coins");
            plugin.getLogger().info("Retrieved farm-coins balance for " + player.getName() + ": " + balance);
            return balance;
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting farm-coins balance: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    public boolean removeFarmCoins(Player player, double amount) {
        if (currencyAPI == null) {
            plugin.getLogger().severe("CurrencyAPI is null when removing farm-coins!");
            return false;
        }
        if (!hasFarmCoins(player, amount)) {
            plugin.getLogger().info("Player doesn't have enough farm-coins to remove");
            return false;
        }
        try {
            currencyAPI.removeCurrency(player.getUniqueId(), "farm-coins", amount);
            plugin.getLogger().info("Removed " + amount + " farm-coins from " + player.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error removing farm-coins: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void addFarmCoins(Player player, double amount) {
        if (currencyAPI == null) {
            plugin.getLogger().severe("CurrencyAPI is null when adding farm-coins!");
            return;
        }
        try {
            currencyAPI.addCurrency(player.getUniqueId(), "farm-coins", amount);
            plugin.getLogger().info("Added " + amount + " farm-coins to " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Error adding farm-coins: " + e.getMessage());
            e.printStackTrace();
        }
    }
}