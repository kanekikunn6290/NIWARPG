package com.mmorpg.command;

import com.mmorpg.util.MobExpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MOBの基礎経験値を設定するコマンド
 * /mobexp <mobname> <value>
 */
public class MobExpCommand implements CommandExecutor, TabCompleter {
    private final MobExpManager mobExpManager;

    public MobExpCommand(MobExpManager mobExpManager) {
        this.mobExpManager = mobExpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // OP権限チェック
        if (!sender.isOp()) {
            sender.sendMessage("§c[エラー] このコマンドはOPのみが実行できます");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c[エラー] 使用方法: /mobexp <MOB名> <経験値>");
            sender.sendMessage("§e例: /mobexp zombie 100");
            return true;
        }

        String mobName = args[0];
        String expStr = args[1];

        // 経験値が数値か確認
        int exp;
        try {
            exp = Integer.parseInt(expStr);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[エラー] 経験値は数値である必要があります");
            return true;
        }

        // 経験値が0以上か確認
        if (exp < 0) {
            sender.sendMessage("§c[エラー] 経験値は0以上である必要があります");
            return true;
        }

        // MOB名が有効か確認（EntityTypeに存在するか）
        EntityType entityType = null;
        try {
            entityType = EntityType.valueOf(mobName.toUpperCase());
            if (!entityType.isAlive()) {
                sender.sendMessage("§c[エラー] \"" + mobName + "\"は生きているMOBではありません");
                return true;
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c[エラー] \"" + mobName + "\"は存在しないMOBです");
            sender.sendMessage("§e例: zombie, skeleton, creeper, spider など");
            return true;
        }

        // 経験値を設定
        mobExpManager.setBaseExp(mobName.toLowerCase(), exp);
        sender.sendMessage("§a[成功] " + mobName + " の基礎経験値を " + exp + " に設定しました");
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            // MOB名の補完
            List<String> mobNames = new ArrayList<>();
            String input = args[0].toLowerCase();
            
            for (EntityType entityType : EntityType.values()) {
                if (entityType.isAlive()) {
                    String name = entityType.name().toLowerCase();
                    if (name.startsWith(input)) {
                        mobNames.add(name);
                    }
                }
            }
            
            return mobNames;
        } else if (args.length == 2) {
            // 経験値の補完（例示）
            return Arrays.asList("100", "150", "200", "250", "300", "350");
        }

        return new ArrayList<>();
    }
}
