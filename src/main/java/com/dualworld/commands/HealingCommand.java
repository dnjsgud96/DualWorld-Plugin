package com.dualworld.commands;

import com.dualworld.DualWorldPlugin;
import com.dualworld.utils.TeleportUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class HealingCommand implements CommandExecutor {
    private final DualWorldPlugin plugin;
    public HealingCommand(DualWorldPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)) { s.sendMessage("플레이어 전용"); return true; }
        if (!s.hasPermission("dualworld.use")) { s.sendMessage(plugin.getMessage("no-permission")); return true; }
        TeleportUtil.teleportToHealing((Player) s, plugin);
        return true;
    }
}
