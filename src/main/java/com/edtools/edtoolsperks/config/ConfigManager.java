package com.edtools.edtoolsperks.config;

import com.edtools.edtoolsperks.EdToolsPerks;
import com.edtools.edtoolsperks.utils.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final EdToolsPerks plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    public ConfigManager(EdToolsPerks plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Create plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Load main configs
        loadConfig("config.yml");
        loadConfig("perks.yml");
        loadConfig("guis.yml");
        loadConfig("messages.yml");

        MessageUtils.sendConsole("&aAll configurations loaded successfully!");
    }

    private void loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        // Copy from resources if it doesn't exist
        if (!configFile.exists()) {
            try (InputStream inputStream = plugin.getResource(fileName)) {
                if (inputStream != null) {
                    Files.copy(inputStream, configFile.toPath());
                    MessageUtils.sendConsole("&aCreated default " + fileName);
                } else {
                    MessageUtils.sendConsole("&cCouldn't find default " + fileName + " in resources!");
                }
            } catch (IOException e) {
                MessageUtils.sendConsole("&cError creating " + fileName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Load the configuration
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            configs.put(fileName, config);
            configFiles.put(fileName, configFile);
            MessageUtils.sendConsole("&aLoaded " + fileName);
        } catch (Exception e) {
            MessageUtils.sendConsole("&cError loading " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadConfigs() {
        configs.clear();
        configFiles.clear();
        loadConfigs();
    }

    public FileConfiguration getConfig() {
        return configs.get("config.yml");
    }

    public FileConfiguration getPerksConfig() {
        return configs.get("perks.yml");
    }

    public FileConfiguration getGUIsConfig() {
        return configs.get("guis.yml");
    }

    public FileConfiguration getMessagesConfig() {
        return configs.get("messages.yml");
    }

    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }

    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        File configFile = configFiles.get(fileName);
        
        if (config != null && configFile != null) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                MessageUtils.sendConsole("&cError saving " + fileName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public String getMessage(String path) {
        FileConfiguration messages = getMessagesConfig();
        if (messages != null) {
            return MessageUtils.colorize(messages.getString(path, "&cMessage not found: " + path));
        }
        return "&cMessages config not loaded!";
    }

    public String getMessage(String path, Object... replacements) {
        String message = getMessage(path);
        return MessageUtils.formatPlaceholders(message, replacements);
    }
}