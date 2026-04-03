package com.dualworld.utils;

import com.dualworld.DualWorldPlugin;
import com.dualworld.managers.PlayerDataManager;
import com.dualworld.managers.WorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeleportUtil {

    public static void teleportToHealing(Player player, DualWorldPlugin plugin) {
        WorldManager wm = plugin.getWorldManager();
        PlayerDataManager pdm = plugin.getPlayerDataManager();

        if (wm.isInHealingWorld(player)) {
            player.sendMessage(plugin.getMessage("already-in-world"));
            return;
        }

        // Save speedrun state
        pdm.saveSpeedrunData(player);

        // Load healing state (inventory, stats, etc.)
        pdm.loadHealingData(player);

        // Teleport to last healing location or spawn
        Location dest = pdm.getLastHealingLocation(player);
        if (dest == null || dest.getWorld() == null) {
            dest = wm.getHealingWorld().getSpawnLocation();
        }

        player.teleport(dest);
        pdm.setCurrentWorld(player, "healing");

        player.sendMessage(plugin.getMessage("moved-to-healing"));
        sendWorldTitle(player, wm.getHealingDisplayName(), "§a힐링 월드에 오신 것을 환영합니다!");
    }

    public static void teleportToSpeedrun(Player player, DualWorldPlugin plugin) {
        WorldManager wm = plugin.getWorldManager();
        PlayerDataManager pdm = plugin.getPlayerDataManager();

        if (wm.isInSpeedrunWorld(player)) {
            player.sendMessage(plugin.getMessage("already-in-world"));
            return;
        }

        World speedrunWorld = wm.getSpeedrunWorld();
        if (speedrunWorld == null) {
            player.sendMessage(plugin.getPrefix() + "§c스피드런 월드가 아직 생성 중입니다. 잠시 후 다시 시도하세요.");
            return;
        }

        // Save healing state
        pdm.saveHealingData(player);

        // Load speedrun state
        pdm.loadSpeedrunData(player);

        // Teleport to last speedrun location or spawn
        Location dest = pdm.getLastSpeedrunLocation(player);
        if (dest == null || dest.getWorld() == null || !dest.getWorld().getName().contains(wm.getSpeedrunWorldName())) {
            dest = speedrunWorld.getSpawnLocation();
        }

        player.teleport(dest);
        pdm.setCurrentWorld(player, "speedrun");

        player.sendMessage(plugin.getMessage("moved-to-speedrun"));
        sendWorldTitle(player, wm.getSpeedrunDisplayName(), "§c주의: 한 명이라도 죽으면 월드가 리셋됩니다!");
    }

    private static void sendWorldTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle, 10, 60, 20);
    }
}
