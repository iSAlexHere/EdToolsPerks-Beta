package com.edtools.edtoolsperks.perks;

import com.edtools.edtoolsperks.EdToolsPerks;
import com.edtools.edtoolsperks.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class PerkManager {

    private final EdToolsPerks plugin;
    private final Map<String, Perk> perks = new HashMap<>();
    private final Map<String, List<Perk>> perksByCategory = new HashMap<>();
    private final Map<String, PerkCategoryConfig> categories = new HashMap<>();

    public PerkManager(EdToolsPerks plugin) {
        this.plugin = plugin;
    }

    public void loadPerks() {
        perks.clear();
        perksByCategory.clear();
        categories.clear();

        FileConfiguration perksConfig = plugin.getConfigManager().getPerksConfig();
        if (perksConfig == null) {
            MessageUtils.sendConsole("&cPerks configuration not found!");
            return;
        }

        // Load categories first
        loadCategories(perksConfig);

        // Load perks
        ConfigurationSection perksSection = perksConfig.getConfigurationSection("perks");
        if (perksSection == null) {
            MessageUtils.sendConsole("&cNo perks section found in perks.yml!");
            return;
        }

        int loadedPerks = 0;
        for (String perkId : perksSection.getKeys(false)) {
            ConfigurationSection perkSection = perksSection.getConfigurationSection(perkId);
            if (perkSection != null) {
                Perk perk = loadPerk(perkId, perkSection);
                if (perk != null) {
                    perks.put(perkId, perk);
                    
                    // Add to category list
                    String category = perk.getCategory();
                    perksByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(perk);
                    
                    loadedPerks++;
                }
            }
        }

        MessageUtils.sendConsole("&aLoaded " + loadedPerks + " perks across " + categories.size() + " categories!");
    }

    private void loadCategories(FileConfiguration config) {
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) {
            MessageUtils.sendConsole("&cNo categories section found in perks.yml! Using defaults.");
            loadDefaultCategories();
            return;
        }

        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
            if (categorySection != null) {
                String displayName = categorySection.getString("display-name", categoryId);
                String color = categorySection.getString("color", "&f");
                String glassColorStr = categorySection.getString("glass-color", "WHITE_STAINED_GLASS_PANE");
                
                Material glassColor;
                try {
                    glassColor = Material.valueOf(glassColorStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    MessageUtils.sendConsole("&cInvalid glass color '" + glassColorStr + "' for category '" + categoryId + "', using WHITE_STAINED_GLASS_PANE");
                    glassColor = Material.WHITE_STAINED_GLASS_PANE;
                }

                PerkCategoryConfig categoryConfig = new PerkCategoryConfig(categoryId, displayName, color, glassColor);
                categories.put(categoryId, categoryConfig);
                
                MessageUtils.sendConsole("&aLoaded category: " + categoryId + " (" + displayName + ")");
            }
        }
    }

    private void loadDefaultCategories() {
        // Default categories as fallback
        categories.put("verde", new PerkCategoryConfig("verde", "&aCommon", "&a", Material.LIME_STAINED_GLASS_PANE));
        categories.put("azul-clarito", new PerkCategoryConfig("azul-clarito", "&bUncommon", "&b", Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        categories.put("azul-oscuro", new PerkCategoryConfig("azul-oscuro", "&9Rare", "&9", Material.BLUE_STAINED_GLASS_PANE));
        categories.put("roja", new PerkCategoryConfig("roja", "&cEpic", "&c", Material.RED_STAINED_GLASS_PANE));
        categories.put("morada", new PerkCategoryConfig("morada", "&5Legendary", "&5", Material.PURPLE_STAINED_GLASS_PANE));
        categories.put("naranja", new PerkCategoryConfig("naranja", "&6Ultimate", "&6", Material.ORANGE_STAINED_GLASS_PANE));
    }

    private Perk loadPerk(String perkId, ConfigurationSection section) {
        try {
            FileConfiguration mainConfig = plugin.getConfigManager().getConfig();
            
            // Get defaults from configuration
            String defaultDisplayName = mainConfig.getString("perk-defaults.display-name", "");
            String defaultDescription = mainConfig.getString("perk-defaults.description", "");
            String defaultCategory = mainConfig.getString("perk-defaults.category", "verde");
            String defaultTool = mainConfig.getString("perk-defaults.tool", "hoe");
            double defaultChance = mainConfig.getDouble("perk-defaults.chance", 1.0);
            String defaultMaterial = mainConfig.getString("perk-defaults.material", "ENCHANTED_BOOK");
            
            // Use configured defaults
            String displayName = section.getString("display-name", defaultDisplayName.isEmpty() ? perkId : defaultDisplayName);
            String description = section.getString("description", defaultDescription);
            String category = section.getString("category", defaultCategory);
            String tool = section.getString("tool", defaultTool);
            double chance = section.getDouble("chance", defaultChance);
            String materialName = section.getString("material", defaultMaterial);

            // Parse material
            Material displayMaterial;
            try {
                displayMaterial = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                MessageUtils.sendConsole("&cInvalid material '" + materialName + "' for perk '" + perkId + "', using ENCHANTED_BOOK");
                displayMaterial = Material.ENCHANTED_BOOK;
            }

            // Load levels
            Map<Integer, Perk.PerkLevel> levels = new HashMap<>();
            ConfigurationSection levelsSection = section.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String levelStr : levelsSection.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelStr);
                        ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelStr);
                        if (levelSection != null) {
                            String boostType = levelSection.getString("boost-type", "");
                            String boostAmount = String.valueOf(levelSection.get("boost-amount", "0"));
                            
                            levels.put(level, new Perk.PerkLevel(boostType, boostAmount));
                        }
                    } catch (NumberFormatException e) {
                        MessageUtils.sendConsole("&cInvalid level number '" + levelStr + "' for perk " + perkId);
                    }
                }
            }

            // Create lore
            List<String> lore = new ArrayList<>();
            lore.add("&7" + description);
            lore.add("");
            
            PerkCategoryConfig perkCategory = getCategory(category);
            lore.add(perkCategory.getColor() + perkCategory.getDisplayName());
            lore.add("&7Chance: &f" + chance + "%");
            lore.add("");
            
            if (!levels.isEmpty()) {
                lore.add("&eLevels:");
                for (Map.Entry<Integer, Perk.PerkLevel> entry : levels.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                    int level = entry.getKey();
                    Perk.PerkLevel perkLevel = entry.getValue();
                    lore.add("&7Level " + level + ": &f" + formatBoostDisplay(perkLevel));
                }
            }

            return new Perk(perkId, displayName, description, category, tool, chance, levels, displayMaterial, lore);

        } catch (Exception e) {
            MessageUtils.sendConsole("&cError loading perk '" + perkId + "': " + e.getMessage());
            return null;
        }
    }

    private String formatBoostDisplay(Perk.PerkLevel level) {
        Map<String, Double> boosts = level.getParsedBoosts();
        if (boosts.isEmpty()) {
            return level.getBoostAmount();
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Double> entry : boosts.entrySet()) {
            String type = entry.getKey();
            Double amount = entry.getValue();
            parts.add(capitalizeFirst(type) + " +" + amount + "%");
        }
        
        return String.join(", ", parts);
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public Perk getPerk(String perkId) {
        return perks.get(perkId);
    }

    public Collection<Perk> getAllPerks() {
        return perks.values();
    }

    public List<Perk> getPerksByCategory(String category) {
        return perksByCategory.getOrDefault(category, new ArrayList<>());
    }

    public Map<String, PerkCategoryConfig> getCategories() {
        return categories;
    }

    public PerkCategoryConfig getCategory(String categoryId) {
        return categories.getOrDefault(categoryId, categories.get("verde"));
    }

    public Perk rollRandomPerk(boolean guaranteePurple) {
        plugin.getLogger().info("Rolling random perk - Guarantee purple: " + guaranteePurple);
        plugin.getLogger().info("Total perks available: " + perks.size());
        
        if (guaranteePurple) {
            List<Perk> purplePerks = getPerksByCategory("morada");
            plugin.getLogger().info("Purple perks available: " + purplePerks.size());
            if (!purplePerks.isEmpty()) {
                Perk selected = purplePerks.get(ThreadLocalRandom.current().nextInt(purplePerks.size()));
                plugin.getLogger().info("Selected guaranteed purple perk: " + selected.getDisplayName());
                return selected;
            }
        }

        // Calculate total weight based on chances
        double totalWeight = perks.values().stream()
                .mapToDouble(Perk::getChance)
                .sum();

        plugin.getLogger().info("Total weight for random selection: " + totalWeight);

        if (totalWeight <= 0) {
            // Fallback to random selection
            plugin.getLogger().info("Using fallback random selection");
            List<Perk> allPerks = new ArrayList<>(perks.values());
            if (allPerks.isEmpty()) {
                plugin.getLogger().severe("ERROR: No perks available at all!");
                return null;
            }
            Perk selected = allPerks.get(ThreadLocalRandom.current().nextInt(allPerks.size()));
            plugin.getLogger().info("Selected fallback perk: " + selected.getDisplayName());
            return selected;
        }

        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double currentWeight = 0;

        plugin.getLogger().info("Random value: " + random + " out of " + totalWeight);

        for (Perk perk : perks.values()) {
            currentWeight += perk.getChance();
            if (random <= currentWeight) {
                plugin.getLogger().info("Selected weighted perk: " + perk.getDisplayName());
                return perk;
            }
        }

        // Fallback
        Perk fallback = perks.values().iterator().next();
        plugin.getLogger().info("Using final fallback perk: " + fallback.getDisplayName());
        return fallback;
    }

    public int rollPerkLevel(Perk perk) {
        if (perk == null || perk.getLevels().isEmpty()) {
            return 1;
        }

        // For now, randomly select from available levels
        // You could implement weighted level selection here
        List<Integer> availableLevels = new ArrayList<>(perk.getLevels().keySet());
        return availableLevels.get(ThreadLocalRandom.current().nextInt(availableLevels.size()));
    }

    public List<Perk> getPerksForTool(String tool) {
        return perks.values().stream()
                .filter(perk -> perk.getTool().equalsIgnoreCase(tool))
                .collect(Collectors.toList());
    }
}