package com.mmorpg.command;

import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.model.Job;
import com.mmorpg.util.ExperienceCalculator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /status コマンド - プレイヤーのステータスを表示
 */
public class StatusCommand implements CommandExecutor {
    private final PlayerJobManager jobManager;

    public StatusCommand(PlayerJobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // プレイヤーのみ実行可能
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみが実行できます");
            return true;
        }

        Player player = (Player) sender;
        showStatus(player);
        return true;
    }

    /**
     * ファンタジー風のステータス表示
     */
    private void showStatus(Player player) {
        Job job = jobManager.getPlayerJob(player);
        int level = jobManager.getPlayerLevel(player);
        long experience = jobManager.getPlayerExperience(player);
        int maxLevel = ExperienceCalculator.getMaxLevel(job);

        // ヘッダー
        player.sendMessage(Component.text("╔══════════════════════════════════════╗")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        player.sendMessage(Component.text("║ ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("   ⚔ PLAYER STATUS ⚔   ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" ║", NamedTextColor.GOLD)));

        player.sendMessage(Component.text("╠══════════════════════════════════════╣")
                .color(NamedTextColor.GOLD));

        // プレイヤー名
        player.sendMessage(Component.text("║ ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("👤 プレイヤー: ", NamedTextColor.WHITE))
                .append(Component.text(player.getName(), NamedTextColor.AQUA))
                .append(Component.text(" ".repeat(Math.max(0, 22 - player.getName().length())), NamedTextColor.WHITE))
                .append(Component.text("║", NamedTextColor.GOLD)));

        // 職業情報
        String jobDisplay = job.getDisplayName();
        String jobDesc = job.getDescription();
        player.sendMessage(Component.text("║ ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("💼 職業: ", NamedTextColor.WHITE))
                .append(Component.text(jobDisplay, NamedTextColor.YELLOW))
                .append(Component.text(" ".repeat(Math.max(0, 19 - jobDisplay.length())), NamedTextColor.WHITE))
                .append(Component.text("║", NamedTextColor.GOLD)));

        player.sendMessage(Component.text("║ ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("   ", NamedTextColor.WHITE))
                .append(Component.text(jobDesc, NamedTextColor.GRAY))
                .append(Component.text(" ".repeat(Math.max(0, 27 - jobDesc.length())), NamedTextColor.WHITE))
                .append(Component.text("║", NamedTextColor.GOLD)));

        player.sendMessage(Component.text("╠══════════════════════════════════════╣")
                .color(NamedTextColor.GOLD));

        // レベル情報
        player.sendMessage(Component.text("║ ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("📊 レベル: ", NamedTextColor.WHITE))
                .append(Component.text(String.format("%2d", level), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" / ", NamedTextColor.WHITE))
                .append(Component.text(String.valueOf(maxLevel), NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(" ".repeat(Math.max(0, 14 - String.valueOf(maxLevel).length())), NamedTextColor.WHITE))
                .append(Component.text("║", NamedTextColor.GOLD)));

        // 経験値プログレスバー
        if (level >= maxLevel) {
            // 最大レベルに達している場合
            player.sendMessage(Component.text("║ ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text("⚡ 経験値: ", NamedTextColor.WHITE))
                    .append(Component.text("MAX", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" ".repeat(20), NamedTextColor.WHITE))
                    .append(Component.text("║", NamedTextColor.GOLD)));
        } else {
            // 経験値バーを表示
            long currentLevelExp = ExperienceCalculator.getRequiredExperience(job, level);
            long nextLevelExp = ExperienceCalculator.getRequiredExperience(job, level + 1);
            long currentProgress = experience - currentLevelExp;
            long requiredProgress = nextLevelExp - currentLevelExp;
            
            float progressPercent = (currentProgress * 100.0f) / requiredProgress;
            int barLength = 20;
            int filledLength = (int) (progressPercent / 5.0f); // 20文字 / 100% = 0.2文字/%
            
            StringBuilder expBar = new StringBuilder();
            for (int i = 0; i < barLength; i++) {
                if (i < filledLength) {
                    expBar.append("█");
                } else {
                    expBar.append("░");
                }
            }
            
            player.sendMessage(Component.text("║ ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text("⚡ 経験値: ", NamedTextColor.WHITE))
                    .append(Component.text(String.format("%.1f%%", progressPercent), NamedTextColor.YELLOW))
                    .append(Component.text(" ", NamedTextColor.WHITE))
                    .append(Component.text("║", NamedTextColor.GOLD)));
            
            // プログレスバー表示
            Component barComponent = Component.text("║ ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text("   ", NamedTextColor.WHITE));
            
            for (int i = 0; i < barLength; i++) {
                if (i < filledLength) {
                    barComponent = barComponent.append(Component.text("█", NamedTextColor.GREEN));
                } else {
                    barComponent = barComponent.append(Component.text("░", NamedTextColor.DARK_GRAY));
                }
            }
            
            barComponent = barComponent
                    .append(Component.text(" ", NamedTextColor.WHITE))
                    .append(Component.text("║", NamedTextColor.GOLD));
            
            player.sendMessage(barComponent);
            
            // 経験値の詳細
            long nextLevelRequiredExp = ExperienceCalculator.getExperienceToNextLevel(job, level, experience);
            player.sendMessage(Component.text("║ ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text("   ", NamedTextColor.WHITE))
                    .append(Component.text(String.format("%,d", currentProgress), NamedTextColor.AQUA))
                    .append(Component.text(" / ", NamedTextColor.WHITE))
                    .append(Component.text(String.format("%,d", requiredProgress), NamedTextColor.AQUA))
                    .append(Component.text(" ".repeat(Math.max(0, 10 - String.valueOf(nextLevelRequiredExp).length())), NamedTextColor.WHITE))
                    .append(Component.text("║", NamedTextColor.GOLD)));
        }

        player.sendMessage(Component.text("╚══════════════════════════════════════╝")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
    }
}
