package com.dualworld.managers;

import com.dualworld.DualWorldPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;
import java.util.logging.Level;

public class WorldManager {

    private final DualWorldPlugin plugin;
    private String healingWorldName;
    private String speedrunWorldName;
    private long currentSpeedrunSeed;

    public WorldManager(DualWorldPlugin plugin) {
        this.plugin = plugin;
        this.healingWorldName = plugin.getConfig().getString("healing-world.name", "world");
        this.speedrunWorldName = plugin.getConfig().getString("speedrun-world.name", "speedrun_world");
        this.currentSpeedrunSeed = plugin.getConfig().getLong("speedrun-world.seed", 0L);
        if (this.currentSpeedrunSeed == 0L) {
            this.currentSpeedrunSeed = new Random().nextLong();
            plugin.getConfig().set("speedrun-world.seed", this.currentSpeedrunSeed);
            plugin.saveConfig();
        }
    }

    public void initializeWorlds() {
        World healingWorld = Bukkit.getWorld(healingWorldName);
        if (healingWorld == null) {
            plugin.getLogger().warning("힐링 월드를 찾을 수 없습니다. 기본 world 사용.");
            healingWorldName = Bukkit.getWorlds().get(0).getName();
        } else {
            String diffStr = plugin.getConfig().getString("healing-world.difficulty", "NORMAL");
            try {
                healingWorld.setDifficulty(Difficulty.valueOf(diffStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                healingWorld.setDifficulty(Difficulty.NORMAL);
            }
        }

        World speedrunWorld = Bukkit.getWorld(speedrunWorldName);
        if (speedrunWorld == null) {
            speedrunWorld = createSpeedrunWorld(currentSpeedrunSeed);
        }
        if (speedrunWorld != null) {
            speedrunWorld.setDifficulty(Difficulty.HARD);
        }
    }

    public World createSpeedrunWorld(long seed) {
        plugin.getLogger().info("스피드런 월드 생성 중... 시드: " + seed);

        WorldCreator creator = new WorldCreator(speedrunWorldName);
        creator.seed(seed);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.NORMAL);
        World world = creator.createWorld();

        if (world != null) {
            world.setDifficulty(Difficulty.HARD);

            WorldCreator netherCreator = new WorldCreator(speedrunWorldName + "_nether");
            netherCreator.seed(seed);
            netherCreator.environment(World.Environment.NETHER);
            World nether = netherCreator.createWorld();
            if (nether != null) nether.setDifficulty(Difficulty.HARD);

            WorldCreator endCreator = new WorldCreator(speedrunWorldName + "_the_end");
            endCreator.seed(seed);
            endCreator.environment(World.Environment.THE_END);
            World end = endCreator.createWorld();
            if (end != null) end.setDifficulty(Difficulty.HARD);

            plugin.getLogger().info("스피드런 월드 생성 완료! 시드: " + seed);
        }
        return world;
    }

    public void resetSpeedrunWorld() {
        long newSeed = new Random().nextLong();
        plugin.getLogger().info("스피드런 월드 리셋! 새 시드: " + newSeed);

        World healingWorld = getHealingWorld();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInSpeedrunWorld(player)) {
                plugin.getPlayerDataManager().saveSpeedrunData(player);
                Location healLoc = plugin.getPlayerDataManager().getLastHealingLocation(player);
                if (healLoc == null || healLoc.getWorld() == null) {
                    healLoc = healingWorld.getSpawnLocation();
                }
                plugin.getPlayerDataManager().loadHealingData(player);
                player.teleport(healLoc);
                plugin.getPlayerDataManager().setCurrentWorld(player, "healing");
                player.sendMessage(plugin.getPrefix() + "§c스피드런 월드가 리셋되어 힐링 월드로 이동되었습니다.");
                player.sendMessage(plugin.getPrefix() + "§a힐링 월드에 오신 것을 환영합니다!");
            }
        }

        unloadAndDeleteSpeedrunWorlds();

        currentSpeedrunSeed = newSeed;
        plugin.getConfig().set("speedrun-world.seed", currentSpeedrunSeed);
        plugin.saveConfig();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            createSpeedrunWorld(currentSpeedrunSeed);
            Bukkit.broadcastMessage(plugin.getMessage("world-reset", "{seed}", String.valueOf(currentSpeedrunSeed)));
        }, 60L);
    }

    private void unloadAndDeleteSpeedrunWorlds() {
        String[] worlds = {speedrunWorldName, speedrunWorldName + "_nether", speedrunWorldName + "_the_end"};
        for (String name : worlds) {
            World w = Bukkit.getWorld(name);
            if (w != null) Bukkit.unloadWorld(w, false);
            File folder = new File(Bukkit.getWorldContainer(), name);
            if (folder.exists()) deleteDirectory(folder);
        }
    }

    private void deleteDirectory(File dir) {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "폴더 삭제 실패: " + dir.getName(), e);
        }
    }

    public boolean isInSpeedrunWorld(Player player) {
        String wName = player.getWorld().getName();
        return wName.equals(speedrunWorldName)
                || wName.equals(speedrunWorldName + "_nether")
                || wName.equals(speedrunWorldName + "_the_end");
    }

    public boolean isInHealingWorld(Player player) {
        return !isInSpeedrunWorld(player);
    }

    public World getHealingWorld() {
        World w = Bukkit.getWorld(healingWorldName);
        if (w == null) w = Bukkit.getWorlds().get(0);
        return w;
    }

    public World getSpeedrunWorld() {
        return Bukkit.getWorld(speedrunWorldName);
    }

    public String getHealingWorldName() { return healingWorldName; }
    public String getSpeedrunWorldName() { return speedrunWorldName; }
    public long getCurrentSpeedrunSeed() { return currentSpeedrunSeed; }

    public String getHealingDisplayName() {
        return plugin.getConfig().getString("healing-world.display-name", "§a힐링 월드");
    }

    public String getSpeedrunDisplayName() {
        return plugin.getConfig().getString("speedrun-world.display-name", "§c스피드런 월드");
    }
}
