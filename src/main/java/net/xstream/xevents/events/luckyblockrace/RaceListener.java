package net.xstream.xevents.events.luckyblockrace;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public final class RaceListener implements Listener {

    private final LuckyBlockRaceModule module;

    public RaceListener(LuckyBlockRaceModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!module.isParticipant(player)) {
            return;
        }
        var lane = module.getLaneOf(player);
        if (lane != null && lane.getPlacer().isTrackedLuckyBlock(event.getBlock())) {
            
            event.setDropItems(false);
            event.setExpToDrop(0);
            module.handleLuckyBlockBroken(player, event.getBlock());
            return;
        }
        
        
        
        if (module.isLuckyBlockOfAnotherLane(player, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!module.isParticipant(player)) {
            return;
        }
        module.handlePlayerMove(player, event.getTo());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (module.isParticipant(player)) {
            module.leave(player);
        }
    }

    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!module.isParticipant(victim)) {
            return;
        }
        if (module.getPhase() == RacePhase.RACING || module.getPhase() == RacePhase.COUNTDOWN
                || module.getPhase() == RacePhase.FINALE_WAITING) {
            event.setCancelled(true);
        }
    }

    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!module.isParticipant(player)) {
            return;
        }
        if (module.getPhase() == RacePhase.COUNTDOWN) {
            event.setCancelled(true);
        }
    }
}