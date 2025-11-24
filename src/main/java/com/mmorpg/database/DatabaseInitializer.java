package com.mmorpg.database;

import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * データベーステーブルを初期化するクラス
 */
public class DatabaseInitializer {

    /**
     * 必要なテーブルを作成
     */
    public static void initializeTables() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // player_jobsテーブルが存在するかチェック
            if (!tableExists(conn, "player_jobs")) {
                String createTableSQL = "CREATE TABLE player_jobs (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "player_uuid VARCHAR(36) NOT NULL UNIQUE," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "job VARCHAR(50) NOT NULL DEFAULT 'UNEMPLOYED'," +
                        "level INT NOT NULL DEFAULT 1," +
                        "experience BIGINT NOT NULL DEFAULT 0," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")";

                stmt.execute(createTableSQL);
                Bukkit.getLogger().info("[MMORPG] player_jobs table created successfully");
            } else {
                // 既存テーブルにカラムが存在するかチェック
                if (!columnExists(conn, "player_jobs", "level")) {
                    String alterTableSQL = "ALTER TABLE player_jobs ADD COLUMN level INT NOT NULL DEFAULT 1";
                    stmt.execute(alterTableSQL);
                    Bukkit.getLogger().info("[MMORPG] Added level column to player_jobs table");
                }
                if (!columnExists(conn, "player_jobs", "experience")) {
                    String alterTableSQL = "ALTER TABLE player_jobs ADD COLUMN experience BIGINT NOT NULL DEFAULT 0";
                    stmt.execute(alterTableSQL);
                    Bukkit.getLogger().info("[MMORPG] Added experience column to player_jobs table");
                }
                Bukkit.getLogger().info("[MMORPG] player_jobs table already exists");
            }

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMORPG] Failed to initialize database tables: " + e.getMessage());
            throw e;
        }
    }

    /**
     * テーブルが存在するか確認
     */
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    /**
     * カラムが存在するか確認
     */
    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }
}
