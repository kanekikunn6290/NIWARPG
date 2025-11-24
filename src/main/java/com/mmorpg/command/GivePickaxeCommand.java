package com.mmorpg.command;

import com.mmorpg.util.MiningPickaxeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /givepickaxe <tier> コマンド
 * 指定したティアの採掘ピッケルを配布する
 */
public class GivePickaxeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }
        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage("§cこのコマンドを実行する権限がありません。");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§c使用法: /givepickaxe <ティア>");
            player.sendMessage("§eティアは1から4まで指定できます。");
            return true;
        }

        int tier;
        try {
            tier = Integer.parseInt(args[0]);
            if (tier < 1 || tier > 4) {
                player.sendMessage("§cティアは1から4の間で指定してください。");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cティアには数値を指定してください。");
            return true;
        }

        ItemStack pickaxe = MiningPickaxeManager.createMiningPickaxe(tier);
        player.getInventory().addItem(pickaxe);
        player.sendMessage("§aティア" + tier + "の採掘ピッケルを入手しました。");

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("1", "2", "3", "4").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return null;
    }
}
