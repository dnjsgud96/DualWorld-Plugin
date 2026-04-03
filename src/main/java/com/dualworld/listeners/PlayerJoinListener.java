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
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().onPlayerJoin(player);

        String worldType = plugin.getPlayerDataManager().getCurrentWorld(player);
        String displayName;
        if ("speedrun".equals(worldType)) {
            displayName = plugin.getWorldManager().getSpeedrunDisplayName();
        } else {
            displayName = plugin.getWorldManager().getHealingDisplayName();
        }
        player.sendMessage(plugin.getPrefix() + "§e현재 위치: " + displayName);
    }
}
