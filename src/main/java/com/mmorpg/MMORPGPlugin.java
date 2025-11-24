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

    // --- 【追加1】 インスタンス保持用の変数 ---
    private static MMORPGPlugin instance;

    // Module Managers
    private PlayerJobManager jobManager;
    private MobExpManager mobExpManager;
    private MiningManager miningManager;
    private OreSpawnerManager oreSpawnerManager;

    // Listeners
    private LevelDisplayListener levelDisplayListener;
    private LevelUpListener levelUpListener;

    @Override
    public void onEnable() {
        // --- 【追加2】 起動時に自分自身を代入 ---
        instance = this;

        Bukkit.getLogger().info("MMORPG Plugin enabled!");

        // --- データベース初期化 ---
        try {
            DatabaseConfig.initialize();
            DatabaseInitializer.initializeTables();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[MMORPG] Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // --- マネージャー初期化 ---
        jobManager = new PlayerJobManager();
        mobExpManager = new MobExpManager(getDataFolder());

        // 新しい採掘マネージャーを初期化して起動
        miningManager = new MiningManager(this);
        miningManager.startup();

        // 採掘スポナーマネージャーを初期化
        oreSpawnerManager = new OreSpawnerManager(this, miningManager);
        miningManager.setOreSpawnerManager(oreSpawnerManager);


        // --- コマンド登録 ---
        getCommand("mmorpg").setExecutor((sender, cmd, label, args) -> {
            sender.sendMessage("§aMMORPG Plugin v" + getDescription().getVersion());
            return true;
        });

        // Job/Level system commands
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

        // Mining system commands
        getCommand("givepiccaxe").setExecutor(new GivePickaxeCommand());
        CreateOreCommand createOreCommand = new CreateOreCommand(miningManager);
        getCommand("createore").setExecutor(createOreCommand);
        getCommand("createore").setTabCompleter(createOreCommand);
        getCommand("orespawner").setExecutor(new OreSpawnerCommand(oreSpawnerManager));

        // --- イベントリスナー登録 ---
        getServer().getPluginManager().registerEvents(new PlayerEventListener(jobManager), this);
        getServer().getPluginManager().registerEvents(displayListener, this);
        getServer().getPluginManager().registerEvents(levelDisplayListener, this);
        getServer().getPluginManager().registerEvents(new MobExperienceListener(jobManager, mobExpManager, levelDisplayListener, this), this);
        levelUpListener = new LevelUpListener(jobManager, this);
        getServer().getPluginManager().registerEvents(levelUpListener, this);

        // 新しい採掘システムリスナーを登録
        getServer().getPluginManager().registerEvents(new com.mmorpg.mining.MiningListener(miningManager), this);
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("MMORPG Plugin disabled!");

        // マネージャーを停止
        if (miningManager != null) {
            miningManager.shutdown();
        }

        // スポナーを保存
        if (oreSpawnerManager != null) {
            oreSpawnerManager.saveSpawners();
        }

        // データをクリア
        if (jobManager != null) {
            jobManager.clearAll();
        }

        // データベース接続をクローズ
        DatabaseConfig.close();
    }

    // --- 【追加3】 他のクラスから呼び出すためのメソッド ---
    public static MMORPGPlugin getInstance() {
        return instance;
    }
}