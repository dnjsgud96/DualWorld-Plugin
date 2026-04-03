package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
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

        // Mark as pending respawn — we'll intercept the respawn event
        plugin.getWorldManager().addPendingRespawn(player.getUniqueId());

        // Record death stat
        plugin.getStatsManager().recordSpeedrunDeath(player);

        // Keep drops in world naturally; clear exp so it doesn't scatter
        event.setDroppedExp(0);

        // ★ 핵심 수정: 사망 시점에 스피드런 데이터를 빈 상태로 초기화
        // 이렇게 해야 나중에 다시 스피드런 입장 시 loadSpeedrunData()가
        // 빈 데이터를 읽어 인벤토리/스탯을 초기화(resetToDefault)합니다.
        plugin.getPlayerDataManager().clearSpeedrunData(player);

        // Trigger countdown reset (only if not already resetting)
        if (!plugin.getWorldManager().isResetInProgress()) {
            plugin.getWorldManager().triggerResetWithCountdown(player.getName());
        }
    }
}
