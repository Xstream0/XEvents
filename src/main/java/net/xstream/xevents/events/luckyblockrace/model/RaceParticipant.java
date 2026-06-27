package net.xstream.xevents.events.luckyblockrace.model;

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
}