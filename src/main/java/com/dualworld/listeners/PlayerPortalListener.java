package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * FIX #3: 힐링 월드 ↔ 스피드런 월드 간 포털이 엉키지 않도록
 * 포털 이동 목적지를 각 월드 계열 안에서만 라우팅.
 *
 * 스피드런 계열: speedrun_world / speedrun_world_nether / speedrun_world_the_end
 * 힐링 계열:    world / world_nether / world_the_end (버킷 기본)
 */
public class PlayerPortalListener implements Listener {

    private final DualWorldPlugin plugin;

    public PlayerPortalListener(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World fromWorld = player.getWorld();
        PortalType portalType = event.getType();

        // WorldManager에서 올바른 목적지 월드를 결정
        World destWorld = plugin.getWorldManager().resolvePortalDestination(fromWorld, portalType);
        if (destWorld == null) {
            // null → 기본 Bukkit 포털 동작 허용 (힐링 계열 등)
            return;
        }

        // 목적지가 확정된 경우 → 이벤트를 취소하고 수동으로 안전 위치 텔레포트
        event.setCancelled(true);

        Location dest;
        if (portalType == PortalType.END) {
            // 엔드 출구 → 오버월드 스폰, 엔드 입구 → 엔드 스폰
            dest = plugin.getWorldManager().getSafeSpawnLocation(destWorld);
        } else {
            // 네더 포털: 비율 계산(8:1) 후 안전 위치
            Location from = player.getLocation();
            double scale = fromWorld.getEnvironment() == World.Environment.NETHER ? 8.0 : 1.0 / 8.0;
            Location scaled = new Location(destWorld,
                    from.getX() * scale,
                    from.getY(),
                    from.getZ() * scale);
            dest = findSafeNetherPortalLocation(destWorld, scaled);
        }

        player.teleport(dest);

        // 스피드런 월드 진입 기록
        if (plugin.getWorldManager().isInSpeedrunWorld(player)
                && !plugin.getTimerManager().isRunning()) {
            plugin.getTimerManager().startTimer();
        }
    }

    /**
     * 네더 포털 목적지 근처에서 안전한 위치를 찾아 반환.
     * 기존 포털 블록이 있으면 그 앞을 반환, 없으면 WorldManager의 getSafeSpawnLocation 사용.
     */
    private Location findSafeNetherPortalLocation(World destWorld, Location approximate) {
        int searchRadius = 16;
        int bx = approximate.getBlockX();
        int bz = approximate.getBlockZ();

        // approximate 주변에서 포털 프레임(OBSIDIAN) 탐색
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                for (int y = 1; y < destWorld.getMaxHeight() - 2; y++) {
                    org.bukkit.block.Block b = destWorld.getBlockAt(bx + dx, y, bz + dz);
                    if (b.getType() == org.bukkit.Material.PORTAL) {
                        // 포털 블록 발견 → 그 앞 공기 위치
                        Location candidate = new Location(destWorld,
                                bx + dx + 0.5, y, bz + dz + 0.5);
                        if (isSafe(candidate)) return candidate;
                    }
                }
            }
        }

        // 포털을 못 찾으면 월드 안전 스폰
        return plugin.getWorldManager().getSafeSpawnLocation(destWorld);
    }

    private boolean isSafe(Location loc) {
        org.bukkit.block.Block feet  = loc.getBlock();
        org.bukkit.block.Block head  = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
        org.bukkit.block.Block floor = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        return (feet.getType() == org.bukkit.Material.AIR || feet.getType() == org.bukkit.Material.PORTAL)
                && (head.getType() == org.bukkit.Material.AIR || head.getType() == org.bukkit.Material.PORTAL)
                && floor.getType().isSolid();
    }
}
