package net.xstream.xevents.events.luckyblockrace.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public final class LaneAllocator {

    private LaneAllocator() {
    }

    
    @NotNull
    public static Vector computePerpendicular(@NotNull Location start, @NotNull Location finish) {
        Vector direction = finish.toVector().subtract(start.toVector());
        direction.setY(0);
        if (direction.lengthSquared() == 0) {
            
            return new Vector(1, 0, 0);
        }
        direction = direction.normalize();
        
        return new Vector(-direction.getZ(), 0, direction.getX()).normalize();
    }

    
    @NotNull
    public static List<RaceLane> generateLanes(@NotNull RaceArena arena, int count, double spacingBlocks,
                                                 @NotNull Material luckyBlockType) {
        List<RaceLane> lanes = new ArrayList<>();
        if (!arena.isComplete() || count <= 0) {
            return lanes;
        }
        Location start = arena.getStart();
        Location finish = arena.getFinish();
        Vector perpendicular = computePerpendicular(start, finish);

        for (int i = 0; i < count; i++) {
            double offset = laneOffset(i) * spacingBlocks;
            Location laneStart = start.clone().add(perpendicular.clone().multiply(offset));
            Location laneFinish = finish.clone().add(perpendicular.clone().multiply(offset));
            lanes.add(new RaceLane(i, laneStart, laneFinish, luckyBlockType));
        }
        return lanes;
    }

    
    private static int laneOffset(int laneIndex) {
        if (laneIndex == 0) {
            return 0;
        }
        int magnitude = (laneIndex + 1) / 2;
        boolean isRightSide = laneIndex % 2 == 1;
        return isRightSide ? magnitude : -magnitude;
    }
}