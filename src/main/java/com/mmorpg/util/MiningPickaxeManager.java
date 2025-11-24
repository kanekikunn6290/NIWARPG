package com.mmorpg.util;

import com.mmorpg.MMORPGPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * 複数のティアを持つカスタムピッケルを管理するクラス
 */
public class MiningPickaxeManager {

    private static final String MINING_PICKAXE_KEY = "MINING_PICKAXE";
    private static final String PICKAXE_DAMAGE_KEY = "PICKAXE_DAMAGE";

    public static NamespacedKey getPickaxeNbtKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(MMORPGPlugin.class), MINING_PICKAXE_KEY);
    }

    public static NamespacedKey getPickaxeDamageNbtKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(MMORPGPlugin.class), PICKAXE_DAMAGE_KEY);
    }

    /**
     * 指定されたティアの採掘用カスタムピッケルを生成
     * @param tier ピッケルのティア (1-4)
     * @return 生成されたピッケルのItemStack
     */
    public static ItemStack createMiningPickaxe(int tier) {
        Material material;
        String displayName;
        double damage;

        switch (tier) {
            case 1:
                material = Material.WOODEN_PICKAXE;
                displayName = "§f駆け出しの採掘ピッケル";
                damage = 5.0;
                break;
            case 2:
                material = Material.STONE_PICKAXE;
                displayName = "§a一人前の採掘ピッケル";
                damage = 10.0;
                break;
            case 3:
                material = Material.IRON_PICKAXE;
                displayName = "§b熟練の採掘ピッケル";
                damage = 20.0;
                break;
            case 4:
            default: // 不正なティアはデフォルトで4とする
                material = Material.DIAMOND_PICKAXE;
                displayName = "§6伝説の採掘ピッケル";
                damage = 40.0;
                break;
        }

        ItemStack pickaxe = new ItemStack(material);
        ItemMeta meta = pickaxe.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(
                    "§7特殊な鉱石を採掘できるピッケル",
                    "§c採掘ダメージ: " + damage
            ));

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);


            // カスタムデータ（NBT）を設定
            meta.getPersistentDataContainer().set(getPickaxeNbtKey(), PersistentDataType.STRING, "true");
            meta.getPersistentDataContainer().set(getPickaxeDamageNbtKey(), PersistentDataType.DOUBLE, damage);

            pickaxe.setItemMeta(meta);
        }

        return pickaxe;
    }

    /**
     * アイテムがカスタムピッケルかどうかを判定
     */
    public static boolean isMiningPickaxe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        try {
            return meta.getPersistentDataContainer().has(getPickaxeNbtKey(), PersistentDataType.STRING);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ピッケルの採掘ダメージを取得
     */
    public static double getPickaxeDamage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1.0; // デフォルト値
        }

        ItemMeta meta = item.getItemMeta();
        try {
            Double damage = meta.getPersistentDataContainer().get(getPickaxeDamageNbtKey(), PersistentDataType.DOUBLE);
            return damage != null ? damage : 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }
}
