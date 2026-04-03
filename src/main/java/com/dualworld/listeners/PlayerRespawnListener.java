package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final DualWorldPlugin plugin;

    public PlayerRespawnListener(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Only intercept if this player died in speedrun world
        if (!plugin.getWorldManager().isPendingRespawn(player.getUniqueId())) return;

        plugin.getWorldManager().removePendingRespawn(player.getUniqueId());

        // Force respawn into healing world
        World hw = plugin.getWorldManager().getHealingWorld();
        Location dest = plugin.getPlayerDataManager().getLastHealingLocation(player);
        if (dest == null || dest.getWorld() == null) dest = hw.getSpawnLocation();

        event.setRespawnLocation(dest);

        // Load healing data after a tick (inventory etc.)
        final Location finalDest = dest;
        final Player finalPlayer = player;
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getPlayerDataManager().loadHealingData(finalPlayer);
            plugin.getPlayerDataManager().setCurrentWorld(finalPlayer, "healing");
            finalPlayer.sendMessage(plugin.getPrefix() + "§a힐링 월드로 리스폰되었습니다.");
        }, 1L);
    }
}
