package com.dualworld.commands;

import com.dualworld.DualWorldPlugin;
import com.dualworld.managers.SpeedrunTimerManager;
import com.dualworld.managers.StatsManager;
import com.dualworld.managers.WorldManager;
import com.dualworld.utils.TeleportUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DualWorldCommand implements CommandExecutor {

    private final DualWorldPlugin plugin;

    public DualWorldCommand(DualWorldPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String p = plugin.getPrefix();
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "help":      sendHelp(sender);                              break;
            case "healing":   requirePlayer(sender, () -> TeleportUtil.teleportToHealing((Player) sender, plugin));  break;
            case "speedrun":  requirePlayer(sender, () -> TeleportUtil.teleportToSpeedrun((Player) sender, plugin)); break;
            case "where":     requirePlayer(sender, () -> sendWhere((Player) sender));   break;
            case "status":    sendStatus(sender);                            break;
            case "timer":     requirePlayer(sender, () -> handleTimer((Player) sender, args)); break;
            case "stats":     handleStats(sender, args);                     break;
            case "reset":     handleReset(sender);                           break;
            case "setdifficulty": handleSetDiff(sender, args);               break;
            case "reload":
                requireAdmin(sender, () -> {
                    plugin.reloadConfig();
                    sender.sendMessage(p + "§a설정 파일을 다시 불러왔습니다.");
                });
                break;
            default:
                sender.sendMessage(p + "§c알 수 없는 명령어입니다. §e/dw help §c참조");
        }
        return true;
    }

    // ─── /dw help ─────────────────────────────────────────────────
    private void sendHelp(CommandSender s) {
        String p = plugin.getPrefix();
        s.sendMessage("§8§m════════════════════════════════════");
        s.sendMessage(p + "§6DualWorld v2.0 §7명령어 도움말");
        s.sendMessage("§8§m════════════════════════════════════");
        s.sendMessage("§e/healing §8│ §e/dw healing    §7힐링 월드로 이동");
        s.sendMessage("§e/speedrun §8│ §e/dw speedrun   §7스피드런 월드로 이동");
        s.sendMessage("§e/dw where                  §7현재 내 월드 확인");
        s.sendMessage("§e/dw status                 §7두 월드 현재 상태");
        s.sendMessage("§e/dw timer hud              §7⏱ HUD 켜기/끄기");
        s.sendMessage("§e/dw timer time             §7현재 타이머 확인");
        s.sendMessage("§e/dw stats                  §7내 스피드런 통계");
        s.sendMessage("§e/dw stats top              §7전체 랭킹 보기");
        s.sendMessage("§e/dw stats all              §7전체 플레이어 통계");
        s.sendMessage("§e/dw stats copy             §7통계 복사용 텍스트");
        if (s.hasPermission("dualworld.admin")) {
            s.sendMessage("§8--- §c관리자 §8---");
            s.sendMessage("§e/dw reset               §c스피드런 월드 강제 리셋");
            s.sendMessage("§e/dw setdifficulty <healing|speedrun> <난이도>");
            s.sendMessage("§e/dw reload              §7설정 재로드");
        }
        s.sendMessage("§8§m════════════════════════════════════");
    }

    // ─── /dw where ────────────────────────────────────────────────
    private void sendWhere(Player p) {
        String prefix = plugin.getPrefix();
        WorldManager wm = plugin.getWorldManager();
        boolean inSpeedrun = "speedrun".equals(plugin.getPlayerDataManager().getCurrentWorld(p));

        if (inSpeedrun) {
            p.sendMessage(prefix + "§c현재 위치: " + wm.getSpeedrunDisplayName());
            p.sendMessage(prefix + "§7시드: §e" + wm.getCurrentSpeedrunSeed());
            SpeedrunTimerManager tm = plugin.getTimerManager();
            p.sendMessage(prefix + "§7타이머: " + (tm.isRunning() ? StatsManager.formatTime(tm.getElapsedMs()) : "§7정지 중"));
            p.sendMessage(prefix + "§7HUD: " + (tm.isHudEnabled(p) ? "§a켜짐" : "§c꺼짐") + " §7(§e/dw timer hud§7)");
        } else {
            p.sendMessage(prefix + "§a현재 위치: " + wm.getHealingDisplayName());
            p.sendMessage(prefix + "§7난이도: §e" + wm.getHealingWorld().getDifficulty().name());
        }
    }

    // ─── /dw status ───────────────────────────────────────────────
    private void sendStatus(CommandSender s) {
        String prefix = plugin.getPrefix();
        WorldManager wm = plugin.getWorldManager();
        SpeedrunTimerManager tm = plugin.getTimerManager();
        StatsManager sm = plugin.getStatsManager();

        s.sendMessage("§8§m════════════════════════════════════");
        s.sendMessage(prefix + "§6DualWorld 서버 현황");
        s.sendMessage("§8§m════════════════════════════════════");

        // Healing world
        World hw = wm.getHealingWorld();
        List<String> healPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(wm::isInHealingWorld).map(Player::getName).collect(Collectors.toList());
        s.sendMessage("§a▶ " + wm.getHealingDisplayName());
        s.sendMessage("  §7난이도: §e" + (hw != null ? hw.getDifficulty().name() : "N/A"));
        s.sendMessage("  §7인원: §e" + healPlayers.size() + "명" + (healPlayers.isEmpty() ? "" : " §f(" + String.join("§7, §f", healPlayers) + "§f)"));

        s.sendMessage("");

        // Speedrun world
        World sw = wm.getSpeedrunWorld();
        List<String> srPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(wm::isInSpeedrunWorld).map(Player::getName).collect(Collectors.toList());
        s.sendMessage("§c▶ " + wm.getSpeedrunDisplayName());
        s.sendMessage("  §7난이도: §cHARD §8(고정)");
        s.sendMessage("  §7시드: §e" + wm.getCurrentSpeedrunSeed());
        s.sendMessage("  §7상태: " + (wm.isResetInProgress() ? "§c리셋 중" : sw != null ? "§a활성" : "§7생성 중"));
        s.sendMessage("  §7인원: §e" + srPlayers.size() + "명" + (srPlayers.isEmpty() ? "" : " §f(" + String.join("§7, §f", srPlayers) + "§f)"));
        s.sendMessage("  §7타이머: " + (tm.isRunning() ? StatsManager.formatTime(tm.getElapsedMs()) : "§7정지 중"));
        s.sendMessage("  §7서버 최고기록: " + StatsManager.formatTime(sm.getGlobalBestTime()));

        s.sendMessage("");
        s.sendMessage("§7총 접속자: §e" + Bukkit.getOnlinePlayers().size() + "명");
        s.sendMessage("§7총 월드 리셋: §e" + sm.getGlobalResets() + "회");
        s.sendMessage("§7총 클리어: §e" + sm.getGlobalFinishes() + "회");
        s.sendMessage("§8§m════════════════════════════════════");
    }

    // ─── /dw timer ────────────────────────────────────────────────
    private void handleTimer(Player p, String[] args) {
        String prefix = plugin.getPrefix();
        SpeedrunTimerManager tm = plugin.getTimerManager();

        if (args.length < 2) {
            p.sendMessage(prefix + "§e/dw timer hud §7- HUD 켜기/끄기");
            p.sendMessage(prefix + "§e/dw timer time §7- 현재 타이머 확인");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "hud":
                boolean on = tm.toggleHud(p);
                p.sendMessage(prefix + "§7타이머 HUD: " + (on ? "§a켜짐 §7(우측 상단 ActionBar)" : "§c꺼짐"));
                break;
            case "time":
                if (!tm.isRunning()) p.sendMessage(prefix + "§7타이머가 실행 중이 아닙니다.");
                else p.sendMessage(prefix + "§7현재 기록: " + StatsManager.formatTime(tm.getElapsedMs()));
                break;
            default:
                p.sendMessage(prefix + "§c알 수 없는 옵션: §e/dw timer hud §7또는 §e/dw timer time");
        }
    }

    // ─── /dw stats ────────────────────────────────────────────────
    private void handleStats(CommandSender sender, String[] args) {
        String prefix = plugin.getPrefix();
        StatsManager sm = plugin.getStatsManager();

        if (args.length < 2) {
            if (!(sender instanceof Player)) { sender.sendMessage("서브커맨드 필요: top, all, copy"); return; }
            sendMyStats((Player) sender);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "top":     sendTopStats(sender);  break;
            case "all":     sendAllStats(sender);  break;
            case "copy":
                requirePlayer(sender, () -> sendCopyStats((Player) sender));
                break;
            default:
                // /dw stats <playername> — admin can look up others
                if (sender.hasPermission("dualworld.admin") && args[1].length() > 0) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) sendOtherStats(sender, target);
                    else sender.sendMessage(prefix + "§c플레이어를 찾을 수 없습니다.");
                } else {
                    sender.sendMessage(prefix + "§c사용법: §e/dw stats [top|all|copy]");
                }
        }
    }

    private void sendMyStats(Player p) {
        String prefix = plugin.getPrefix();
        StatsManager sm = plugin.getStatsManager();
        UUID id = p.getUniqueId();

        p.sendMessage("§8§m════════════════════════════════════");
        p.sendMessage(prefix + "§6" + p.getName() + " §7님의 스피드런 통계");
        p.sendMessage("§8§m════════════════════════════════════");
        p.sendMessage("§7참가 횟수:    §e" + sm.getJoins(id) + "회");
        p.sendMessage("§7사망 횟수:    §e" + sm.getDeaths(id) + "회");
        p.sendMessage("§7클리어 횟수:  §e" + sm.getFinishes(id) + "회");
        p.sendMessage("§7개인 최고기록: " + StatsManager.formatTime(sm.getBestTime(id)));
        p.sendMessage("§7마지막 기록:   " + StatsManager.formatTime(sm.getLastTime(id)));
        p.sendMessage("§8§m════════════════════════════════════");
        p.sendMessage("§7서버 최고기록: " + StatsManager.formatTime(sm.getGlobalBestTime()));
        p.sendMessage("§7서버 총 클리어: §e" + sm.getGlobalFinishes() + "회");
        p.sendMessage("§8§m════════════════════════════════════");
    }

    private void sendOtherStats(CommandSender s, Player target) {
        StatsManager sm = plugin.getStatsManager();
        UUID id = target.getUniqueId();
        String prefix = plugin.getPrefix();

        s.sendMessage("§8§m════════════════════════════════════");
        s.sendMessage(prefix + "§6" + target.getName() + " §7통계");
        s.sendMessage("§7참가: §e" + sm.getJoins(id) + "회  §7사망: §e" + sm.getDeaths(id) + "회  §7클리어: §e" + sm.getFinishes(id) + "회");
        s.sendMessage("§7최고기록: " + StatsManager.formatTime(sm.getBestTime(id)));
        s.sendMessage("§8§m════════════════════════════════════");
    }

    private void sendTopStats(CommandSender s) {
        StatsManager sm = plugin.getStatsManager();
        List<StatsManager.PlayerStat> list = sm.getAllStats();
        String prefix = plugin.getPrefix();

        s.sendMessage("§8§m════════════════════════════════════");
        s.sendMessage(prefix + "§6🏆 스피드런 랭킹 (최고기록 기준)");
        s.sendMessage("§8§m════════════════════════════════════");
        s.sendMessage(String.format("§8%-3s %-16s %-10s %-10s %-10s %-14s",
                "#", "닉네임", "참가", "사망", "클리어", "최고기록"));
        s.sendMessage("§8§m────────────────────────────────────");

        List<StatsManager.PlayerStat> finished = list.stream()
                .filter(st -> st.bestTime > 0).collect(Collectors.toList());
        List<StatsManager.PlayerStat> noRecord = list.stream()
                .filter(st -> st.bestTime <= 0).collect(Collectors.toList());

        int rank = 1;
        for (StatsManager.PlayerStat st : finished) {
            String rankColor = rank == 1 ? "§6" : rank == 2 ? "§7" : rank == 3 ? "§c" : "§f";
            s.sendMessage(String.format("%s%-3d §f%-16s §e%-10d §e%-10d §e%-10d %s",
                    rankColor, rank++, st.name, st.joins, st.deaths, st.finishes,
                    StatsManager.formatTime(st.bestTime)));
        }
        for (StatsManager.PlayerStat st : noRecord) {
            s.sendMessage(String.format("§8%-3s §7%-16s §8%-10d §8%-10d §8%-10d §8%-14s",
                    "-", st.name, st.joins, st.deaths, st.finishes, "기록 없음"));
        }
        if (list.isEmpty()) s.sendMessage("§7아직 기록이 없습니다.");
        s.sendMessage("§8§m════════════════════════════════════");
    }

    private void sendAllStats(CommandSender s) {
        StatsManager sm = plugin.getStatsManager();
        List<StatsManager.PlayerStat> list = sm.getAllStats();
        String prefix = plugin.getPrefix();

        s.sendMessage("§8§m════════════════════════════════════");
        s.sendMessage(prefix + "§6전체 플레이어 통계");
        s.sendMessage("§8§m════════════════════════════════════");
        s.sendMessage("§7서버 총 리셋: §e" + sm.getGlobalResets() + "회");
        s.sendMessage("§7서버 총 클리어: §e" + sm.getGlobalFinishes() + "회");
        s.sendMessage("§7서버 최고기록: " + StatsManager.formatTime(sm.getGlobalBestTime()));
        s.sendMessage("§8§m────────────────────────────────────");

        for (StatsManager.PlayerStat st : list) {
            s.sendMessage("§e" + st.name + " §8│ §7참가 §e" + st.joins
                    + " §7사망 §e" + st.deaths
                    + " §7클리어 §e" + st.finishes
                    + " §7기록 " + StatsManager.formatTime(st.bestTime));
        }
        if (list.isEmpty()) s.sendMessage("§7아직 기록이 없습니다.");
        s.sendMessage("§8§m════════════════════════════════════");
    }

    private void sendCopyStats(Player p) {
        StatsManager sm = plugin.getStatsManager();
        List<StatsManager.PlayerStat> list = sm.getAllStats();
        StringBuilder sb = new StringBuilder();

        sb.append("=== DualWorld 스피드런 통계 ===\n");
        sb.append("서버 총 리셋: ").append(sm.getGlobalResets()).append("회\n");
        sb.append("서버 총 클리어: ").append(sm.getGlobalFinishes()).append("회\n");
        sb.append("서버 최고기록: ").append(formatTimePlain(sm.getGlobalBestTime())).append("\n");
        sb.append("─────────────────────────────\n");
        sb.append(String.format("%-16s %5s %5s %6s %14s\n", "닉네임", "참가", "사망", "클리어", "최고기록"));
        for (StatsManager.PlayerStat st : list) {
            sb.append(String.format("%-16s %5d %5d %6d %14s\n",
                    st.name, st.joins, st.deaths, st.finishes, formatTimePlain(st.bestTime)));
        }
        sb.append("=== Generated by DualWorld ===");

        // Send as chat message for easy copy
        p.sendMessage("§8§m════════════════════════════════════");
        p.sendMessage(plugin.getPrefix() + "§7아래 텍스트를 복사하세요:");
        p.sendMessage("§8§m════════════════════════════════════");
        for (String line : sb.toString().split("\n")) p.sendMessage("§f" + line);
        p.sendMessage("§8§m════════════════════════════════════");
    }

    private String formatTimePlain(long ms) {
        if (ms < 0) return "기록 없음";
        long totalSec = ms / 1000;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        long ml = ms % 1000;
        if (h > 0) return String.format("%d:%02d:%02d.%03d", h, m, s, ml);
        return String.format("%d:%02d.%03d", m, s, ml);
    }

    // ─── /dw reset ────────────────────────────────────────────────
    private void handleReset(CommandSender s) {
        requireAdmin(s, () -> {
            if (plugin.getWorldManager().isResetInProgress()) {
                s.sendMessage(plugin.getPrefix() + "§c이미 리셋이 진행 중입니다.");
                return;
            }
            Bukkit.broadcastMessage(plugin.getPrefix() + "§c관리자에 의해 스피드런 월드 리셋!");
            plugin.getWorldManager().triggerResetWithCountdown("관리자");
        });
    }

    // ─── /dw setdifficulty ────────────────────────────────────────
    private void handleSetDiff(CommandSender s, String[] args) {
        requireAdmin(s, () -> {
            String prefix = plugin.getPrefix();
            if (args.length < 3) {
                s.sendMessage(prefix + "§c사용법: /dw setdifficulty <healing|speedrun> <PEACEFUL|EASY|NORMAL|HARD>");
                return;
            }
            Difficulty diff;
            try { diff = Difficulty.valueOf(args[2].toUpperCase()); }
            catch (IllegalArgumentException e) {
                s.sendMessage(prefix + "§c난이도: PEACEFUL, EASY, NORMAL, HARD");
                return;
            }
            if ("healing".equals(args[1].toLowerCase())) {
                World hw = plugin.getWorldManager().getHealingWorld();
                if (hw != null) {
                    hw.setDifficulty(diff);
                    plugin.getConfig().set("healing-world.difficulty", diff.name());
                    plugin.saveConfig();
                    s.sendMessage(prefix + "§a힐링 월드 난이도 → §e" + diff.name());
                }
            } else {
                s.sendMessage(prefix + "§c스피드런 월드는 항상 HARD입니다.");
            }
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────
    private void requirePlayer(CommandSender s, Runnable action) {
        if (!(s instanceof Player)) { s.sendMessage("플레이어 전용 명령어입니다."); return; }
        if (!s.hasPermission("dualworld.use")) { s.sendMessage(plugin.getMessage("no-permission")); return; }
        action.run();
    }

    private void requireAdmin(CommandSender s, Runnable action) {
        if (!s.hasPermission("dualworld.admin")) { s.sendMessage(plugin.getMessage("no-permission")); return; }
        action.run();
    }
}
