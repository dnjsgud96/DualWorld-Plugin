package com.dualworld.listeners;

import com.dualworld.DualWorldPlugin;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerPortalListener implements Listener {

    private final DualWorldPlugin plugin;

    public PlayerPortalListener(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getWorld().getName();
        String speedrunBase = plugin.getWorldManager().getSpeedrunWorldName();
        String healingName  = plugin.getWorldManager().getHealingWorldName();

        boolean fromSpeedrun = fromWorld.equals(speedrunBase)
                || fromWorld.equals(speedrunBase + "_nether")
                || fromWorld.equals(speedrunBase + "_the_end");

        boolean fromHealing = fromWorld.startsWith(healingName)
                && !fromSpeedrun;

        // ── 스피드런 포탈: 오버월드 ↔ 네더 ↔ 엔드 내부에서만 이동 ──
        if (fromSpeedrun) {
            PlayerTeleportEvent.TeleportCause cause = event.getCause();

            if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                // 오버월드 → 네더
                if (fromWorld.equals(speedrunBase)) {
                    World nether = org.bukkit.Bukkit.getWorld(speedrunBase + "_nether");
                    if (nether == null) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getPrefix() + "§c스피드런 네더가 아직 준비되지 않았습니다.");
                        return;
                    }
                    // 바닐라 네더 포탈 좌표 계산 (x/8, z/8)
                    double nx = event.getFrom().getX() / 8.0;
                    double nz = event.getFrom().getZ() / 8.0;
                    org.bukkit.Location dest = new org.bukkit.Location(nether, nx, 64, nz);
                    event.setTo(dest);
                    return;
                }
                // 네더 → 오버월드
                if (fromWorld.equals(speedrunBase + "_nether")) {
                    World overworld = org.bukkit.Bukkit.getWorld(speedrunBase);
                    if (overworld == null) {
                        event.setCancelled(true);
                        return;
                    }
                    double ox = event.getFrom().getX() * 8.0;
                    double oz = event.getFrom().getZ() * 8.0;
                    org.bukkit.Location dest = new org.bukkit.Location(overworld, ox, 64, oz);
                    event.setTo(dest);
                    return;
                }
            }

            if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                // 오버월드 → 엔드
                if (fromWorld.equals(speedrunBase)) {
                    World end = org.bukkit.Bukkit.getWorld(speedrunBase + "_the_end");
                    if (end == null) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getPrefix() + "§c스피드런 엔드가 아직 준비되지 않았습니다.");
                        return;
                    }
                    event.setTo(end.getSpawnLocation());
                    return;
                }
                // 엔드 → 오버월드 (엔드 탈출 포탈)
                if (fromWorld.equals(speedrunBase + "_the_end")) {
                    World overworld = org.bukkit.Bukkit.getWorld(speedrunBase);
                    if (overworld == null) {
                        event.setCancelled(true);
                        return;
                    }
                    event.setTo(overworld.getSpawnLocation());
                    return;
                }
            }

            // 그 외 알 수 없는 포탈: 취소 (힐링 월드로 넘어가는 것 방지)
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + "§c스피드런 월드 내부 이동만 가능합니다.");
            return;
        }

        // ── 힐링 포탈: 바닐라 world, world_nether, world_the_end 내부에서만 ──
        if (fromHealing) {
            PlayerTeleportEvent.TeleportCause cause = event.getCause();

            // 목적지가 스피드런 월드면 강제 취소
            if (event.getTo() != null) {
                String toWorld = event.getTo().getWorld() != null ? event.getTo().getWorld().getName() : "";
                boolean toSpeedrun = toWorld.equals(speedrunBase)
                        || toWorld.equals(speedrunBase + "_nether")
                        || toWorld.equals(speedrunBase + "_the_end");
                if (toSpeedrun) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + "§c포탈로는 스피드런 월드에 진입할 수 없습니다. §e/speedrun §c명령어를 사용하세요.");
                }
            }
            // 힐링 내부 포탈은 바닐라에 맡김 (cancel 안 함)
        }
    }
}
