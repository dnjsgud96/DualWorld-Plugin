package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final DualWorldPlugin plugin;

    public PlayerQuitListener(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String worldType = plugin.getPlayerDataManager().getCurrentWorld(player);
        if ("speedrun".equals(worldType)) {
            plugin.getPlayerDataManager().saveSpeedrunData(player);
        } else {
            plugin.getPlayerDataManager().saveHealingData(player);
        }
    }
}
