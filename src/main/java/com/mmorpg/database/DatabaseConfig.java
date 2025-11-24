package com.mmorpg.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * MySQLデータベース接続を管理するクラス
 */
public class DatabaseConfig {
    private static HikariDataSource dataSource;
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE = "mmorpg";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    /**
     * データベース接続を初期化
     */
    public static void initialize() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        try {
            dataSource = new HikariDataSource(config);
            Bukkit.getLogger().info("[MMORPG] Database connected successfully!");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[MMORPG] Failed to connect to database: " + e.getMessage());
            throw new SQLException(e);
        }
    }

    /**
     * コネクションを取得
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource has not been initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * データベース接続をクローズ
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Bukkit.getLogger().info("[MMORPG] Database connection closed");
        }
    }

    /**
     * データベースが接続しているか確認
     */
    public static boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
