package com.mmorpg.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MOB固有の基礎経験値を管理するクラス
 */
public class MobExpManager {
    private static final String CONFIG_FILE_NAME = "mobexp.yml";
    private static final int DEFAULT_BASE_EXP = 100;
    
    private final File configFile;
    private FileConfiguration config;
    private final Map<String, Integer> mobExpCache;

    public MobExpManager(File dataFolder) {
        this.configFile = new File(dataFolder, CONFIG_FILE_NAME);
        this.mobExpCache = new HashMap<>();
        loadConfig();
    }

    /**
     * 設定ファイルを読み込む
     */
    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadCacheFromConfig();
    }

    /**
     * デフォルト設定を作成
     */
    private void createDefaultConfig() {
        config = new YamlConfiguration();
        
        // Minecraft標準MOBの基礎経験値を設定
        config.set("mobs.zombie", 100);
        config.set("mobs.skeleton", 150);
        config.set("mobs.creeper", 200);
        config.set("mobs.spider", 120);
        config.set("mobs.cave_spider", 150);
        config.set("mobs.enderman", 250);
        config.set("mobs.witch", 300);
        config.set("mobs.blaze", 350);
        config.set("mobs.ghast", 400);
        config.set("mobs.wither_skeleton", 500);
        config.set("mobs.slime", 80);
        config.set("mobs.magma_cube", 100);
        config.set("mobs.guardian", 450);
        config.set("mobs.elder_guardian", 800);
        
        saveConfig();
    }

    /**
     * キャッシュを設定ファイルから読み込む
     */
    private void loadCacheFromConfig() {
        mobExpCache.clear();
        if (config.contains("mobs")) {
            for (String mobName : config.getConfigurationSection("mobs").getKeys(false)) {
                int exp = config.getInt("mobs." + mobName, DEFAULT_BASE_EXP);
                mobExpCache.put(mobName.toLowerCase(), exp);
            }
        }
    }

    /**
     * 設定ファイルを保存
     */
    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * MOBの基礎経験値を取得
     * 
     * @param entityType MOBのタイプ
     * @return 基礎経験値
     */
    public int getBaseExp(EntityType entityType) {
        if (entityType == null) {
            return DEFAULT_BASE_EXP;
        }
        String mobName = entityType.name().toLowerCase();
        return mobExpCache.getOrDefault(mobName, DEFAULT_BASE_EXP);
    }

    /**
     * MOBの基礎経験値を設定
     * 
     * @param mobName MOB名（英字、小文字）
     * @param exp 経験値
     */
    public void setBaseExp(String mobName, int exp) {
        if (exp < 0) {
            return;
        }
        
        mobName = mobName.toLowerCase();
        mobExpCache.put(mobName, exp);
        config.set("mobs." + mobName, exp);
        saveConfig();
    }

    /**
     * キャッシュをリロード
     */
    public void reload() {
        loadConfig();
    }

    /**
     * すべてのMOB名を取得
     */
    public java.util.Set<String> getAllMobNames() {
        return mobExpCache.keySet();
    }
}
