package com.mmorpg.listener;

import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.model.Job;
import com.mmorpg.util.ExperienceCalculator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * プレイヤーのレベルと経験値を表示するリスナー
 */
public class LevelDisplayListener implements Listener {
    private final PlayerJobManager jobManager;
    private Plugin plugin;

    public LevelDisplayListener(PlayerJobManager jobManager) {
        this.jobManager = jobManager;
        this.plugin = Bukkit.getPluginManager().getPlugin("MMORPG");
    }

    /**
     * プレイヤー参加時にレベル表示を更新
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 1ティック遅延させて更新
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            updateLevelDisplay(player);
        }, 1L);
    }

    /**
     * プレイヤーのレベル表示を更新（クライアント同期）
     */
    public void updateLevelDisplay(Player player) {
        Job job = jobManager.getPlayerJob(player);
        int level = jobManager.getPlayerLevel(player);
        long experience = jobManager.getPlayerExperience(player);
        
        // 経験値の進捗をバーで計算（0-100 をスケール）
        float expProgress = ExperienceCalculator.getLevelUpProgress(job, level, experience) / 100.0f;
        
        // NaN チェック + 範囲チェック
        if (Float.isNaN(expProgress) || Float.isInfinite(expProgress)) {
            expProgress = 0.0f;
        }
        expProgress = Math.max(0.0f, Math.min(1.0f, expProgress));
        
        // クライアントに同期させるために、スケジューラーを使用
        final float finalExpProgress = expProgress;
        final int finalLevel = level;
        if (plugin != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.setLevel(finalLevel);
                player.setExp(finalExpProgress);
            });
        } else {
            // フォールバック
            player.setLevel(level);
            player.setExp(expProgress);
        }
    }

    /**
     * 外部から定期的にレベル表示を更新する場合
     */
    public void schedulePeriodicUpdate(Player player, long intervalTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    updateLevelDisplay(player);
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(
                org.bukkit.Bukkit.getPluginManager().getPlugin("MMORPG"),
                0L,
                intervalTicks
        );
    }
}
