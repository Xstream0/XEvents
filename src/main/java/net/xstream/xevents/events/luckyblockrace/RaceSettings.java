package net.xstream.xevents.events.luckyblockrace;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RaceSettings {

    private final Material luckyBlockMaterial;
    private final Material startItemMaterial;
    private final int startItemUnbreakingLevel;
    private final double blockInterval;
    private final double startOffset;
    private final int minPlayers;
    private final int countdownSeconds;
    private final int maxDurationSeconds;
    private final boolean finaleEnabled;
    private final int finaleWaitSeconds;
    private final int finaleCountdownSeconds;
    private final double finaleArenaRadius;
    private final boolean broadcastJoinLeave;
    private final int maxLanes;
    private final double laneSpacing;

    public RaceSettings(@Nullable ConfigurationSection section) {
        if (section == null) {
            this.luckyBlockMaterial = Material.GOLD_BLOCK;
            this.startItemMaterial = Material.DIAMOND_PICKAXE;
            this.startItemUnbreakingLevel = 10;
            this.blockInterval = 4.0;
            this.startOffset = 3.0;
            this.minPlayers = 2;
            this.countdownSeconds = 15;
            this.maxDurationSeconds = 900;
            this.finaleEnabled = false;
            this.finaleWaitSeconds = 15;
            this.finaleCountdownSeconds = 10;
            this.finaleArenaRadius = 10.0;
            this.broadcastJoinLeave = true;
            this.maxLanes = 8;
            this.laneSpacing = 6.0;
            return;
        }

        Material parsedMaterial = Material.matchMaterial(section.getString("lucky-block-material", "GOLD_BLOCK"));
        this.luckyBlockMaterial = parsedMaterial != null ? parsedMaterial : Material.GOLD_BLOCK;

        Material parsedStartItem = Material.matchMaterial(section.getString("start-item.material", "DIAMOND_PICKAXE"));
        this.startItemMaterial = parsedStartItem != null ? parsedStartItem : Material.DIAMOND_PICKAXE;
        this.startItemUnbreakingLevel = Math.max(0, section.getInt("start-item.unbreaking-level", 10));

        this.blockInterval = Math.max(1.0, section.getDouble("block-interval", 4.0));
        this.startOffset = Math.max(0.0, section.getDouble("start-offset", 3.0));
        this.minPlayers = Math.max(1, section.getInt("min-players", 2));
        this.countdownSeconds = Math.max(0, section.getInt("countdown-seconds", 15));
        this.maxDurationSeconds = Math.max(0, section.getInt("max-duration-seconds", 900));
        this.broadcastJoinLeave = section.getBoolean("broadcast-join-leave", true);
        this.maxLanes = Math.max(1, section.getInt("max-lanes", 8));
        this.laneSpacing = Math.max(1.0, section.getDouble("lane-spacing", 6.0));

        ConfigurationSection finale = section.getConfigurationSection("finale");
        if (finale != null) {
            this.finaleEnabled = finale.getBoolean("enabled", false);
            this.finaleWaitSeconds = Math.max(0, finale.getInt("wait-seconds", 15));
            this.finaleCountdownSeconds = Math.max(0, finale.getInt("countdown-seconds", 10));
            this.finaleArenaRadius = Math.max(2.0, finale.getDouble("arena-radius", 10.0));
        } else {
            this.finaleEnabled = false;
            this.finaleWaitSeconds = 15;
            this.finaleCountdownSeconds = 10;
            this.finaleArenaRadius = 10.0;
        }
    }

    @NotNull public Material getLuckyBlockMaterial() { return luckyBlockMaterial; }
    @NotNull public Material getStartItemMaterial() { return startItemMaterial; }
    public int getStartItemUnbreakingLevel() { return startItemUnbreakingLevel; }
    public double getBlockInterval() { return blockInterval; }
    public double getStartOffset() { return startOffset; }
    public int getMinPlayers() { return minPlayers; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public int getMaxDurationSeconds() { return maxDurationSeconds; }
    public boolean isFinaleEnabled() { return finaleEnabled; }
    public int getFinaleWaitSeconds() { return finaleWaitSeconds; }
    public int getFinaleCountdownSeconds() { return finaleCountdownSeconds; }
    public double getFinaleArenaRadius() { return finaleArenaRadius; }
    public boolean isBroadcastJoinLeave() { return broadcastJoinLeave; }
    public int getMaxLanes() { return maxLanes; }
    public double getLaneSpacing() { return laneSpacing; }
}