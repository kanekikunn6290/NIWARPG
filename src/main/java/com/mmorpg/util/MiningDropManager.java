package com.mmorpg.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 採掘対象Oreのドロップアイテム管理
 */
public class MiningDropManager {
    
    // Ore名 → ドロップアイテムのマッピング
    private static final Map<String, ItemStack> dropMap = new HashMap<>();
    
    static {
        // デフォルトドロップを設定
        setupDefaultDrops();
    }
    
    /**
     * デフォルトドロップを設定
     */
    private static void setupDefaultDrops() {
        // 丸石
        dropMap.put("丸石", createOreItem("丸石", Material.COBBLESTONE));
    }
    
    /**
     * ドロップアイテムを作成
     */
    private static ItemStack createOreItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        return item;
    }
    
    /**
     * Ore名に対応するドロップアイテムを取得
     */
    public static ItemStack getDropItem(String oreName) {
        ItemStack drop = dropMap.get(oreName);
        if (drop != null) {
            return drop.clone();
        }
        // デフォルト：石
        return new ItemStack(Material.STONE);
    }
    
    /**
     * ドロップアイテムを設定（カスタマイズ用）
     */
    public static void setDropItem(String oreName, ItemStack item) {
        if (item != null) {
            dropMap.put(oreName, item.clone());
        } else {
            dropMap.remove(oreName);
        }
    }
    
    /**
     * すべてのドロップアイテムをリセット
     */
    public static void resetDrops() {
        dropMap.clear();
        setupDefaultDrops();
    }
}
