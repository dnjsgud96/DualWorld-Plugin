package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final DualWorldPlugin plugin;

    public PlayerJoinListener(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        plugin.getPlayerDataManager().onPlayerJoin(p);

        boolean inSpeedrun = plugin.getWorldManager().isInSpeedrunWorld(p);
        String displayName = inSpeedrun
                ? plugin.getWorldManager().getSpeedrunDisplayName()
                : plugin.getWorldManager().getHealingDisplayName();

        p.sendMessage(plugin.getPrefix() + "§e현재 위치: " + displayName);

        // If joining speedrun world and timer not started yet, start it
        if (inSpeedrun && !plugin.getTimerManager().isRunning()) {
            plugin.getTimerManager().startTimer();
            plugin.getStatsManager().recordSpeedrunJoin(p);
        }
    }
}
