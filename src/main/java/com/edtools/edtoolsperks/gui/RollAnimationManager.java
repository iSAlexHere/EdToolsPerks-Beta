package com.edtools.edtoolsperks.gui;

import com.edtools.edtoolsperks.EdToolsPerks;
import com.edtools.edtoolsperks.animation.AnimationConfig;
import com.edtools.edtoolsperks.perks.Perk;
import com.edtools.edtoolsperks.perks.PerkCategoryConfig;
import com.edtools.edtoolsperks.utils.ItemUtils;
import com.edtools.edtoolsperks.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

public class RollAnimationManager {

    private final EdToolsPerks plugin;
    private final Player player;
    private final Perk resultPerk;
    private final int resultLevel;
    private final boolean wasGuaranteed;
    private final AnimationConfig animationConfig;
    
    private Inventory animationInventory;
    private BukkitTask animationTask;
    private int animationTick = 0;

    public RollAnimationManager(EdToolsPerks plugin, Player player, Perk resultPerk, int resultLevel, boolean wasGuaranteed) {
        this.plugin = plugin;
        this.player = player;
        this.resultPerk = resultPerk;
        this.resultLevel = resultLevel;
        this.wasGuaranteed = wasGuaranteed;
        this.animationConfig = new AnimationConfig(plugin.getConfigManager().getConfig());
    }

    public void startAnimation() {
        plugin.getLogger().info("Starting animation for player: " + player.getName());
        
        // Mark player as in animation BEFORE opening the inventory
        plugin.getGuiManager().openGUIs.put(player.getUniqueId(), "animation");
        
        createAnimationInventory();
        plugin.getLogger().info("Opening animation inventory");
        player.openInventory(animationInventory);
        
        // Play start sound
        player.playSound(player.getLocation(), animationConfig.getStartSound(), 1.0f, 1.0f);
        
        // Start animation task
        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (animationTick >= animationConfig.getDurationTicks()) {
                    finishAnimation();
                    return;
                }
                
                updateAnimation();
                animationTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        plugin.getLogger().info("Animation started successfully");
    }

    private void createAnimationInventory() {
        animationInventory = Bukkit.createInventory(null, 54, MessageUtils.colorize("&8Rolling..."));
        
        // Border
        ItemStack border = ItemUtils.createItem(animationConfig.getBorderMaterial().name(), "&f", null);
        for (int slot : animationConfig.getBorderSlots()) {
            animationInventory.setItem(slot, border);
        }
        
        // Tool display in center
        ItemStack toolDisplay = player.getInventory().getItemInMainHand().clone();
        if (toolDisplay.getType() == Material.AIR) {
            toolDisplay = ItemUtils.createItem("DIAMOND_HOE", "&bTool", new String[]{"&7Rolling for perk..."});
        }
        animationInventory.setItem(animationConfig.getToolSlot(), toolDisplay);
        
        // Initial crystal setup
        for (int slot : animationConfig.getCrystalSlots()) {
            ItemStack crystal = ItemUtils.createItem(animationConfig.getCrystalMaterial(0).name(), "&f", null);
            animationInventory.setItem(slot, crystal);
        }
    }

    private void updateAnimation() {
        // Update crystals with random colors (more intense towards the end)
        double intensity = (double) animationTick / animationConfig.getDurationTicks();
        
        for (int slot : animationConfig.getCrystalSlots()) {
            Material crystalMaterial;
            
            if (intensity < animationConfig.getEarlyPhase()) {
                // Early animation - mostly early materials
                int maxMaterials = Math.max(1, (int)(animationConfig.getCrystalMaterials().length * 0.4));
                crystalMaterial = animationConfig.getCrystalMaterial(ThreadLocalRandom.current().nextInt(maxMaterials));
            } else if (intensity < animationConfig.getMidPhase()) {
                // Mid animation - more colors
                int maxMaterials = Math.max(1, (int)(animationConfig.getCrystalMaterials().length * 0.7));
                crystalMaterial = animationConfig.getCrystalMaterial(ThreadLocalRandom.current().nextInt(maxMaterials));
            } else {
                // Late animation - all colors including result category colors
                if (animationTick > animationConfig.getDurationTicks() - animationConfig.getFinalTicks()) {
                    // Final ticks - show result category color more often
                    PerkCategoryConfig category = plugin.getPerkManager().getCategory(resultPerk.getCategory());
                    if (ThreadLocalRandom.current().nextDouble() < animationConfig.getResultProbability()) {
                        crystalMaterial = category.getGlassColor();
                    } else {
                        crystalMaterial = animationConfig.getCrystalMaterial(
                            ThreadLocalRandom.current().nextInt(animationConfig.getCrystalMaterials().length));
                    }
                } else {
                    crystalMaterial = animationConfig.getCrystalMaterial(
                        ThreadLocalRandom.current().nextInt(animationConfig.getCrystalMaterials().length));
                }
            }
            
            ItemStack crystal = ItemUtils.createItem(crystalMaterial.name(), "&f", null);
            animationInventory.setItem(slot, crystal);
        }
        
        // Play occasional sound effects
        if (animationTick % animationConfig.getTickInterval() == 0) {
            player.playSound(player.getLocation(), animationConfig.getTickSound(), 
                animationConfig.getTickVolume(), 1.0f + (float)(intensity * 0.5));
        }
    }

    private void finishAnimation() {
        animationTask.cancel();
        
        // Set final crystal colors to result category
        PerkCategoryConfig category = plugin.getPerkManager().getCategory(resultPerk.getCategory());
        ItemStack finalCrystal = ItemUtils.createItem(category.getGlassColor().name(), 
            category.getColor() + category.getDisplayName(), null);
            
        for (int slot : animationConfig.getCrystalSlots()) {
            animationInventory.setItem(slot, finalCrystal);
        }
        
        // Play result sound based on category
        Sound resultSound = getResultSound(category);
        player.playSound(player.getLocation(), resultSound, 1.0f, 1.0f);
        
        // Apply the perk to the tool
        applyPerkToTool();
        
        // Show result message
        String message = wasGuaranteed ? "roll.guaranteed-perk" : "roll.perk-applied";
        MessageUtils.send(player, plugin.getConfigManager().getMessage(message,
            "category_color", category.getColor(),
            "perk_name", resultPerk.getDisplayName(),
            "level", resultLevel));
        
        // Wait a moment then return to main GUI
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getGuiManager().openMainGUI(player);
        }, animationConfig.getFinishDelayTicks());
    }

    private Sound getResultSound(PerkCategoryConfig category) {
        // Get sound from configuration
        String soundName = plugin.getConfigManager().getConfig().getString("sounds.category-sounds." + category.getId());
        
        if (soundName != null) {
            try {
                return Sound.valueOf(soundName.toUpperCase());
            } catch (IllegalArgumentException e) {
                MessageUtils.sendConsole("&cInvalid sound '" + soundName + "' for category '" + category.getId() + "', using default");
            }
        } else {
            MessageUtils.sendConsole("&cNo sound configured for category '" + category.getId() + "', using default");
        }
        
        // Default fallback sound if configuration is missing or invalid
        String defaultSound = plugin.getConfigManager().getConfig().getString("sounds.roll-end-common", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try {
            return Sound.valueOf(defaultSound.toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtils.sendConsole("&cDefault sound '" + defaultSound + "' is invalid, using ENTITY_EXPERIENCE_ORB_PICKUP");
            return Sound.ENTITY_EXPERIENCE_ORB_PICKUP; // Last resort hardcoded fallback
        }
    }

    private void applyPerkToTool() {
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        // Apply perk via EdTools integration and NBT
        plugin.getEdToolsIntegration().applyPerkToTool(tool, resultPerk, resultLevel);
        
        // Save to database (wait for completion to ensure synchronization)
        String toolUUID = plugin.getEdToolsIntegration().getToolUUID(tool);
        if (toolUUID != null) {
            try {
                // Wait for database save to complete before proceeding
                plugin.getDatabaseManager().savePerkToTool(toolUUID, player.getUniqueId(), resultPerk.getId(), resultLevel).join();
                plugin.getDatabaseManager().saveRollHistory(player.getUniqueId(), resultPerk.getId(), resultLevel, resultPerk.getCategory(), wasGuaranteed);
                plugin.getLogger().info("Perk saved to database successfully: " + resultPerk.getId() + ":" + resultLevel);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save perk to database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void stopAnimation() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }
    }
}