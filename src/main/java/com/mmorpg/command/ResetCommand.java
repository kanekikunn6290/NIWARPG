package com.mmorpg.command;

import com.mmorpg.listener.JobDisplayListener;
import com.mmorpg.listener.LevelDisplayListener;
import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.model.Job;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /reset コマンドの実行処理
 * プレイヤーの職業と経験値をリセット
 */
public class ResetCommand implements CommandExecutor, TabCompleter {
    private final PlayerJobManager jobManager;
    private LevelDisplayListener levelDisplayListener;
    private JobDisplayListener jobDisplayListener;

    public ResetCommand(PlayerJobManager jobManager) {
        this.jobManager = jobManager;
    }

    public void setLevelDisplayListener(LevelDisplayListener levelDisplayListener) {
        this.levelDisplayListener = levelDisplayListener;
    }

    public void setJobDisplayListener(JobDisplayListener jobDisplayListener) {
        this.jobDisplayListener = jobDisplayListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // OP権限チェック
        if (!sender.isOp()) {
            sender.sendMessage("§c[エラー] このコマンドはOP権限が必要です");
            return true;
        }

        // 引数チェック
        if (args.length < 1) {
            sender.sendMessage("§c使用方法: /reset <プレイヤー名>");
            return true;
        }

        String playerName = args[0];

        // プレイヤーを取得
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage("§c[エラー] プレイヤー '" + playerName + "' が見つかりません");
            return true;
        }

        // リセット確認メッセージ（2回コマンドを入力する必要があるようにする場合はこのロジックを拡張）
        String currentJob = jobManager.getPlayerJob(targetPlayer).getDisplayName();
        int currentLevel = jobManager.getPlayerLevel(targetPlayer);
        long currentExp = jobManager.getPlayerExperience(targetPlayer);

        // 職業をリセット（無職に設定）
        jobManager.resetPlayerJob(targetPlayer);

        // レベル表示を更新
        if (levelDisplayListener != null) {
            levelDisplayListener.updateLevelDisplay(targetPlayer);
        }

        // 職業表示も更新
        if (jobDisplayListener != null) {
            jobDisplayListener.updatePlayerListName(targetPlayer);
        }

        sender.sendMessage("§a[成功] " + targetPlayer.getName() + " をリセットしました");
        sender.sendMessage("§e  旧職業: " + currentJob);
        sender.sendMessage("§e  旧レベル: " + currentLevel);
        sender.sendMessage("§e  旧経験値: " + currentExp);

        targetPlayer.sendMessage("§b[通知] あなたの職業・レベル・経験値がリセットされました");
        targetPlayer.sendMessage("§b  新職業: §7無職");

        return true;
    }

    /**
     * TAB補完
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.isOp()) {
            return completions;
        }

        // プレイヤー名の補完
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}
