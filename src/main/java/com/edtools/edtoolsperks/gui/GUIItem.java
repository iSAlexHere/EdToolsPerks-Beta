package com.edtools.edtoolsperks.gui;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class GUIItem {
    
    private final String material;
    private final String name;
    private final List<String> lore;
    private final List<Integer> slots;
    private final Map<String, List<String>> actions;
    
    public GUIItem(String material, String name, List<String> lore, List<Integer> slots, Map<String, List<String>> actions) {
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.slots = slots;
        this.actions = actions;
    }
    
    public String getMaterial() {
        return material;
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public List<Integer> getSlots() {
        return slots;
    }
    
    public Map<String, List<String>> getActions() {
        return actions;
    }
    
    public List<String> getActions(String clickType) {
        return actions.getOrDefault(clickType, actions.getOrDefault("click", List.of()));
    }
}