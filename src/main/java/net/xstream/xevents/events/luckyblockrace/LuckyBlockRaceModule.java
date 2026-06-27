package net.xstream.xevents.events.luckyblockrace;

import net.kyori.adventure.text.Component;
import net.xstream.xevents.api.XEventContext;
import net.xstream.xevents.api.XEventModule;
import net.xstream.xevents.api.XEventState;
import net.xstream.xevents.config.MessageService;
import net.xstream.xevents.events.luckyblockrace.model.LaneAllocator;
import net.xstream.xevents.events.luckyblockrace.model.RaceArena;
import net.xstream.xevents.events.luckyblockrace.model.RaceLane;
import net.xstream.xevents.events.luckyblockrace.model.RaceParticipant;
import net.xstream.xevents.events.luckyblockrace.outcome.LuckyBlockOutcome;
import net.xstream.xevents.events.luckyblockrace.outcome.OutcomeExecutor;
import net.xstream.xevents.events.luckyblockrace.outcome.OutcomePool;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class LuckyBlockRaceModule implements XEventModule {

    private static final String ID = "luckyblockrace";

    private XEventContext context;
    private MessageService messages;
    private Logger logger;

    private RaceSettings settings;
    private final RaceArena arena = new RaceArena();
    private final OutcomePool outcomePool = new OutcomePool();
    private OutcomeExecutor outcomeExecutor;

    private RaceListener listener;
    private File arenaFile;

    private RacePhase phase = RacePhase.IDLE;
    private final Map<UUID, RaceParticipant> participants = new LinkedHashMap<>();
    private final List<UUID> waitingQueue = new ArrayList<>();
    private final List<UUID> finishOrder = new ArrayList<>();

    private List<RaceLane> lanes = new ArrayList<>();

    private long raceStartMillis = -1;
    private BukkitTask activeTask;
    private int finishedCount = 0;

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "<gradient:#FFD700:#FFA500>Lucky Block Race</gradient>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Corsa a tappe: rompi Lucky Block lungo il percorso e arriva primo al traguardo.";
    }

    @Override
    public @NotNull XEventState getState() {
        return switch (phase) {
            case IDLE -> XEventState.IDLE;
            case COUNTDOWN -> XEventState.WAITING;
            case RACING, FINALE_WAITING, FINALE_FIGHT -> XEventState.RUNNING;
            case ENDING -> XEventState.ENDING;
        };
    }

    @Override
    public void onRegister(@NotNull XEventContext context) {
        this.context = context;
        this.messages = context.messages();
        this.logger = context.logger();
        this.outcomeExecutor = new OutcomeExecutor(messages, logger);

        reloadSettingsAndOutcomes();
        loadArenaFromDisk();

        this.listener = new RaceListener(this);
        Bukkit.getPluginManager().registerEvents(listener, context.asPlugin());
    }

    @Override
    public void onUnregister() {
        forceStop();
    }

    public void reloadSettingsAndOutcomes() {
        this.settings = new RaceSettings(context.getModuleConfig());
        var outcomesSection = context.getModuleConfig() != null
                ? context.getModuleConfig().getConfigurationSection("outcomes")
                : null;
        outcomePool.load(outcomesSection, logger);
    }

    @Override
    public boolean isReadyToStart() {
        return getSetupIssues().isEmpty();
    }

    @Override
    public @NotNull List<String> getSetupIssues() {
        List<String> issues = new ArrayList<>();
        if (!arena.hasStart()) {
            issues.add(messages.raw("luckyblockrace.setup.missing-start"));
        }
        if (!arena.hasFinish()) {
            issues.add(messages.raw("luckyblockrace.setup.missing-finish"));
        }
        if (arena.hasStart() && arena.hasFinish() && !arena.isComplete()) {
            issues.add(messages.raw("luckyblockrace.setup.different-worlds"));
        }
        if (outcomePool.isEmpty()) {
            issues.add(messages.raw("luckyblockrace.setup.no-outcomes"));
        }
        if (phase != RacePhase.IDLE) {
            issues.add(messages.raw("luckyblockrace.setup.already-running"));
        }
        return issues;
    }

    public void setStart(@NotNull Location location) {
        arena.setStart(location);
        saveArenaToDisk();
    }

    public void setFinish(@NotNull Location location) {
        arena.setFinish(location);
        saveArenaToDisk();
    }

    public void setWaitingLobby(@NotNull Location location) {
        arena.setWaitingLobby(location);
        saveArenaToDisk();
    }

    public void clearArena() {
        arena.clear();
        saveArenaToDisk();
    }

    @NotNull
    public RaceArena getArena() {
        return arena;
    }

    @NotNull
    public RaceSettings getSettings() {
        return settings;
    }

    @NotNull
    public OutcomePool getOutcomePool() {
        return outcomePool;
    }

    @NotNull
    public List<RaceLane> getLanes() {
        return lanes;
    }

    @org.jetbrains.annotations.Nullable
    public RaceLane getLaneOf(@NotNull Player player) {
        RaceParticipant participant = participants.get(player.getUniqueId());
        if (participant == null || !participant.hasLane()) {
            return null;
        }
        int idx = participant.getLaneIndex();
        return (idx >= 0 && idx < lanes.size()) ? lanes.get(idx) : null;
    }

    @NotNull
    public RacePhase getPhase() {
        return phase;
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public int getWaitingCount() {
        return waitingQueue.size();
    }

    @Override
    public boolean join(@NotNull Player player) {
        if (phase != RacePhase.COUNTDOWN) {
            messages.send(player, "luckyblockrace.join.not-open");
            return false;
        }
        if (participants.containsKey(player.getUniqueId())) {
            messages.send(player, "luckyblockrace.join.already-joined");
            return false;
        }
        participants.put(player.getUniqueId(), new RaceParticipant(player.getUniqueId(), player.getName()));

        Location lobby = arena.getWaitingLobby() != null ? arena.getWaitingLobby() : arena.getStart();
        if (lobby != null) {
            player.teleport(lobby);
        }
        player.setGameMode(GameMode.ADVENTURE);

        if (settings.isBroadcastJoinLeave()) {
            broadcastToParticipantsAndStaff("luckyblockrace.join.broadcast", "player", player.getName(),
                    "count", String.valueOf(participants.size()));
        }
        return true;
    }

    @Override
    public void leave(@NotNull Player player) {
        RaceParticipant participant = participants.remove(player.getUniqueId());
        waitingQueue.remove(player.getUniqueId());
        if (participant == null) {
            return;
        }
        finishOrder.remove(player.getUniqueId());
        player.setGameMode(GameMode.SURVIVAL);

        if (participant.hasLane() && participant.getLaneIndex() < lanes.size()) {
            lanes.get(participant.getLaneIndex()).release();
        }

        if (settings.isBroadcastJoinLeave() && phase != RacePhase.IDLE) {
            broadcastToParticipantsAndStaff("luckyblockrace.leave.broadcast", "player", player.getName());
        }

        checkForEarlyRaceEnd();
    }

    @Override
    public boolean isParticipant(@NotNull Player player) {
        return participants.containsKey(player.getUniqueId());
    }

    @Override
    public boolean start(@NotNull Player initiator) {
        if (!isReadyToStart()) {
            return false;
        }
        participants.clear();
        waitingQueue.clear();
        finishOrder.clear();
        finishedCount = 0;
        lanes = new ArrayList<>();
        phase = RacePhase.COUNTDOWN;

        Bukkit.broadcast(messages.get("luckyblockrace.announce.opened",
                "initiator", initiator.getName(),
                "seconds", String.valueOf(settings.getCountdownSeconds())));

        runCountdown();
        return true;
    }

    private void runCountdown() {
        cancelActiveTask();
        final int[] remaining = {settings.getCountdownSeconds()};

        activeTask = Bukkit.getScheduler().runTaskTimer(context.asPlugin(), () -> {
            if (remaining[0] <= 0) {
                cancelActiveTask();
                beginRace();
                return;
            }
            if (remaining[0] <= 5 || remaining[0] % 5 == 0) {
                broadcastToParticipantsAndStaff("luckyblockrace.announce.countdown-tick",
                        "seconds", String.valueOf(remaining[0]));
            }
            remaining[0]--;
        }, 0L, 20L);
    }

    private void beginRace() {
        if (participants.size() < settings.getMinPlayers()) {
            Bukkit.broadcast(messages.get("luckyblockrace.announce.not-enough-players",
                    "min", String.valueOf(settings.getMinPlayers())));
            endRaceImmediately();
            return;
        }

        int laneCount = Math.min(participants.size(), settings.getMaxLanes());
        lanes = LaneAllocator.generateLanes(arena, laneCount, settings.getLaneSpacing(), settings.getLuckyBlockMaterial());

        int totalPlaced = 0;
        for (RaceLane lane : lanes) {
            totalPlaced += lane.getPlacer().placeAlongPath(
                    lane.getStart(), lane.getFinish(), settings.getBlockInterval(), settings.getStartOffset());
        }
        logger.info("[LuckyBlockRace] Generate " + lanes.size() + " corsie, piazzati " + totalPlaced + " Lucky Block totali.");

        phase = RacePhase.RACING;
        raceStartMillis = System.currentTimeMillis();

        int laneIndex = 0;
        for (UUID id : participants.keySet()) {
            Player player = Bukkit.getPlayer(id);
            RaceParticipant participant = participants.get(id);
            if (laneIndex < lanes.size()) {
                RaceLane lane = lanes.get(laneIndex);
                lane.assign(id);
                participant.setLaneIndex(laneIndex);
                if (player != null) {
                    player.teleport(lane.getStart());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                    giveStartItem(player);
                }
                laneIndex++;
            } else {
                waitingQueue.add(id);
                if (player != null) {
                    messages.send(player, "luckyblockrace.queue.waiting");
                }
            }
        }

        if (!waitingQueue.isEmpty()) {
            Bukkit.broadcast(messages.get("luckyblockrace.announce.race-started-with-queue",
                    "lanes", String.valueOf(lanes.size()),
                    "waiting", String.valueOf(waitingQueue.size())));
        } else {
            Bukkit.broadcast(messages.get("luckyblockrace.announce.race-started"));
        }

        if (settings.getMaxDurationSeconds() > 0) {
            scheduleMaxDurationTimeout();
        }
        scheduleStartItemWatcher();
    }

    @NotNull
    private org.bukkit.inventory.ItemStack createStartItem() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(settings.getStartItemMaterial());
        if (settings.getStartItemUnbreakingLevel() > 0) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, settings.getStartItemUnbreakingLevel());
        }
        return item;
    }

    private void giveStartItem(@NotNull Player player) {
        player.getInventory().addItem(createStartItem());
    }

    private void scheduleStartItemWatcher() {
        Bukkit.getScheduler().runTaskTimer(context.asPlugin(), task -> {
            if (phase != RacePhase.RACING) {
                task.cancel();
                return;
            }
            for (RaceParticipant participant : participants.values()) {
                if (!participant.hasLane() || participant.isFinished()) {
                    continue;
                }
                Player player = Bukkit.getPlayer(participant.getPlayerId());
                if (player == null) {
                    continue;
                }
                if (!player.getInventory().contains(settings.getStartItemMaterial())) {
                    giveStartItem(player);
                }
            }
        }, 40L, 40L);
    }

    private void scheduleMaxDurationTimeout() {
        Bukkit.getScheduler().runTaskLater(context.asPlugin(), () -> {
            if (phase == RacePhase.RACING) {
                Bukkit.broadcast(messages.get("luckyblockrace.announce.time-up"));
                concludeRacingPhase();
            }
        }, settings.getMaxDurationSeconds() * 20L);
    }

    @Override
    public void stop() {
        if (phase == RacePhase.IDLE) {
            return;
        }
        Bukkit.broadcast(messages.get("luckyblockrace.announce.stopped-by-staff"));
        endRaceImmediately();
    }

    @Override
    public void forceStop() {
        endRaceImmediately();
    }

    private void endRaceImmediately() {
        cancelActiveTask();
        for (UUID id : new ArrayList<>(participants.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        for (RaceLane lane : lanes) {
            lane.release();
        }
        lanes = new ArrayList<>();
        participants.clear();
        waitingQueue.clear();
        finishOrder.clear();
        finishedCount = 0;
        phase = RacePhase.IDLE;
        context.getPlugin().getEventManager().clearActiveIfMatches(ID);
    }

    private void cancelActiveTask() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    public boolean isLuckyBlockOfAnotherLane(@NotNull Player player, @NotNull Block block) {
        RaceLane ownLane = getLaneOf(player);
        for (RaceLane lane : lanes) {
            if (lane == ownLane) {
                continue;
            }
            if (lane.getPlacer().isTrackedLuckyBlock(block)) {
                return true;
            }
        }
        return false;
    }

    public void handleLuckyBlockBroken(@NotNull Player player, @NotNull Block block) {
        RaceLane lane = getLaneOf(player);
        if (lane != null) {
            lane.getPlacer().markBroken(block);
        }

        RaceParticipant participant = participants.get(player.getUniqueId());
        if (participant != null) {
            participant.incrementLuckyBlocksBroken();
        }

        if (outcomePool.isEmpty()) {
            return;
        }
        LuckyBlockOutcome outcome = outcomePool.rollRandom();
        Vector direction = lane != null ? lane.getDirection() : new Vector(0, 0, 0);
        outcomeExecutor.apply(outcome, player, block.getLocation(), () -> direction);
    }

    public void handlePlayerMove(@NotNull Player player, @NotNull Location to) {
        if (phase != RacePhase.RACING) {
            return;
        }
        RaceParticipant participant = participants.get(player.getUniqueId());
        if (participant == null || participant.isFinished()) {
            return;
        }
        RaceLane lane = getLaneOf(player);
        if (lane == null) {
            return;
        }
        Location finish = lane.getFinish();
        if (finish.getWorld() == null || !finish.getWorld().equals(to.getWorld())) {
            return;
        }
        if (lane.horizontalDistanceSquaredToFinish(to) > 2.25) {
            return;
        }
        if (lane.getPlacer().getRemainingCount() > 0) {
            notifyFinishBlockedOnce(player, participant);
            return;
        }
        onPlayerFinished(player, participant);
    }

    private void notifyFinishBlockedOnce(Player player, RaceParticipant participant) {
        long now = System.currentTimeMillis();
        if (now - participant.getLastFinishWarningMillis() < 3000) {
            return;
        }
        participant.setLastFinishWarningMillis(now);
        RaceLane lane = getLaneOf(player);
        int remaining = lane != null ? lane.getPlacer().getRemainingCount() : 0;
        messages.send(player, "luckyblockrace.race.finish-blocked", "remaining", String.valueOf(remaining));
    }

    private void onPlayerFinished(Player player, RaceParticipant participant) {
        finishedCount++;
        participant.markFinished(finishedCount, raceStartMillis);
        finishOrder.add(player.getUniqueId());

        Bukkit.broadcast(messages.get("luckyblockrace.announce.player-finished",
                "player", player.getName(),
                "position", String.valueOf(finishedCount)));

        if (settings.isFinaleEnabled()) {
            moveToFinaleLobby(player);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }

        checkForEarlyRaceEnd();
    }

    private void moveToFinaleLobby(Player player) {
        Location finaleCenter = arena.getFinaleArenaCenter() != null ? arena.getFinaleArenaCenter() : arena.getFinish();
        if (finaleCenter != null) {
            player.teleport(finaleCenter);
        }
        player.setGameMode(GameMode.ADVENTURE);
    }

    private void checkForEarlyRaceEnd() {
        if (phase != RacePhase.RACING) {
            return;
        }
        List<RaceParticipant> racing = participants.values().stream()
                .filter(RaceParticipant::hasLane)
                .toList();
        boolean everyoneDone = !racing.isEmpty() && racing.stream().allMatch(RaceParticipant::isFinished);
        boolean nobodyRacing = racing.isEmpty();
        if (everyoneDone || nobodyRacing) {
            concludeRacingPhase();
        }
    }

    private void concludeRacingPhase() {
        cancelActiveTask();
        if (settings.isFinaleEnabled() && finishOrder.size() >= 2) {
            startFinale();
        } else {
            announceFinalResultsAndEnd();
        }
    }

    private void startFinale() {
        phase = RacePhase.FINALE_WAITING;
        Bukkit.broadcast(messages.get("luckyblockrace.announce.finale-incoming",
                "seconds", String.valueOf(settings.getFinaleWaitSeconds())));

        Bukkit.getScheduler().runTaskLater(context.asPlugin(), this::runFinaleCountdown,
                settings.getFinaleWaitSeconds() * 20L);
    }

    private void runFinaleCountdown() {
        if (phase != RacePhase.FINALE_WAITING) {
            return;
        }
        phase = RacePhase.FINALE_FIGHT;
        for (UUID id : finishOrder) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20.0);
                player.getInventory().clear();
            }
        }
        Bukkit.broadcast(messages.get("luckyblockrace.announce.finale-started"));
        scheduleFinaleWatcher();
    }

    private void scheduleFinaleWatcher() {
        activeTask = Bukkit.getScheduler().runTaskTimer(context.asPlugin(), () -> {
            if (phase != RacePhase.FINALE_FIGHT) {
                cancelActiveTask();
                return;
            }
            long alive = finishOrder.stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline() && p.getGameMode() != GameMode.SPECTATOR)
                    .count();
            if (alive <= 1) {
                cancelActiveTask();
                announceFinalResultsAndEnd();
            }
        }, 20L, 20L);
    }

    private void announceFinalResultsAndEnd() {
        phase = RacePhase.ENDING;

        UUID winnerId = determineWinner();
        String winnerName = winnerId != null ? safePlayerName(winnerId) : null;

        if (winnerName != null) {
            Bukkit.broadcast(messages.get("luckyblockrace.announce.winner", "player", winnerName));
        } else {
            Bukkit.broadcast(messages.get("luckyblockrace.announce.no-winner"));
        }

        sendLeaderboard();

        Bukkit.getScheduler().runTaskLater(context.asPlugin(), this::endRaceImmediately, 60L);
    }

    @org.jetbrains.annotations.Nullable
    private UUID determineWinner() {
        if (settings.isFinaleEnabled() && !finishOrder.isEmpty()) {
            return finishOrder.stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline() && p.getGameMode() != GameMode.SPECTATOR)
                    .map(Player::getUniqueId)
                    .findFirst()
                    .orElse(finishOrder.get(0));
        }
        return finishOrder.isEmpty() ? null : finishOrder.get(0);
    }

    private void sendLeaderboard() {
        List<RaceParticipant> ranked = finishOrder.stream()
                .map(participants::get)
                .filter(p -> p != null)
                .toList();

        if (ranked.isEmpty()) {
            return;
        }

        Component header = messages.getRaw("luckyblockrace.leaderboard.header");
        Bukkit.broadcast(header);
        int position = 1;
        for (RaceParticipant participant : ranked) {
            double seconds = participant.getFinishTimeMillis() / 1000.0;
            Component line = messages.getRaw("luckyblockrace.leaderboard.line",
                    "position", String.valueOf(position),
                    "player", participant.getPlayerName(),
                    "time", String.format("%.1f", seconds),
                    "blocks", String.valueOf(participant.getLuckyBlocksBroken()));
            Bukkit.broadcast(line);
            position++;
        }
    }

    private String safePlayerName(UUID id) {
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            return player.getName();
        }
        RaceParticipant participant = participants.get(id);
        return participant != null ? participant.getPlayerName() : "???";
    }

    private void broadcastToParticipantsAndStaff(String key, Object... placeholders) {
        Component component = messages.get(key, placeholders);
        for (UUID id : participants.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendMessage(component);
            }
        }
    }

    private void loadArenaFromDisk() {
        arenaFile = new File(new File(context.getPlugin().getDataFolder(), "luckyblockrace"), "arena.yml");
        if (!arenaFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(arenaFile);
        arena.loadFrom(config);
    }

    private void saveArenaToDisk() {
        try {
            if (arenaFile == null) {
                arenaFile = new File(new File(context.getPlugin().getDataFolder(), "luckyblockrace"), "arena.yml");
            }
            File parent = arenaFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            YamlConfiguration config = new YamlConfiguration();
            arena.saveTo(config);
            config.save(arenaFile);
        } catch (IOException e) {
            logger.warning("[LuckyBlockRace] Impossibile salvare arena.yml: " + e.getMessage());
        }
    }
}