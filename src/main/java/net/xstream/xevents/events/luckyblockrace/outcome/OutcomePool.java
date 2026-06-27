package net.xstream.xevents.events.luckyblockrace.outcome;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;


public final class OutcomePool {

    private final List<LuckyBlockOutcome> outcomes = new ArrayList<>();
    private int totalWeight = 0;

    
    public void load(@Nullable ConfigurationSection outcomesSection, @NotNull Logger logger) {
        outcomes.clear();
        totalWeight = 0;

        if (outcomesSection == null) {
            logger.warning("[LuckyBlockRace] Sezione 'outcomes' assente in config.yml: nessun esito caricato.");
            return;
        }

        for (String key : outcomesSection.getKeys(false)) {
            ConfigurationSection section = outcomesSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                LuckyBlockOutcome outcome = parseOutcome(key, section, logger);
                if (outcome.getWeight() <= 0) {
                    continue; 
                }
                outcomes.add(outcome);
                totalWeight += outcome.getWeight();
            } catch (Exception e) {
                logger.warning("[LuckyBlockRace] Esito '" + key + "' ignorato per errore di configurazione: " + e.getMessage());
            }
        }

        logger.info("[LuckyBlockRace] Caricati " + outcomes.size() + " possibili esiti per i Lucky Block (peso totale: " + totalWeight + ").");
    }

    private LuckyBlockOutcome parseOutcome(String key, ConfigurationSection section, Logger logger) {
        LuckyOutcomeType type;
        try {
            type = LuckyOutcomeType.valueOf(section.getString("type", "NOTHING").toUpperCase());
        } catch (IllegalArgumentException ex) {
            logger.warning("[LuckyBlockRace] Tipo esito non valido per '" + key + "', impostato su NOTHING.");
            type = LuckyOutcomeType.NOTHING;
        }

        LuckyBlockOutcome.Builder builder = new LuckyBlockOutcome.Builder()
                .id(key)
                .type(type)
                .weight(section.getInt("weight", 1))
                .displayMessageKey(section.getString("message", null));

        switch (type) {
            case GIVE_ITEM -> {
                List<Material> materials = new ArrayList<>();
                for (String matName : section.getStringList("items")) {
                    Material material = Material.matchMaterial(matName);
                    if (material != null) {
                        materials.add(material);
                    } else {
                        logger.warning("[LuckyBlockRace] Materiale '" + matName + "' non valido in esito '" + key + "'.");
                    }
                }
                builder.items(materials);
                builder.amountRange(
                        section.getInt("min-amount", 1),
                        section.getInt("max-amount", 1)
                );
            }
            case POTION_EFFECT -> {
                String effectName = section.getString("effect", "SPEED");
                PotionEffectType effectType = resolvePotionEffectType(effectName);
                if (effectType == null) {
                    logger.warning("[LuckyBlockRace] Effetto pozione '" + effectName + "' non valido in esito '" + key + "', uso SPEED.");
                    effectType = PotionEffectType.SPEED;
                }
                builder.potionEffectType(effectType);
                builder.potionDurationSeconds(section.getInt("duration-seconds", 10));
                builder.potionAmplifier(Math.max(0, section.getInt("amplifier", 0)));
            }
            case SPAWN_MOBS -> {
                builder.mobCount(Math.max(1, section.getInt("count", 1)));
                builder.mobType(section.getString("mob", "ZOMBIE").toUpperCase());
            }
            case TNT -> {
                builder.tntPower(section.getDouble("power", 3.0));
                builder.tntBreaksBlocks(section.getBoolean("breaks-blocks", false));
            }
            case LIGHTNING -> builder.lightningDamaging(section.getBoolean("damaging", false));
            case TELEPORT_BACK, TELEPORT_FORWARD -> builder.teleportBlocks(Math.max(1, section.getInt("blocks", 5)));
            case BLOCK_EFFECT, COSMETIC, NOTHING -> {  }
        }

        builder.soundKey(section.getString("sound", null));
        return builder.build();
    }

    @SuppressWarnings("deprecation") 
    
    
    
    private PotionEffectType resolvePotionEffectType(String name) {
        return PotionEffectType.getByName(name.toUpperCase());
    }

    public boolean isEmpty() {
        return outcomes.isEmpty() || totalWeight <= 0;
    }

    
    @NotNull
    public LuckyBlockOutcome rollRandom() {
        if (isEmpty()) {
            throw new IllegalStateException("La pool di esiti è vuota: impossibile estrarre un risultato.");
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (LuckyBlockOutcome outcome : outcomes) {
            cumulative += outcome.getWeight();
            if (roll < cumulative) {
                return outcome;
            }
        }
        
        return outcomes.get(outcomes.size() - 1);
    }

    @NotNull
    public Set<String> getOutcomeIds() {
        return outcomes.stream().map(LuckyBlockOutcome::getId).collect(java.util.stream.Collectors.toSet());
    }

    public int size() {
        return outcomes.size();
    }
}