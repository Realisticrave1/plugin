package RavenMC.ravenRPG.Managers;

import RavenMC.ravenRPG.RavenRPG;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final RavenRPG plugin;
    private Connection connection;
    private String databaseType;

    public DatabaseManager(RavenRPG plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfig().getString("database.type", "yaml");
    }

    public void initialize() {
        if (databaseType.equalsIgnoreCase("mysql")) {
            connectToMySQL();
            setupTables();
        } else {
            plugin.getLogger().info("Using YAML for data storage.");
        }
    }

    private void connectToMySQL() {
        try {
            FileConfiguration config = plugin.getConfig();
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "ravenrpg");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&autoReconnect=true";

            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("Successfully connected to MySQL database!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            plugin.getLogger().warning("Falling back to YAML storage.");
            databaseType = "yaml";
        }
    }

    private void setupTables() {
        if (connection == null) return;

        try {
            // Create player data table
            PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_data (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "balance DOUBLE DEFAULT 0, " +
                            "race VARCHAR(32) DEFAULT 'human', " +
                            "mana INT DEFAULT 100, " +
                            "max_mana INT DEFAULT 100, " +
                            "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                            ");"
            );
            stmt.executeUpdate();

            // Create skills table
            stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_skills (" +
                            "uuid VARCHAR(36), " +
                            "skill_name VARCHAR(32), " +
                            "level INT DEFAULT 1, " +
                            "xp INT DEFAULT 0, " +
                            "PRIMARY KEY (uuid, skill_name), " +
                            "FOREIGN KEY (uuid) REFERENCES player_data(uuid) ON DELETE CASCADE" +
                            ");"
            );
            stmt.executeUpdate();

            // Create ravens table
            stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_ravens (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "raven_type VARCHAR(32) DEFAULT 'default', " +
                            "level INT DEFAULT 1, " +
                            "xp INT DEFAULT 0, " +
                            "color INT DEFAULT 0, " +
                            "FOREIGN KEY (uuid) REFERENCES player_data(uuid) ON DELETE CASCADE" +
                            ");"
            );
            stmt.executeUpdate();

            plugin.getLogger().info("Database tables created successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set up database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        // Check if connection is still valid
        if (databaseType.equalsIgnoreCase("mysql")) {
            try {
                if (connection == null || connection.isClosed()) {
                    connectToMySQL();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Lost connection to database: " + e.getMessage());
                return null;
            }
        }

        return connection;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
            }
        }
    }

    // MySQL implementation for saving player data would go here
    // This would be used by PlayerManager if MySQL is enabled

    // For simplicity, the current implementation uses YAML files
    // A full MySQL implementation would require modifying PlayerManager
    // to use the database instead of YAML files
}