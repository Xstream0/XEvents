package net.xstream.xevents.command;

import net.xstream.xevents.XEvents;
import net.xstream.xevents.api.XEventModule;
import net.xstream.xevents.events.luckyblockrace.LuckyBlockRaceModule;
import net.xstream.xevents.gui.EventsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public final class XEventsCommand implements CommandExecutor, TabCompleter {

    private final XEvents plugin;

    public XEventsCommand(XEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "list" -> handleList(sender);
            case "gui" -> handleGui(sender);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender, args);
            case "forcestop" -> handleForceStop(sender, args);
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender);
            case "reload" -> handleReload(sender);
            case "setstart" -> handleSetStart(sender);
            case "setfinish" -> handleSetFinish(sender);
            case "setlobby" -> handleSetLobby(sender);
            case "status" -> handleStatus(sender);
            default -> plugin.getMessageService().send(sender, "command.unknown-subcommand");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessageService().send(sender, "command.help.header");
        plugin.getMessageService().send(sender, "command.help.list");
        plugin.getMessageService().send(sender, "command.help.gui");
        plugin.getMessageService().send(sender, "command.help.start");
        plugin.getMessageService().send(sender, "command.help.stop");
        plugin.getMessageService().send(sender, "command.help.join");
        plugin.getMessageService().send(sender, "command.help.leave");
        plugin.getMessageService().send(sender, "command.help.reload");
        if (sender.hasPermission("xevents.admin")) {
            plugin.getMessageService().send(sender, "command.help.setstart");
            plugin.getMessageService().send(sender, "command.help.setfinish");
            plugin.getMessageService().send(sender, "command.help.setlobby");
        }
    }

    private void handleList(CommandSender sender) {
        var modules = plugin.getEventManager().getModules();
        if (modules.isEmpty()) {
            plugin.getMessageService().send(sender, "command.list.empty");
            return;
        }
        plugin.getMessageService().send(sender, "command.list.header");
        for (XEventModule module : modules) {
            sender.sendMessage(plugin.getMessageService().getRaw("command.list.entry",
                    "id", module.getId(),
                    "name", module.getDisplayName(),
                    "state", module.getState().name()));
        }
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "command.players-only");
            return;
        }
        if (!player.hasPermission("xevents.admin")) {
            plugin.getMessageService().send(sender, "command.no-permission");
            return;
        }
        new EventsGui(plugin).open(player);
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xevents.admin")) {
            plugin.getMessageService().send(sender, "command.no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "command.players-only");
            return;
        }
        if (args.length < 2) {
            plugin.getMessageService().send(sender, "command.start.usage");
            return;
        }
        String moduleId = args[1];
        var moduleOpt = plugin.getEventManager().getModule(moduleId);
        if (moduleOpt.isEmpty()) {
            plugin.getMessageService().send(sender, "command.unknown-module", "id", moduleId);
            return;
        }
        XEventModule module = moduleOpt.get();

        if (!plugin.getEventManager().trySetActive(module.getId())) {
            plugin.getMessageService().send(sender, "command.another-event-active");
            return;
        }
        if (!module.isReadyToStart()) {
            plugin.getMessageService().send(sender, "command.start.not-ready");
            for (String issue : module.getSetupIssues()) {
                sender.sendMessage(plugin.getMessageService().getRaw("command.start.issue-line", "issue", issue));
            }
            plugin.getEventManager().clearActiveIfMatches(module.getId());
            return;
        }
        boolean started = module.start(player);
        if (!started) {
            plugin.getMessageService().send(sender, "command.start.failed");
            plugin.getEventManager().clearActiveIfMatches(module.getId());
        } else {
            plugin.getMessageService().send(sender, "command.start.success", "name", module.getDisplayName());
        }
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xevents.admin")) {
            plugin.getMessageService().send(sender, "command.no-permission");
            return;
        }
        var activeOpt = plugin.getEventManager().getActiveModule();
        if (activeOpt.isEmpty()) {
            plugin.getMessageService().send(sender, "command.stop.none-active");
            return;
        }
        activeOpt.get().stop();
        plugin.getMessageService().send(sender, "command.stop.success");
    }

    private void handleForceStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xevents.admin")) {
            plugin.getMessageService().send(sender, "command.no-permission");
            return;
        }
        var activeOpt = plugin.getEventManager().getActiveModule();
        if (activeOpt.isEmpty()) {
            plugin.getMessageService().send(sender, "command.stop.none-active");
            return;
        }
        activeOpt.get().forceStop();
        plugin.getMessageService().send(sender, "command.forcestop.success");
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "command.players-only");
            return;
        }
        if (!player.hasPermission("xevents.join")) {
            plugin.getMessageService().send(sender, "command.no-permission");
            return;
        }
        var activeOpt = plugin.getEventManager().getActiveModule();
        if (activeOpt.isEmpty()) {
            plugin.getMessageService().send(sender, "command.join.none-active");
            return;
        }
        activeOpt.get().join(player);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "command.players-only");
            return;
        }
        var moduleOpt = plugin.getEventManager().findModuleWithParticipant(player);
        if (moduleOpt.isEmpty()) {
            plugin.getMessageService().send(sender, "command.leave.not-in-event");
            return;
        }
        moduleOpt.get().leave(player);
        plugin.getMessageService().send(sender, "command.leave.success");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("xevents.admin")) {
            plugin.getMessageService().send(sender, "command.no-permission");
            return;
        }
        plugin.reloadXEvents();
        var lbr = plugin.getEventManager().getModule("luckyblockrace");
        lbr.ifPresent(module -> {
            if (module instanceof LuckyBlockRaceModule raceModule) {
                raceModule.reloadSettingsAndOutcomes();
            }
        });
        plugin.getMessageService().send(sender, "command.reload.success");
    }

    private void handleSetStart(CommandSender sender) {
        if (!requireAdminPlayer(sender)) return;
        Player player = (Player) sender;
        getLuckyBlockRaceModule().ifPresentOrElse(module -> {
            module.setStart(player.getLocation());
            plugin.getMessageService().send(sender, "luckyblockrace.setup.start-set");
        }, () -> plugin.getMessageService().send(sender, "command.unknown-module", "id", "luckyblockrace"));
    }

    private void handleSetFinish(CommandSender sender) {
        if (!requireAdminPlayer(sender)) return;
        Player player = (Player) sender;
        getLuckyBlockRaceModule().ifPresentOrElse(module -> {
            module.setFinish(player.getLocation());
            plugin.getMessageService().send(sender, "luckyblockrace.setup.finish-set");
        }, () -> plugin.getMessageService().send(sender, "command.unknown-module", "id", "luckyblockrace"));
    }

    private void handleSetLobby(CommandSender sender) {
        if (!requireAdminPlayer(sender)) return;
        Player player = (Player) sender;
        getLuckyBlockRaceModule().ifPresentOrElse(module -> {
            module.setWaitingLobby(player.getLocation());
            plugin.getMessageService().send(sender, "luckyblockrace.setup.lobby-set");
        }, () -> plugin.getMessageService().send(sender, "command.unknown-module", "id", "luckyblockrace"));
    }

    private void handleStatus(CommandSender sender) {
        var activeOpt = plugin.getEventManager().getActiveModule();
        if (activeOpt.isEmpty()) {
            plugin.getMessageService().send(sender, "command.status.none-active");
            return;
        }
        XEventModule module = activeOpt.get();
        sender.sendMessage(plugin.getMessageService().getRaw("command.status.line",
                "name", module.getDisplayName(),
                "state", module.getState().name()));

        if (module instanceof LuckyBlockRaceModule raceModule) {
            sender.sendMessage(plugin.getMessageService().getRaw("command.status.lanes-line",
                    "lanes", String.valueOf(raceModule.getLanes().size()),
                    "waiting", String.valueOf(raceModule.getWaitingCount())));
        }
    }

    private boolean requireAdminPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageService().send(sender, "command.players-only");
            return false;
        }
        if (!sender.hasPermission("xevents.admin")) {
            plugin.getMessageService().send(sender, "command.no-permission");
            return false;
        }
        return true;
    }

    private java.util.Optional<LuckyBlockRaceModule> getLuckyBlockRaceModule() {
        return plugin.getEventManager().getModule("luckyblockrace")
                .filter(m -> m instanceof LuckyBlockRaceModule)
                .map(m -> (LuckyBlockRaceModule) m);
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of(
                    "help", "list", "gui", "start", "stop", "forcestop", "join", "leave",
                    "reload", "setstart", "setfinish", "setlobby", "status"
            ));
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filter(plugin.getEventManager().getModules().stream()
                    .map(XEventModule::getId)
                    .collect(Collectors.toList()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
    }
}