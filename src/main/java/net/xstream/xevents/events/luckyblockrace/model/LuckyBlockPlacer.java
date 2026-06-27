package net.xstream.xevents.events.luckyblockrace.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LuckyBlockPlacer {

    private final Material luckyBlockMaterial;
    private final Map<Location, BlockData> placedBlocks = new LinkedHashMap<>();

    public LuckyBlockPlacer(@NotNull Material luckyBlockMaterial) {
        this.luckyBlockMaterial = luckyBlockMaterial;
    }

    public int placeAlongPath(@NotNull Location start, @NotNull Location finish, double intervalBlocks, double offsetBlocks) {
        placedBlocks.clear(); // sicurezza: non lasciare residui da una run precedente non ripulita

        if (start.getWorld() == null || finish.getWorld() == null || !start.getWorld().equals(finish.getWorld())) {
            return 0;
        }
        org.bukkit.World world = start.getWorld();

        double totalDistance = start.distance(finish);
        if (totalDistance <= 0 || intervalBlocks <= 0) {
            return 0;
        }

        org.bukkit.util.Vector direction = finish.toVector().subtract(start.toVector());
        if (direction.lengthSquared() == 0) {
            return 0;
        }
        direction = direction.normalize();

        int placedCount = 0;
        for (double d = offsetBlocks; d < totalDistance; d += intervalBlocks) {
            Location point = start.clone().add(direction.clone().multiply(d));
            int x = point.getBlockX();
            int z = point.getBlockZ();

            int groundY = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.MOTION_BLOCKING_NO_LEAVES);

            Block block = world.getBlockAt(x, groundY + 1, z);
            Location key = block.getLocation();
            placedBlocks.put(key, block.getBlockData().clone());
            block.setType(luckyBlockMaterial, false);
            placedCount++;
        }
        return placedCount;
    }

    public boolean isTrackedLuckyBlock(@NotNull Block block) {
        if (block.getType() != luckyBlockMaterial) {
            return false;
        }
        return placedBlocks.containsKey(block.getLocation());
    }

    public void markBroken(@NotNull Block block) {
        placedBlocks.remove(block.getLocation());
    }

    public void restoreAll() {
        for (Map.Entry<Location, BlockData> entry : placedBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            block.setBlockData(entry.getValue(), false);
        }
        placedBlocks.clear();
    }

    public int getRemainingCount() {
        return placedBlocks.size();
    }
}