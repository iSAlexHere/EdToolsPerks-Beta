package com.edtools.edtoolsperks;

import com.edtools.edtoolsperks.commands.MainCommand;
import com.edtools.edtoolsperks.config.ConfigManager;
import com.edtools.edtoolsperks.database.DatabaseManager;
import com.edtools.edtoolsperks.gui.GUIManager;
import com.edtools.edtoolsperks.integration.EdToolsIntegration;
import com.edtools.edtoolsperks.listeners.InventoryListener;
import com.edtools.edtoolsperks.listeners.PlayerListener;
import com.edtools.edtoolsperks.perks.PerkManager;
import com.edtools.edtoolsperks.utils.MessageUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class EdToolsPerks extends JavaPlugin {

    private static EdToolsPerks instance;
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PerkManager perkManager;
    private GUIManager guiManager;
    private EdToolsIntegration edToolsIntegration;

    @Override
    public void onEnable() {
        instance = this;
        
        // Inicializar configuraciones primero
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Verificar dependencias
        if (!checkDependencies()) {
            getLogger().severe(configManager.getMessage("plugin.dependencies-missing"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Inicializar componentes
        initializeComponents();
        
        // Registrar listeners
        registerListeners();
        
        // Registrar comandos
        registerCommands();
        
        MessageUtils.sendConsole(configManager.getMessage("plugin.enabled"));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        if (configManager != null) {
            MessageUtils.sendConsole(configManager.getMessage("plugin.disabled"));
        } else {
            MessageUtils.sendConsole("&cEdToolsPerks deshabilitado.");
        }
        instance = null;
    }

    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("EdTools") == null) {
            getLogger().severe(configManager.getMessage("plugin.edtools-missing"));
            return false;
        }
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe(configManager.getMessage("plugin.placeholderapi-missing"));
            return false;
        }
        
        return true;
    }

    private void initializeComponents() {
        // La configuración ya se inicializó en onEnable()
        
        // Base de datos
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        // Perks
        perkManager = new PerkManager(this);
        perkManager.loadPerks();
        
        // GUI
        guiManager = new GUIManager(this);
        guiManager.loadGUIs();
        
        // Integración con EdTools
        edToolsIntegration = new EdToolsIntegration(this);
        edToolsIntegration.initialize();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    private void registerCommands() {
        getCommand("edtoolsperks").setExecutor(new MainCommand(this));
    }

    // Getters
    public static EdToolsPerks getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PerkManager getPerkManager() {
        return perkManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public EdToolsIntegration getEdToolsIntegration() {
        return edToolsIntegration;
    }

    public NamespacedKey getKey(String key) {
        return new NamespacedKey(this, key);
    }
}