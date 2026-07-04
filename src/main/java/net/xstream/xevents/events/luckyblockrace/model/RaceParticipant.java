package net.xstream.xevents.events.luckyblockrace.model;

import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class RaceParticipant {

    private final UUID playerId;
    private final String playerName;

    private boolean finished = false;
    private long finishTimeMillis = -1;
    private int finishPosition = -1;
    private int luckyBlocksBroken = 0;

    private int laneIndex = -1;

    private long lastFinishWarningMillis = 0;

    private PlayerSnapshot snapshot;

    public RaceParticipant(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isFinished() {
        return finished;
    }

    public void markFinished(int position, long raceStartMillis) {
        this.finished = true;
        this.finishPosition = position;
        this.finishTimeMillis = System.currentTimeMillis() - raceStartMillis;
    }

    public long getFinishTimeMillis() {
        return finishTimeMillis;
    }

    public int getFinishPosition() {
        return finishPosition;
    }

    public void incrementLuckyBlocksBroken() {
        luckyBlocksBroken++;
    }

    public int getLuckyBlocksBroken() {
        return luckyBlocksBroken;
    }

    public int getLaneIndex() {
        return laneIndex;
    }

    public void setLaneIndex(int laneIndex) {
        this.laneIndex = laneIndex;
    }

    public boolean hasLane() {
        return laneIndex >= 0;
    }

    public long getLastFinishWarningMillis() {
        return lastFinishWarningMillis;
    }

    public void setLastFinishWarningMillis(long millis) {
        this.lastFinishWarningMillis = millis;
    }

    public void setSnapshot(@Nullable PlayerSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Nullable
    public PlayerSnapshot getSnapshot() {
        return snapshot;
    }

    public static final class PlayerSnapshot {
        private final ItemStack[] inventoryContents;
        private final ItemStack[] armorContents;
        private final ItemStack offHand;
        private final GameMode gameMode;

        public PlayerSnapshot(ItemStack[] inventoryContents, ItemStack[] armorContents,
                               ItemStack offHand, GameMode gameMode) {
            this.inventoryContents = inventoryContents;
            this.armorContents = armorContents;
            this.offHand = offHand;
            this.gameMode = gameMode;
        }

        public ItemStack[] getInventoryContents() {
            return inventoryContents;
        }

        public ItemStack[] getArmorContents() {
            return armorContents;
        }

        public ItemStack getOffHand() {
            return offHand;
        }

        public GameMode getGameMode() {
            return gameMode;
        }
    }
}