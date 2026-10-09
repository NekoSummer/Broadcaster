package cn.xinyue_neko.plugins.Broadcaster;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Broadcaster extends JavaPlugin {

    private BukkitTask broadcastTask;
    private int currentIndex = 0;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();

        // 启动公告任务
        startBroadcastTask();

        getLogger().info("Broadcaster 已启用！");
    }

    @Override
    public void onDisable() {
        // 取消定时任务，防止内存泄漏
        if (broadcastTask != null && !broadcastTask.isCancelled()) {
            broadcastTask.cancel();
        }
        getLogger().info("Broadcaster 已禁用！");
    }

    /**
     * 启动定时公告任务
     */
    private void startBroadcastTask() {
        // 如果已有任务在运行，先取消
        if (broadcastTask != null && !broadcastTask.isCancelled()) {
            broadcastTask.cancel();
        }

        FileConfiguration config = getConfig();
        int intervalSeconds = config.getInt("interval", 60);
        // 转换为 ticks（20 ticks = 1 秒）
        long intervalTicks = intervalSeconds * 20L;

        broadcastTask = Bukkit.getScheduler().runTaskTimer(
                this,
                this::broadcastNext,
                intervalTicks,  // 首次延迟
                intervalTicks   // 后续间隔
        );

        getLogger().info("公告任务已启动，间隔: " + intervalSeconds + " 秒");
    }

    /**
     * 发送下一条公告
     * 核心逻辑：遍历所有在线玩家，不检查任何权限
     */
    private void broadcastNext() {
        List<String> announcements = getConfig().getStringList("announcements");
        if (announcements.isEmpty()) {
            return;
        }

        // 获取当前公告并轮转索引
        String message = announcements.get(currentIndex);
        currentIndex = (currentIndex + 1) % announcements.size();

        message = message.replace("\\n", "\n");

        // 转换颜色代码
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        // 【核心】遍历所有在线玩家，直接发送消息
        // 不调用 hasPermission()，不检查 isOp()
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(coloredMessage);
        }

        // 可选：在控制台输出
        if (getConfig().getBoolean("console-logging", true)) {
            getLogger().info(ChatColor.stripColor(coloredMessage));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("broadcaster")) {
            return false;
        }

        // 只有拥有 simpleannouncer.admin 权限的玩家才能执行管理命令
        if (!sender.hasPermission("broadcaster.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/broadcaster reload - 重载配置");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            startBroadcastTask(); // 重启任务以应用新的间隔
            sender.sendMessage(ChatColor.GREEN + "配置已重载！");
            return true;
        }

        return true;
    }

}