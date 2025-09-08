package com.edtools.edtoolsperks.perks;

import org.bukkit.Material;

public class PerkCategoryConfig {
    
    private final String id;
    private final String displayName;
    private final String color;
    private final Material glassColor;

    public PerkCategoryConfig(String id, String displayName, String color, Material glassColor) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.glassColor = glassColor;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public Material getGlassColor() {
        return glassColor;
    }

    @Override
    public String toString() {
        return "PerkCategoryConfig{id='" + id + "', displayName='" + displayName + "'}";
    }
}