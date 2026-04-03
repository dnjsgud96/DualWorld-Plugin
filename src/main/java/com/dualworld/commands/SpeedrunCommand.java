package com.dualworld.commands;

import com.dualworld.DualWorldPlugin;
import com.dualworld.utils.TeleportUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpeedrunCommand implements CommandExecutor {

    private final DualWorldPlugin plugin;

    public SpeedrunCommand(DualWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!sender.hasPermission("dualworld.use")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        TeleportUtil.teleportToSpeedrun((Player) sender, plugin);
        return true;
    }
}
