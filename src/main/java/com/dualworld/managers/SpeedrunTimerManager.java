package com.dualworld.managers;

import com.dualworld.DualWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SpeedrunTimerManager {

    private final DualWorldPlugin plugin;

    private long startTime   = -1;
    private boolean running  = false;
    private BukkitTask displayTask = null;

    // Players who have scoreboard HUD enabled
    private final Set<UUID> hudEnabled = new HashSet<>();

    public SpeedrunTimerManager(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Control ──────────────────────────────────────────────────
    public void startTimer() {
        if (running) return;
        startTime = System.currentTimeMillis();
        running   = true;
        startDisplayTask();
        Bukkit.broadcastMessage(plugin.getPrefix() + "§a⏱ 스피드런 타이머 시작!");
    }

    public void stopTimer() {
        running = false;
        if (displayTask != null) { displayTask.cancel(); displayTask = null; }
    }

    public void onDragonDeath() {
        if (!running) return;
        long elapsed = getElapsedMs();
        stopTimer();

        String timeStr = StatsManager.formatTime(elapsed);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l★ 엔더 드래곤 처치! 스피드런 완료! ★");
        Bukkit.broadcastMessage("§7클리어 시간: " + timeStr);
        Bukkit.broadcastMessage("");

        // Record global best
        plugin.getStatsManager().recordGlobalFinish(elapsed);

        long globalBest = plugin.getStatsManager().getGlobalBestTime();
        if (elapsed <= globalBest) {
            Bukkit.broadcastMessage("§b§l🏆 새로운 최고 기록! " + timeStr);
        }
    }

    // ─── HUD display ─────────────────────────────────────────────
    private void startDisplayTask() {
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!running) return;
            String timeStr = StatsManager.formatTime(getElapsedMs());
            long globalBest = plugin.getStatsManager().getGlobalBestTime();
            String bestStr = StatsManager.formatTime(globalBest);

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!hudEnabled.contains(p.getUniqueId())) continue;
                if (!plugin.getWorldManager().isInSpeedrunWorld(p)) continue;

                // ActionBar display (1.12 supports this via sendActionBar)
                p.sendActionBar("§c⏱ " + timeStr + "  §7│  §6최고: " + bestStr);
            }
        }, 0L, 10L); // update every 0.5 sec
    }

    // ─── HUD toggle ──────────────────────────────────────────────
    public boolean toggleHud(Player p) {
        UUID id = p.getUniqueId();
        if (hudEnabled.contains(id)) {
            hudEnabled.remove(id);
            return false;
        } else {
            hudEnabled.add(id);
            return true;
        }
    }

    public boolean isHudEnabled(Player p) { return hudEnabled.contains(p.getUniqueId()); }

    // ─── Info ─────────────────────────────────────────────────────
    public boolean isRunning()   { return running; }
    public long getStartTime()   { return startTime; }
    public long getElapsedMs()   { return running ? System.currentTimeMillis() - startTime : -1; }
}
