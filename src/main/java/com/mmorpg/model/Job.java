package com.mmorpg.model;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * プレイヤーの職業を定義するEnum
 */
public enum Job {
    UNEMPLOYED("無職", NamedTextColor.GRAY, "Tier 0"),
    FRONTLINER("フロントライナー", NamedTextColor.RED, "Tier 1 - 戦闘系"),
    ARTISAN("アーティザン", NamedTextColor.GOLD, "Tier 1 - 生産系"),
    BUILDER("ビルダー", NamedTextColor.BLUE, "Tier 1 - 建築系"),
    HARVESTER("ハーベスター", NamedTextColor.GREEN, "Tier 1 - 採取系"),
    MYSTIC("ミスティック", NamedTextColor.LIGHT_PURPLE, "Tier 1 - 支援系");

    private final String displayName;
    private final TextColor color;
    private final String description;

    Job(String displayName, TextColor color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    /**
     * 職業の表示名を取得
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 職業の色を取得
     */
    public TextColor getColor() {
        return color;
    }

    /**
     * 職業の説明を取得
     */
    public String getDescription() {
        return description;
    }

    /**
     * 職業の接頭辞を取得（チャット表示用）
     */
    public String getPrefix() {
        return "§" + colorToCode(color) + "[" + displayName + "]§r ";
    }

    /**
     * TextColorをカラーコードに変換
     */
    private static String colorToCode(TextColor color) {
        if (color == NamedTextColor.GRAY) return "7";
        if (color == NamedTextColor.RED) return "c";
        if (color == NamedTextColor.GOLD) return "6";
        if (color == NamedTextColor.BLUE) return "9";
        if (color == NamedTextColor.GREEN) return "a";
        if (color == NamedTextColor.LIGHT_PURPLE) return "d";
        return "f";
    }

    /**
     * 職業名から職業Enumを取得
     */
    public static Job fromName(String name) {
        for (Job job : Job.values()) {
            if (job.displayName.equalsIgnoreCase(name) || job.name().equalsIgnoreCase(name)) {
                return job;
            }
        }
        return null;
    }
}
