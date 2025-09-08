package com.edtools.edtoolsperks.database;

import com.edtools.edtoolsperks.EdToolsPerks;
import com.edtools.edtoolsperks.utils.MessageUtils;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final EdToolsPerks plugin;
    private Connection connection;
    private final String url;

    public DatabaseManager(EdToolsPerks plugin) {
        this.plugin = plugin;
        String dbFile = plugin.getConfigManager().getConfig().getString("database.file", "edtoolsperks.db");
        this.url = "jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/" + dbFile + ";MODE=MySQL";
    }

    public void initialize() {
        try {
            Class.forName("org.h2.Driver");
            connect();
            createTables();
            MessageUtils.sendConsole("&aDatabase initialized successfully!");
        } catch (Exception e) {
            MessageUtils.sendConsole("&cError initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        connection = DriverManager.getConnection(url);
    }

    private void createTables() throws SQLException {
        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS players (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                total_rolls INT DEFAULT 0,
                current_rolls INT DEFAULT 0,
                pity_counter INT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createPerksTable = """
            CREATE TABLE IF NOT EXISTS tool_perks (
                id INT AUTO_INCREMENT PRIMARY KEY,
                tool_uuid VARCHAR(36) NOT NULL,
                owner_uuid VARCHAR(36) NOT NULL,
                perk_name VARCHAR(50) NOT NULL,
                perk_level INT DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_tool_perk (tool_uuid, perk_name)
            )
            """;

        String createRollHistoryTable = """
            CREATE TABLE IF NOT EXISTS roll_history (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                perk_name VARCHAR(50) NOT NULL,
                perk_level INT NOT NULL,
                perk_category VARCHAR(20) NOT NULL,
                was_guaranteed BOOLEAN DEFAULT FALSE,
                roll_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_player_uuid (player_uuid),
                INDEX idx_roll_time (roll_time)
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createPerksTable);
            stmt.execute(createRollHistoryTable);
        }
    }

    // Player data methods
    public CompletableFuture<Void> createPlayer(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT IGNORE INTO players (uuid, username) VALUES (?, ?)";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, username);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Integer> getPlayerRolls(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT current_rolls FROM players WHERE uuid = ?";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("current_rolls");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }

    public CompletableFuture<Void> setPlayerRolls(UUID uuid, int rolls) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE players SET current_rolls = ? WHERE uuid = ?";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, rolls);
                    stmt.setString(2, uuid.toString());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> addPlayerRolls(UUID uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                connect();
                
                // First ensure player exists
                String ensurePlayerSql = "INSERT IGNORE INTO players (uuid, username, current_rolls, total_rolls, pity_counter) VALUES (?, 'Unknown', 0, 0, 0)";
                try (PreparedStatement ensureStmt = connection.prepareStatement(ensurePlayerSql)) {
                    ensureStmt.setString(1, uuid.toString());
                    int inserted = ensureStmt.executeUpdate();
                    if (inserted > 0) {
                        plugin.getLogger().info("Created new player record for UUID: " + uuid);
                    }
                }
                
                // Then update rolls
                String updateSql = "UPDATE players SET current_rolls = current_rolls + ? WHERE uuid = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, amount);
                    updateStmt.setString(2, uuid.toString());
                    int rowsAffected = updateStmt.executeUpdate();
                    plugin.getLogger().info("addPlayerRolls: Added " + amount + " rolls to player " + uuid + ", rows affected: " + rowsAffected);
                    
                    if (rowsAffected == 0) {
                        plugin.getLogger().severe("ERROR: Failed to add rolls - no rows affected!");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("ERROR in addPlayerRolls: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Integer> getTotalRolls(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT total_rolls FROM players WHERE uuid = ?";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("total_rolls");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }

    public CompletableFuture<Void> incrementTotalRolls(UUID uuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                connect();
                
                // First ensure player exists
                String ensurePlayerSql = "INSERT IGNORE INTO players (uuid, username, current_rolls, total_rolls, pity_counter) VALUES (?, 'Unknown', 0, 0, 0)";
                try (PreparedStatement ensureStmt = connection.prepareStatement(ensurePlayerSql)) {
                    ensureStmt.setString(1, uuid.toString());
                    int inserted = ensureStmt.executeUpdate();
                    if (inserted > 0) {
                        plugin.getLogger().info("Created new player record for UUID: " + uuid);
                    }
                }
                
                // Then update total rolls
                String updateSql = "UPDATE players SET total_rolls = total_rolls + ? WHERE uuid = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, amount);
                    updateStmt.setString(2, uuid.toString());
                    int rowsAffected = updateStmt.executeUpdate();
                    plugin.getLogger().info("incrementTotalRolls: Added " + amount + " to total rolls for player " + uuid + ", rows affected: " + rowsAffected);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("ERROR in incrementTotalRolls: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Integer> getPityCounter(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT pity_counter FROM players WHERE uuid = ?";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("pity_counter");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }

    public CompletableFuture<Void> setPityCounter(UUID uuid, int counter) {
        return CompletableFuture.runAsync(() -> {
            try {
                connect();
                
                // First ensure player exists
                String ensurePlayerSql = "INSERT IGNORE INTO players (uuid, username, current_rolls, total_rolls, pity_counter) VALUES (?, 'Unknown', 0, 0, 0)";
                try (PreparedStatement ensureStmt = connection.prepareStatement(ensurePlayerSql)) {
                    ensureStmt.setString(1, uuid.toString());
                    int inserted = ensureStmt.executeUpdate();
                    if (inserted > 0) {
                        plugin.getLogger().info("Created new player record for UUID: " + uuid);
                    }
                }
                
                // Then update pity counter
                String updateSql = "UPDATE players SET pity_counter = ? WHERE uuid = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, counter);
                    updateStmt.setString(2, uuid.toString());
                    int rowsAffected = updateStmt.executeUpdate();
                    plugin.getLogger().info("setPityCounter: Set pity counter to " + counter + " for player " + uuid + ", rows affected: " + rowsAffected);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("ERROR in setPityCounter: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Tool perk methods
    public CompletableFuture<Void> savePerkToTool(String toolUuid, UUID ownerUuid, String perkName, int level) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO tool_perks (tool_uuid, owner_uuid, perk_name, perk_level) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE perk_level = ?";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, toolUuid);
                    stmt.setString(2, ownerUuid.toString());
                    stmt.setString(3, perkName);
                    stmt.setInt(4, level);
                    stmt.setInt(5, level);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<String> getToolPerk(String toolUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT perk_name, perk_level FROM tool_perks WHERE tool_uuid = ?";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, toolUuid);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getString("perk_name") + ":" + rs.getInt("perk_level");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Void> removeToolPerk(String toolUuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM tool_perks WHERE tool_uuid = ?";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, toolUuid);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Roll history methods
    public CompletableFuture<Void> saveRollHistory(UUID playerUuid, String perkName, int level, String category, boolean guaranteed) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO roll_history (player_uuid, perk_name, perk_level, perk_category, was_guaranteed) VALUES (?, ?, ?, ?, ?)";
            try {
                connect();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, perkName);
                    stmt.setInt(3, level);
                    stmt.setString(4, category);
                    stmt.setBoolean(5, guaranteed);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                MessageUtils.sendConsole("&aDatabase connection closed.");
            }
        } catch (SQLException e) {
            MessageUtils.sendConsole("&cError closing database connection: " + e.getMessage());
        }
    }
}