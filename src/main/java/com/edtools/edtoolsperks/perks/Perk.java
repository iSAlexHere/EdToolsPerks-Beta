package com.edtools.edtoolsperks.perks;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Perk {
    
    private final String id;
    private final String displayName;
    private final String description;
    private final String category;
    private final String tool;
    private final double chance;
    private final Map<Integer, PerkLevel> levels;
    private final Material displayMaterial;
    private final List<String> lore;

    public Perk(String id, String displayName, String description, String category, 
                String tool, double chance, Map<Integer, PerkLevel> levels,
                Material displayMaterial, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.tool = tool;
        this.chance = chance;
        this.levels = levels;
        this.displayMaterial = displayMaterial;
        this.lore = lore;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getTool() {
        return tool;
    }

    public double getChance() {
        return chance;
    }

    public Map<Integer, PerkLevel> getLevels() {
        return levels;
    }

    public PerkLevel getLevel(int level) {
        return levels.get(level);
    }

    public int getMaxLevel() {
        return levels.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    public Material getDisplayMaterial() {
        return displayMaterial != null ? displayMaterial : Material.ENCHANTED_BOOK;
    }

    public List<String> getLore() {
        return lore;
    }

    public static class PerkLevel {
        private final String boostType;
        private final String boostAmount;
        private final Map<String, Double> parsedBoosts;

        public PerkLevel(String boostType, String boostAmount) {
            this.boostType = boostType;
            this.boostAmount = boostAmount;
            this.parsedBoosts = parseBoosts(boostType, boostAmount);
        }

        private Map<String, Double> parseBoosts(String types, String amounts) {
            Map<String, Double> boosts = new HashMap<>();
            
            if (types == null || amounts == null) {
                return boosts;
            }
            
            String[] typeArray = types.split(",");
            String[] amountArray = amounts.split(",");
            
            for (int i = 0; i < typeArray.length && i < amountArray.length; i++) {
                String type = typeArray[i].trim();
                try {
                    double amount = Double.parseDouble(amountArray[i].trim());
                    boosts.put(type, amount);
                } catch (NumberFormatException e) {
                    // Skip invalid amounts
                }
            }
            
            return boosts;
        }

        public String getBoostType() {
            return boostType;
        }

        public String getBoostAmount() {
            return boostAmount;
        }

        public Map<String, Double> getParsedBoosts() {
            return parsedBoosts;
        }

        public double getBoostForType(String type) {
            return parsedBoosts.getOrDefault(type.toLowerCase(), 0.0);
        }

        public boolean hasBoostType(String type) {
            return parsedBoosts.containsKey(type.toLowerCase());
        }
    }
}