package com.dualworld;

import com.dualworld.commands.DualWorldCommand;
import com.dualworld.commands.HealingCommand;
import com.dualworld.commands.SpeedrunCommand;
import com.dualworld.listeners.EntityDeathListener;
import com.dualworld.listeners.PlayerDeathListener;
import com.dualworld.listeners.PlayerJoinListener;
import com.dualworld.listeners.PlayerPortalListener;
import com.dualworld.listeners.PlayerQuitListener;
import com.dualworld.listeners.PlayerRespawnListener;
import com.dualworld.managers.PlayerDataManager;
import com.dualworld.managers.SpeedrunTimerManager;
import com.dualworld.managers.StatsManager;
import com.dualworld.managers.WorldManager;
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

        getCommand("dualworld").setExecutor(new DualWorldCommand(this));
        getCommand("healing").setExecutor(new HealingCommand(this));
        getCommand("speedrun").setExecutor(new SpeedrunCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this),   this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this),    this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this),   this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this),    this);
        // FIX #3: 포털 격리 리스너 등록
        getServer().getPluginManager().registerEvents(new PlayerPortalListener(this),  this);

        getLogger().info("DualWorld v2.1.0 활성화!");
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
