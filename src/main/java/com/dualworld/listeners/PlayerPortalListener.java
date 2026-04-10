package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * FIX #3: 힐링 ↔ 스피드런 포털 격리.
 *
 * 핵심 원칙: 포털 목적지(event.getTo())의 월드만 스피드런 계열로 교체하고,
 * 좌표·스폰 계산은 Bukkit 기본 로직에 완전히 맡긴다.
 * → 포털 앞에 정상적으로 도착하는 기본 동작을 그대로 유지.
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
        World.Environment fromEnv = fromWorld.getEnvironment();

        String speedrunBase = plugin.getWorldManager().getSpeedrunWorldName();
        boolean isSpeedrun  = fromWorld.getName().startsWith(speedrunBase);

        // 힐링 계열은 Bukkit 기본 포털 동작 그대로 허용
        if (!isSpeedrun) return;

        // ── 스피드런 계열: 목적지 월드만 올바른 스피드런 월드로 교체 ──
        Location to = event.getTo();
        if (to == null) return;

        World correctDest = resolveDestWorld(fromEnv, speedrunBase, to);
        if (correctDest == null) return; // 이미 올바른 월드 또는 판별 불가 -> 그대로

        // 좌표는 Bukkit이 계산한 to 값을 그대로 쓰고 월드만 교체
        Location fixedTo = new Location(
                correctDest,
                to.getX(), to.getY(), to.getZ(),
                to.getYaw(), to.getPitch()
        );
        event.setTo(fixedTo);
        // event 취소하지 않음 -> 포털 생성/탐색 로직은 Bukkit이 처리
    }

    /**
     * 현재 환경(fromEnv)과 Bukkit이 계산한 목적지(to)를 보고
     * 올바른 스피드런 계열 월드를 반환.
     * 이미 올바른 월드라면 null 반환(개입 불필요).
     */
    private World resolveDestWorld(World.Environment fromEnv, String speedrunBase, Location to) {
        World toWorld = to.getWorld();
        if (toWorld == null) return null;

        String toName = toWorld.getName();
        World.Environment toEnv = toWorld.getEnvironment();

        if (fromEnv == World.Environment.NORMAL) {
            if (toEnv == World.Environment.NETHER) {
                String wanted = speedrunBase + "_nether";
                return toName.equals(wanted) ? null : plugin.getServer().getWorld(wanted);
            } else if (toEnv == World.Environment.THE_END) {
                String wanted = speedrunBase + "_the_end";
                return toName.equals(wanted) ? null : plugin.getServer().getWorld(wanted);
            }
        } else if (fromEnv == World.Environment.NETHER) {
            if (toEnv == World.Environment.NORMAL) {
                return toName.equals(speedrunBase) ? null : plugin.getServer().getWorld(speedrunBase);
            }
        } else if (fromEnv == World.Environment.THE_END) {
            if (toEnv == World.Environment.NORMAL) {
                return toName.equals(speedrunBase) ? null : plugin.getServer().getWorld(speedrunBase);
            }
        }

        return null;
    }
}
