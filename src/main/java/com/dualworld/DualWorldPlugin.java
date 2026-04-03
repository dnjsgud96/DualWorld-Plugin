package com.dualworld;

import com.dualworld.commands.DualWorldCommand;
import com.dualworld.commands.HealingCommand;
import com.dualworld.commands.SpeedrunCommand;
import com.dualworld.listeners.PlayerDeathListener;
import com.dualworld.listeners.PlayerJoinListener;
import com.dualworld.listeners.PlayerQuitListener;
import com.dualworld.managers.PlayerDataManager;
import com.dualworld.managers.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DualWorldPlugin extends JavaPlugin {

    private static DualWorldPlugin instance;
    private WorldManager worldManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.worldManager = new WorldManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        worldManager.initializeWorlds();

        getCommand("dualworld").setExecutor(new DualWorldCommand(this));
        getCommand("healing").setExecutor(new HealingCommand(this));
        getCommand("speedrun").setExecutor(new SpeedrunCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);

        getLogger().info("================================================================");
        getLogger().info("  DualWorld v1.0.0 활성화!");
        getLogger().info("  힐링 월드: " + worldManager.getHealingWorldName());
        getLogger().info("  스피드런 월드: " + worldManager.getSpeedrunWorldName());
        getLogger().info("================================================================");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        getLogger().info("DualWorld 플러그인이 비활성화되었습니다. 모든 데이터 저장 완료.");
    }

    public static DualWorldPlugin getInstance() { return instance; }
    public WorldManager getWorldManager() { return worldManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }

    public String getPrefix() {
        return getConfig().getString("messages.prefix", "§8[§aDualWorld§8] ");
    }

    public String getMessage(String key) {
        return getPrefix() + getConfig().getString("messages." + key, "§cMessage not found: " + key);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }
}
