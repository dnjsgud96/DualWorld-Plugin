package com.dualworld.managers;

import com.dualworld.DualWorldPlugin;
import org.bukkit.*;
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
        World hw = Bukkit.getWorld(healingWorldName);
        if (hw == null) {
            plugin.getLogger().warning("힐링 월드 없음 - 기본 world 사용");
            healingWorldName = Bukkit.getWorlds().get(0).getName();
            hw = Bukkit.getWorlds().get(0);
        }
        String diffStr = plugin.getConfig().getString("healing-world.difficulty", "NORMAL");
        try { hw.setDifficulty(Difficulty.valueOf(diffStr.toUpperCase())); }
        catch (IllegalArgumentException e) { hw.setDifficulty(Difficulty.NORMAL); }

        World sw = Bukkit.getWorld(speedrunWorldName);
        if (sw == null) sw = createSpeedrunWorld(currentSpeedrunSeed);
        if (sw != null) sw.setDifficulty(Difficulty.HARD);

        plugin.getLogger().info("월드 초기화 완료");
    }

    public World createSpeedrunWorld(long seed) {
        plugin.getLogger().info("스피드런 월드 생성 중... 시드: " + seed);

        WorldCreator ow = new WorldCreator(speedrunWorldName);
        ow.seed(seed); ow.environment(World.Environment.NORMAL); ow.type(WorldType.NORMAL);
        World world = ow.createWorld();
        if (world == null) return null;
        world.setDifficulty(Difficulty.HARD);

        WorldCreator nw = new WorldCreator(speedrunWorldName + "_nether");
        nw.seed(seed); nw.environment(World.Environment.NETHER);
        World nether = nw.createWorld();
        if (nether != null) nether.setDifficulty(Difficulty.HARD);

        WorldCreator ew = new WorldCreator(speedrunWorldName + "_the_end");
        ew.seed(seed); ew.environment(World.Environment.THE_END);
        World end = ew.createWorld();
        if (end != null) end.setDifficulty(Difficulty.HARD);

        plugin.getLogger().info("스피드런 월드 생성 완료! 시드: " + seed);
        return world;
    }

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
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInSpeedrunWorld(p) || pendingRespawn.contains(p.getUniqueId())) {
                plugin.getPlayerDataManager().saveSpeedrunData(p);
                plugin.getPlayerDataManager().loadHealingData(p);
                Location dest = plugin.getPlayerDataManager().getLastHealingLocation(p);
                if (dest == null || dest.getWorld() == null) dest = hw.getSpawnLocation();
                p.teleport(dest);
                plugin.getPlayerDataManager().setCurrentWorld(p, "healing");
                p.sendMessage(plugin.getPrefix() + "§c스피드런 월드가 리셋되어 힐링 월드로 이동됩니다.");
            }
        }
        pendingRespawn.clear();
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

    public void addPendingRespawn(UUID uuid)    { pendingRespawn.add(uuid); }
    public boolean isPendingRespawn(UUID uuid)  { return pendingRespawn.contains(uuid); }
    public void removePendingRespawn(UUID uuid) { pendingRespawn.remove(uuid); }

    public boolean isInSpeedrunWorld(Player p) {
        String n = p.getWorld().getName();
        return n.equals(speedrunWorldName) || n.equals(speedrunWorldName + "_nether") || n.equals(speedrunWorldName + "_the_end");
    }
    public boolean isInHealingWorld(Player p)  { return !isInSpeedrunWorld(p); }
    public boolean isResetInProgress()         { return resetInProgress; }

    public World getHealingWorld() {
        World w = Bukkit.getWorld(healingWorldName);
        return w != null ? w : Bukkit.getWorlds().get(0);
    }
    public World getSpeedrunWorld()            { return Bukkit.getWorld(speedrunWorldName); }
    public String getHealingWorldName()        { return healingWorldName; }
    public String getSpeedrunWorldName()       { return speedrunWorldName; }
    public long getCurrentSpeedrunSeed()       { return currentSpeedrunSeed; }
    public String getHealingDisplayName()      { return plugin.getConfig().getString("healing-world.display-name", "§a힐링 월드"); }
    public String getSpeedrunDisplayName()     { return plugin.getConfig().getString("speedrun-world.display-name", "§c스피드런 월드"); }
}
