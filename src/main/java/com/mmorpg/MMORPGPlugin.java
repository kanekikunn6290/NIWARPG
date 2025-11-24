package com.mmorpg;

import com.mmorpg.command.*;
import com.mmorpg.database.DatabaseConfig;
import com.mmorpg.database.DatabaseInitializer;
import com.mmorpg.listener.JobDisplayListener;
import com.mmorpg.listener.LevelDisplayListener;
import com.mmorpg.listener.LevelUpListener;
import com.mmorpg.listener.MobExperienceListener;
import com.mmorpg.manager.PlayerJobManager;
import com.mmorpg.mining.MiningManager;
import com.mmorpg.mining.spawner.OreSpawnerManager;
import com.mmorpg.util.MobExpManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class MMORPGPlugin extends JavaPlugin {

    private static MMORPGPlugin instance;

    private PlayerJobManager jobManager;
    private MobExpManager mobExpManager;
    private MiningManager miningManager;
    private OreSpawnerManager oreSpawnerManager;

    private LevelDisplayListener levelDisplayListener;
    private LevelUpListener levelUpListener;

    @Override
    public void onEnable() {
        instance = this;

        Bukkit.getLogger().info("MMORPG Plugin enabled!");

        try {
            DatabaseConfig.initialize();
            DatabaseInitializer.initializeTables();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMORPG] Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        jobManager = new PlayerJobManager();
        mobExpManager = new MobExpManager(getDataFolder());

        miningManager = new MiningManager(this);
        miningManager.startup();

        oreSpawnerManager = new OreSpawnerManager(this, miningManager);
        miningManager.setOreSpawnerManager(oreSpawnerManager);

        getCommand("mmorpg").setExecutor((sender, cmd, label, args) -> {
            sender.sendMessage("§aMMORPG Plugin v" + getDescription().getVersion());
            return true;
        });

        JobCommand jobCommand = new JobCommand(jobManager);
        JobDisplayListener displayListener = new JobDisplayListener(jobManager);
        jobCommand.setDisplayListener(displayListener);
        getCommand("job").setExecutor(jobCommand);
        getCommand("job").setTabCompleter(jobCommand);

        LevelCommand levelCommand = new LevelCommand(jobManager);
        levelDisplayListener = new LevelDisplayListener(jobManager);
        levelCommand.setLevelDisplayListener(levelDisplayListener);
        getCommand("level").setExecutor(levelCommand);
        getCommand("level").setTabCompleter(levelCommand);
        
        jobCommand.setLevelDisplayListener(levelDisplayListener);

        ResetCommand resetCommand = new ResetCommand(jobManager);
        resetCommand.setLevelDisplayListener(levelDisplayListener);
        resetCommand.setJobDisplayListener(displayListener);
        getCommand("reset").setExecutor(resetCommand);
        getCommand("reset").setTabCompleter(resetCommand);

        MobExpCommand mobExpCommand = new MobExpCommand(mobExpManager);
        getCommand("mobexp").setExecutor(mobExpCommand);
        getCommand("mobexp").setTabCompleter(mobExpCommand);

        getCommand("status").setExecutor(new StatusCommand(jobManager));

        getCommand("givepiccaxe").setExecutor(new GivePickaxeCommand());
        CreateOreCommand createOreCommand = new CreateOreCommand(miningManager);
        getCommand("createore").setExecutor(createOreCommand);
        getCommand("createore").setTabCompleter(createOreCommand);
        getCommand("orespawner").setExecutor(new OreSpawnerCommand(oreSpawnerManager));

        getServer().getPluginManager().registerEvents(new PlayerEventListener(jobManager), this);
        getServer().getPluginManager().registerEvents(displayListener, this);
        getServer().getPluginManager().registerEvents(levelDisplayListener, this);
        getServer().getPluginManager().registerEvents(new MobExperienceListener(jobManager, mobExpManager, levelDisplayListener, this), this);
        levelUpListener = new LevelUpListener(jobManager, this);
        getServer().getPluginManager().registerEvents(levelUpListener, this);

        getServer().getPluginManager().registerEvents(new com.mmorpg.mining.MiningListener(miningManager), this);
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("MMORPG Plugin disabled!");

        if (miningManager != null) {
            miningManager.shutdown();
        }

        if (oreSpawnerManager != null) {
            // スポナーを保存してからエンティティを削除
            oreSpawnerManager.saveSpawners();
            oreSpawnerManager.cleanupAllOres();
        }

        if (jobManager != null) {
            jobManager.clearAll();
        }

        DatabaseConfig.close();
    }

    public static MMORPGPlugin getInstance() {
        return instance;
    }
}
