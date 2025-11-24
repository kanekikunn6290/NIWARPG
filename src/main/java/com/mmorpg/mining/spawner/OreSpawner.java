package com.mmorpg.mining.spawner;

import org.bukkit.Location;
import org.bukkit.Material;
import java.util.UUID;

/**
 * Represents an ore spawner that periodically respawns an ore block.
 */
public class OreSpawner {

    private final String id;
    private final Location location;
    private final Material material;
    private final String mythicItemId;
    private final int durability;
    private final int quantity;
    private final long respawnSeconds;
    private UUID spawnedOreEntityId;

    public OreSpawner(String id, Location location, Material material, String mythicItemId, int durability, int quantity, long respawnSeconds) {
        this.id = id;
        this.location = location;
        this.material = material;
        this.mythicItemId = mythicItemId;
        this.durability = durability;
        this.quantity = quantity;
        this.respawnSeconds = respawnSeconds;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public Material getMaterial() {
        return material;
    }

    public String getMythicItemId() {
        return mythicItemId;
    }

    public int getDurability() {
        return durability;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getRespawnSeconds() {
        return respawnSeconds;
    }

    public UUID getSpawnedOreEntityId() {
        return spawnedOreEntityId;
    }

    public void setSpawnedOreEntityId(UUID spawnedOreEntityId) {
        this.spawnedOreEntityId = spawnedOreEntityId;
    }
}
