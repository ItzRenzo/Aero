package me.itzrenzo.aero.database;

import me.itzrenzo.aero.Aero;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    
    private final Aero plugin;
    private Connection connection;
    private String databaseType;
    
    public DatabaseManager(Aero plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
    }
    
    public void initialize() {
        try {
            if (databaseType.equals("mysql")) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }
            createTables();
            plugin.getLogger().info("Database initialized successfully using " + databaseType.toUpperCase());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeSQLite() throws SQLException {
        String filename = plugin.getConfig().getString("database.sqlite.filename", "aero.db");
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/" + filename;
        
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        
        connection = DriverManager.getConnection(url);
        
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("SQLite connection established: " + url);
        }
    }
    
    private void initializeMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "aero");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");
        
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true", 
                                 host, port, database);
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
        
        connection = DriverManager.getConnection(url, username, password);
        
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("MySQL connection established: " + url);
        }
    }
    
    private void createTables() throws SQLException {
        String createTableSQL;
        
        if (databaseType.equals("mysql")) {
            createTableSQL = """
                CREATE TABLE IF NOT EXISTS aero_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    total_fly_time BIGINT DEFAULT 0,
                    current_session_time INT DEFAULT 0,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        } else {
            createTableSQL = """
                CREATE TABLE IF NOT EXISTS aero_players (
                    uuid TEXT PRIMARY KEY,
                    username TEXT NOT NULL,
                    total_fly_time INTEGER DEFAULT 0,
                    current_session_time INTEGER DEFAULT 0,
                    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
        }
        
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("Database tables created/verified");
        }
    }
    
    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            plugin.getLogger().info("Database connection lost, reconnecting...");
            initialize();
        }
    }
    
    public CompletableFuture<Void> savePlayerData(Player player, int currentSessionTime) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnection();
                
                String sql = """
                    INSERT INTO aero_players (uuid, username, current_session_time, last_seen) 
                    VALUES (?, ?, ?, ?) 
                    ON DUPLICATE KEY UPDATE 
                    username = VALUES(username), 
                    current_session_time = VALUES(current_sessionTime), 
                    last_seen = VALUES(last_seen)
                    """;
                
                if (databaseType.equals("sqlite")) {
                    sql = """
                        INSERT OR REPLACE INTO aero_players (uuid, username, current_session_time, last_seen) 
                        VALUES (?, ?, ?, datetime('now'))
                        """;
                }
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, player.getName());
                    stmt.setInt(3, currentSessionTime);
                    
                    if (databaseType.equals("mysql")) {
                        stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    }
                    
                    stmt.executeUpdate();
                    
                    if (plugin.getConfig().getBoolean("settings.debug", false)) {
                        plugin.getLogger().info("Saved data for player: " + player.getName());
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public CompletableFuture<Integer> loadPlayerSessionTime(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnection();
                
                String sql = "SELECT current_session_time FROM aero_players WHERE uuid = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int sessionTime = rs.getInt("current_session_time");
                            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                                plugin.getLogger().info("Loaded session time " + sessionTime + " for: " + playerUUID);
                            }
                            return sessionTime;
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player session time: " + e.getMessage());
                e.printStackTrace();
            }
            return 0;
        });
    }
    
    public CompletableFuture<Void> addToTotalFlyTime(UUID playerUUID, int timeToAdd) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnection();
                
                String sql = """
                    UPDATE aero_players 
                    SET total_fly_time = total_fly_time + ?, current_session_time = 0 
                    WHERE uuid = ?
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, timeToAdd);
                    stmt.setString(2, playerUUID.toString());
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected == 0) {
                        // Player doesn't exist in database, create entry
                        String insertSql = """
                            INSERT INTO aero_players (uuid, username, total_fly_time, current_session_time) 
                            VALUES (?, 'Unknown', ?, 0)
                            """;
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                            insertStmt.setString(1, playerUUID.toString());
                            insertStmt.setInt(2, timeToAdd);
                            insertStmt.executeUpdate();
                        }
                    }
                    
                    if (plugin.getConfig().getBoolean("settings.debug", false)) {
                        plugin.getLogger().info("Added " + timeToAdd + " seconds to total fly time for: " + playerUUID);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update total fly time: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public CompletableFuture<Long> getTotalFlyTime(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnection();
                
                String sql = "SELECT total_fly_time FROM aero_players WHERE uuid = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("total_fly_time");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get total fly time: " + e.getMessage());
                e.printStackTrace();
            }
            return 0L;
        });
    }
    
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
            }
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}