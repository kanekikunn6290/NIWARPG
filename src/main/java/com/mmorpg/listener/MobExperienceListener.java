package com.mmorpg.listener;

import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.model.Job;
import com.mmorpg.util.MobExpManager;
import com.mmorpg.util.MobRankManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MOBからの経験値獲得を管理するリスナー
 */
public class MobExperienceListener implements Listener {
    private final PlayerJobManager jobManager;
    private final MobExpManager mobExpManager;
    private final LevelDisplayListener levelDisplayListener;
    private final Plugin plugin;
    
    // MOBの総ダメージを記録（UUID -> 総ダメージ）
    private final Map<UUID, Double> mobTotalDamageMap = new HashMap<>();
    
    // プレイヤーごとのダメージを記録（MOB UUID -> (プレイヤー UUID -> ダメージ)）
    private final Map<UUID, Map<UUID, Double>> playerDamageMap = new HashMap<>();

    public MobExperienceListener(PlayerJobManager jobManager, MobExpManager mobExpManager, LevelDisplayListener levelDisplayListener, Plugin plugin) {
        this.jobManager = jobManager;
        this.mobExpManager = mobExpManager;
        this.levelDisplayListener = levelDisplayListener;
        this.plugin = plugin;
    }

    /**
     * ダメージイベント - ダメージ追跡
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // ダメージ対象がMOBか確認
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity damagedEntity = (LivingEntity) event.getEntity();
        
        // プレイヤーはスキップ（プレイヤー同士のPvPはスキップ）
        if (damagedEntity instanceof Player) {
            return;
        }

        // ダメージを与えた人がプレイヤーか確認
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        double damage = event.getDamage();
        UUID mobUUID = damagedEntity.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        // MOBの総ダメージを追跡
        mobTotalDamageMap.put(mobUUID, mobTotalDamageMap.getOrDefault(mobUUID, 0.0) + damage);

        // プレイヤーのダメージを追跡
        playerDamageMap.computeIfAbsent(mobUUID, k -> new HashMap<>())
                       .put(playerUUID, playerDamageMap.getOrDefault(mobUUID, new HashMap<>()).getOrDefault(playerUUID, 0.0) + damage);
    }

    /**
     * MOB死亡イベント - 経験値授与
     */
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        UUID mobUUID = deadEntity.getUniqueId();

        // ダメージを与えたプレイヤーがいるか確認
        if (!playerDamageMap.containsKey(mobUUID)) {
            cleanupMobData(mobUUID);
            return;
        }

        // MOBの基礎経験値を取得
        int baseExp = mobExpManager.getBaseExp(deadEntity.getType());

        // MOBのランク係数を取得
        double rankCoefficient = MobRankManager.getRankCoefficient(deadEntity);

        // MOBの最大HPを取得
        double maxHealth = deadEntity.getMaxHealth();
        double totalDamage = mobTotalDamageMap.getOrDefault(mobUUID, 0.0);

        // すべてのプレイヤーに経験値を授与
        Map<UUID, Double> playerDamages = playerDamageMap.get(mobUUID);
        
        for (Map.Entry<UUID, Double> entry : playerDamages.entrySet()) {
            UUID playerUUID = entry.getKey();
            double playerDamage = entry.getValue();
            
            // オンラインのプレイヤーを取得
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player == null) {
                continue;
            }

            // 無職は経験値を獲得できない
            Job playerJob = jobManager.getPlayerJob(player);
            if (playerJob == Job.UNEMPLOYED) {
                continue;
            }

            // 貢献度を計算（与ダメージ ÷ MOB総HP）
            double contribution = Math.min(playerDamage / maxHealth, 1.0);

            // 経験値を計算：基礎XP × ランク係数 × 貢献度
            long gainedExp = (long) (baseExp * rankCoefficient * contribution);

            // レベルアップ前のレベルを記録
            int levelBefore = jobManager.getPlayerLevel(player);

            // 経験値を追加
            jobManager.addExperience(player, gainedExp);

            // レベルアップ後のレベルを取得
            int levelAfter = jobManager.getPlayerLevel(player);

            // プレイヤーに通知
            player.sendMessage("§a[経験値] " + gainedExp + " の経験値を獲得しました！");
            
            // レベルバーを更新
            if (levelDisplayListener != null) {
                levelDisplayListener.updateLevelDisplay(player);
            }
            
            // レベルアップしたかチェック
            if (levelAfter > levelBefore) {
                showLevelUpEffect(player, levelBefore, levelAfter);
            }
        }

        // MOBのデータをクリア
        cleanupMobData(mobUUID);
    }

    /**
     * MOBのダメージデータをクリア
     */
    private void cleanupMobData(UUID mobUUID) {
        mobTotalDamageMap.remove(mobUUID);
        playerDamageMap.remove(mobUUID);
    }

    /**
     * レベルアップエフェクトを表示
     */
    private void showLevelUpEffect(Player player, int oldLevel, int newLevel) {
        org.bukkit.Location playerLocation = player.getLocation();
        Job job = jobManager.getPlayerJob(player);

        // === サウンド再生 ===
        player.playSound(playerLocation, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.5f);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.playSound(playerLocation, org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        }, 2L);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.playSound(playerLocation, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }, 5L);

        // === パーティクル表示 ===
        for (int i = 0; i < 30; i++) {
            double angle = (Math.PI * 2 * i) / 30;
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;

            player.spawnParticle(org.bukkit.Particle.ENCHANT, 
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
                org.bukkit.Location particleLocation = playerLocation.clone().add(0, offsetY, 0);

                player.spawnParticle(org.bukkit.Particle.ENCHANT, particleLocation, 3, 
                        0.3, 0.3, 0.3, 0.05);

                org.bukkit.Color jobColor = getJobColor(job);
                player.spawnParticle(org.bukkit.Particle.DUST, particleLocation, 2,
                        0, 0, 0, new org.bukkit.Particle.DustOptions(jobColor, 1.0f));

                count++;
            }
        }, 0L, 1L);

        // === タイトル表示 ===
        net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.text("§6✦ ")
                .append(net.kyori.adventure.text.Component.text("LEVEL UP", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                .append(net.kyori.adventure.text.Component.text(" ✦", net.kyori.adventure.text.format.NamedTextColor.GOLD));

        net.kyori.adventure.text.Component subtitle = net.kyori.adventure.text.Component.text("Lv " + oldLevel, net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .append(net.kyori.adventure.text.Component.text(" → ", net.kyori.adventure.text.format.NamedTextColor.WHITE))
                .append(net.kyori.adventure.text.Component.text("Lv " + newLevel, net.kyori.adventure.text.format.NamedTextColor.GOLD));

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
    private org.bukkit.Color getJobColor(Job job) {
        return switch (job) {
            case FRONTLINER -> org.bukkit.Color.fromRGB(255, 0, 0);      // 赤
            case ARTISAN -> org.bukkit.Color.fromRGB(255, 165, 0);       // オレンジ
            case BUILDER -> org.bukkit.Color.fromRGB(0, 255, 0);         // 緑
            case HARVESTER -> org.bukkit.Color.fromRGB(0, 0, 255);       // 青
            case MYSTIC -> org.bukkit.Color.fromRGB(255, 0, 255);        // マゼンタ
            default -> org.bukkit.Color.fromRGB(128, 128, 128);          // グレー
        };
    }
}
