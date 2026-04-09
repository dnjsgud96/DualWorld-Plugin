package com.dualworld.utils;

import com.dualworld.DualWorldPlugin;
import com.dualworld.managers.PlayerDataManager;
import com.dualworld.managers.WorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeleportUtil {

    public static void teleportToHealing(Player p, DualWorldPlugin plugin) {
        WorldManager wm       = plugin.getWorldManager();
        PlayerDataManager pdm = plugin.getPlayerDataManager();

        if (wm.isInHealingWorld(p)) {
            p.sendMessage(plugin.getMessage("already-in-world"));
            return;
        }

        pdm.saveSpeedrunData(p);
        pdm.loadHealingData(p);

        Location dest = pdm.getLastHealingLocation(p);
        // FIX #4: 저장 위치가 없으면 안전 스폰 사용
        if (dest == null || dest.getWorld() == null) {
            dest = wm.getSafeSpawnLocation(wm.getHealingWorld());
        }

        p.teleport(dest);
        pdm.setCurrentWorld(p, "healing");
        p.sendMessage(plugin.getMessage("moved-to-healing"));
        p.sendTitle(wm.getHealingDisplayName(), "§a힐링 월드에 오신 것을 환영합니다!", 10, 60, 20);
    }

    public static void teleportToSpeedrun(Player p, DualWorldPlugin plugin) {
        WorldManager wm       = plugin.getWorldManager();
        PlayerDataManager pdm = plugin.getPlayerDataManager();

        if (wm.isInSpeedrunWorld(p)) {
            p.sendMessage(plugin.getMessage("already-in-world"));
            return;
        }
        if (wm.isResetInProgress()) {
            p.sendMessage(plugin.getPrefix() + "§c스피드런 월드가 리셋 중입니다. 잠시 후 다시 시도하세요.");
            return;
        }

        World sw = wm.getSpeedrunWorld();
        if (sw == null) {
            p.sendMessage(plugin.getPrefix() + "§c스피드런 월드가 아직 생성되지 않았습니다.");
            return;
        }

        pdm.saveHealingData(p);

        // FIX #1 연계: 리셋 후라면 스피드런 저장 데이터가 없으므로 loadSpeedrunData가 빈 상태로 시작함
        pdm.loadSpeedrunData(p);

        // FIX #4: 스피드런 저장 위치가 있어도 해당 월드가 유효한지 재확인
        Location dest = pdm.getLastSpeedrunLocation(p);
        if (dest == null
                || dest.getWorld() == null
                || !dest.getWorld().getName().startsWith(wm.getSpeedrunWorldName())) {
            // 저장 위치 없음 or 이전 시드 월드 → 안전 스폰으로 이동
            dest = wm.getSafeSpawnLocation(sw);
        }

        p.teleport(dest);
        pdm.setCurrentWorld(p, "speedrun");
        p.sendMessage(plugin.getMessage("moved-to-speedrun"));
        p.sendTitle(wm.getSpeedrunDisplayName(), "§c주의: 사망 시 월드 리셋!", 10, 60, 20);

        plugin.getStatsManager().recordSpeedrunJoin(p);
        if (!plugin.getTimerManager().isRunning()) {
            plugin.getTimerManager().startTimer();
        }
    }
}
