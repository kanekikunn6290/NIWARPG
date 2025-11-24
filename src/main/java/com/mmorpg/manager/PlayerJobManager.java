package com.mmorpg.manager;

import com.mmorpg.database.DatabaseConfig;
import com.mmorpg.model.Job;
import com.mmorpg.util.ExperienceCalculator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * プレイヤーの職業・経験値・レベル情報をデータベースで管理するクラス
 */
public class PlayerJobManager {
    // メモリキャッシュ（ゲーム中のアクセス高速化）
    private final Map<UUID, Job> playerJobsCache = new HashMap<>();
    private final Map<UUID, Integer> playerLevelsCache = new HashMap<>();
    private final Map<UUID, Long> playerExperienceCache = new HashMap<>();

    /**
     * プレイヤーに職業を設定（DB保存）
     */
    public void setPlayerJob(Player player, Job job) {
        UUID uuid = player.getUniqueId();
        Job targetJob = job != null ? job : Job.UNEMPLOYED;

        // メモリキャッシュに追加
        playerJobsCache.put(uuid, targetJob);
        
        // 職業変更時はレベルを1にリセット
        playerLevelsCache.put(uuid, 1);
        playerExperienceCache.put(uuid, 0L);

        // 非同期でDB保存
        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("MMORPG"),
                () -> saveJobToDatabase(uuid, player.getName(), targetJob, 1, 0)
        );
    }

    /**
     * プレイヤーの職業を取得（キャッシュまたはDB）
     */
    public Job getPlayerJob(Player player) {
        UUID uuid = player.getUniqueId();

        // メモリキャッシュから取得
        if (playerJobsCache.containsKey(uuid)) {
            return playerJobsCache.get(uuid);
        }

        // DB から取得してキャッシュに保存
        Job job = loadJobFromDatabase(uuid);
        if (job == null) {
            job = Job.UNEMPLOYED;
        }
        playerJobsCache.put(uuid, job);
        return job;
    }

    /**
     * プレイヤーのレベルを取得
     */
    public int getPlayerLevel(Player player) {
        UUID uuid = player.getUniqueId();

        // キャッシュから取得
        if (playerLevelsCache.containsKey(uuid)) {
            return playerLevelsCache.get(uuid);
        }

        // DB から取得してキャッシュに保存
        PlayerData data = loadPlayerDataFromDatabase(uuid);
        if (data == null) {
            return 1;
        }
        playerLevelsCache.put(uuid, data.level);
        playerExperienceCache.put(uuid, data.experience);
        return data.level;
    }

    /**
     * プレイヤーの経験値を取得
     */
    public long getPlayerExperience(Player player) {
        UUID uuid = player.getUniqueId();

        // キャッシュから取得
        if (playerExperienceCache.containsKey(uuid)) {
            return playerExperienceCache.get(uuid);
        }

        // DB から取得してキャッシュに保存
        PlayerData data = loadPlayerDataFromDatabase(uuid);
        if (data == null) {
            return 0L;
        }
        playerExperienceCache.put(uuid, data.experience);
        playerLevelsCache.put(uuid, data.level);
        return data.experience;
    }

    /**
     * 経験値を追加
     */
    public void addExperience(Player player, long amount) {
        UUID uuid = player.getUniqueId();
        Job job = getPlayerJob(player);

        // 無職の場合は経験値取得不可
        if (job == Job.UNEMPLOYED) {
            return;
        }

        // 現在の経験値を取得
        long currentExp = getPlayerExperience(player);
        int currentLevel = getPlayerLevel(player);
        int maxLevel = ExperienceCalculator.getMaxLevel(job);

        // 最大レベルに達しているなら経験値追加しない
        if (currentLevel >= maxLevel) {
            return;
        }

        // 経験値を追加
        long newExp = currentExp + amount;

        // 新しいレベルを計算
        int newLevel = ExperienceCalculator.calculateLevel(job, newExp);
        newLevel = ExperienceCalculator.clipLevel(job, newLevel);

        // 最大レベルをクリップ
        if (newLevel > maxLevel) {
            newLevel = maxLevel;
            newExp = ExperienceCalculator.getRequiredExperience(job, maxLevel);
        }

        // キャッシュを更新
        playerExperienceCache.put(uuid, newExp);
        playerLevelsCache.put(uuid, newLevel);

        // 非同期でDB保存（final変数を使用）
        final long finalExp = newExp;
        final int finalLevel = newLevel;
        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("MMORPG"),
                () -> saveExperienceToDatabase(uuid, finalExp, finalLevel)
        );
    }

    /**
     * プレイヤーの職業をリセット
     */
    public void resetPlayerJob(Player player) {
        setPlayerJob(player, Job.UNEMPLOYED);
    }

    /**
     * 経験値とレベルを直接設定
     */
    public void setExperienceAndLevel(Player player, long experience) {
        UUID uuid = player.getUniqueId();
        Job job = getPlayerJob(player);

        // 経験値をクリップ
        int maxLevel = ExperienceCalculator.getMaxLevel(job);
        long maxExp = ExperienceCalculator.getRequiredExperience(job, maxLevel);
        long clippedExp = Math.max(0, Math.min(experience, maxExp));

        // 新しいレベルを計算
        int newLevel = ExperienceCalculator.calculateLevel(job, clippedExp);
        newLevel = ExperienceCalculator.clipLevel(job, newLevel);

        // キャッシュを更新
        playerExperienceCache.put(uuid, clippedExp);
        playerLevelsCache.put(uuid, newLevel);

        // 非同期でDB保存
        final long finalExp = clippedExp;
        final int finalLevel = newLevel;
        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("MMORPG"),
                () -> saveExperienceToDatabase(uuid, finalExp, finalLevel)
        );
    }

    /**
     * プレイヤーが特定の職業を持っているか確認
     */
    public boolean hasJob(Player player, Job job) {
        return getPlayerJob(player) == job;
    }

    /**
     * キャッシュからプレイヤーを削除（ログアウト時）
     */
    public void removeOfflinePlayer(UUID uuid) {
        playerJobsCache.remove(uuid);
        playerLevelsCache.remove(uuid);
        playerExperienceCache.remove(uuid);
    }

    /**
     * すべてのキャッシュをクリア
     */
    public void clearAll() {
        playerJobsCache.clear();
        playerLevelsCache.clear();
        playerExperienceCache.clear();
    }

    /**
     * データベースに職業を保存
     */
    private void saveJobToDatabase(UUID uuid, String playerName, Job job, int level, long experience) {
        String sql = "INSERT INTO player_jobs (player_uuid, player_name, job, level, experience) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = ?, job = ?, level = ?, experience = ?, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, playerName);
            pstmt.setString(3, job.name());
            pstmt.setInt(4, level);
            pstmt.setLong(5, experience);
            pstmt.setString(6, playerName);
            pstmt.setString(7, job.name());
            pstmt.setInt(8, level);
            pstmt.setLong(9, experience);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMORPG] Failed to save job to database: " + e.getMessage());
        }
    }

    /**
     * データベースに経験値を保存
     */
    private void saveExperienceToDatabase(UUID uuid, long experience, int level) {
        String sql = "UPDATE player_jobs SET experience = ?, level = ?, updated_at = CURRENT_TIMESTAMP WHERE player_uuid = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, experience);
            pstmt.setInt(2, level);
            pstmt.setString(3, uuid.toString());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMORPG] Failed to save experience to database: " + e.getMessage());
        }
    }

    /**
     * データベースから職業を読み込み
     */
    private Job loadJobFromDatabase(UUID uuid) {
        String sql = "SELECT job FROM player_jobs WHERE player_uuid = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String jobName = rs.getString("job");
                return Job.valueOf(jobName);
            }

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMORPG] Failed to load job from database: " + e.getMessage());
        }

        return null;
    }

    /**
     * データベースからプレイヤーデータを読み込み
     */
    private PlayerData loadPlayerDataFromDatabase(UUID uuid) {
        String sql = "SELECT job, level, experience FROM player_jobs WHERE player_uuid = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Job job = Job.valueOf(rs.getString("job"));
                int level = rs.getInt("level");
                long experience = rs.getLong("experience");
                return new PlayerData(job, level, experience);
            }

        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMORPG] Failed to load player data from database: " + e.getMessage());
        }

        return null;
    }

    /**
     * プレイヤーデータを保持する内部クラス
     */
    private static class PlayerData {
        Job job;
        int level;
        long experience;

        PlayerData(Job job, int level, long experience) {
            this.job = job;
            this.level = level;
            this.experience = experience;
        }
    }
}

