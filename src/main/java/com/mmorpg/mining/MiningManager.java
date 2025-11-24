package com.mmorpg.mining;

import com.mmorpg.MMORPGPlugin;
import com.mmorpg.mining.spawner.OreSpawnerManager;
import com.mmorpg.util.MiningPickaxeManager;
import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MiningManager {
    private final MMORPGPlugin plugin;
    private final Map<UUID, UUID> miningSessions = new HashMap<>();
    private BukkitTask mainTask;
    private OreSpawnerManager oreSpawnerManager;

    private static final double MINING_DISTANCE_SQUARED = 25;
    private static final int TICK_INTERVAL = 4;

    // 一時的な状態保持用（ダメージ量などはリセットされても良いためメタデータを使用）
    private static final String DAMAGE_MAP_KEY = "mmorpg_damage_map";
    
    // 永続的なデータ保存用 (PDCキー)
    private final NamespacedKey IS_ORE_KEY;
    private final NamespacedKey ORE_MATERIAL_KEY;
    private final NamespacedKey ORE_MYTHIC_ITEM_KEY;
    private final NamespacedKey ORE_MAX_DURABILITY_KEY;
    private final NamespacedKey ORE_CURRENT_DURABILITY_KEY;
    private final NamespacedKey ORE_LOOT_QUANTITY_KEY;

    public MiningManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        // NamespacedKeyの初期化
        this.IS_ORE_KEY = new NamespacedKey(plugin, "is_ore");
        this.ORE_MATERIAL_KEY = new NamespacedKey(plugin, "ore_material");
        this.ORE_MYTHIC_ITEM_KEY = new NamespacedKey(plugin, "ore_mythic_item");
        this.ORE_MAX_DURABILITY_KEY = new NamespacedKey(plugin, "ore_max_durability");
        this.ORE_CURRENT_DURABILITY_KEY = new NamespacedKey(plugin, "ore_current_durability");
        this.ORE_LOOT_QUANTITY_KEY = new NamespacedKey(plugin, "ore_loot_quantity");
    }

    public void setOreSpawnerManager(OreSpawnerManager oreSpawnerManager) {
        this.oreSpawnerManager = oreSpawnerManager;
    }

    public void startup() {
        if (mainTask != null && !mainTask.isCancelled()) return;
        this.mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, TICK_INTERVAL);
    }

    public void shutdown() {
        if (mainTask != null) mainTask.cancel();
        mainTask = null;
        miningSessions.clear();
    }

    private void tick() {
        if (miningSessions.isEmpty()) return;

        new HashSet<>(miningSessions.keySet()).forEach(playerUUID -> {
            UUID oreUUID = miningSessions.get(playerUUID);
            if (oreUUID == null) return;

            Player player = Bukkit.getPlayer(playerUUID);
            Entity entity = Bukkit.getEntity(oreUUID);

            if (player == null || !player.isOnline() || !(entity instanceof ArmorStand) || entity.isDead()) {
                stopMiningSession(playerUUID);
                return;
            }
            ArmorStand ore = (ArmorStand) entity;
            if (player.getLocation().distanceSquared(ore.getLocation()) > MINING_DISTANCE_SQUARED) {
                stopMiningSession(playerUUID);
                player.sendActionBar(Component.text("§c鉱石から離れすぎたため採掘を中断しました。"));
                return;
            }

            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!MiningPickaxeManager.isMiningPickaxe(tool)) {
                stopMiningSession(playerUUID);
                player.sendActionBar(Component.text("§c専用のピッケルを手に持ってください。"));
                return;
            }

            double damagePerSecond = MiningPickaxeManager.getPickaxeDamage(tool);
            double damagePerTick = damagePerSecond * (TICK_INTERVAL / 20.0);
            
            // PDCから耐久値を取得
            double currentDurability = getDoublePDC(ore, ORE_CURRENT_DURABILITY_KEY, 0.0);
            double newDurability = currentDurability - damagePerTick;

            Map<UUID, Double> damageMap = getDamageMap(ore);
            damageMap.merge(playerUUID, damagePerTick, Double::sum);
            setDamageMap(ore, damageMap);

            ore.getWorld().playSound(ore.getLocation(), Sound.BLOCK_STONE_HIT, 0.5f, 1.2f);

            if (newDurability <= 0) {
                completeMining(ore);
            } else {
                // PDCに耐久値を保存
                setDoublePDC(ore, ORE_CURRENT_DURABILITY_KEY, newDurability);
                updateOreDisplayName(ore);
            }
        });
    }

    public void startMiningSession(Player player, ArmorStand ore) {
        if (miningSessions.containsKey(player.getUniqueId())) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!MiningPickaxeManager.isMiningPickaxe(tool)) {
            player.sendActionBar(Component.text("§c専用のピッケルで採掘してください。"));
            return;
        }
        miningSessions.put(player.getUniqueId(), ore.getUniqueId());
        player.sendActionBar(Component.text("§a採掘を開始しました..."));
    }

    public void stopMiningSession(UUID playerUUID) {
        miningSessions.remove(playerUUID);
    }

    private void completeMining(ArmorStand ore) {
        miningSessions.entrySet().removeIf(entry -> entry.getValue().equals(ore.getUniqueId()));
        ore.getWorld().playSound(ore.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
        ore.getWorld().playSound(ore.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        distributeLoot(ore);

        if (oreSpawnerManager != null) {
            oreSpawnerManager.onOreDestroyed(ore.getUniqueId());
        }

        ore.remove();
    }
    
    public void removeOre(UUID oreUuid) {
        Entity entity = Bukkit.getEntity(oreUuid);
        if (entity instanceof ArmorStand && isOre(entity)) {
            miningSessions.entrySet().removeIf(entry -> entry.getValue().equals(oreUuid));
            entity.remove();
        }
    }

    private void distributeLoot(ArmorStand ore) {
        Map<UUID, Double> damageMap = getDamageMap(ore);
        if (damageMap.isEmpty()) return;

        String mythicItemId = getStringPDC(ore, ORE_MYTHIC_ITEM_KEY, null);
        if (mythicItemId == null) return;
        
        Optional<MythicItem> mythicItemOpt = MythicBukkit.inst().getItemManager().getItem(mythicItemId);
        if (mythicItemOpt.isEmpty()) {
            Bukkit.getLogger().warning("[MMORPG] 指定されたMythicItemIDが見つかりません: " + mythicItemId);
            return;
        }
        MythicItem mythicItem = mythicItemOpt.get();

        int totalQuantity = getIntPDC(ore, ORE_LOOT_QUANTITY_KEY, 1);
        double maxDurability = getDoublePDC(ore, ORE_MAX_DURABILITY_KEY, 1.0);
        
        if (totalQuantity == 1) {
            damageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .ifPresent(winnerUUID -> {
                    Player winner = Bukkit.getPlayer(winnerUUID);
                    if (winner != null && winner.isOnline()) {
                        AbstractItemStack abstractItem = mythicItem.generateItemStack(1);
                        if (abstractItem != null) {
                            giveItems(winner, BukkitAdapter.adapt(abstractItem));
                        }
                    }
                });
        } else {
            damageMap.forEach((playerUUID, damageDealt) -> {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null && player.isOnline()) {
                    double contribution = damageDealt / maxDurability;
                    int amountToGive = (int) (totalQuantity * contribution);
                    if (amountToGive > 0) {
                        AbstractItemStack abstractItem = mythicItem.generateItemStack(amountToGive);
                        if (abstractItem != null) {
                           giveItems(player, BukkitAdapter.adapt(abstractItem));
                        }
                    }
                }
            });
        }
    }

    private void giveItems(Player player, ItemStack items) {
        player.getInventory().addItem(items).forEach((index, item) -> 
            player.getWorld().dropItemNaturally(player.getLocation(), item)
        );
    }
    
    public ArmorStand createOre(Location location, Material blockMat, String mythicId, double durability, int quantity) {
        // 重複防止：同じ場所に古い鉱石があれば削除
        cleanupOldOresAt(location);

        double yOffset = -1.4;
        Location spawnLocation = location.clone().add(0.5, yOffset, 0.5);
        ArmorStand ore = (ArmorStand) location.getWorld().spawnEntity(spawnLocation, EntityType.ARMOR_STAND);

        ore.setVisible(false);
        ore.setGravity(false);
        ore.setMarker(false); 
        ore.setInvulnerable(true);
        ore.getEquipment().setHelmet(new ItemStack(blockMat));

        // PDC にデータを保存 (サーバー再起動後も残る)
        setBytePDC(ore, IS_ORE_KEY, (byte) 1);
        setStringPDC(ore, ORE_MATERIAL_KEY, blockMat.name());
        setStringPDC(ore, ORE_MYTHIC_ITEM_KEY, mythicId);
        setDoublePDC(ore, ORE_MAX_DURABILITY_KEY, durability);
        setDoublePDC(ore, ORE_CURRENT_DURABILITY_KEY, durability);
        setIntPDC(ore, ORE_LOOT_QUANTITY_KEY, quantity);
        
        // ダメージマップは一時的なのでMetadataでOK
        setDamageMap(ore, new HashMap<>());

        updateOreDisplayName(ore);
        ore.setCustomNameVisible(true);
        return ore;
    }

    /**
     * 指定座標付近の古い鉱石エンティティを削除する
     */
    public void cleanupOldOresAt(Location location) {
        if (location.getWorld() == null) return;
        // 指定座標の半径1ブロック以内のエンティティを検索
        location.getWorld().getNearbyEntities(location.clone().add(0.5, 0, 0.5), 1, 2, 1).stream()
                .filter(this::isOre)
                .forEach(Entity::remove);
    }

    public boolean isOre(Entity entity) {
        if (!(entity instanceof ArmorStand)) return false;
        // PDCをチェック
        return entity.getPersistentDataContainer().has(IS_ORE_KEY, PersistentDataType.BYTE);
    }

    // --- Helper Methods ---

    @SuppressWarnings("unchecked")
    private Map<UUID, Double> getDamageMap(ArmorStand ore) {
        if (ore.hasMetadata(DAMAGE_MAP_KEY)) {
            return (Map<UUID, Double>) ore.getMetadata(DAMAGE_MAP_KEY).get(0).value();
        }
        return new HashMap<>();
    }
    
    private void setDamageMap(ArmorStand ore, Map<UUID, Double> map) {
        ore.setMetadata(DAMAGE_MAP_KEY, new FixedMetadataValue(plugin, map));
    }
    
    private void updateOreDisplayName(ArmorStand ore) {
        double current = getDoublePDC(ore, ORE_CURRENT_DURABILITY_KEY, 0.0);
        String materialName = getStringPDC(ore, ORE_MATERIAL_KEY, "Unknown");
        double maxDurability = getDoublePDC(ore, ORE_MAX_DURABILITY_KEY, 1.0);
        double progress = Math.max(0, current) / maxDurability;
        
        String name = "§f" + materialName + " " + createProgressBar(progress);
        ore.setCustomName(name);
    }

    private static String createProgressBar(double progress) {
        int totalBars = 10;
        int greenBars = (int) (totalBars * progress);
        return "§f[" + "§a|".repeat(greenBars) + "§7|".repeat(totalBars - greenBars) + "§f]";
    }

    // PDC Helpers
    private void setStringPDC(Entity entity, NamespacedKey key, String value) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
    }
    private String getStringPDC(Entity entity, NamespacedKey key, String def) {
        return entity.getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, def);
    }
    private void setDoublePDC(Entity entity, NamespacedKey key, double value) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
    }
    private double getDoublePDC(Entity entity, NamespacedKey key, double def) {
        return entity.getPersistentDataContainer().getOrDefault(key, PersistentDataType.DOUBLE, def);
    }
    private void setIntPDC(Entity entity, NamespacedKey key, int value) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
    }
    private int getIntPDC(Entity entity, NamespacedKey key, int def) {
        return entity.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, def);
    }
    private void setBytePDC(Entity entity, NamespacedKey key, byte value) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, value);
    }
}
