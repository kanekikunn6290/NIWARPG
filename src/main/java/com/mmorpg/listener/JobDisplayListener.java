package com.mmorpg.listener;

import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.model.Job;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * プレイヤーの表示名に職業情報を追加するリスナー
 */
public class JobDisplayListener implements Listener {
    private final PlayerJobManager jobManager;

    public JobDisplayListener(PlayerJobManager jobManager) {
        this.jobManager = jobManager;
    }

    /**
     * プレイヤーが参加したときにプレイヤーリスト上の表示名を更新
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // プレイヤーリスト表示名を更新
        updatePlayerListName(player);
    }

    /**
     * プレイヤーのプレイヤーリスト表示名を更新
     */
    public void updatePlayerListName(Player player) {
        Job job = jobManager.getPlayerJob(player);
        
        // 職業情報を含む表示名を作成
        String jobPrefix = job.getPrefix();
        Component displayName = Component.text(jobPrefix + player.getName() + "§r");
        
        // プレイヤーリスト名を設定
        player.playerListName(displayName);
    }
}

