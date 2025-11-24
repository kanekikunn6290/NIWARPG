package com.mmorpg.listener;

import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.model.Job;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * レベルアップ時のエフェクトを管理するリスナー
 */
public class LevelUpListener implements Listener {
    private final PlayerJobManager jobManager;
    private final Plugin plugin;
    
    // プレイヤーの前回レベル（UUID -> 前回レベル）
    private final Map<UUID, Integer> previousLevelMap = new HashMap<>();
    private final Map<UUID, BukkitTask> levelCheckTasks = new HashMap<>();

    public LevelUpListener(PlayerJobManager jobManager, Plugin plugin) {
        this.jobManager = jobManager;
        this.plugin = plugin;
    }

    /**
     * プレイヤー参加時 - レベルチェック開始
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 初期レベルを記録
        int currentLevel = jobManager.getPlayerLevel(player);
        previousLevelMap.put(playerUUID, currentLevel);

        // レベルチェックタスクをキャンセル（存在する場合）
        if (levelCheckTasks.containsKey(playerUUID)) {
            levelCheckTasks.get(playerUUID).cancel();
        }

        // レベルチェックタスクを開始（1秒ごと）
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkLevelUp(player);
        }, 20L, 5L); // 1秒後、5ティックごと（0.25秒）

        levelCheckTasks.put(playerUUID, task);
    }

    /**
     * プレイヤーがオフラインになったときにタスクを停止
     */
    public void onPlayerQuit(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (levelCheckTasks.containsKey(playerUUID)) {
            levelCheckTasks.get(playerUUID).cancel();
            levelCheckTasks.remove(playerUUID);
        }
        
        previousLevelMap.remove(playerUUID);
    }

    /**
     * レベルアップをチェック
     */
    private void checkLevelUp(Player player) {
        if (!player.isOnline()) {
            onPlayerQuit(player);
            return;
        }

        UUID playerUUID = player.getUniqueId();
        int currentLevel = jobManager.getPlayerLevel(player);
        int previousLevel = previousLevelMap.getOrDefault(playerUUID, currentLevel);

        // レベルが上がったか確認
        if (currentLevel > previousLevel) {
            // レベルアップエフェクトを表示
            showLevelUpEffect(player, previousLevel, currentLevel);
            previousLevelMap.put(playerUUID, currentLevel);
        }
    }

    /**
     * 豪華なレベルアップエフェクトを表示
     */
    private void showLevelUpEffect(Player player, int oldLevel, int newLevel) {
        Location playerLocation = player.getLocation();
        Job job = jobManager.getPlayerJob(player);

        // === サウンド再生 ===
        // 低音から高音へのジングル効果
        player.playSound(playerLocation, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.5f);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.playSound(playerLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        }, 2L);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.playSound(playerLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }, 5L);

        // === パーティクル表示 ===
        // 金色のスパークル（プレイヤーの周囲）
        for (int i = 0; i < 30; i++) {
            double angle = (Math.PI * 2 * i) / 30;
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;

            player.spawnParticle(Particle.ENCHANT, 
                    playerLocation.add(x, 1.5, z), 
                    2, 0, 0, 0, 0);
            playerLocation.subtract(x, 1.5, z);
        }

        // 上昇するパーティクル
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private int count = 0;

            @Override
            public void run() {
                if (count > 20) {
                    return;
                }

                double offsetY = count * 0.1;
                Location particleLocation = playerLocation.clone().add(0, offsetY, 0);

                // プリズマリンパーティクル（魔法的効果）
                player.spawnParticle(Particle.ENCHANT, particleLocation, 3, 
                        0.3, 0.3, 0.3, 0.05);

                // 職業に応じた色のパーティクル
                Color jobColor = getJobColor(job);
                player.spawnParticle(Particle.DUST, particleLocation, 2,
                        0, 0, 0, new Particle.DustOptions(jobColor, 1.0f));

                count++;
            }
        }, 0L, 1L);

        // === タイトル表示 ===
        Component title = Component.text("§6✦ ")
                .append(Component.text("LEVEL UP", NamedTextColor.YELLOW))
                .append(Component.text(" ✦", NamedTextColor.GOLD));

        Component subtitle = Component.text("Lv " + oldLevel, NamedTextColor.GRAY)
                .append(Component.text(" → ", NamedTextColor.WHITE))
                .append(Component.text("Lv " + newLevel, NamedTextColor.GOLD));

        player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle));

        // === チャット通知 ===
        String levelName = job.getDisplayName();
        String announcement = "§d✦ " + player.getName() + " が " + levelName + " Lv" + newLevel + " になりました！ ✦";
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(announcement);
        }
    }

    /**
     * 職業に応じた色を取得
     */
    private Color getJobColor(Job job) {
        return switch (job) {
            case FRONTLINER -> Color.fromRGB(255, 0, 0);      // 赤
            case ARTISAN -> Color.fromRGB(255, 165, 0);       // オレンジ
            case BUILDER -> Color.fromRGB(0, 255, 0);         // 緑
            case HARVESTER -> Color.fromRGB(0, 0, 255);       // 青
            case MYSTIC -> Color.fromRGB(255, 0, 255);        // マゼンタ
            default -> Color.fromRGB(128, 128, 128);          // グレー
        };
    }
}
