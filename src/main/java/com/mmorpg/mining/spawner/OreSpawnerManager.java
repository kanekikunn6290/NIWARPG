package com.mmorpg.mining.spawner;

import com.mmorpg.MMORPGPlugin;
import com.mmorpg.mining.MiningManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class OreSpawnerManager {

    private final MMORPGPlugin plugin;
    private final MiningManager miningManager;
    private final Map<String, OreSpawner> spawners = new ConcurrentHashMap<>();
    private final Map<UUID, OreSpawner> oreEntityToSpawnerMap = new ConcurrentHashMap<>();
    private final File spawnersFile;
    private FileConfiguration spawnersConfig;

    public OreSpawnerManager(MMORPGPlugin plugin, MiningManager miningManager) {
        this.plugin = plugin;
        this.miningManager = miningManager;
        this.spawnersFile = new File(plugin.getDataFolder(), "spawners.yml");
        loadSpawners();
    }

    public void loadSpawners() {
        if (!spawnersFile.exists()) {
            plugin.saveResource("spawners.yml", false);
        }
        spawnersConfig = YamlConfiguration.loadConfiguration(spawnersFile);
        ConfigurationSection spawnerSection = spawnersConfig.getConfigurationSection("spawners");
        if (spawnerSection == null) {
            return;
        }

        // 既存のスポナーとエンティティをクリア
        cleanupAllOres();
        spawners.clear();
        oreEntityToSpawnerMap.clear();

        for (String id : spawnerSection.getKeys(false)) {
            ConfigurationSection section = spawnerSection.getConfigurationSection(id);
            if (section != null) {
                try {
                    Location location = section.getLocation("location");
                    Material material = Material.valueOf(section.getString("material"));
                    String mythicItemId = section.getString("mythic-item-id");
                    int durability = section.getInt("durability");
                    int quantity = section.getInt("quantity");
                    long respawnSeconds = section.getLong("respawn-seconds");

                    if (location != null) {
                        OreSpawner spawner = new OreSpawner(id, location, material, mythicItemId, durability, quantity, respawnSeconds);
                        spawners.put(id, spawner);
                        spawnOreForSpawner(spawner);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load spawner with id: " + id, e);
                }
            }
        }
        plugin.getLogger().info(spawners.size() + " ore spawners loaded.");
    }

    public void saveSpawners() {
        spawnersConfig.set("spawners", null); // Clear existing spawners
        ConfigurationSection spawnerSection = spawnersConfig.createSection("spawners");
        for (OreSpawner spawner : spawners.values()) {
            ConfigurationSection section = spawnerSection.createSection(spawner.getId());
            section.set("location", spawner.getLocation());
            section.set("material", spawner.getMaterial().name());
            section.set("mythic-item-id", spawner.getMythicItemId());
            section.set("durability", spawner.getDurability());
            section.set("quantity", spawner.getQuantity());
            section.set("respawn-seconds", spawner.getRespawnSeconds());
        }
        try {
            spawnersConfig.save(spawnersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save spawners to " + spawnersFile, e);
        }
    }

    public void spawnOreForSpawner(OreSpawner spawner) {
        if (spawner.getLocation().isWorldLoaded()) {
            // 重複防止のため、MiningManager側で古いエンティティを削除してから作成する機能を呼び出し
            ArmorStand ore = miningManager.createOre(
                    spawner.getLocation(),
                    spawner.getMaterial(),
                    spawner.getMythicItemId(),
                    spawner.getDurability(),
                    spawner.getQuantity()
            );
            spawner.setSpawnedOreEntityId(ore.getUniqueId());
            oreEntityToSpawnerMap.put(ore.getUniqueId(), spawner);
        }
    }

    public void scheduleRespawn(OreSpawner spawner) {
        if (spawner == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnOreForSpawner(spawner), spawner.getRespawnSeconds() * 20L);
    }

    public void onOreDestroyed(UUID entityId) {
        OreSpawner spawner = oreEntityToSpawnerMap.remove(entityId);
        if (spawner != null) {
            spawner.setSpawnedOreEntityId(null);
            scheduleRespawn(spawner);
        }
    }
    
    public Optional<OreSpawner> createSpawner(String id, Location location, Material material, String mythicItemId, int durability, int quantity, long respawnSeconds) {
        if (spawners.containsKey(id)) {
            return Optional.empty();
        }
        // 位置をブロックの中心に合わせる
        Location blockCenter = location.getBlock().getLocation().add(0.5, 0, 0.5);
        
        OreSpawner spawner = new OreSpawner(id, blockCenter, material, mythicItemId, durability, quantity, respawnSeconds);
        spawners.put(id, spawner);
        spawnOreForSpawner(spawner);
        saveSpawners();
        return Optional.of(spawner);
    }

    public boolean deleteSpawner(String id) {
        OreSpawner spawner = spawners.remove(id);
        if (spawner != null) {
            if (spawner.getSpawnedOreEntityId() != null) {
                oreEntityToSpawnerMap.remove(spawner.getSpawnedOreEntityId());
                miningManager.removeOre(spawner.getSpawnedOreEntityId());
            }
            saveSpawners();
            return true;
        }
        return false;
    }

    /**
     * プラグイン停止時にすべての鉱石エンティティを削除する
     */
    public void cleanupAllOres() {
        for (OreSpawner spawner : spawners.values()) {
            if (spawner.getSpawnedOreEntityId() != null) {
                Entity entity = Bukkit.getEntity(spawner.getSpawnedOreEntityId());
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        oreEntityToSpawnerMap.clear();
    }

    public Collection<OreSpawner> getAllSpawners() {
        return Collections.unmodifiableCollection(spawners.values());
    }
}
