package net.xstream.xevents.events.luckyblockrace.model;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class RaceArena {

    private Location start;
    private Location finish;
    private Location waitingLobby;
    private Location finaleArenaCenter;

    public RaceArena() {
    }

    public boolean hasStart() {
        return start != null;
    }

    public boolean hasFinish() {
        return finish != null;
    }

    public boolean isComplete() {
        return hasStart() && hasFinish() && start.getWorld() != null
                && finish.getWorld() != null
                && start.getWorld().equals(finish.getWorld());
    }

    @Nullable
    public Location getStart() {
        return start;
    }

    public void setStart(@NotNull Location start) {
        this.start = start.clone();
    }

    @Nullable
    public Location getFinish() {
        return finish;
    }

    public void setFinish(@NotNull Location finish) {
        this.finish = finish.clone();
    }

    @Nullable
    public Location getWaitingLobby() {
        return waitingLobby;
    }

    public void setWaitingLobby(@Nullable Location waitingLobby) {
        this.waitingLobby = waitingLobby == null ? null : waitingLobby.clone();
    }

    @Nullable
    public Location getFinaleArenaCenter() {
        return finaleArenaCenter;
    }

    public void setFinaleArenaCenter(@Nullable Location finaleArenaCenter) {
        this.finaleArenaCenter = finaleArenaCenter == null ? null : finaleArenaCenter.clone();
    }

    public double distance() {
        if (!isComplete()) {
            return 0;
        }
        return start.distance(finish);
    }

    public void clear() {
        start = null;
        finish = null;
        waitingLobby = null;
        finaleArenaCenter = null;
    }

    

    public void saveTo(@NotNull YamlConfiguration config) {
        saveLocation(config, "start", start);
        saveLocation(config, "finish", finish);
        saveLocation(config, "waiting-lobby", waitingLobby);
        saveLocation(config, "finale-arena-center", finaleArenaCenter);
    }

    public void loadFrom(@NotNull YamlConfiguration config) {
        this.start = loadLocation(config, "start");
        this.finish = loadLocation(config, "finish");
        this.waitingLobby = loadLocation(config, "waiting-lobby");
        this.finaleArenaCenter = loadLocation(config, "finale-arena-center");
    }

    private void saveLocation(YamlConfiguration config, String path, Location location) {
        if (location == null || location.getWorld() == null) {
            config.set(path, null);
            return;
        }
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    @Nullable
    private Location loadLocation(YamlConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        var world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }
}
