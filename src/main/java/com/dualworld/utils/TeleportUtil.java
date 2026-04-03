package com.dualworld.utils;

import com.dualworld.DualWorldPlugin;
import com.dualworld.managers.PlayerDataManager;
import com.dualworld.managers.WorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeleportUtil {

    public static void teleportToHealing(Player p, DualWorldPlugin plugin) {
        WorldManager wm   = plugin.getWorldManager();
        PlayerDataManager pdm = plugin.getPlayerDataManager();

        if (wm.isInHealingWorld(p)) {
            p.sendMessage(plugin.getMessage("already-in-world"));
            return;
        }

        pdm.saveSpeedrunData(p);
        pdm.loadHealingData(p);

        Location dest = pdm.getLastHealingLocation(p);
        if (dest == null || dest.getWorld() == null) dest = wm.getHealingWorld().getSpawnLocation();

        p.teleport(dest);
        pdm.setCurrentWorld(p, "healing");
        p.sendMessage(plugin.getMessage("moved-to-healing"));
        p.sendTitle(wm.getHealingDisplayName(), "§a힐링 월드에 오신 것을 환영합니다!", 10, 60, 20);
    }

    public static void teleportToSpeedrun(Player p, DualWorldPlugin plugin) {
        WorldManager wm   = plugin.getWorldManager();
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
        pdm.loadSpeedrunData(p);

        // 스피드런 데이터가 초기화된 상태(사망 후)라면 스폰으로 이동
        // getLastSpeedrunLocation()이 null을 반환하면 resetToDefault가 이미 적용됨
        Location dest = pdm.getLastSpeedrunLocation(p);
        if (dest == null || dest.getWorld() == null || !dest.getWorld().getName().contains(wm.getSpeedrunWorldName()))
            dest = sw.getSpawnLocation();

        p.teleport(dest);
        pdm.setCurrentWorld(p, "speedrun");
        p.sendMessage(plugin.getMessage("moved-to-speedrun"));
        p.sendTitle(wm.getSpeedrunDisplayName(), "§c주의: 사망 시 월드 리셋!", 10, 60, 20);

        // Record join stat + start timer on first arrival
        plugin.getStatsManager().recordSpeedrunJoin(p);
        if (!plugin.getTimerManager().isRunning()) {
            plugin.getTimerManager().startTimer();
        }
    }
}
