package com.dualworld.commands;

import com.dualworld.DualWorldPlugin;
import com.dualworld.managers.WorldManager;
import com.dualworld.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class DualWorldCommand implements CommandExecutor {

    private final DualWorldPlugin plugin;

    public DualWorldCommand(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String p = plugin.getPrefix();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "help":
                sendHelp(sender);
                break;

            case "healing":
            case "go":
                if (!(sender instanceof Player)) { sender.sendMessage("플레이어 전용 명령어."); return true; }
                if (!sender.hasPermission("dualworld.use")) { sender.sendMessage(plugin.getMessage("no-permission")); return true; }
                TeleportUtil.teleportToHealing((Player) sender, plugin);
                break;

            case "speedrun":
                if (!(sender instanceof Player)) { sender.sendMessage("플레이어 전용 명령어."); return true; }
                if (!sender.hasPermission("dualworld.use")) { sender.sendMessage(plugin.getMessage("no-permission")); return true; }
                TeleportUtil.teleportToSpeedrun((Player) sender, plugin);
                break;

            case "where":
            case "location":
                if (!(sender instanceof Player)) { sender.sendMessage("플레이어 전용 명령어."); return true; }
                sendWhereInfo((Player) sender);
                break;

            case "status":
                sendStatus(sender);
                break;

            case "reset":
                if (!sender.hasPermission("dualworld.admin")) { sender.sendMessage(plugin.getMessage("no-permission")); return true; }
                sender.sendMessage(p + "§c스피드런 월드를 강제 리셋합니다...");
                Bukkit.broadcastMessage(p + "§c관리자에 의해 스피드런 월드가 리셋됩니다!");
                plugin.getWorldManager().resetSpeedrunWorld();
                break;

            case "setdifficulty":
                if (!sender.hasPermission("dualworld.admin")) { sender.sendMessage(plugin.getMessage("no-permission")); return true; }
                handleSetDifficulty(sender, args);
                break;

            case "reload":
                if (!sender.hasPermission("dualworld.admin")) { sender.sendMessage(plugin.getMessage("no-permission")); return true; }
                plugin.reloadConfig();
                sender.sendMessage(p + "§a설정 파일을 다시 불러왔습니다.");
                break;

            default:
                sender.sendMessage(p + "§c알 수 없는 명령어입니다. §e/dualworld help §c를 참조하세요.");
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        String p = plugin.getPrefix();
        sender.sendMessage("§8§m══════════════════════════════");
        sender.sendMessage(p + "§6DualWorld 플러그인 도움말");
        sender.sendMessage("§8§m══════════════════════════════");
        sender.sendMessage("§e/healing §7또는 §e/dw healing §7- §a힐링 월드로 이동");
        sender.sendMessage("§e/speedrun §7또는 §e/dw speedrun §7- §c스피드런 월드로 이동");
        sender.sendMessage("§e/dw where §7- §f현재 어떤 월드에 있는지 확인");
        sender.sendMessage("§e/dw status §7- §f두 월드의 현재 상태 확인");
        if (sender.hasPermission("dualworld.admin")) {
            sender.sendMessage("§8--- §c관리자 명령어 §8---");
            sender.sendMessage("§e/dw reset §7- §c스피드런 월드 강제 리셋");
            sender.sendMessage("§e/dw setdifficulty <healing|speedrun> <difficulty> §7- 난이도 변경");
            sender.sendMessage("§e/dw reload §7- 설정 파일 재로드");
        }
        sender.sendMessage("§8§m══════════════════════════════");
        sender.sendMessage("§7힐링 월드: 자유롭게 즐기는 야생 서바이벌");
        sender.sendMessage("§7스피드런 월드: §c한 명이라도 죽으면 월드 리셋!");
        sender.sendMessage("§8§m══════════════════════════════");
    }

    private void sendWhereInfo(Player player) {
        String p = plugin.getPrefix();
        WorldManager wm = plugin.getWorldManager();
        String currentWorld = plugin.getPlayerDataManager().getCurrentWorld(player);

        if ("speedrun".equals(currentWorld)) {
            player.sendMessage(p + "§c현재 위치: " + wm.getSpeedrunDisplayName());
            player.sendMessage(p + "§7시드: §e" + wm.getCurrentSpeedrunSeed());
            player.sendMessage(p + "§c주의: 사망 시 월드가 리셋됩니다!");
            long speedrunPlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(wm::isInSpeedrunWorld).count();
            player.sendMessage(p + "§7스피드런 참가자: §e" + speedrunPlayers + "명");
        } else {
            player.sendMessage(p + "§a현재 위치: " + wm.getHealingDisplayName());
            World hw = wm.getHealingWorld();
            player.sendMessage(p + "§7난이도: §e" + hw.getDifficulty().name());
            long healingPlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(wm::isInHealingWorld).count();
            player.sendMessage(p + "§7힐링 월드 인원: §e" + healingPlayers + "명");
        }
    }

    private void sendStatus(CommandSender sender) {
        String p = plugin.getPrefix();
        WorldManager wm = plugin.getWorldManager();

        sender.sendMessage("§8§m══════════════════════════════");
        sender.sendMessage(p + "§6DualWorld 서버 현황");
        sender.sendMessage("§8§m══════════════════════════════");

        // Healing world status
        World hw = wm.getHealingWorld();
        List<String> healingPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(wm::isInHealingWorld)
                .map(Player::getName)
                .collect(Collectors.toList());
        sender.sendMessage("§a▶ " + wm.getHealingDisplayName());
        sender.sendMessage("  §7난이도: §e" + (hw != null ? hw.getDifficulty().name() : "N/A"));
        sender.sendMessage("  §7접속 인원: §e" + healingPlayers.size() + "명");
        if (!healingPlayers.isEmpty()) {
            sender.sendMessage("  §7플레이어: §f" + String.join("§7, §f", healingPlayers));
        }

        sender.sendMessage("");

        // Speedrun world status
        World sw = wm.getSpeedrunWorld();
        List<String> speedrunPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(wm::isInSpeedrunWorld)
                .map(Player::getName)
                .collect(Collectors.toList());
        sender.sendMessage("§c▶ " + wm.getSpeedrunDisplayName());
        sender.sendMessage("  §7난이도: §eHARD §c(고정)");
        sender.sendMessage("  §7현재 시드: §e" + wm.getCurrentSpeedrunSeed());
        sender.sendMessage("  §7접속 인원: §e" + speedrunPlayers.size() + "명");
        if (!speedrunPlayers.isEmpty()) {
            sender.sendMessage("  §7플레이어: §f" + String.join("§7, §f", speedrunPlayers));
        }
        sender.sendMessage("  §7월드 상태: " + (sw != null ? "§a활성" : "§c비활성/리셋 중"));

        sender.sendMessage("§8§m══════════════════════════════");
        sender.sendMessage("§7총 접속자: §e" + Bukkit.getOnlinePlayers().size() + "명");
        sender.sendMessage("§8§m══════════════════════════════");
    }

    private void handleSetDifficulty(CommandSender sender, String[] args) {
        String p = plugin.getPrefix();
        if (args.length < 3) {
            sender.sendMessage(p + "§c사용법: /dw setdifficulty <healing|speedrun> <PEACEFUL|EASY|NORMAL|HARD>");
            return;
        }
        String worldType = args[1].toLowerCase();
        Difficulty diff;
        try {
            diff = Difficulty.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(p + "§c올바른 난이도: PEACEFUL, EASY, NORMAL, HARD");
            return;
        }

        if ("healing".equals(worldType)) {
            if (plugin.getWorldManager().getHealingWorld() != null) {
                plugin.getWorldManager().getHealingWorld().setDifficulty(diff);
                plugin.getConfig().set("healing-world.difficulty", diff.name());
                plugin.saveConfig();
                sender.sendMessage(p + "§a힐링 월드 난이도가 " + diff.name() + "으로 변경되었습니다.");
            }
        } else if ("speedrun".equals(worldType)) {
            sender.sendMessage(p + "§c스피드런 월드 난이도는 HARD로 고정입니다.");
        } else {
            sender.sendMessage(p + "§c월드 타입: healing 또는 speedrun");
        }
    }
}
