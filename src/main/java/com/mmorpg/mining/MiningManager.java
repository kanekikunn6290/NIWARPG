package com.mmorpg.mining;

import com.mmorpg.MMORPGPlugin;
import com.mmorpg.mining.spawner.OreSpawnerManager;
import com.mmorpg.util.MiningPickaxeManager;
import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MiningManager {
    private final MMORPGPlugin plugin;
    private final Map<UUID, UUID> miningSessions = new HashMap<>();
    private BukkitTask mainTask;
    private OreSpawnerManager oreSpawnerManager;

    private static final double MINING_DISTANCE_SQUARED = 25;
    private static final int TICK_INTERVAL = 4;

    private static final String ORE_BLOCK_MATERIAL_KEY = "mmorpg_ore_material";
    private static final String ORE_MYTHIC_ITEM_KEY = "mmorpg_mythic_item";
    private static final String DURABILITY_KEY = "mmorpg_ore_durability";
    private static final String MAX_DURABILITY_KEY = "mmorpg_ore_max_durability";
    private static final String LOOT_QUANTITY_KEY = "mmorpg_loot_quantity";
    private static final String DAMAGE_MAP_KEY = "mmorpg_damage_map";
    private static final String IS_ORE_KEY = "mmorpg_is_ore";

    public MiningManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
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
                player.sendActionBar(net.kyori.adventure.text.Component.text("§c鉱石から離れすぎたため採掘を中断しました。"));
                return;
            }

            ItemStack tool = player.getInventory().getItemInMainHand();
            if (!MiningPickaxeManager.isMiningPickaxe(tool)) {
                stopMiningSession(playerUUID);
                player.sendActionBar(net.kyori.adventure.text.Component.text("§c専用のピッケルを手に持ってください。"));
                return;
            }

            double damagePerSecond = MiningPickaxeManager.getPickaxeDamage(tool);
            double damagePerTick = damagePerSecond * (TICK_INTERVAL / 20.0);
            double currentDurability = getDoubleMetadata(ore, DURABILITY_KEY, 0.0);
            double newDurability = currentDurability - damagePerTick;

            Map<UUID, Double> damageMap = getDamageMap(ore);
            damageMap.merge(playerUUID, damagePerTick, Double::sum);
            setDamageMap(ore, damageMap);

            ore.getWorld().playSound(ore.getLocation(), Sound.BLOCK_STONE_HIT, 0.5f, 1.2f);

            if (newDurability <= 0) {
                completeMining(ore);
            } else {
                ore.setMetadata(DURABILITY_KEY, new FixedMetadataValue(plugin, newDurability));
                updateOreDisplayName(ore);
            }
        });
    }

    public void startMiningSession(Player player, ArmorStand ore) {
        if (miningSessions.containsKey(player.getUniqueId())) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!MiningPickaxeManager.isMiningPickaxe(tool)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§c専用のピッケルで採掘してください。"));
            return;
        }
        miningSessions.put(player.getUniqueId(), ore.getUniqueId());
        player.sendActionBar(net.kyori.adventure.text.Component.text("§a採掘を開始しました..."));
    }

    public void stopMiningSession(UUID playerUUID) {
        miningSessions.remove(playerUUID);
    }

    private void completeMining(ArmorStand ore) {
        miningSessions.entrySet().removeIf(entry -> entry.getValue().equals(ore.getUniqueId()));
        ore.getWorld().playSound(ore.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
        ore.getWorld().playSound(ore.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        distributeLoot(ore);

        // Notify the spawner manager BEFORE removing the entity
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

        String mythicItemId = getStringMetadata(ore, ORE_MYTHIC_ITEM_KEY, null);
        if (mythicItemId == null) return;
        
        Optional<MythicItem> mythicItemOpt = MythicBukkit.inst().getItemManager().getItem(mythicItemId);
        if (mythicItemOpt.isEmpty()) {
            Bukkit.getLogger().warning("[MMORPG] 指定されたMythicItemIDが見つかりません: " + mythicItemId);
            return;
        }
        MythicItem mythicItem = mythicItemOpt.get();

        int totalQuantity = getIntMetadata(ore, LOOT_QUANTITY_KEY, 1);
        double maxDurability = getDoubleMetadata(ore, MAX_DURABILITY_KEY, 1.0);
        
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
        double yOffset = -1.4;
        Location spawnLocation = location.clone().add(0.5, yOffset, 0.5);
        ArmorStand ore = (ArmorStand) location.getWorld().spawnEntity(spawnLocation, EntityType.ARMOR_STAND);

        ore.setVisible(false);
        ore.setGravity(false);
        ore.setMarker(false);
        ore.setInvulnerable(true);
        ore.getEquipment().setHelmet(new ItemStack(blockMat));

        ore.setMetadata(IS_ORE_KEY, new FixedMetadataValue(plugin, true));
        ore.setMetadata(ORE_BLOCK_MATERIAL_KEY, new FixedMetadataValue(plugin, blockMat.name()));
        ore.setMetadata(ORE_MYTHIC_ITEM_KEY, new FixedMetadataValue(plugin, mythicId));
        ore.setMetadata(MAX_DURABILITY_KEY, new FixedMetadataValue(plugin, durability));
        ore.setMetadata(DURABILITY_KEY, new FixedMetadataValue(plugin, durability));
        ore.setMetadata(LOOT_QUANTITY_KEY, new FixedMetadataValue(plugin, quantity));
        setDamageMap(ore, new HashMap<>());

        updateOreDisplayName(ore);
        ore.setCustomNameVisible(true);
        return ore;
    }

    public boolean isOre(Entity entity) {
        if (!(entity instanceof ArmorStand)) return false;
        for (MetadataValue value : entity.getMetadata(IS_ORE_KEY)) {
            if (value.getOwningPlugin() == plugin) return value.asBoolean();
        }
        return false;
    }

    // --- Metadata Helper Methods ---

    @SuppressWarnings("unchecked")
    private Map<UUID, Double> getDamageMap(ArmorStand ore) {
        for (MetadataValue value : ore.getMetadata(DAMAGE_MAP_KEY)) {
            if (value.getOwningPlugin() == plugin && value.value() instanceof Map) {
                return (Map<UUID, Double>) value.value();
            }
        }
        return new HashMap<>();
    }
    
    private void setDamageMap(ArmorStand ore, Map<UUID, Double> map) {
        ore.setMetadata(DAMAGE_MAP_KEY, new FixedMetadataValue(plugin, map));
    }
    
    private void updateOreDisplayName(ArmorStand ore) {
        double current = getDoubleMetadata(ore, DURABILITY_KEY, 0.0);
        String materialName = getStringMetadata(ore, ORE_BLOCK_MATERIAL_KEY, "Unknown");
        double maxDurability = getDoubleMetadata(ore, MAX_DURABILITY_KEY, 1.0);
        double progress = Math.max(0, current) / maxDurability;
        
        String name = "§f" + materialName + " " + createProgressBar(progress);
        ore.setCustomName(name);
    }

    private static String createProgressBar(double progress) {
        int totalBars = 10;
        int greenBars = (int) (totalBars * progress);
        return "§f[" + "§a|".repeat(greenBars) + "§7|".repeat(totalBars - greenBars) + "§f]";
    }

    private String getStringMetadata(ArmorStand ore, String key, String def) {
        for (MetadataValue value : ore.getMetadata(key)) {
            if (value.getOwningPlugin() == plugin) return value.asString();
        }
        return def;
    }

    private double getDoubleMetadata(ArmorStand ore, String key, double def) {
        for (MetadataValue value : ore.getMetadata(key)) {
            if (value.getOwningPlugin() == plugin) return value.asDouble();
        }
        return def;
    }

    private int getIntMetadata(ArmorStand ore, String key, int def) {
        for (MetadataValue value : ore.getMetadata(key)) {
            if (value.getOwningPlugin() == plugin) return value.asInt();
        }
        return def;
    }
}

