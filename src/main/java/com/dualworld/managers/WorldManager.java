package com.dualworld.managers;

import com.dualworld.DualWorldPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;

public class WorldManager {

    private final DualWorldPlugin plugin;
    private String healingWorldName;
    private String speedrunWorldName;
    private long currentSpeedrunSeed;

    private boolean resetInProgress = false;
    private final Set<UUID> pendingRespawn = new HashSet<>();

    public WorldManager(DualWorldPlugin plugin) {
        this.plugin = plugin;
        healingWorldName  = plugin.getConfig().getString("healing-world.name", "world");
        speedrunWorldName = plugin.getConfig().getString("speedrun-world.name", "speedrun_world");
        currentSpeedrunSeed = plugin.getConfig().getLong("speedrun-world.seed", 0L);
        if (currentSpeedrunSeed == 0L) {
            currentSpeedrunSeed = new Random().nextLong();
            plugin.getConfig().set("speedrun-world.seed", currentSpeedrunSeed);
            plugin.saveConfig();
        }
    }

    public void initializeWorlds() {
        // 힐링 월드
        World hw = Bukkit.getWorld(healingWorldName);
        if (hw == null) {
            plugin.getLogger().warning("힐링 월드 없음 - 기본 world 사용");
            healingWorldName = Bukkit.getWorlds().get(0).getName();
            hw = Bukkit.getWorlds().get(0);
        }
        String diffStr = plugin.getConfig().getString("healing-world.difficulty", "NORMAL");
        try { hw.setDifficulty(Difficulty.valueOf(diffStr.toUpperCase())); }
        catch (IllegalArgumentException e) { hw.setDifficulty(Difficulty.NORMAL); }

        // 스피드런 월드
        World sw = Bukkit.getWorld(speedrunWorldName);
        if (sw == null) sw = createSpeedrunWorld(currentSpeedrunSeed);
        if (sw != null) sw.setDifficulty(Difficulty.HARD);

        plugin.getLogger().info("월드 초기화 완료");
    }

    public World createSpeedrunWorld(long seed) {
        plugin.getLogger().info("스피드런 월드 생성 중... 시드: " + seed);

        WorldCreator ow = new WorldCreator(speedrunWorldName);
        ow.seed(seed);
        ow.environment(World.Environment.NORMAL);
        ow.type(WorldType.NORMAL);
        World overworld = ow.createWorld();
        if (overworld == null) return null;
        overworld.setDifficulty(Difficulty.HARD);
        overworld.setKeepSpawnInMemory(false);

        WorldCreator nw = new WorldCreator(speedrunWorldName + "_nether");
        nw.seed(seed);
        nw.environment(World.Environment.NETHER);
        World nether = nw.createWorld();
        if (nether != null) {
            nether.setDifficulty(Difficulty.HARD);
            nether.setKeepSpawnInMemory(false);
        }

        WorldCreator ew = new WorldCreator(speedrunWorldName + "_the_end");
        ew.seed(seed);
        ew.environment(World.Environment.THE_END);
        World end = ew.createWorld();
        if (end != null) {
            end.setDifficulty(Difficulty.HARD);
            end.setKeepSpawnInMemory(false);
        }

        plugin.getLogger().info("스피드런 월드 생성 완료! 시드: " + seed);
        return overworld;
    }

    // ─── FIX #4: 안전한 스폰 위치 계산 ──────────────────────────────
    /**
     * 주어진 월드의 스폰 근처에서 발이 고체 블록 위, 머리가 공기인 안전한 위치를 탐색.
     * 찾지 못하면 월드 스폰에 Y+1 위치를 반환 (최후 수단).
     */
    public Location getSafeSpawnLocation(World world) {
        Location base = world.getSpawnLocation();
        // 스폰 주변 ±3 범위에서 안전한 위치 탐색
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int y = base.getBlockY() + 3; y >= base.getBlockY() - 3; y--) {
                    Location candidate = new Location(world,
                            base.getBlockX() + dx + 0.5,
                            y,
                            base.getBlockZ() + dz + 0.5,
                            base.getYaw(), base.getPitch());
                    Block feet = candidate.getBlock();
                    Block head = world.getBlockAt(candidate.getBlockX(), y + 1, candidate.getBlockZ());
                    Block ground = world.getBlockAt(candidate.getBlockX(), y - 1, candidate.getBlockZ());
                    if (feet.getType() == Material.AIR
                            && head.getType() == Material.AIR
                            && ground.getType().isSolid()) {
                        return candidate;
                    }
                }
            }
        }
        // 탐색 실패 시 스폰 Y+1에 강제 배치
        return new Location(world,
                base.getBlockX() + 0.5,
                world.getHighestBlockYAt(base.getBlockX(), base.getBlockZ()) + 1,
                base.getBlockZ() + 0.5,
                base.getYaw(), base.getPitch());
    }

    // ─── FIX #3: 포털 이동 처리 (PlayerPortalListener에서 호출) ────────
    /**
     * 플레이어가 포털을 통과할 때 올바른 목적지 월드를 반환.
     * 힐링/스피드런이 서로 섞이지 않도록 강제 라우팅.
     *
     * @param fromWorld 현재 월드
     * @param portalType 포털 종류 (NETHER/END)
     * @return 이동할 목적지 월드, null이면 기본 동작
     */
    public World resolvePortalDestination(World fromWorld, PortalType portalType) {
        String name = fromWorld.getName();
        boolean isSpeedrunRelated = name.startsWith(speedrunWorldName);

        if (portalType == PortalType.NETHER) {
            if (isSpeedrunRelated) {
                // 스피드런 계열 → 스피드런 네더 ↔ 스피드런 오버월드
                if (name.equals(speedrunWorldName + "_nether")) {
                    return Bukkit.getWorld(speedrunWorldName);         // 네더 → 오버월드
                } else {
                    return Bukkit.getWorld(speedrunWorldName + "_nether"); // 오버월드 → 네더
                }
            } else {
                // 힐링 계열 → 힐링 네더 ↔ 힐링 오버월드
                // 힐링 네더는 Bukkit 기본 규칙(world_nether)을 따름 → null 반환(기본처리)
                return null;
            }
        } else if (portalType == PortalType.END) {
            if (isSpeedrunRelated) {
                if (name.equals(speedrunWorldName + "_the_end")) {
                    return Bukkit.getWorld(speedrunWorldName);            // 엔드 → 오버월드
                } else {
                    return Bukkit.getWorld(speedrunWorldName + "_the_end"); // 오버월드 → 엔드
                }
            } else {
                // 힐링 엔드는 기본 처리
                return null;
            }
        }
        return null;
    }

    // ─── 리셋 ────────────────────────────────────────────────────
    public void triggerResetWithCountdown(String killerName) {
        if (resetInProgress) return;
        resetInProgress = true;

        int delay = plugin.getConfig().getInt("speedrun-world.death-reset-delay", 5);
        Bukkit.broadcastMessage(plugin.getMessage("player-died",
                "{player}", killerName, "{delay}", String.valueOf(delay)));

        for (int i = delay; i >= 1; i--) {
            final int sec = i;
            long tickDelay = (long)(delay - i) * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                Bukkit.broadcastMessage(plugin.getPrefix() + "§c월드 리셋까지 §e" + sec + "§c초..."),
                tickDelay);
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::doReset, (long) delay * 20L);
    }

    private void doReset() {
        plugin.getLogger().info("스피드런 월드 리셋 실행!");
        plugin.getTimerManager().stopTimer();

        World hw = getHealingWorld();

        // 스피드런 계열 월드에 있는 플레이어를 힐링으로 이동
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInSpeedrunWorld(p) || pendingRespawn.contains(p.getUniqueId())) {
                // 인벤토리 저장하지 않음 (리셋이므로 버림)
                // 힐링 데이터 로드
                plugin.getPlayerDataManager().loadHealingData(p);
                Location dest = plugin.getPlayerDataManager().getLastHealingLocation(p);
                if (dest == null || dest.getWorld() == null) dest = getSafeSpawnLocation(hw);
                p.teleport(dest);
                plugin.getPlayerDataManager().setCurrentWorld(p, "healing");
                p.sendMessage(plugin.getPrefix() + "§c스피드런 월드가 리셋되어 힐링 월드로 이동됩니다.");
            }
        }
        pendingRespawn.clear();

        // FIX #1: 모든 플레이어의 스피드런 저장 데이터 삭제
        plugin.getPlayerDataManager().clearAllSpeedrunData();

        unloadAndDelete();

        currentSpeedrunSeed = new Random().nextLong();
        plugin.getConfig().set("speedrun-world.seed", currentSpeedrunSeed);
        plugin.saveConfig();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            createSpeedrunWorld(currentSpeedrunSeed);
            Bukkit.broadcastMessage(plugin.getMessage("world-reset",
                    "{seed}", String.valueOf(currentSpeedrunSeed)));
            resetInProgress = false;
            plugin.getStatsManager().incrementWorldResets();
        }, 60L);
    }

    private void unloadAndDelete() {
        String[] names = {speedrunWorldName, speedrunWorldName + "_nether", speedrunWorldName + "_the_end"};
        for (String name : names) {
            World w = Bukkit.getWorld(name);
            if (w != null) Bukkit.unloadWorld(w, false);
            File folder = new File(Bukkit.getWorldContainer(), name);
            if (folder.exists()) deleteDir(folder);
        }
    }

    private void deleteDir(File dir) {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                    Files.delete(f); return FileVisitResult.CONTINUE; }
                @Override public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                    Files.delete(d); return FileVisitResult.CONTINUE; }
            });
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "폴더 삭제 실패: " + dir.getName(), e);
        }
    }

    // ─── Getters / Setters ───────────────────────────────────────
    public void addPendingRespawn(UUID uuid)    { pendingRespawn.add(uuid); }
    public boolean isPendingRespawn(UUID uuid)  { return pendingRespawn.contains(uuid); }
    public void removePendingRespawn(UUID uuid) { pendingRespawn.remove(uuid); }

    public boolean isInSpeedrunWorld(Player p) {
        String n = p.getWorld().getName();
        return n.equals(speedrunWorldName)
                || n.equals(speedrunWorldName + "_nether")
                || n.equals(speedrunWorldName + "_the_end");
    }
    public boolean isInHealingWorld(Player p)  { return !isInSpeedrunWorld(p); }
    public boolean isResetInProgress()         { return resetInProgress; }

    public World getHealingWorld() {
        World w = Bukkit.getWorld(healingWorldName);
        return w != null ? w : Bukkit.getWorlds().get(0);
    }
    public World getSpeedrunWorld()        { return Bukkit.getWorld(speedrunWorldName); }
    public String getHealingWorldName()    { return healingWorldName; }
    public String getSpeedrunWorldName()   { return speedrunWorldName; }
    public long getCurrentSpeedrunSeed()   { return currentSpeedrunSeed; }
    public String getHealingDisplayName()  { return plugin.getConfig().getString("healing-world.display-name",  "§a힐링 월드"); }
    public String getSpeedrunDisplayName() { return plugin.getConfig().getString("speedrun-world.display-name", "§c스피드런 월드"); }
}
