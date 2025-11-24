package com.mmorpg.util;

import org.bukkit.entity.LivingEntity;

/**
 * MOBランク係数を管理するユーティリティクラス
 * タグによってMOBのランク係数を取得
 */
public class MobRankManager {
    
    // ランクタグの定義
    public static final String TAG_NORMAL = "rank_normal";
    public static final String TAG_ENHANCED = "rank_enhanced";
    public static final String TAG_RARE = "rank_rare";
    public static final String TAG_ELITE = "rank_elite";
    public static final String TAG_BOSS = "rank_boss";

    /**
     * MOBのランク係数を取得
     * デフォルトは1.0（通常）
     * 
     * @param entity MOBエンティティ
     * @return ランク係数
     */
    public static double getRankCoefficient(LivingEntity entity) {
        if (entity == null) {
            return 1.0;
        }

        // タグからランクを判定
        if (entity.getScoreboardTags().contains(TAG_BOSS)) {
            return 4.0;
        } else if (entity.getScoreboardTags().contains(TAG_ELITE)) {
            return 2.5;
        } else if (entity.getScoreboardTags().contains(TAG_RARE)) {
            return 1.7;
        } else if (entity.getScoreboardTags().contains(TAG_ENHANCED)) {
            return 1.3;
        } else if (entity.getScoreboardTags().contains(TAG_NORMAL)) {
            return 1.0;
        }

        // タグがない場合はデフォルト
        return 1.0;
    }

    /**
     * MOBにランクタグを追加
     * 
     * @param entity MOBエンティティ
     * @param rankTag ランクタグ
     */
    public static void addRankTag(LivingEntity entity, String rankTag) {
        if (entity == null) {
            return;
        }

        // 既存のランクタグを削除
        entity.removeScoreboardTag(TAG_NORMAL);
        entity.removeScoreboardTag(TAG_ENHANCED);
        entity.removeScoreboardTag(TAG_RARE);
        entity.removeScoreboardTag(TAG_ELITE);
        entity.removeScoreboardTag(TAG_BOSS);

        // 新しいランクタグを追加
        if (rankTag.equals(TAG_BOSS) || rankTag.equals(TAG_ELITE) || 
            rankTag.equals(TAG_RARE) || rankTag.equals(TAG_ENHANCED) || 
            rankTag.equals(TAG_NORMAL)) {
            entity.addScoreboardTag(rankTag);
        }
    }

    /**
     * MOBのランク名を取得
     * 
     * @param entity MOBエンティティ
     * @return ランク名（日本語）
     */
    public static String getRankName(LivingEntity entity) {
        double coefficient = getRankCoefficient(entity);
        
        if (coefficient == 4.0) {
            return "ボス";
        } else if (coefficient == 2.5) {
            return "精鋭";
        } else if (coefficient == 1.7) {
            return "レア";
        } else if (coefficient == 1.3) {
            return "強化";
        } else {
            return "通常";
        }
    }

    /**
     * MOBのランク名からランクタグを取得
     * 
     * @param rankName ランク名
     * @return ランクタグ
     */
    public static String getRankTagFromName(String rankName) {
        return switch (rankName.toLowerCase()) {
            case "boss", "ボス" -> TAG_BOSS;
            case "elite", "精鋭" -> TAG_ELITE;
            case "rare", "レア" -> TAG_RARE;
            case "enhanced", "強化" -> TAG_ENHANCED;
            default -> TAG_NORMAL;
        };
    }
}
