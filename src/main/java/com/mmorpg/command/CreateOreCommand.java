package com.mmorpg.command;

import com.mmorpg.mining.MiningManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 新しい鉱石を生成するコマンド /createore <material> <mythic_item> <durability> <quantity>
 */
public class CreateOreCommand implements CommandExecutor, TabCompleter {
    private final MiningManager miningManager;

    public CreateOreCommand(MiningManager miningManager) {
        this.miningManager = miningManager;
    }

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

        if (args.length < 4) {
            player.sendMessage("§c使用法: /createore <ブロック見た目> <MythicItemID> <耐久値> <戦利品量>");
            return true;
        }

        Material material = Material.matchMaterial(args[0].toUpperCase());
        if (material == null || !material.isBlock()) {
            player.sendMessage("§c無効なブロック名です: " + args[0]);
            return true;
        }

        String mythicItemId = args[1];

        double durability;
        try {
            durability = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c耐久値には数値を指定してください。");
            return true;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c戦利品量には整数を指定してください。");
            return true;
        }

        Location loc = player.getTargetBlock(null, 10).getLocation().add(0, 1, 0);

        miningManager.createOre(loc, material, mythicItemId, durability, quantity);
        player.sendMessage("§a新しい鉱石 (見た目: " + material.name() + ", アイテム: " + mythicItemId + ") を設置しました。");

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Material.values())
                    .filter(Material::isBlock)
                    .map(Enum::name)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Collections.singletonList("<MythicItemID>");
        }
        if (args.length == 3) {
            return Collections.singletonList("<耐久値>");
        }
        if (args.length == 4) {
            return Collections.singletonList("<戦利品量>");
        }
        return null;
    }
}
