package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final DualWorldPlugin plugin;

    public PlayerDeathListener(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!plugin.getWorldManager().isInSpeedrunWorld(player)) return;

        // Keep drops in the world (natural death behavior)
        String deathMsg = event.getDeathMessage();
        if (deathMsg == null) deathMsg = player.getName() + "님이 사망했습니다.";

        final String finalMsg = deathMsg;
        Bukkit.broadcastMessage(plugin.getMessage("player-died", "{player}", player.getName()));

        // Clear drops so items don't scatter (speedrun reset = fresh start)
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Delay reset so death event finishes first
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getWorldManager().resetSpeedrunWorld();
        }, 20L);
    }
}
