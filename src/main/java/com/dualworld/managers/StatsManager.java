package com.dualworld.managers;

import com.dualworld.DualWorldPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class StatsManager {

    private final DualWorldPlugin plugin;
    private final File statsFile;
    private FileConfiguration cfg;

    public StatsManager(DualWorldPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try { statsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        cfg = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void saveAll() {
        try { cfg.save(statsFile); }
        catch (IOException e) { plugin.getLogger().warning("통계 저장 실패"); }
    }

    private void save() { saveAll(); }

    // ─── Player stats ─────────────────────────────────────────────
    private String key(UUID id, String field) { return "players." + id + "." + field; }

    public void recordSpeedrunJoin(Player p) {
        UUID id = p.getUniqueId();
        cfg.set("players." + id + ".name", p.getName());
        cfg.set(key(id, "joins"), cfg.getInt(key(id, "joins"), 0) + 1);
        save();
    }

    public void recordSpeedrunDeath(Player p) {
        UUID id = p.getUniqueId();
        cfg.set("players." + id + ".name", p.getName());
        cfg.set(key(id, "deaths"), cfg.getInt(key(id, "deaths"), 0) + 1);
        save();
    }

    public void recordSpeedrunFinish(Player p, long milliseconds) {
        UUID id = p.getUniqueId();
        cfg.set("players." + id + ".name", p.getName());
        cfg.set(key(id, "finishes"), cfg.getInt(key(id, "finishes"), 0) + 1);

        long best = cfg.getLong(key(id, "bestTime"), Long.MAX_VALUE);
        if (milliseconds < best) {
            cfg.set(key(id, "bestTime"), milliseconds);
        }
        // Last finish time
        cfg.set(key(id, "lastTime"), milliseconds);
        save();
    }

    // ─── World stats ──────────────────────────────────────────────
    public void incrementWorldResets() {
        cfg.set("global.totalResets", cfg.getInt("global.totalResets", 0) + 1);
        save();
    }

    public void recordGlobalFinish(long milliseconds) {
        long best = cfg.getLong("global.bestTime", Long.MAX_VALUE);
        if (milliseconds < best) cfg.set("global.bestTime", milliseconds);
        cfg.set("global.totalFinishes", cfg.getInt("global.totalFinishes", 0) + 1);
        save();
    }

    // ─── 기록 초기화 ──────────────────────────────────────────────

    /** 모든 플레이어 기록 + 서버 전역 기록 전부 삭제 */
    public void clearAllStats() {
        cfg.set("players", null);
        cfg.set("global", null);
        save();
    }

    /** 서버 전역 기록(최고기록·클리어수)만 삭제 */
    public void clearGlobalStats() {
        cfg.set("global.bestTime", null);
        cfg.set("global.totalFinishes", null);
        save();
    }

    /** UUID로 특정 플레이어 기록 삭제 */
    public void clearPlayerStats(UUID id) {
        cfg.set("players." + id, null);
        save();
    }

    /**
     * 이름으로 특정 플레이어 기록 삭제 (오프라인 포함).
     * @return 찾아서 삭제했으면 true, 없으면 false
     */
    public boolean clearPlayerStatsByName(String name) {
        if (!cfg.contains("players")) return false;
        for (String idStr : cfg.getConfigurationSection("players").getKeys(false)) {
            String storedName = cfg.getString("players." + idStr + ".name", "");
            if (storedName.equalsIgnoreCase(name)) {
                cfg.set("players." + idStr, null);
                save();
                return true;
            }
        }
        return false;
    }

    // ─── Getters ──────────────────────────────────────────────────
    public int getJoins(UUID id)    { return cfg.getInt(key(id, "joins"),    0); }
    public int getDeaths(UUID id)   { return cfg.getInt(key(id, "deaths"),   0); }
    public int getFinishes(UUID id) { return cfg.getInt(key(id, "finishes"), 0); }
    public long getBestTime(UUID id){ return cfg.getLong(key(id, "bestTime"), -1); }
    public long getLastTime(UUID id){ return cfg.getLong(key(id, "lastTime"), -1); }

    public int getGlobalResets()    { return cfg.getInt("global.totalResets",   0); }
    public int getGlobalFinishes()  { return cfg.getInt("global.totalFinishes", 0); }
    public long getGlobalBestTime() { return cfg.getLong("global.bestTime",     -1); }

    // ─── Ranking list ─────────────────────────────────────────────
    public static class PlayerStat {
        public final String name;
        public final UUID uuid;
        public final int joins, deaths, finishes;
        public final long bestTime;

        public PlayerStat(String name, UUID uuid, int joins, int deaths, int finishes, long bestTime) {
            this.name = name; this.uuid = uuid;
            this.joins = joins; this.deaths = deaths; this.finishes = finishes; this.bestTime = bestTime;
        }
    }

    public List<PlayerStat> getAllStats() {
        List<PlayerStat> list = new ArrayList<>();
        if (!cfg.contains("players")) return list;
        for (String idStr : cfg.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                String name = cfg.getString("players." + idStr + ".name", idStr);
                list.add(new PlayerStat(name, id,
                        cfg.getInt(key(id, "joins"),    0),
                        cfg.getInt(key(id, "deaths"),   0),
                        cfg.getInt(key(id, "finishes"), 0),
                        cfg.getLong(key(id, "bestTime"), -1)));
            } catch (IllegalArgumentException ignored) {}
        }
        list.sort(Comparator.comparingLong(s -> (s.bestTime < 0 ? Long.MAX_VALUE : s.bestTime)));
        return list;
    }

    // ─── Time format helper ───────────────────────────────────────
    public static String formatTime(long ms) {
        if (ms < 0) return "§7기록 없음";
        long totalSec = ms / 1000;
        long hours   = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        long millis  = ms % 1000;
        if (hours > 0)
            return String.format("§e%d§7시간 §e%02d§7분 §e%02d§7.§e%03d§7초", hours, minutes, seconds, millis);
        else if (minutes > 0)
            return String.format("§e%d§7분 §e%02d§7.§e%03d§7초", minutes, seconds, millis);
        else
            return String.format("§e%d§7.§e%03d§7초", seconds, millis);
    }
}
