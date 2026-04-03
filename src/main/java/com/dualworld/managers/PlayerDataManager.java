package com.dualworld.managers;

import com.dualworld.DualWorldPlugin;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
        applyState(p, cfg, "healing");
    }
    public Location getLastHealingLocation(Player p) { return getLocation(getCfg(p), "healing"); }

    public void saveSpeedrunData(Player p) {
        FileConfiguration cfg = getCfg(p);
        saveState(p, cfg, "speedrun");
        saveCfg(p, cfg);
    }
    public void loadSpeedrunData(Player p) {
        FileConfiguration cfg = getCfg(p);
        applyState(p, cfg, "speedrun");
    }
    public Location getLastSpeedrunLocation(Player p) { return getLocation(getCfg(p), "speedrun"); }

    // ─── Core serialization ───────────────────────────────────────
    private void saveState(Player p, FileConfiguration cfg, String prefix) {
        // Clear old data sections
        cfg.set(prefix + ".inventory", null);
        cfg.set(prefix + ".armor", null);
        cfg.set(prefix + ".effects", null);

        // Inventory
        ItemStack[] inv   = p.getInventory().getContents();
        ItemStack[] armor = p.getInventory().getArmorContents();
        for (int i = 0; i < inv.length; i++)
            if (inv[i] != null) cfg.set(prefix + ".inventory." + i, inv[i]);
        for (int i = 0; i < armor.length; i++)
            if (armor[i] != null) cfg.set(prefix + ".armor." + i, armor[i]);

        // ── 체력: maxHealth 먼저, health 나중 ──
        double maxHp = getMaxHealth(p);
        cfg.set(prefix + ".maxHealth",  maxHp);
        cfg.set(prefix + ".health",     Math.min(p.getHealth(), maxHp));

        // 나머지 스탯
        cfg.set(prefix + ".food",       p.getFoodLevel());
        cfg.set(prefix + ".saturation", (double) p.getSaturation());
        cfg.set(prefix + ".exhaustion", (double) p.getExhaustion());
        cfg.set(prefix + ".exp",        (double) p.getExp());
        cfg.set(prefix + ".level",      p.getLevel());
        cfg.set(prefix + ".totalExp",   p.getTotalExperience());
        cfg.set(prefix + ".gamemode",   p.getGameMode().name());

        // 포션 효과
        int idx = 0;
        for (PotionEffect effect : p.getActivePotionEffects()) {
            cfg.set(prefix + ".effects." + idx + ".type",      effect.getType().getName());
            cfg.set(prefix + ".effects." + idx + ".duration",  effect.getDuration());
            cfg.set(prefix + ".effects." + idx + ".amplifier", effect.getAmplifier());
            cfg.set(prefix + ".effects." + idx + ".ambient",   effect.isAmbient());
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

    /**
     * applyState: 인벤/포션은 즉시 적용, 체력/허기는 1틱 후 적용
     * (teleport 직후 바로 setHealth 하면 무시되는 버그 방지)
     */
    private void applyState(Player p, FileConfiguration cfg, String prefix) {
        if (!cfg.contains(prefix)) {
            // 첫 방문: 체력/허기 풀로 설정
            resetToDefault(p);
            return;
        }

        // 인벤토리 즉시 클리어 후 복원
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);

        for (int i = 0; i < 36; i++) {
            ItemStack item = cfg.getItemStack(prefix + ".inventory." + i);
            if (item != null) p.getInventory().setItem(i, item);
        }
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) armor[i] = cfg.getItemStack(prefix + ".armor." + i);
        p.getInventory().setArmorContents(armor);

        // 포션 효과 즉시 교체
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        if (cfg.contains(prefix + ".effects")) {
            for (String key : cfg.getConfigurationSection(prefix + ".effects").getKeys(false)) {
                String typeName = cfg.getString(prefix + ".effects." + key + ".type");
                int    duration  = cfg.getInt(prefix + ".effects." + key + ".duration");
                int    amplifier = cfg.getInt(prefix + ".effects." + key + ".amplifier");
                boolean ambient  = cfg.getBoolean(prefix + ".effects." + key + ".ambient", false);
                PotionEffectType type = PotionEffectType.getByName(typeName != null ? typeName : "");
                if (type != null) p.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient));
            }
        }

        // GameMode 즉시 적용
        String gm = cfg.getString(prefix + ".gamemode", "SURVIVAL");
        try { p.setGameMode(GameMode.valueOf(gm)); }
        catch (IllegalArgumentException e) { p.setGameMode(GameMode.SURVIVAL); }

        // ── 체력/허기: 1틱 뒤에 적용 (teleport 후 안전하게) ──
        final double maxHp    = cfg.getDouble(prefix + ".maxHealth", 20.0);
        final double hp       = cfg.getDouble(prefix + ".health", maxHp);
        final int    food     = cfg.getInt(prefix + ".food", 20);
        final float  sat      = (float) cfg.getDouble(prefix + ".saturation", 5.0);
        final float  exh      = (float) cfg.getDouble(prefix + ".exhaustion", 0.0);
        final float  exp      = (float) cfg.getDouble(prefix + ".exp", 0.0);
        final int    level    = cfg.getInt(prefix + ".level", 0);
        final int    totalExp = cfg.getInt(prefix + ".totalExp", 0);

        new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) return;
                setMaxHealth(p, maxHp);
                p.setHealth(Math.min(Math.max(hp, 0.5), getMaxHealth(p)));
                p.setFoodLevel(food);
                p.setSaturation(sat);
                p.setExhaustion(exh);
                p.setExp(Math.max(0, Math.min(exp, 1.0f)));
                p.setLevel(level);
                p.setTotalExperience(totalExp);
            }
        }.runTaskLater(plugin, 2L);
    }

    private void resetToDefault(Player p) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.setGameMode(GameMode.SURVIVAL);

        new BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) return;
                setMaxHealth(p, 20.0);
                p.setHealth(20.0);
                p.setFoodLevel(20);
                p.setSaturation(5.0f);
                p.setExhaustion(0.0f);
                p.setExp(0);
                p.setLevel(0);
                p.setTotalExperience(0);
            }
        }.runTaskLater(plugin, 2L);
    }

    // ─── MaxHealth 호환 헬퍼 (1.12 API) ─────────────────────────
    @SuppressWarnings("deprecation")
    private double getMaxHealth(Player p) {
        try {
            // 1.9+ API
            org.bukkit.attribute.AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) return attr.getValue();
        } catch (Throwable ignored) {}
        return p.getMaxHealth();
    }

    @SuppressWarnings("deprecation")
    private void setMaxHealth(Player p, double value) {
        try {
            org.bukkit.attribute.AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) { attr.setBaseValue(value); return; }
        } catch (Throwable ignored) {}
        p.setMaxHealth(value);
    }

    // ─── Location helper ─────────────────────────────────────────
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

    // ─── Bulk save ───────────────────────────────────────────────
    public void saveAllData() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ("speedrun".equals(getCurrentWorld(p))) saveSpeedrunData(p);
            else saveHealingData(p);
        }
        plugin.getLogger().info("모든 플레이어 데이터 저장 완료");
    }

    public void onPlayerJoin(Player p) {
        if (plugin.getWorldManager().isInSpeedrunWorld(p)) setCurrentWorld(p, "speedrun");
        else setCurrentWorld(p, "healing");
    }
}
