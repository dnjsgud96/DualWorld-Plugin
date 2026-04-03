package com.dualworld;

import com.dualworld.commands.DualWorldCommand;
import com.dualworld.commands.HealingCommand;
import com.dualworld.commands.SpeedrunCommand;
import com.dualworld.listeners.*;
import com.dualworld.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class DualWorldPlugin extends JavaPlugin {

    private static DualWorldPlugin instance;
    private WorldManager worldManager;
    private PlayerDataManager playerDataManager;
    private StatsManager statsManager;
    private SpeedrunTimerManager timerManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.worldManager      = new WorldManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.statsManager      = new StatsManager(this);
        this.timerManager      = new SpeedrunTimerManager(this);

        worldManager.initializeWorlds();

        // Commands
        getCommand("dualworld").setExecutor(new DualWorldCommand(this));
        getCommand("healing").setExecutor(new HealingCommand(this));
        getCommand("speedrun").setExecutor(new SpeedrunCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this),   this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this),    this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this),    this);
        getServer().getPluginManager().registerEvents(new PlayerPortalListener(this),  this); // 포탈 버그 수정
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this),   this);

        getLogger().info("================================================");
        getLogger().info("  DualWorld v2.1.0 활성화!");
        getLogger().info("  힐링 월드 : " + worldManager.getHealingWorldName());
        getLogger().info("  스피드런  : " + worldManager.getSpeedrunWorldName());
        getLogger().info("  수정사항  : 포탈 버그 / 체력 분리 / 클릭 복사");
        getLogger().info("================================================");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAllData();
        if (statsManager      != null) statsManager.saveAll();
        if (timerManager      != null) timerManager.stopTimer();
        getLogger().info("DualWorld 비활성화 완료");
    }

    public static DualWorldPlugin getInstance() { return instance; }
    public WorldManager getWorldManager()           { return worldManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public StatsManager getStatsManager()           { return statsManager; }
    public SpeedrunTimerManager getTimerManager()   { return timerManager; }

    public String getPrefix() {
        return getConfig().getString("messages.prefix", "§8[§aDualWorld§8] ");
    }
    public String getMessage(String key) {
        return getPrefix() + getConfig().getString("messages." + key, "§cMissing: " + key);
    }
    public String getMessage(String key, String... kv) {
        String msg = getMessage(key);
        for (int i = 0; i + 1 < kv.length; i += 2) msg = msg.replace(kv[i], kv[i + 1]);
        return msg;
    }
}
