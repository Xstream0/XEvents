package net.xstream.xevents.events.luckyblockrace.outcome;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public final class LuckyBlockOutcome {

    private final String id;
    private final LuckyOutcomeType type;
    private final int weight;
    private final String displayMessageKey;

    
    private final List<Material> items;
    private final int minAmount;
    private final int maxAmount;

    private final PotionEffectType potionEffectType;
    private final int potionDurationSeconds;
    private final int potionAmplifier;

    private final int mobCount;
    private final String mobType;

    private final double tntPower;
    private final boolean tntBreaksBlocks;

    private final boolean lightningDamaging;

    private final int teleportBlocks;

    private final String soundKey;

    public LuckyBlockOutcome(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.weight = builder.weight;
        this.displayMessageKey = builder.displayMessageKey;
        this.items = builder.items;
        this.minAmount = builder.minAmount;
        this.maxAmount = builder.maxAmount;
        this.potionEffectType = builder.potionEffectType;
        this.potionDurationSeconds = builder.potionDurationSeconds;
        this.potionAmplifier = builder.potionAmplifier;
        this.mobCount = builder.mobCount;
        this.mobType = builder.mobType;
        this.tntPower = builder.tntPower;
        this.tntBreaksBlocks = builder.tntBreaksBlocks;
        this.lightningDamaging = builder.lightningDamaging;
        this.teleportBlocks = builder.teleportBlocks;
        this.soundKey = builder.soundKey;
    }

    @NotNull public String getId() { return id; }
    @NotNull public LuckyOutcomeType getType() { return type; }
    public int getWeight() { return weight; }
    @Nullable public String getDisplayMessageKey() { return displayMessageKey; }

    @NotNull public List<Material> getItems() { return items; }
    public int getMinAmount() { return minAmount; }
    public int getMaxAmount() { return maxAmount; }

    @Nullable public PotionEffectType getPotionEffectType() { return potionEffectType; }
    public int getPotionDurationSeconds() { return potionDurationSeconds; }
    public int getPotionAmplifier() { return potionAmplifier; }

    public int getMobCount() { return mobCount; }
    @Nullable public String getMobType() { return mobType; }

    public double getTntPower() { return tntPower; }
    public boolean isTntBreaksBlocks() { return tntBreaksBlocks; }

    public boolean isLightningDamaging() { return lightningDamaging; }

    public int getTeleportBlocks() { return teleportBlocks; }

    @Nullable public String getSoundKey() { return soundKey; }

    public static final class Builder {
        private String id = "unknown";
        private LuckyOutcomeType type = LuckyOutcomeType.NOTHING;
        private int weight = 1;
        private String displayMessageKey = null;

        private List<Material> items = List.of();
        private int minAmount = 1;
        private int maxAmount = 1;

        private PotionEffectType potionEffectType = null;
        private int potionDurationSeconds = 10;
        private int potionAmplifier = 0;

        private int mobCount = 1;
        private String mobType = "ZOMBIE";

        private double tntPower = 3.0;
        private boolean tntBreaksBlocks = false;

        private boolean lightningDamaging = false;

        private int teleportBlocks = 5;

        private String soundKey = null;

        public Builder id(String id) { this.id = id; return this; }
        public Builder type(LuckyOutcomeType type) { this.type = type; return this; }
        public Builder weight(int weight) { this.weight = Math.max(0, weight); return this; }
        public Builder displayMessageKey(String key) { this.displayMessageKey = key; return this; }

        public Builder items(List<Material> items) { this.items = items; return this; }
        public Builder amountRange(int min, int max) { this.minAmount = min; this.maxAmount = max; return this; }

        public Builder potionEffectType(PotionEffectType type) { this.potionEffectType = type; return this; }
        public Builder potionDurationSeconds(int seconds) { this.potionDurationSeconds = seconds; return this; }
        public Builder potionAmplifier(int amplifier) { this.potionAmplifier = amplifier; return this; }

        public Builder mobCount(int count) { this.mobCount = count; return this; }
        public Builder mobType(String mobType) { this.mobType = mobType; return this; }

        public Builder tntPower(double power) { this.tntPower = power; return this; }
        public Builder tntBreaksBlocks(boolean breaksBlocks) { this.tntBreaksBlocks = breaksBlocks; return this; }

        public Builder lightningDamaging(boolean damaging) { this.lightningDamaging = damaging; return this; }

        public Builder teleportBlocks(int blocks) { this.teleportBlocks = blocks; return this; }

        public Builder soundKey(String soundKey) { this.soundKey = soundKey; return this; }

        public LuckyBlockOutcome build() { return new LuckyBlockOutcome(this); }
    }
}
