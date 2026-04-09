package com.dualworld.managers;

import com.dualworld.DualWorldPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

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

    // ─── World tracking ──────────────────────────────────────────
    public void setCurrentWorld(Player p, String type) { currentWorldMap.put(p.getUniqueId(), type); }
    public String getCurrentWorld(Player p) { return currentWorldMap.getOrDefault(p.getUniqueId(), "healing"); }

    // ─── File helpers ─────────────────────────────────────────────
    private File getFile(Player p)             { return new File(dataFolder, p.getUniqueId() + ".yml"); }
    private FileConfiguration getCfg(Player p) { return YamlConfiguration.loadConfiguration(getFile(p)); }
    private void saveCfg(Player p, FileConfiguration cfg) {
        try { cfg.save(getFile(p)); }
        catch (IOException e) { plugin.getLogger().warning("저장 실패: " + p.getName()); }
    }

    // ─── Public API ───────────────────────────────────────────────
    public void saveHealingData(Player p) {
        FileConfiguration cfg = getCfg(p);
        saveState(p, cfg, "healing");
        saveCfg(p, cfg);
    }
    public void loadHealingData(Player p) {
        FileConfiguration cfg = getCfg(p);
        loadState(p, cfg, "healing");
    }
    public Location getLastHealingLocation(Player p) {
        return getLocation(getCfg(p), "healing");
    }

    public void saveSpeedrunData(Player p) {
        FileConfiguration cfg = getCfg(p);
        saveState(p, cfg, "speedrun");
        saveCfg(p, cfg);
    }
    public void loadSpeedrunData(Player p) {
        FileConfiguration cfg = getCfg(p);
        loadState(p, cfg, "speedrun");
    }
    public Location getLastSpeedrunLocation(Player p) {
        return getLocation(getCfg(p), "speedrun");
    }

    /**
     * FIX #1: 월드 리셋 시 모든 플레이어의 스피드런 저장 데이터를 완전히 삭제.
     * 다음 스피드런 진입 시 빈 인벤토리/기본 스탯으로 시작하게 됨.
     */
    public void clearSpeedrunData(Player p) {
        FileConfiguration cfg = getCfg(p);
        cfg.set("speedrun", null);   // speedrun 섹션 전체 제거
        saveCfg(p, cfg);
    }

    /** 모든 온라인 플레이어의 스피드런 데이터 삭제 (리셋 시 호출) */
    public void clearAllSpeedrunData() {
        // 온라인 플레이어
        for (Player p : Bukkit.getOnlinePlayers()) {
            clearSpeedrunData(p);
        }
        // 오프라인 플레이어 파일도 정리
        File[] files = dataFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            cfg.set("speedrun", null);
            try { cfg.save(f); } catch (IOException e) { /* ignore */ }
        }
    }

    // ─── Core serialization ───────────────────────────────────────

    /**
     * FIX #2: 왼손(offhand) 슬롯 포함 전체 인벤토리 저장.
     */
    private void saveState(Player p, FileConfiguration cfg, String prefix) {
        PlayerInventory inv = p.getInventory();

        // 인벤토리 (0-35 메인, 36-39 방어구, 40 오프핸드)
        cfg.set(prefix + ".inventory", null);
        ItemStack[] contents = inv.getContents(); // 0~35
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) cfg.set(prefix + ".inventory." + i, contents[i]);
        }

        // 방어구 (0=부츠, 1=레깅스, 2=흉갑, 3=투구)
        cfg.set(prefix + ".armor", null);
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) cfg.set(prefix + ".armor." + i, armor[i]);
        }

        // FIX #2: 오프핸드(왼손) 저장
        cfg.set(prefix + ".offhand", null);
        ItemStack offhand = inv.getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            cfg.set(prefix + ".offhand", offhand);
        }

        // 스탯
        cfg.set(prefix + ".health",     p.getHealth());
        cfg.set(prefix + ".maxHealth",  p.getMaxHealth());
        cfg.set(prefix + ".food",       p.getFoodLevel());
        cfg.set(prefix + ".saturation", (double) p.getSaturation());
        cfg.set(prefix + ".exp",        (double) p.getExp());
        cfg.set(prefix + ".level",      p.getLevel());
        cfg.set(prefix + ".totalExp",   p.getTotalExperience());
        cfg.set(prefix + ".gamemode",   p.getGameMode().name());

        // 포션 효과
        cfg.set(prefix + ".effects", null);
        int idx = 0;
        for (PotionEffect effect : p.getActivePotionEffects()) {
            cfg.set(prefix + ".effects." + idx + ".type",      effect.getType().getName());
            cfg.set(prefix + ".effects." + idx + ".duration",  effect.getDuration());
            cfg.set(prefix + ".effects." + idx + ".amplifier", effect.getAmplifier());
            idx++;
        }

        // 위치
        Location loc = p.getLocation();
        cfg.set(prefix + ".loc.world", loc.getWorld().getName());
        cfg.set(prefix + ".loc.x",     loc.getX());
        cfg.set(prefix + ".loc.y",     loc.getY());
        cfg.set(prefix + ".loc.z",     loc.getZ());
        cfg.set(prefix + ".loc.yaw",   (double) loc.getYaw());
        cfg.set(prefix + ".loc.pitch", (double) loc.getPitch());
    }

    private void loadState(Player p, FileConfiguration cfg, String prefix) {
        PlayerInventory inv = p.getInventory();

        // 전체 초기화
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        for (PotionEffect effect : p.getActivePotionEffects())
            p.removePotionEffect(effect.getType());

        if (!cfg.contains(prefix)) {
            // 저장 데이터 없음 → 깨끗한 상태(빈 인벤토리, 풀 체력)로 시작
            p.setMaxHealth(20.0);
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            p.setExp(0f);
            p.setLevel(0);
            p.setTotalExperience(0);
            p.setGameMode(GameMode.SURVIVAL);
            return;
        }

        // 메인 인벤토리
        for (int i = 0; i < 36; i++) {
            ItemStack item = cfg.getItemStack(prefix + ".inventory." + i);
            if (item != null) inv.setItem(i, item);
        }

        // 방어구
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) armor[i] = cfg.getItemStack(prefix + ".armor." + i);
        inv.setArmorContents(armor);

        // FIX #2: 오프핸드 복원
        ItemStack offhand = cfg.getItemStack(prefix + ".offhand");
        if (offhand != null) inv.setItemInOffHand(offhand);

        // 스탯
        double maxHp = cfg.getDouble(prefix + ".maxHealth", 20.0);
        p.setMaxHealth(maxHp);
        double hp = cfg.getDouble(prefix + ".health", maxHp);
        p.setHealth(Math.min(Math.max(hp, 0.1), maxHp));
        p.setFoodLevel(cfg.getInt(prefix + ".food", 20));
        p.setSaturation((float) cfg.getDouble(prefix + ".saturation", 5.0));
        p.setExp((float) cfg.getDouble(prefix + ".exp", 0));
        p.setLevel(cfg.getInt(prefix + ".level", 0));
        p.setTotalExperience(cfg.getInt(prefix + ".totalExp", 0));

        String gm = cfg.getString(prefix + ".gamemode", "SURVIVAL");
        try { p.setGameMode(GameMode.valueOf(gm)); }
        catch (IllegalArgumentException e) { p.setGameMode(GameMode.SURVIVAL); }

        // 포션 효과
        if (cfg.contains(prefix + ".effects")) {
            for (String key : cfg.getConfigurationSection(prefix + ".effects").getKeys(false)) {
                String typeName = cfg.getString(prefix + ".effects." + key + ".type");
                int duration    = cfg.getInt(prefix + ".effects." + key + ".duration");
                int amplifier   = cfg.getInt(prefix + ".effects." + key + ".amplifier");
                org.bukkit.potion.PotionEffectType type =
                        org.bukkit.potion.PotionEffectType.getByName(typeName);
                if (type != null) p.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }
    }

    private Location getLocation(FileConfiguration cfg, String prefix) {
        if (!cfg.contains(prefix + ".loc")) return null;
        String wn = cfg.getString(prefix + ".loc.world");
        World w = wn != null ? Bukkit.getWorld(wn) : null;
        if (w == null) return null;
        return new Location(w,
            cfg.getDouble(prefix + ".loc.x"),
            cfg.getDouble(prefix + ".loc.y"),
            cfg.getDouble(prefix + ".loc.z"),
            (float) cfg.getDouble(prefix + ".loc.yaw"),
            (float) cfg.getDouble(prefix + ".loc.pitch"));
    }

    public void saveAllData() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ("speedrun".equals(getCurrentWorld(p))) saveSpeedrunData(p);
            else saveHealingData(p);
        }
    }

    public void onPlayerJoin(Player p) {
        if (plugin.getWorldManager().isInSpeedrunWorld(p)) setCurrentWorld(p, "speedrun");
        else setCurrentWorld(p, "healing");
    }
}
