package dev.zm.rankup.storage;

import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {

    private static final String JDBC_PREFIX = "jdbc:h2:file:";

    private final zMRankup plugin;
    private final String dbPath;

    public StorageManager(zMRankup plugin) {
        this.plugin = plugin;
        this.dbPath = new File(plugin.getDataFolder(), "data").getAbsolutePath();
    }

    public void initialize() {
        try {
            Class.forName("org.h2.Driver");
            plugin.getDataFolder().mkdirs();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "current_rank_id VARCHAR(64)," +
                        "total_rankups INT DEFAULT 0," +
                        "last_rankup_time BIGINT DEFAULT 0," +
                        "player_kills BIGINT DEFAULT 0," +
                        "deaths BIGINT DEFAULT 0," +
                        "total_blocks_mined BIGINT DEFAULT 0," +
                        "total_mob_kills BIGINT DEFAULT 0," +
                        "mob_kills CLOB DEFAULT '{}'," +
                        "blocks_mined CLOB DEFAULT '{}'," +
                        "system_ranks CLOB DEFAULT '{}'" +
                        ")");

                if (!hasColumn(conn, "PLAYER_DATA", "SYSTEM_RANKS")) {
                    stmt.execute("ALTER TABLE player_data ADD COLUMN system_ranks CLOB DEFAULT '{}'");
                    if (hasColumn(conn, "PLAYER_DATA", "CURRENT_RANK_ID")) {
                        stmt.execute("UPDATE player_data SET system_ranks = CONCAT('{\"rankups\":\"', current_rank_id, '\"}') WHERE current_rank_id IS NOT NULL");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize H2 storage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        // Connections are opened per operation and closed immediately.
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_PREFIX + dbPath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE LOWER(TABLE_NAME) = LOWER(?) AND LOWER(COLUMN_NAME) = LOWER(?)")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        CompletableFuture<PlayerData> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_data WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    PlayerData data = new PlayerData(uuid);
                    if (rs.next()) {
                        // We don't read current_rank_id into the object directly anymore, but it's migrated to system_ranks
                        data.setTotalRankups(rs.getInt("total_rankups"));
                        data.setLastRankupTime(rs.getLong("last_rankup_time"));
                        data.setPlayerKills(rs.getLong("player_kills"));
                        data.setDeaths(rs.getLong("deaths"));
                        data.setTotalBlocksMined(rs.getLong("total_blocks_mined"));
                        data.setTotalMobKills(rs.getLong("total_mob_kills"));
                        data.setMobKills(jsonToMap(rs.getString("mob_kills")));
                        data.setBlocksMined(jsonToMap(rs.getString("blocks_mined")));
                        data.setSystemRanks(jsonToStringMap(rs.getString("system_ranks")));
                    }
                    data.markClean();
                    future.complete(data);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player data for " + uuid + ": " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement("MERGE INTO player_data " +
                         "(uuid, current_rank_id, total_rankups, last_rankup_time, player_kills, deaths, " +
                         "total_blocks_mined, total_mob_kills, mob_kills, blocks_mined, system_ranks) " +
                         "KEY(uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, data.getUuid().toString());
                ps.setString(2, ""); // No longer used, but schema might require it or we just send empty string
                ps.setInt(3, data.getTotalRankups());
                ps.setLong(4, data.getLastRankupTime());
                ps.setLong(5, data.getPlayerKills());
                ps.setLong(6, data.getDeaths());
                ps.setLong(7, data.getTotalBlocksMined());
                ps.setLong(8, data.getTotalMobKills());
                ps.setString(9, mapToJson(data.getMobKills()));
                ps.setString(10, mapToJson(data.getBlocksMined()));
                ps.setString(11, stringMapToJson(data.getSystemRanks()));
                ps.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player data for " + data.getUuid() + ": " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private String mapToJson(Map<String, Long> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey().replace("\"", "\\\"")).append("\":").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String stringMapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey().replace("\"", "\\\"")).append("\":\"").append(entry.getValue().replace("\"", "\\\"")).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Long> jsonToMap(String json) {
        Map<String, Long> map = new ConcurrentHashMap<>();
        if (json == null || json.isBlank() || json.equals("{}")) return map;
        String content = json.substring(1, json.length() - 1);
        if (content.isBlank()) return map;
        String[] pairs = content.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].replace("\"", "").trim();
                try {
                    long value = Long.parseLong(kv[1].trim());
                    map.put(key, value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return map;
    }

    private Map<String, String> jsonToStringMap(String json) {
        Map<String, String> map = new ConcurrentHashMap<>();
        if (json == null || json.isBlank() || json.equals("{}")) return map;
        String content = json.substring(1, json.length() - 1);
        if (content.isBlank()) return map;
        String[] pairs = content.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].replace("\"", "").trim();
                String value = kv[1].replace("\"", "").trim();
                map.put(key, value);
            }
        }
        return map;
    }
}
