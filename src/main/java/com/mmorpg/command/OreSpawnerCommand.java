package com.mmorpg.command;

import com.mmorpg.mining.spawner.OreSpawner;
import com.mmorpg.mining.spawner.OreSpawnerManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

public class OreSpawnerCommand implements CommandExecutor {

    private final OreSpawnerManager spawnerManager;

    public OreSpawnerCommand(OreSpawnerManager spawnerManager) {
        this.spawnerManager = spawnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreateCommand(player, args);
            case "list":
                return handleListCommand(player);
            case "delete":
                return handleDeleteCommand(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreateCommand(Player player, String[] args) {
        if (args.length != 7) {
            player.sendMessage(ChatColor.RED + "Usage: /orespawner create <id> <material> <mythic_item> <durability> <quantity> <respawn_seconds>");
            return true;
        }
        String id = args[1];
        Material material;
        try {
            material = Material.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid material: " + args[2]);
            return true;
        }
        String mythicItemId = args[3];
        int durability;
        int quantity;
        long respawnSeconds;
        try {
            durability = Integer.parseInt(args[4]);
            quantity = Integer.parseInt(args[5]);
            respawnSeconds = Long.parseLong(args[6]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Durability, quantity, and respawn_seconds must be numbers.");
            return true;
        }

        Location location = player.getLocation();
        Optional<OreSpawner> spawner = spawnerManager.createSpawner(id, location, material, mythicItemId, durability, quantity, respawnSeconds);

        if (spawner.isPresent()) {
            player.sendMessage(ChatColor.GREEN + "Ore spawner '" + id + "' created successfully.");
        } else {
            player.sendMessage(ChatColor.RED + "An ore spawner with ID '" + id + "' already exists.");
        }
        return true;
    }

    private boolean handleListCommand(Player player) {
        Collection<OreSpawner> spawners = spawnerManager.getAllSpawners();
        if (spawners.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no ore spawners.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "--- Ore Spawners ---");
        for (OreSpawner spawner : spawners) {
            Location loc = spawner.getLocation();
            String locationStr = String.format("%s, %d, %d, %d", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            player.sendMessage(ChatColor.AQUA + spawner.getId() + ": " + ChatColor.WHITE + locationStr);
        }
        return true;
    }

    private boolean handleDeleteCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /orespawner delete <id>");
            return true;
        }
        String id = args[1];
        if (spawnerManager.deleteSpawner(id)) {
            player.sendMessage(ChatColor.GREEN + "Ore spawner '" + id + "' deleted successfully.");
        } else {
            player.sendMessage(ChatColor.RED + "Could not find an ore spawner with ID '" + id + "'.");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Ore Spawner Commands ---");
        sender.sendMessage(ChatColor.AQUA + "/orespawner create <id> <material> <mythic_item> <durability> <quantity> <respawn_seconds>" + ChatColor.WHITE + " - Creates an ore spawner.");
        sender.sendMessage(ChatColor.AQUA + "/orespawner list" + ChatColor.WHITE + " - Lists all ore spawners.");
        sender.sendMessage(ChatColor.AQUA + "/orespawner delete <id>" + ChatColor.WHITE + " - Deletes an ore spawner.");
    }
}
