package com.edtools.edtoolsperks.animation;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.stream.Collectors;

public class AnimationConfig {
    
    private final int durationTicks;
    private final int[] crystalSlots;
    private final int[] borderSlots;
    private final int toolSlot;
    private final Material[] crystalMaterials;
    private final Material borderMaterial;
    
    // Intensity phases
    private final double earlyPhase;
    private final double midPhase;
    private final int finalTicks;
    private final double resultProbability;
    
    // Sound effects
    private final Sound startSound;
    private final int tickInterval;
    private final Sound tickSound;
    private final float tickVolume;
    private final int finishDelayTicks;
    
    public AnimationConfig(FileConfiguration config) {
        this.durationTicks = config.getInt("animation.duration-ticks", 60);
        
        // Parse slots
        List<Integer> crystalSlotsList = config.getIntegerList("animation.crystal-slots");
        this.crystalSlots = crystalSlotsList.stream().mapToInt(Integer::intValue).toArray();
        
        List<Integer> borderSlotsList = config.getIntegerList("animation.border-slots");
        this.borderSlots = borderSlotsList.stream().mapToInt(Integer::intValue).toArray();
        
        this.toolSlot = config.getInt("animation.tool-slot", 22);
        
        // Parse materials
        List<String> crystalMaterialNames = config.getStringList("animation.crystal-materials");
        this.crystalMaterials = crystalMaterialNames.stream()
            .map(name -> {
                try {
                    return Material.valueOf(name.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return Material.WHITE_STAINED_GLASS_PANE;
                }
            })
            .toArray(Material[]::new);
        
        String borderMaterialName = config.getString("animation.border-material", "BLACK_STAINED_GLASS_PANE");
        Material tempBorderMaterial;
        try {
            tempBorderMaterial = Material.valueOf(borderMaterialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            tempBorderMaterial = Material.BLACK_STAINED_GLASS_PANE;
        }
        this.borderMaterial = tempBorderMaterial;
        
        // Intensity phases
        this.earlyPhase = config.getDouble("animation.intensity-phases.early", 0.3);
        this.midPhase = config.getDouble("animation.intensity-phases.mid", 0.7);
        this.finalTicks = config.getInt("animation.intensity-phases.final-ticks", 10);
        this.resultProbability = config.getDouble("animation.intensity-phases.result-probability", 0.7);
        
        // Sound effects
        String startSoundName = config.getString("animation.sound-effects.start", "BLOCK_BEACON_ACTIVATE");
        Sound tempStartSound;
        try {
            tempStartSound = Sound.valueOf(startSoundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            tempStartSound = Sound.BLOCK_BEACON_ACTIVATE;
        }
        this.startSound = tempStartSound;
        
        this.tickInterval = config.getInt("animation.sound-effects.tick-interval", 10);
        
        String tickSoundName = config.getString("animation.sound-effects.tick-sound", "UI_BUTTON_CLICK");
        Sound tempTickSound;
        try {
            tempTickSound = Sound.valueOf(tickSoundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            tempTickSound = Sound.UI_BUTTON_CLICK;
        }
        this.tickSound = tempTickSound;
        
        this.tickVolume = (float) config.getDouble("animation.sound-effects.tick-volume", 0.5);
        this.finishDelayTicks = config.getInt("animation.sound-effects.finish-delay-ticks", 40);
    }
    
    // Getters
    public int getDurationTicks() { return durationTicks; }
    public int[] getCrystalSlots() { return crystalSlots; }
    public int[] getBorderSlots() { return borderSlots; }
    public int getToolSlot() { return toolSlot; }
    public Material[] getCrystalMaterials() { return crystalMaterials; }
    public Material getBorderMaterial() { return borderMaterial; }
    
    public double getEarlyPhase() { return earlyPhase; }
    public double getMidPhase() { return midPhase; }
    public int getFinalTicks() { return finalTicks; }
    public double getResultProbability() { return resultProbability; }
    
    public Sound getStartSound() { return startSound; }
    public int getTickInterval() { return tickInterval; }
    public Sound getTickSound() { return tickSound; }
    public float getTickVolume() { return tickVolume; }
    public int getFinishDelayTicks() { return finishDelayTicks; }
    
    public Material getCrystalMaterial(int index) {
        if (crystalMaterials.length == 0) return Material.WHITE_STAINED_GLASS_PANE;
        return crystalMaterials[index % crystalMaterials.length];
    }
}