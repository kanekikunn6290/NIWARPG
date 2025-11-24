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
 * /job コマンドの実行処理
 */
public class JobCommand implements CommandExecutor, TabCompleter {
    private final PlayerJobManager jobManager;
    private JobDisplayListener displayListener;
    private LevelDisplayListener levelDisplayListener;

    public JobCommand(PlayerJobManager jobManager) {
        this.jobManager = jobManager;
    }

    public void setDisplayListener(JobDisplayListener displayListener) {
        this.displayListener = displayListener;
    }

    public void setLevelDisplayListener(LevelDisplayListener levelDisplayListener) {
        this.levelDisplayListener = levelDisplayListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // OP権限チェック
        if (!sender.isOp()) {
            sender.sendMessage("§c[エラー] このコマンドはOP権限が必要です");
            return true;
        }

        // 引数チェック
        if (args.length < 3) {
            sender.sendMessage("§e使用方法: /job set <プレイヤー名> <職業名>");
            sender.sendMessage("§e利用可能な職業:");
            for (Job job : Job.values()) {
                sender.sendMessage("§e  - " + job.getDisplayName() + " (" + job.getDescription() + ")");
            }
            return true;
        }

        // コマンド種別をチェック
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("set")) {
            return handleSetCommand(sender, args);
        } else if (subCommand.equals("get")) {
            return handleGetCommand(sender, args);
        } else if (subCommand.equals("reset")) {
            return handleResetCommand(sender, args);
        } else {
            sender.sendMessage("§c不正なサブコマンド: " + subCommand);
            return true;
        }
    }

    /**
     * /job set コマンドの処理
     */
    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c使用方法: /job set <プレイヤー名> <職業名>");
            return true;
        }

        String playerName = args[1];
        String jobName = args[2];

        // プレイヤーを取得
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage("§c[エラー] プレイヤー '" + playerName + "' が見つかりません");
            return true;
        }

        // 職業を取得
        Job job = Job.fromName(jobName);
        if (job == null) {
            sender.sendMessage("§c[エラー] 職業 '" + jobName + "' が見つかりません");
            sender.sendMessage("§e利用可能な職業: " + getJobNames());
            return true;
        }

        // 変更前の職業を記録
        Job oldJob = jobManager.getPlayerJob(targetPlayer);

        // 職業を設定（職業変更時はレベルと経験値がリセットされる）
        jobManager.setPlayerJob(targetPlayer, job);
        
        // TAB表示を更新
        if (displayListener != null) {
            displayListener.updatePlayerListName(targetPlayer);
        }
        
        // レベルバー表示を更新
        if (levelDisplayListener != null) {
            levelDisplayListener.updateLevelDisplay(targetPlayer);
        }
        
        String oldJobName = oldJob.getDisplayName();
        sender.sendMessage("§a[成功] " + targetPlayer.getName() + " の職業を " + oldJobName + " から " + job.getDisplayName() + " に変更しました");
        sender.sendMessage("§e  レベル・経験値はリセットされました");
        targetPlayer.sendMessage("§b[通知] あなたの職業が " + oldJobName + " から " + job.getDisplayName() + " に変更されました");
        targetPlayer.sendMessage("§e  レベルと経験値がリセットされました");
        
        return true;
    }

    /**
     * /job get コマンドの処理
     */
    private boolean handleGetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用方法: /job get <プレイヤー名>");
            return true;
        }

        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage("§c[エラー] プレイヤー '" + playerName + "' が見つかりません");
            return true;
        }

        Job job = jobManager.getPlayerJob(targetPlayer);
        sender.sendMessage("§e" + targetPlayer.getName() + " の職業: " + job.getDisplayName());
        
        return true;
    }

    /**
     * /job reset コマンドの処理
     */
    private boolean handleResetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用方法: /job reset <プレイヤー名>");
            return true;
        }

        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage("§c[エラー] プレイヤー '" + playerName + "' が見つかりません");
            return true;
        }

        // 変更前の職業を記録
        Job oldJob = jobManager.getPlayerJob(targetPlayer);

        jobManager.resetPlayerJob(targetPlayer);
        
        // TAB表示を更新
        if (displayListener != null) {
            displayListener.updatePlayerListName(targetPlayer);
        }
        
        // レベルバー表示を更新
        if (levelDisplayListener != null) {
            levelDisplayListener.updateLevelDisplay(targetPlayer);
        }
        
        String oldJobName = oldJob.getDisplayName();
        sender.sendMessage("§a[成功] " + targetPlayer.getName() + " の職業をリセットしました");
        sender.sendMessage("§e  " + oldJobName + " → 無職");
        targetPlayer.sendMessage("§b[通知] あなたの職業がリセットされました");
        targetPlayer.sendMessage("§e  " + oldJobName + " → 無職");
        targetPlayer.sendMessage("§e  レベルと経験値もリセットされました");
        
        return true;
    }

    /**
     * 職業名のリストを取得（TAB補完用）
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.isOp()) {
            return completions;
        }

        // サブコマンドの補完
        if (args.length == 1) {
            completions.add("set");
            completions.add("get");
            completions.add("reset");
            return completions;
        }

        // "set"コマンドの場合
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 2) {
                // プレイヤー名の補完
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args.length == 3) {
                // 職業名の補完
                for (Job job : Job.values()) {
                    completions.add(job.getDisplayName());
                }
            }
        }

        // "get"と"reset"コマンドの場合
        if (args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("reset")) {
            if (args.length == 2) {
                // プレイヤー名の補完
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    /**
     * 利用可能な職業名をカンマ区切りで取得
     */
    private String getJobNames() {
        StringBuilder sb = new StringBuilder();
        Job[] jobs = Job.values();
        for (int i = 0; i < jobs.length; i++) {
            sb.append(jobs[i].getDisplayName());
            if (i < jobs.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
