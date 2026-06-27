package dev.zm.rankup.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.zm.rankup.zMRankup;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.Properties;

public class StorageManager {

    private static final Gson GSON = new Gson();
    private static final TypeToken<Map<String, String>> STRING_MAP_TYPE = new TypeToken<>() {
    };
    private static final TypeToken<Map<String, Long>> LONG_MAP_TYPE = new TypeToken<>() {
    };
    private static final TypeToken<Map<String, Integer>> INT_MAP_TYPE = new TypeToken<>() {
    };

    private static final String H2_DRIVER = "org.h2.Driver";

    private final zMRankup plugin;
    private HikariDataSource dataSource;
    private ExecutorService executor;

    public StorageManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        ensureSQLiteDriver();

        plugin.getDataFolder().mkdirs();

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "zMRankup-Storage");
            t.setDaemon(true);
            return t;
        });

        // SQLite database lives in the data subfolder
        File dbFile = new File(dataFolder, "playerdata.db");
        HikariConfig cfg = new HikariConfig();

        cfg.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        cfg.setPoolName("zMRankup-Pool");

        cfg.setMaximumPoolSize(1);
        cfg.setMinimumIdle(1);

        cfg.addDataSourceProperty("journal_mode", "WAL");
        cfg.addDataSourceProperty("synchronous", "NORMAL");
        cfg.addDataSourceProperty("busy_timeout", "5000");
        plugin.getLogger().info("SQLite URL -> " + cfg.getJdbcUrl());
        this.dataSource = new HikariDataSource(cfg);

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        uuid               TEXT PRIMARY KEY,
                        total_rankups      INTEGER DEFAULT 0,
                        last_rankup_time   INTEGER DEFAULT 0,
                        player_kills       INTEGER DEFAULT 0,
                        deaths             INTEGER DEFAULT 0,
                        total_blocks_mined INTEGER DEFAULT 0,
                        total_mob_kills    INTEGER DEFAULT 0,
                        mob_kills          TEXT    DEFAULT '{}',
                        blocks_mined       TEXT    DEFAULT '{}',
                        prestiges          TEXT    DEFAULT '{}',
                        system_ranks       TEXT    DEFAULT '{}'
                    )""");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialise SQLite storage: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // One-shot migration from legacy H2 files (located in the root plugin folder)
        migrateFromH2IfNeeded();
    }

    private void ensureSQLiteDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
            return;
        } catch (ClassNotFoundException ignored) {
        }

        File libFolder = new File(plugin.getDataFolder(), "lib");
        if (!libFolder.exists()) {
            libFolder.mkdirs();
        }
        File sqliteFile = new File(libFolder, "sqlite-jdbc-3.47.1.0.jar");

        if (!sqliteFile.exists()) {
            plugin.getLogger().info("[Storage] Downloading SQLite JDBC driver...");
            try {
                java.net.URL url = new java.net.URL(
                        "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.47.1.0/sqlite-jdbc-3.47.1.0.jar");
                try (java.io.InputStream in = url.openStream();
                        java.io.FileOutputStream out = new java.io.FileOutputStream(sqliteFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer, 0, 8192)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                plugin.getLogger().info("[Storage] SQLite JDBC driver downloaded successfully.");
            } catch (Exception e) {
                plugin.getLogger()
                        .severe("[Storage] Failed to download SQLite JDBC driver! Database features will not work.");
                e.printStackTrace();
                return;
            }
        }

        try {
            java.net.URLClassLoader loader = new java.net.URLClassLoader(
                    new java.net.URL[] { sqliteFile.toURI().toURL() },
                    getClass().getClassLoader());
            Class<?> driverClass = Class.forName("org.sqlite.JDBC", true, loader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverProxy(driver));
            plugin.getLogger().info("[Storage] SQLite JDBC driver loaded successfully from lib folder.");
        } catch (Exception e) {
            plugin.getLogger().severe("[Storage] Failed to load SQLite JDBC driver.");
            e.printStackTrace();
        }
    }

    /**
     * Graceful shutdown: waits up to 10 s for in-flight saves, then closes pool.
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("Storage executor did not finish in 10 s – forcing shutdown.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM player_data WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    PlayerData data = new PlayerData(uuid);
                    if (rs.next()) {
                        data.setTotalRankups(rs.getInt("total_rankups"));
                        data.setLastRankupTime(rs.getLong("last_rankup_time"));
                        data.setPlayerKills(rs.getLong("player_kills"));
                        data.setDeaths(rs.getLong("deaths"));
                        data.setTotalBlocksMined(rs.getLong("total_blocks_mined"));
                        data.setTotalMobKills(rs.getLong("total_mob_kills"));
                        data.setMobKills(fromJson(rs.getString("mob_kills"), LONG_MAP_TYPE));
                        data.setBlocksMined(fromJson(rs.getString("blocks_mined"), LONG_MAP_TYPE));
                        data.setPrestiges(fromJson(rs.getString("prestiges"), INT_MAP_TYPE));
                        data.setSystemRanks(fromJson(rs.getString("system_ranks"), STRING_MAP_TYPE));
                    }
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("[DEBUG-DB-READ] Raw de DB: prestiges="
                                + rs.getString("prestiges") + " uuid=" + uuid);
                    }
                    data.markClean();
                    return data;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player data for " + uuid + ": " + e.getMessage());
                PlayerData safe = new PlayerData(uuid);
                safe.markClean();

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().severe("[DEBUG-DB-READ] FALLO: " + e.getMessage());
                }
                e.printStackTrace();
                return safe;
            }
        }, executor);
    }

    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        final String uuid = data.getUuid().toString();
        final int totalRankups = data.getTotalRankups();
        final long lastRankup = data.getLastRankupTime();
        final long playerKills = data.getPlayerKills();
        final long deaths = data.getDeaths();
        final long totalBlocks = data.getTotalBlocksMined();
        final long totalMobs = data.getTotalMobKills();
        final String mobKills = GSON.toJson(data.getMobKills());
        final String blocks = GSON.toJson(data.getBlocksMined());
        final String prestiges = GSON.toJson(data.getPrestiges());
        final String systemRanks = GSON.toJson(data.getSystemRanks());

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT OR REPLACE INTO player_data " +
                                    "(uuid, total_rankups, last_rankup_time, player_kills, deaths, " +
                                    "total_blocks_mined, total_mob_kills, mob_kills, blocks_mined, " +
                                    "prestiges, system_ranks) " +
                                    "VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, uuid);
                ps.setInt(2, totalRankups);
                ps.setLong(3, lastRankup);
                ps.setLong(4, playerKills);
                ps.setLong(5, deaths);
                ps.setLong(6, totalBlocks);
                ps.setLong(7, totalMobs);
                ps.setString(8, mobKills);
                ps.setString(9, blocks);
                ps.setString(10, prestiges);
                ps.setString(11, systemRanks);
                int rows = ps.executeUpdate();
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-DB-WRITE] Write OK rows=" + rows + " prestiges=" + prestiges);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }

    private void migrateFromH2IfNeeded() {
        File folder = plugin.getDataFolder();
        // Try both possible legacy file names (located in the root plugin folder)
        File h2File = new File(folder, "data.mv.db");
        if (!h2File.exists()) {
            h2File = new File(folder, "zmrankup_data.mv.db");
        }
        if (!h2File.exists())
            return;

        boolean debug = plugin.getConfigManager().isDebugEnabled();
        if (debug)
            plugin.getLogger()
                    .info("[Storage] Found legacy H2 file: " + h2File.getName() + " — migrating to SQLite...");

        String h2Base = h2File.getAbsolutePath().replace(".mv.db", "");
        String h2Url = "jdbc:h2:file:" + h2Base + ";IFEXISTS=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE";

        if (debug)
            plugin.getLogger().info("[Storage] Opening H2 URL: " + h2Url);

        int migrated = 0;

        try {

            Class<?> clazz = Class.forName(H2_DRIVER, true, getClass().getClassLoader());

            if (debug)
                plugin.getLogger().info(
                        "[Storage] Loaded H2 driver -> " + clazz.getProtectionDomain().getCodeSource().getLocation());

            Driver h2Driver = (Driver) clazz.getDeclaredConstructor().newInstance();

            try (Connection h2 = h2Driver.connect(h2Url, new Properties())) {

                if (h2 == null) {
                    plugin.getLogger().warning("[Storage] Could not open legacy H2 database.");
                    return;
                }

                boolean hasSystemRanks = columnExists(h2, "player_data", "system_ranks");
                boolean hasPrestiges = columnExists(h2, "player_data", "prestiges");
                boolean hasCurrentRank = columnExists(h2, "player_data", "current_rank_id");

                String select = "SELECT uuid,total_rankups,last_rankup_time,player_kills,deaths," +
                        "total_blocks_mined,total_mob_kills,mob_kills,blocks_mined" +
                        (hasPrestiges ? ",prestiges" : "") +
                        (hasSystemRanks ? ",system_ranks" : "") +
                        (hasCurrentRank ? ",current_rank_id" : "") +
                        " FROM player_data";

                try (
                        Statement stmt = h2.createStatement();
                        ResultSet rs = stmt.executeQuery(select);
                        Connection sqlite = getConnection()) {

                    sqlite.setAutoCommit(false);

                    try (PreparedStatement ins = sqlite.prepareStatement("""
                            INSERT INTO player_data
                            (uuid,total_rankups,last_rankup_time,
                            player_kills,deaths,total_blocks_mined,
                            total_mob_kills,mob_kills,blocks_mined,
                            prestiges,system_ranks)
                            VALUES (?,?,?,?,?,?,?,?,?,?,?)
                            ON CONFLICT(uuid) DO NOTHING
                            """)) {

                        while (rs.next()) {

                            String systemRanks = "{}";

                            if (hasSystemRanks) {
                                String val = rs.getString("system_ranks");
                                if (val != null && !val.isBlank())
                                    systemRanks = val;
                            }

                            if ("{}".equals(systemRanks)) {
                                String oldRank = safeStr(rs, "current_rank_id");
                                if (oldRank != null && !oldRank.isBlank()) {
                                    systemRanks = "{\"rankups\":\"" + oldRank + "\"}";
                                }
                            }

                            ins.setString(1, rs.getString("uuid"));
                            ins.setInt(2, rs.getInt("total_rankups"));
                            ins.setLong(3, rs.getLong("last_rankup_time"));
                            ins.setLong(4, rs.getLong("player_kills"));
                            ins.setLong(5, rs.getLong("deaths"));
                            ins.setLong(6, rs.getLong("total_blocks_mined"));
                            ins.setLong(7, rs.getLong("total_mob_kills"));
                            ins.setString(8, coalesce(safeStr(rs, "mob_kills"), "{}"));
                            ins.setString(9, coalesce(safeStr(rs, "blocks_mined"), "{}"));
                            ins.setString(10, hasPrestiges ? coalesce(safeStr(rs, "prestiges"), "{}") : "{}");
                            ins.setString(11, systemRanks);

                            ins.addBatch();
                            migrated++;
                        }

                        ins.executeBatch();
                        sqlite.commit();

                    } catch (SQLException ex) {
                        sqlite.rollback();
                        throw ex;
                    }

                    sqlite.setAutoCommit(true);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[Storage] H2 migration failed: " + e.getMessage());
            if (debug)
                e.printStackTrace();
            return;
        }

        File renamed = new File(h2File.getParent(), h2File.getName() + ".migrated");
        if (migrated > 0) {
            plugin.getLogger().info("[Storage] Migration complete: " + migrated + " player records imported from H2.");
        } else {
            if (debug)
                plugin.getLogger().info("[Storage] Legacy H2 file was empty — nothing to migrate.");
        }
        h2File.renameTo(renamed);
    }

    private boolean columnExists(Connection conn, String table, String column) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private String safeStr(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private String coalesce(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    @SuppressWarnings("unchecked")
    private <T> T fromJson(String json, TypeToken<T> type) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            // Return correct empty map type
            if (type == LONG_MAP_TYPE)
                return (T) new ConcurrentHashMap<String, Long>();
            if (type == INT_MAP_TYPE)
                return (T) new ConcurrentHashMap<String, Integer>();
            if (type == STRING_MAP_TYPE)
                return (T) new ConcurrentHashMap<String, String>();
        }
        try {
            Object raw = GSON.fromJson(json, type.getType());
            if (raw == null)
                return fromJson(null, type);

            return (T) new ConcurrentHashMap<>((Map<?, ?>) raw);
        } catch (Exception e) {
            e.printStackTrace();
            return fromJson(null, type);
        }
    }

    private static class DriverProxy implements Driver {
        private final Driver target;

        public DriverProxy(Driver target) {
            this.target = target;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return target.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return target.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return target.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return target.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return target.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return target.jdbcCompliant();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return target.getParentLogger();
        }
    }
}