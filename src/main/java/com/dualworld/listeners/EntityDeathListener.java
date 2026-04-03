package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    private final DualWorldPlugin plugin;

    public EntityDeathListener(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;

        // Only track in speedrun world
        String worldName = event.getEntity().getWorld().getName();
        if (!worldName.contains(plugin.getWorldManager().getSpeedrunWorldName())) return;

        plugin.getTimerManager().onDragonDeath();

        // Record for all players in speedrun world
        long elapsed = plugin.getTimerManager().getElapsedMs();
        // elapsed already stopped in onDragonDeath but we captured before stop — use global best logic
        long best = plugin.getStatsManager().getGlobalBestTime();

        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (plugin.getWorldManager().isInSpeedrunWorld(p)) {
                plugin.getStatsManager().recordSpeedrunFinish(p, best);
            }
        }
    }
}
