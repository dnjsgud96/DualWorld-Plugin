package com.dualworld.managers;

import com.dualworld.DualWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final DualWorldPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, String> currentWorldMap = new HashMap<>();

    public PlayerDataManager(DualWorldPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    // ─── World tracking ───────────────────────────────────────────
    public void setCurrentWorld(Player player, String worldType) {
        currentWorldMap.put(player.getUniqueId(), worldType);
    }

    public String getCurrentWorld(Player player) {
        return currentWorldMap.getOrDefault(player.getUniqueId(), "healing");
    }

    // ─── Save / Load helpers ──────────────────────────────────────
    private File getPlayerFile(Player player) {
        return new File(dataFolder, player.getUniqueId().toString() + ".yml");
    }

    private FileConfiguration getPlayerConfig(Player player) {
        return YamlConfiguration.loadConfiguration(getPlayerFile(player));
    }

    private void savePlayerConfig(Player player, FileConfiguration cfg) {
        try {
            cfg.save(getPlayerFile(player));
        } catch (IOException e) {
            plugin.getLogger().warning("플레이어 데이터 저장 실패: " + player.getName());
        }
    }

    // ─── Healing world ────────────────────────────────────────────
    public void saveHealingData(Player player) {
        FileConfiguration cfg = getPlayerConfig(player);
        savePlayerState(player, cfg, "healing");
        savePlayerConfig(player, cfg);
    }

    public void loadHealingData(Player player) {
        FileConfiguration cfg = getPlayerConfig(player);
        loadPlayerState(player, cfg, "healing");
    }

    public Location getLastHealingLocation(Player player) {
        FileConfiguration cfg = getPlayerConfig(player);
        return getLocationFromConfig(cfg, "healing");
    }

    // ─── Speedrun world ───────────────────────────────────────────
    public void saveSpeedrunData(Player player) {
        FileConfiguration cfg = getPlayerConfig(player);
        savePlayerState(player, cfg, "speedrun");
        savePlayerConfig(player, cfg);
    }

    public void loadSpeedrunData(Player player) {
        FileConfiguration cfg = getPlayerConfig(player);
        loadPlayerState(player, cfg, "speedrun");
    }

    public Location getLastSpeedrunLocation(Player player) {
        FileConfiguration cfg = getPlayerConfig(player);
        return getLocationFromConfig(cfg, "speedrun");
    }

    // ─── Core serialization ───────────────────────────────────────
    private void savePlayerState(Player player, FileConfiguration cfg, String prefix) {
        // Inventory
        ItemStack[] inv = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null) cfg.set(prefix + ".inventory." + i, inv[i]);
        }
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) cfg.set(prefix + ".armor." + i, armor[i]);
        }
        // Stats
        cfg.set(prefix + ".health", player.getHealth());
        cfg.set(prefix + ".food", player.getFoodLevel());
        cfg.set(prefix + ".saturation", player.getSaturation());
        cfg.set(prefix + ".exp", player.getExp());
        cfg.set(prefix + ".level", player.getLevel());
        cfg.set(prefix + ".totalexp", player.getTotalExperience());
        cfg.set(prefix + ".gamemode", player.getGameMode().name());

        // Location
        Location loc = player.getLocation();
        cfg.set(prefix + ".location.world", loc.getWorld().getName());
        cfg.set(prefix + ".location.x", loc.getX());
        cfg.set(prefix + ".location.y", loc.getY());
        cfg.set(prefix + ".location.z", loc.getZ());
        cfg.set(prefix + ".location.yaw", loc.getYaw());
        cfg.set(prefix + ".location.pitch", loc.getPitch());
    }

    private void loadPlayerState(Player player, FileConfiguration cfg, String prefix) {
        if (!cfg.contains(prefix)) return;

        // Clear current
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Inventory
        for (int i = 0; i < 36; i++) {
            if (cfg.contains(prefix + ".inventory." + i)) {
                player.getInventory().setItem(i, (ItemStack) cfg.getItemStack(prefix + ".inventory." + i));
            }
        }
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            if (cfg.contains(prefix + ".armor." + i)) {
                armor[i] = cfg.getItemStack(prefix + ".armor." + i);
            }
        }
        player.getInventory().setArmorContents(armor);

        // Stats
        double health = cfg.getDouble(prefix + ".health", 20.0);
        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(cfg.getInt(prefix + ".food", 20));
        player.setSaturation((float) cfg.getDouble(prefix + ".saturation", 5.0));
        player.setExp((float) cfg.getDouble(prefix + ".exp", 0));
        player.setLevel(cfg.getInt(prefix + ".level", 0));
        player.setTotalExperience(cfg.getInt(prefix + ".totalexp", 0));

        String gamemodeStr = cfg.getString(prefix + ".gamemode", "SURVIVAL");
        try {
            player.setGameMode(GameMode.valueOf(gamemodeStr));
        } catch (IllegalArgumentException e) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private Location getLocationFromConfig(FileConfiguration cfg, String prefix) {
        if (!cfg.contains(prefix + ".location")) return null;
        String worldName = cfg.getString(prefix + ".location.world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) return null;
        return new Location(
                Bukkit.getWorld(worldName),
                cfg.getDouble(prefix + ".location.x"),
                cfg.getDouble(prefix + ".location.y"),
                cfg.getDouble(prefix + ".location.z"),
                (float) cfg.getDouble(prefix + ".location.yaw"),
                (float) cfg.getDouble(prefix + ".location.pitch")
        );
    }

    // ─── On server shutdown ───────────────────────────────────────
    public void saveAllData() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String world = getCurrentWorld(player);
            if ("speedrun".equals(world)) {
                saveSpeedrunData(player);
            } else {
                saveHealingData(player);
            }
        }
        plugin.getLogger().info("모든 플레이어 데이터 저장 완료.");
    }

    public void onPlayerJoin(Player player) {
        if (plugin.getWorldManager().isInSpeedrunWorld(player)) {
            setCurrentWorld(player, "speedrun");
        } else {
            setCurrentWorld(player, "healing");
        }
    }
}
