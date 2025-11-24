package com.mmorpg;

import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.model.Job;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

public class PlayerEventListener implements Listener {
    private final PlayerJobManager jobManager;

    // ファンタジー風な参加メッセージ
    private static final String[] FIRST_JOIN_MESSAGES = {
        "§b✨ 伝説の冒険者 §e{player}§b が新たな世界へ降り立った！",
        "§d★ 新しき勇者 §e{player}§d が現れた！冒険が始まる…",
        "§6✦ 光の加護を受けた §e{player}§6 が参加した！",
        "§a🌿 森の精霊に選ばれた §e{player}§a が現れた！",
        "§c⚔ 戦士 §e{player}§c が剣を握り参加した！"
    };

    private static final String[] JOIN_MESSAGES = {
        "§b⟿ §e{player}§b が冒険に戻ってきた…",
        "§d✧ §e{player}§d が世界に帰還した",
        "§6✤ §e{player}§6 が再び姿を現わした",
        "§a🌲 §e{player}§a が森から戻ってきた",
        "§c⚡ §e{player}§c が戦地に帰ってきた"
    };

    public PlayerEventListener(PlayerJobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        // DB から職業を読み込む（存在しない場合は無職として登録）
        Job playerJob = jobManager.getPlayerJob(player);
        
        // 初回参加かどうかを判定
        if (!player.hasPlayedBefore()) {
            // 初回参加メッセージ
            String firstJoinMessage = getRandomMessage(FIRST_JOIN_MESSAGES);
            event.setJoinMessage(firstJoinMessage.replace("{player}", playerName));
            
            // DB に無職として登録
            jobManager.setPlayerJob(player, Job.UNEMPLOYED);
            
            // コンソールにも出力
            Bukkit.getLogger().info(playerName + " joined for the first time!");
        } else {
            // 通常の参加メッセージ
            String joinMessage = getRandomMessage(JOIN_MESSAGES);
            event.setJoinMessage(joinMessage.replace("{player}", playerName));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        // ファンタジー風な退出メッセージ
        String[] quitMessages = {
            "§7❄ §e{player}§7 は冒険の旅に出た…",
            "§8✖ §e{player}§8 は消えてしまった",
            "§5☾ §e{player}§5 は影の中へ消えた",
            "§7⟸ §e{player}§7 が世界から去った"
        };
        
        String quitMessage = getRandomMessage(quitMessages);
        event.setQuitMessage(quitMessage.replace("{player}", playerName));
        
        // オフラインプレイヤーの職業データをクリア
        jobManager.removeOfflinePlayer(player.getUniqueId());
    }

    /**
     * チャットメッセージに職業プレフィックスを追加
     */
    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Job job = jobManager.getPlayerJob(player);
        
        // 職業プレフィックスを取得
        String jobPrefix = job.getPrefix();
        
        // メッセージをフォーマット
        // デフォルトフォーマット: <Player> message を [職業] <Player> message に変更
        Component newMessage = Component.text(jobPrefix)
                .append(event.message());
        
        event.message(newMessage);
    }

    /**
     * TABリストに職業情報を表示
     */
    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        // TABリストの更新はプレイヤーごとに動的に行う
        // （PlayerList経由でプレイヤーの表示名を更新）
    }

    /**
     * メッセージ配列からランダムにメッセージを選択
     */
    private String getRandomMessage(String[] messages) {
        return messages[(int) (Math.random() * messages.length)];
    }
}
