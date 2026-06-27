package net.xstream.xevents.events.luckyblockrace.outcome;

import net.xstream.xevents.config.MessageService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;


public final class OutcomeExecutor {

    private final MessageService messages;
    private final Logger logger;

    public OutcomeExecutor(@NotNull MessageService messages, @NotNull Logger logger) {
        this.messages = messages;
        this.logger = logger;
    }

    public void apply(@NotNull LuckyBlockOutcome outcome, @NotNull Player player, @NotNull Location blockLocation,
                       @NotNull RaceDirectionProvider directionProvider) {
        switch (outcome.getType()) {
            case GIVE_ITEM -> giveItem(outcome, player);
            case POTION_EFFECT -> applyPotion(outcome, player);
            case TNT -> spawnTnt(outcome, blockLocation);
            case LIGHTNING -> strikeLightning(outcome, blockLocation);
            case SPAWN_MOBS -> spawnMobs(outcome, blockLocation);
            case BLOCK_EFFECT -> applyBlockEffect(blockLocation);
            case TELEPORT_BACK -> teleportAlongPath(player, directionProvider, -outcome.getTeleportBlocks());
            case TELEPORT_FORWARD -> teleportAlongPath(player, directionProvider, outcome.getTeleportBlocks());
            case COSMETIC -> {  }
            case NOTHING -> {  }
        }

        playSound(outcome, blockLocation);
        announceToPlayer(outcome, player);
    }

    private void giveItem(LuckyBlockOutcome outcome, Player player) {
        if (outcome.getItems().isEmpty()) {
            return;
        }
        Material material = outcome.getItems().get(ThreadLocalRandom.current().nextInt(outcome.getItems().size()));
        int min = Math.min(outcome.getMinAmount(), outcome.getMaxAmount());
        int max = Math.max(outcome.getMinAmount(), outcome.getMaxAmount());
        int amount = (min == max) ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        amount = Math.max(1, amount);

        ItemStack stack = new ItemStack(material, amount);
        var leftover = player.getInventory().addItem(stack);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }

    private void applyPotion(LuckyBlockOutcome outcome, Player player) {
        if (outcome.getPotionEffectType() == null) {
            return;
        }
        int durationTicks = Math.max(1, outcome.getPotionDurationSeconds()) * 20;
        player.addPotionEffect(new PotionEffect(outcome.getPotionEffectType(), durationTicks, outcome.getPotionAmplifier(), false, true, true));
    }

    private void spawnTnt(LuckyBlockOutcome outcome, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        var tnt = world.spawn(location.clone().add(0.5, 0.5, 0.5), org.bukkit.entity.TNTPrimed.class);
        tnt.setFuseTicks(40);
        tnt.setSource(null);
        
        
        tnt.setYield(outcome.isTntBreaksBlocks() ? (float) Math.max(0.0, outcome.getTntPower()) : 0f);
    }

    private void strikeLightning(LuckyBlockOutcome outcome, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        if (outcome.isLightningDamaging()) {
            world.strikeLightning(location);
        } else {
            
            
            world.strikeLightningEffect(location);
        }
    }

    private void spawnMobs(LuckyBlockOutcome outcome, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        EntityType type;
        try {
            type = EntityType.valueOf(outcome.getMobType());
        } catch (IllegalArgumentException e) {
            logger.warning("[LuckyBlockRace] Tipo di mob non valido: " + outcome.getMobType());
            return;
        }
        for (int i = 0; i < outcome.getMobCount(); i++) {
            Location spawnAt = location.clone().add(
                    ThreadLocalRandom.current().nextDouble(-1.5, 1.5),
                    1.0,
                    ThreadLocalRandom.current().nextDouble(-1.5, 1.5)
            );
            var entity = world.spawnEntity(spawnAt, type);
            if (entity instanceof LivingEntity living) {
                living.setRemoveWhenFarAway(true);
            }
        }
    }

    private void applyBlockEffect(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        
        
        
        world.spawnParticle(org.bukkit.Particle.CLOUD, location.clone().add(0.5, 1, 0.5), 20, 0.5, 0.5, 0.5, 0.05);
    }

    private void teleportAlongPath(Player player, RaceDirectionProvider directionProvider, int blocksSigned) {
        Vector direction = directionProvider.getRaceDirection();
        if (direction.lengthSquared() == 0) {
            return;
        }
        direction = direction.normalize();
        Location destination = player.getLocation().add(direction.multiply(blocksSigned));
        destination.setY(player.getLocation().getY());
        player.teleport(destination);
    }

    @SuppressWarnings("deprecation") 
    
    
    
    private void playSound(LuckyBlockOutcome outcome, Location location) {
        if (outcome.getSoundKey() == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(outcome.getSoundKey().toUpperCase());
            world.playSound(location, sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            logger.warning("[LuckyBlockRace] Suono non valido configurato: " + outcome.getSoundKey());
        }
    }

    private void announceToPlayer(LuckyBlockOutcome outcome, Player player) {
        if (outcome.getDisplayMessageKey() == null) {
            return;
        }
        player.sendMessage(messages.getRaw(outcome.getDisplayMessageKey(), "player", player.getName()));
    }

    
    public interface RaceDirectionProvider {
        @NotNull Vector getRaceDirection();
    }
}