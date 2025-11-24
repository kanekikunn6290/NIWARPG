package com.mmorpg.util;

import com.mmorpg.model.Job;

/**
 * 経験値計算とレベルシステムを管理するクラス
 */
public class ExperienceCalculator {
    // 無職の最大レベル
    private static final int UNEMPLOYED_MAX_LEVEL = 10;
    // 一次職業の最大レベル
    private static final int PRIMARY_JOB_MAX_LEVEL = 30;
    
    // 無職の最大経験値（10レベルまで10000）
    private static final long UNEMPLOYED_TOTAL_EXP = 10000;
    // 一次職業の最大経験値（30レベルまで160000）
    private static final long PRIMARY_JOB_TOTAL_EXP = 160000;

    /**
     * 各レベルの累積経験値テーブルを計算
     * 二次関数: f(x) = x^1.4 (係数調整で目標値に収束)
     */
    private static final long[] UNEMPLOYED_EXPERIENCE_TABLE = new long[11]; // レベル0-10
    private static final long[] EXPERIENCE_TABLE = new long[31]; // レベル0-30

    static {
        // ===== 無職の経験値テーブル =====
        // 各レベルの差分を計算してから累積値を算出
        double unemployedCoefficient = UNEMPLOYED_TOTAL_EXP / Math.pow(UNEMPLOYED_MAX_LEVEL, 1.4);
        
        UNEMPLOYED_EXPERIENCE_TABLE[0] = 0; // レベル0（未使用）
        UNEMPLOYED_EXPERIENCE_TABLE[1] = 0; // レベル1は0から開始
        
        for (int level = 2; level <= UNEMPLOYED_MAX_LEVEL; level++) {
            // 各レベルの二次関数値を計算
            double currentValue = unemployedCoefficient * Math.pow(level, 1.4);
            double prevValue = unemployedCoefficient * Math.pow(level - 1, 1.4);
            
            // 各レベルで必要な差分経験値を計算
            long levelExp = Math.round(currentValue - prevValue);
            // 最低1経験値は必要
            levelExp = Math.max(levelExp, 1);
            
            // 累積経験値 = 前のレベルの累積 + この段階での必要経験値
            UNEMPLOYED_EXPERIENCE_TABLE[level] = UNEMPLOYED_EXPERIENCE_TABLE[level - 1] + levelExp;
        }
        
        // ===== 一次職業の経験値テーブル =====
        // 各レベルの差分を計算してから累積値を算出
        double coefficient = PRIMARY_JOB_TOTAL_EXP / Math.pow(PRIMARY_JOB_MAX_LEVEL, 1.4);
        
        EXPERIENCE_TABLE[0] = 0; // レベル0（未使用）
        EXPERIENCE_TABLE[1] = 0; // レベル1は0から開始
        
        for (int level = 2; level <= PRIMARY_JOB_MAX_LEVEL; level++) {
            // 各レベルの二次関数値を計算
            double currentValue = coefficient * Math.pow(level, 1.4);
            double prevValue = coefficient * Math.pow(level - 1, 1.4);
            
            // 各レベルで必要な差分経験値を計算
            long levelExp = Math.round(currentValue - prevValue);
            // 最低1経験値は必要
            levelExp = Math.max(levelExp, 1);
            
            // 累積経験値 = 前のレベルの累積 + この段階での必要経験値
            EXPERIENCE_TABLE[level] = EXPERIENCE_TABLE[level - 1] + levelExp;
        }
    }

    /**
     * 職業の最大レベルを取得
     */
    public static int getMaxLevel(Job job) {
        if (job == Job.UNEMPLOYED) {
            return UNEMPLOYED_MAX_LEVEL;
        }
        return PRIMARY_JOB_MAX_LEVEL;
    }

    /**
     * 指定レベルに必要な累積経験値を取得
     */
    public static long getRequiredExperience(Job job, int level) {
        // レベルの範囲チェック
        int maxLevel = getMaxLevel(job);
        if (level < 1 || level > maxLevel) {
            return -1;
        }

        // レベル1は常に経験値0で開始
        if (level == 1) {
            return 0;
        }

        // 無職の場合は無職用テーブルから取得
        if (job == Job.UNEMPLOYED) {
            return UNEMPLOYED_EXPERIENCE_TABLE[level];
        }

        // 一次職業の経験値テーブルから取得
        return EXPERIENCE_TABLE[level];
    }

    /**
     * 現在の経験値からレベルを計算
     */
    public static int calculateLevel(Job job, long currentExperience) {
        int maxLevel = getMaxLevel(job);

        // 経験値からレベルを逆算
        for (int level = maxLevel; level >= 1; level--) {
            if (currentExperience >= getRequiredExperience(job, level)) {
                return level;
            }
        }

        return 1;
    }

    /**
     * 次のレベルまでの必要経験値を計算
     */
    public static long getExperienceToNextLevel(Job job, int currentLevel, long currentExperience) {
        int maxLevel = getMaxLevel(job);

        // 最大レベルに達している場合
        if (currentLevel >= maxLevel) {
            return 0;
        }

        // 次のレベルの必要経験値
        long nextLevelExp = getRequiredExperience(job, currentLevel + 1);
        long remaining = nextLevelExp - currentExperience;

        return Math.max(0, remaining);
    }

    /**
     * 現在のレベルアップ進捗をパーセンテージで取得（0-100）
     */
    public static float getLevelUpProgress(Job job, int currentLevel, long currentExperience) {
        int maxLevel = getMaxLevel(job);

        // 最大レベルに達している場合
        if (currentLevel >= maxLevel) {
            return 100.0f;
        }

        // 現在のレベルの必要経験値
        long currentLevelExp = getRequiredExperience(job, currentLevel);
        long nextLevelExp = getRequiredExperience(job, currentLevel + 1);

        // 経験値がレベルの必要値より低い場合は0%
        if (currentExperience < currentLevelExp) {
            return 0.0f;
        }

        long progressExp = currentExperience - currentLevelExp;
        long requiredExp = nextLevelExp - currentLevelExp;

        // ゼロ除算防止
        if (requiredExp <= 0) {
            return 0.0f;
        }

        float progress = (progressExp * 100.0f) / requiredExp;
        
        // マイナス値防止
        return Math.max(0.0f, Math.min(100.0f, progress));
    }

    /**
     * 経験値が有効なレベルをクリップする
     */
    public static int clipLevel(Job job, int level) {
        int maxLevel = getMaxLevel(job);
        return Math.min(Math.max(level, 1), maxLevel);
    }
}
