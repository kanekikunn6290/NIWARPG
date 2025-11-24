package com.mmorpg.mining;

import org.bukkit.Material;

/**
 * 鉱石の種類を定義するEnum
 * それぞれの鉱石が持つべきMaterial, デフォルト耐久値, ドロップアイテムを定義
 */
public enum OreType {
    STONE("丸石", Material.COBBLESTONE, 50.0, Material.COBBLESTONE),
    COAL_ORE("石炭鉱石", Material.COAL_ORE, 80.0, Material.COAL),
    IRON_ORE("鉄鉱石", Material.IRON_ORE, 150.0, Material.RAW_IRON),
    DIAMOND_ORE("ダイヤモンド鉱石", Material.DIAMOND_ORE, 300.0, Material.DIAMOND);

    private final String displayName;
    private final Material blockMaterial;
    private final double defaultDurability;
    private final Material dropItem;

    OreType(String displayName, Material blockMaterial, double defaultDurability, Material dropItem) {
        this.displayName = displayName;
        this.blockMaterial = blockMaterial;
        this.defaultDurability = defaultDurability;
        this.dropItem = dropItem;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public double getDefaultDurability() {
        return defaultDurability;
    }

    public Material getDropItem() {
        return dropItem;
    }

    public static OreType fromString(String name) {
        for (OreType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
