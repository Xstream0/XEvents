package net.xstream.xevents.events.luckyblockrace.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


public final class RaceLane {

    private final int index;
    private final Location start;
    private final Location finish;
    private final LuckyBlockPlacer placer;

    @Nullable
    private UUID occupantId;

    public RaceLane(int index, @NotNull Location start, @NotNull Location finish, @NotNull Material luckyBlockMaterial) {
        this.index = index;
        this.start = start.clone();
        this.finish = finish.clone();
        this.placer = new LuckyBlockPlacer(luckyBlockMaterial);
    }

    public int getIndex() {
        return index;
    }

    @NotNull
    public Location getStart() {
        return start.clone();
    }

    @NotNull
    public Location getFinish() {
        return finish.clone();
    }

    @NotNull
    public LuckyBlockPlacer getPlacer() {
        return placer;
    }

    public boolean isFree() {
        return occupantId == null;
    }

    public void assign(@NotNull UUID playerId) {
        this.occupantId = playerId;
    }

    public void release() {
        this.occupantId = null;
        placer.restoreAll();
    }

    @Nullable
    public UUID getOccupantId() {
        return occupantId;
    }

    public boolean isOccupiedBy(@NotNull Player player) {
        return occupantId != null && occupantId.equals(player.getUniqueId());
    }

    
    @NotNull
    public Vector getDirection() {
        return finish.toVector().subtract(start.toVector());
    }

    
    public double horizontalDistanceSquaredToFinish(@NotNull Location location) {
        double dx = location.getX() - finish.getX();
        double dz = location.getZ() - finish.getZ();
        return dx * dx + dz * dz;
    }
}