package com.edtools.edtoolsperks.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class ItemUtils {

    public static ItemStack createItem(String material, String name, String[] lore) {
        Material mat;
        try {
            mat = Material.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(MessageUtils.colorize(name));
            }
            
            if (lore != null) {
                meta.setLore(MessageUtils.colorize(Arrays.asList(lore)));
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    public static ItemStack createSkullItem(String texture, String name, String[] lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        if (meta != null) {
            // Set display name and lore
            if (name != null) {
                meta.setDisplayName(MessageUtils.colorize(name));
            }
            
            if (lore != null) {
                meta.setLore(MessageUtils.colorize(Arrays.asList(lore)));
            }
            
            // Set texture
            try {
                setSkullTexture(meta, texture);
            } catch (Exception e) {
                // If texture setting fails, just use default skull
            }
            
            skull.setItemMeta(meta);
        }
        
        return skull;
    }

    private static void setSkullTexture(SkullMeta meta, String texture) {
        try {
            // Decode base64 texture
            String decoded = new String(Base64.getDecoder().decode(texture));
            
            // Extract URL from the decoded JSON
            // Format: {"textures":{"SKIN":{"url":"http://..."}}}
            String urlStart = "\"url\":\"";
            int urlStartIndex = decoded.indexOf(urlStart) + urlStart.length();
            int urlEndIndex = decoded.indexOf("\"", urlStartIndex);
            
            if (urlStartIndex > urlStart.length() - 1 && urlEndIndex > urlStartIndex) {
                String textureUrl = decoded.substring(urlStartIndex, urlEndIndex);
                
                // Create player profile with texture
                PlayerProfile profile = meta.getOwnerProfile();
                if (profile == null) {
                    profile = org.bukkit.Bukkit.createPlayerProfile(UUID.randomUUID(), "");
                }
                
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URL(textureUrl));
                profile.setTextures(textures);
                
                meta.setOwnerProfile(profile);
            }
        } catch (Exception e) {
            // Silently fail - skull will just use default texture
        }
    }

    public static boolean isSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return item1 == item2;
        }
        
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        if (meta1 == null || meta2 == null) {
            return meta1 == meta2;
        }
        
        return meta1.getDisplayName().equals(meta2.getDisplayName());
    }

    public static Material parseMaterial(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            return Material.STONE;
        }

        // Handle texture format
        if (materialString.startsWith("texture-")) {
            return Material.PLAYER_HEAD;
        }

        try {
            return Material.valueOf(materialString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}