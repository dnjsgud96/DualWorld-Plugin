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

        // 스피드런 월드에서 사망한 플레이어만 처리
        if (!plugin.getWorldManager().isPendingRespawn(player.getUniqueId())) return;

        plugin.getWorldManager().removePendingRespawn(player.getUniqueId());

        // 힐링 월드로 강제 리스폰
        World hw = plugin.getWorldManager().getHealingWorld();
        Location dest = plugin.getPlayerDataManager().getLastHealingLocation(player);

        // FIX #4: 저장 위치가 없거나 유효하지 않으면 안전 스폰 사용
        if (dest == null || dest.getWorld() == null) {
            dest = plugin.getWorldManager().getSafeSpawnLocation(hw);
        }

        event.setRespawnLocation(dest);

        final Location finalDest = dest;
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getPlayerDataManager().loadHealingData(player);
            plugin.getPlayerDataManager().setCurrentWorld(player, "healing");
            player.sendMessage(plugin.getPrefix() + "§a힐링 월드로 리스폰되었습니다.");
        }, 1L);
    }
}
