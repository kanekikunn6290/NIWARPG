package com.mmorpg.command;

import com.mmorpg.listener.LevelDisplayListener;
import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.model.Job;
import com.mmorpg.util.ExperienceCalculator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /level コマンドの実行処理
 * 経験値とレベルを管理するコマンド
 */
public class LevelCommand implements CommandExecutor, TabCompleter {
    private final PlayerJobManager jobManager;
    private LevelDisplayListener levelDisplayListener;

    public LevelCommand(PlayerJobManager jobManager) {
        this.jobManager = jobManager;
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
            showUsage(sender);
            return true;
        }

        String playerName = args[0];
        String type = args[1].toLowerCase();
        String operation = args[2].toLowerCase();

        // プレイヤーを取得
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage("§c[エラー] プレイヤー '" + playerName + "' が見つかりません");
            return true;
        }

        // タイプ別の処理
        if (type.equals("exp")) {
            return handleExpCommand(sender, targetPlayer, operation, args);
        } else if (type.equals("level")) {
            return handleLevelCommand(sender, targetPlayer, operation, args);
        } else {
            sender.sendMessage("§c[エラー] 不正なタイプです。'exp' または 'level' を指定してください");
            return true;
        }
    }

    /**
     * 経験値コマンドの処理
     */
    private boolean handleExpCommand(CommandSender sender, Player targetPlayer, String operation, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c使用方法: /level <プレイヤー名> exp <set/add/remove> <数値>");
            return true;
        }

        long value;
        try {
            value = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[エラー] 数値を入力してください");
            return true;
        }

        Job job = jobManager.getPlayerJob(targetPlayer);
        long currentExp = jobManager.getPlayerExperience(targetPlayer);
        int currentLevel = jobManager.getPlayerLevel(targetPlayer);

        if (operation.equals("set")) {
            // 経験値を設定
            long newExp = Math.max(0, value);
            
            // 最大レベルの経験値を超えないようにクリップ
            int maxLevel = ExperienceCalculator.getMaxLevel(job);
            long maxExp = ExperienceCalculator.getRequiredExperience(job, maxLevel);
            newExp = Math.min(newExp, maxExp);

            jobManager.setExperienceAndLevel(targetPlayer, newExp);
            
            int newLevel = jobManager.getPlayerLevel(targetPlayer);
            sender.sendMessage("§a[成功] " + targetPlayer.getName() + " の経験値を " + newExp + " に設定しました (レベル: " + newLevel + ")");
            targetPlayer.sendMessage("§b[通知] 経験値が " + newExp + " に設定されました");

        } else if (operation.equals("add")) {
            // 経験値を追加
            if (job == Job.UNEMPLOYED) {
                sender.sendMessage("§c[エラー] 無職は経験値を獲得できません");
                return true;
            }

            jobManager.addExperience(targetPlayer, value);
            
            long newExp = jobManager.getPlayerExperience(targetPlayer);
            int newLevel = jobManager.getPlayerLevel(targetPlayer);
            sender.sendMessage("§a[成功] " + targetPlayer.getName() + " に " + value + " の経験値を追加しました (合計: " + newExp + ", レベル: " + newLevel + ")");
            targetPlayer.sendMessage("§b[通知] " + value + " の経験値を獲得しました!");

        } else if (operation.equals("remove")) {
            // 経験値を削除
            long newExp = Math.max(0, currentExp - value);
            jobManager.setExperienceAndLevel(targetPlayer, newExp);
            
            int newLevel = jobManager.getPlayerLevel(targetPlayer);
            sender.sendMessage("§a[成功] " + targetPlayer.getName() + " から " + value + " の経験値を削除しました (合計: " + newExp + ", レベル: " + newLevel + ")");
            targetPlayer.sendMessage("§b[通知] " + value + " の経験値が削除されました");

        } else {
            sender.sendMessage("§c[エラー] 不正な操作です。'set', 'add', または 'remove' を指定してください");
            return true;
        }

        // レベル表示を更新
        if (levelDisplayListener != null) {
            levelDisplayListener.updateLevelDisplay(targetPlayer);
        }

        return true;
    }

    /**
     * レベルコマンドの処理
     */
    private boolean handleLevelCommand(CommandSender sender, Player targetPlayer, String operation, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c使用方法: /level <プレイヤー名> level <set/add/remove> <数値>");
            return true;
        }

        int value;
        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[エラー] 数値を入力してください");
            return true;
        }

        Job job = jobManager.getPlayerJob(targetPlayer);
        int currentLevel = jobManager.getPlayerLevel(targetPlayer);
        int maxLevel = ExperienceCalculator.getMaxLevel(job);

        if (operation.equals("set")) {
            // レベルを設定
            int newLevel = Math.max(1, Math.min(value, maxLevel));
            long requiredExp = ExperienceCalculator.getRequiredExperience(job, newLevel);
            
            jobManager.setExperienceAndLevel(targetPlayer, requiredExp);
            
            // キャッシュから即座に読み込んで確認
            int actualLevel = jobManager.getPlayerLevel(targetPlayer);
            
            sender.sendMessage("§a[成功] " + targetPlayer.getName() + " のレベルを " + actualLevel + " に設定しました");
            targetPlayer.sendMessage("§b[通知] レベルが " + actualLevel + " に設定されました");
            
            // レベル表示を更新
            if (levelDisplayListener != null) {
                levelDisplayListener.updateLevelDisplay(targetPlayer);
            }

        } else if (operation.equals("add")) {
            // レベルを追加
            int newLevel = Math.min(currentLevel + value, maxLevel);
            long requiredExp = ExperienceCalculator.getRequiredExperience(job, newLevel);
            
            jobManager.setExperienceAndLevel(targetPlayer, requiredExp);
            
            // キャッシュから即座に読み込んで確認
            int actualLevel = jobManager.getPlayerLevel(targetPlayer);
            
            sender.sendMessage("§a[成功] " + targetPlayer.getName() + " にレベル+" + value + " を追加しました (新レベル: " + actualLevel + ")");
            targetPlayer.sendMessage("§b[通知] レベルが " + actualLevel + " になりました!");
            
            // レベル表示を更新
            if (levelDisplayListener != null) {
                levelDisplayListener.updateLevelDisplay(targetPlayer);
            }

        } else if (operation.equals("remove")) {
            // レベルを削除
            int newLevel = Math.max(1, currentLevel - value);
            long requiredExp = ExperienceCalculator.getRequiredExperience(job, newLevel);
            
            jobManager.setExperienceAndLevel(targetPlayer, requiredExp);
            
            // キャッシュから即座に読み込んで確認
            int actualLevel = jobManager.getPlayerLevel(targetPlayer);
            
            sender.sendMessage("§a[成功] " + targetPlayer.getName() + " からレベル-" + value + " を削除しました (新レベル: " + actualLevel + ")");
            targetPlayer.sendMessage("§b[通知] レベルが " + actualLevel + " になりました");
            
            // レベル表示を更新
            if (levelDisplayListener != null) {
                levelDisplayListener.updateLevelDisplay(targetPlayer);
            }

        } else {
            sender.sendMessage("§c[エラー] 不正な操作です。'set', 'add', または 'remove' を指定してください");
            return true;
        }

        return true;
    }

    /**
     * 使用方法を表示
     */
    private void showUsage(CommandSender sender) {
        sender.sendMessage("§e使用方法:");
        sender.sendMessage("§e  /level <プレイヤー名> exp <set/add/remove> <数値>");
        sender.sendMessage("§e  /level <プレイヤー名> level <set/add/remove> <数値>");
        sender.sendMessage("§e例:");
        sender.sendMessage("§e  /level Player1 exp set 10000   - 経験値を10000に設定");
        sender.sendMessage("§e  /level Player1 exp add 1000     - 1000の経験値を追加");
        sender.sendMessage("§e  /level Player1 level set 20     - レベルを20に設定");
        sender.sendMessage("§e  /level Player1 level add 5      - レベル+5");
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

        // タイプの補完
        if (args.length == 2) {
            completions.add("exp");
            completions.add("level");
        }

        // 操作の補完
        if (args.length == 3) {
            completions.add("set");
            completions.add("add");
            completions.add("remove");
        }

        return completions;
    }
}
