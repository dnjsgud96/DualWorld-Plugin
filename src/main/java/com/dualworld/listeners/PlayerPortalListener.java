package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * FIX #3: 힐링 ↔ 스피드런 포털 격리.
 * 1.12.2 API에는 PortalType.END / PlayerPortalEvent#getType() 이 없으므로
 * 현재 월드의 Environment 로 포털 종류를 판별합니다.
 */
public class PlayerPortalListener implements Listener {

    private final DualWorldPlugin plugin;

    public PlayerPortalListener(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player    = event.getPlayer();
        World  fromWorld = player.getWorld();
        World.Environment env = fromWorld.getEnvironment();

        String speedrunBase = plugin.getWorldManager().getSpeedrunWorldName();
        boolean isSpeedrun  = fromWorld.getName().startsWith(speedrunBase);

        // 힐링 계열이면 Bukkit 기본 동작에 맡김
        if (!isSpeedrun) return;

        // ── 스피드런 계열 포털 강제 라우팅 ──────────────────────────
        World dest;
        boolean isEnd = (env == World.Environment.THE_END);

        if (isEnd) {
            // 엔드 -> 스피드런 오버월드
            dest = plugin.getServer().getWorld(speedrunBase);
        } else if (env == World.Environment.NETHER) {
            // 네더 -> 스피드런 오버월드
            dest = plugin.getServer().getWorld(speedrunBase);
        } else {
            // 오버월드: event.getTo() 월드의 Environment 로 목적지 추론
            Location toLocation = event.getTo();
            if (toLocation != null && toLocation.getWorld() != null) {
                World.Environment toEnv = toLocation.getWorld().getEnvironment();
                if (toEnv == World.Environment.NETHER) {
                    dest = plugin.getServer().getWorld(speedrunBase + "_nether");
                } else if (toEnv == World.Environment.THE_END) {
                    dest = plugin.getServer().getWorld(speedrunBase + "_the_end");
                } else {
                    return; // 이미 올바른 월드
                }
            } else {
                return;
            }
        }

        if (dest == null) {
            player.sendMessage(plugin.getPrefix() + "§c목적지 월드를 찾을 수 없습니다.");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        Location safeDest;
        if (isEnd) {
            safeDest = plugin.getWorldManager().getSafeSpawnLocation(dest);
        } else {
            Location from = player.getLocation();
            double scale  = (env == World.Environment.NETHER) ? 8.0 : (1.0 / 8.0);
            Location scaled = new Location(dest,
                    from.getX() * scale,
                    from.getY(),
                    from.getZ() * scale);
            safeDest = findSafeNetherLocation(dest, scaled);
        }

        player.teleport(safeDest);
    }

    private Location findSafeNetherLocation(World world, Location approximate) {
        int bx = approximate.getBlockX();
        int bz = approximate.getBlockZ();
        int searchRadius = 16;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                for (int y = 1; y < world.getMaxHeight() - 2; y++) {
                    org.bukkit.block.Block b = world.getBlockAt(bx + dx, y, bz + dz);
                    if (b.getType() == Material.PORTAL) {
                        Location candidate = new Location(world,
                                bx + dx + 0.5, y, bz + dz + 0.5);
                        if (isSafe(candidate)) return candidate;
                    }
                }
            }
        }
        return plugin.getWorldManager().getSafeSpawnLocation(world);
    }

    private boolean isSafe(Location loc) {
        World w = loc.getWorld();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        Material feet  = w.getBlockAt(x, y,     z).getType();
        Material head  = w.getBlockAt(x, y + 1, z).getType();
        Material floor = w.getBlockAt(x, y - 1, z).getType();
        return (feet  == Material.AIR || feet  == Material.PORTAL)
            && (head  == Material.AIR || head  == Material.PORTAL)
            && floor.isSolid();
    }
}
