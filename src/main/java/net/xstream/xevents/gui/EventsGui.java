package net.xstream.xevents.gui;

import net.kyori.adventure.text.Component;
import net.xstream.xevents.XEvents;
import net.xstream.xevents.api.XEventModule;
import net.xstream.xevents.api.XEventState;
import net.xstream.xevents.events.luckyblockrace.LuckyBlockRaceModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public final class EventsGui implements Listener {

    private static final String TITLE_KEY = "gui.title";
    private final XEvents plugin;
    private Inventory inventory;

    public EventsGui(XEvents plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(@NotNull Player player) {
        Component title = plugin.getMessageService().getRaw(TITLE_KEY);
        inventory = plugin.getServer().createInventory(new GuiHolder(), 27, title);

        int slot = 10;
        for (XEventModule module : plugin.getEventManager().getModules()) {
            if (slot > 16) break; 
            inventory.setItem(slot, buildModuleItem(module));
            slot++;
        }

        ItemStack close = namedItem(Material.BARRIER, plugin.getMessageService().getRaw("gui.close-item"));
        inventory.setItem(22, close);

        player.openInventory(inventory);
    }

    private ItemStack buildModuleItem(XEventModule module) {
        Material material = switch (module.getState()) {
            case IDLE -> Material.LIME_DYE;
            case SETUP -> Material.YELLOW_DYE;
            case WAITING -> Material.CLOCK;
            case RUNNING -> Material.REDSTONE;
            case ENDING -> Material.GRAY_DYE;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getMessageService().miniMessage().deserialize(module.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.getMessageService().getRaw("gui.lore.description", "description", module.getDescription()));
        lore.add(plugin.getMessageService().getRaw("gui.lore.state", "state", module.getState().name()));

        if (module instanceof LuckyBlockRaceModule && module.getState() == XEventState.IDLE) {
            if (!module.isReadyToStart()) {
                lore.add(plugin.getMessageService().getRaw("gui.lore.not-ready"));
                for (String issue : module.getSetupIssues()) {
                    lore.add(plugin.getMessageService().getRaw("gui.lore.issue", "issue", issue));
                }
            } else {
                lore.add(plugin.getMessageService().getRaw("gui.lore.ready"));
            }
            lore.add(plugin.getMessageService().getRaw("gui.lore.click-start"));
        } else if (module.getState() != XEventState.IDLE) {
            lore.add(plugin.getMessageService().getRaw("gui.lore.click-stop"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack namedItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();
        List<XEventModule> modules = new ArrayList<>(plugin.getEventManager().getModules());
        int index = slot - 10;
        if (index < 0 || index >= modules.size()) {
            return;
        }
        XEventModule module = modules.get(index);

        if (module.getState() == XEventState.IDLE) {
            if (!plugin.getEventManager().trySetActive(module.getId())) {
                plugin.getMessageService().send(player, "command.another-event-active");
                return;
            }
            if (!module.isReadyToStart()) {
                plugin.getMessageService().send(player, "command.start.not-ready");
                plugin.getEventManager().clearActiveIfMatches(module.getId());
                player.closeInventory();
                return;
            }
            module.start(player);
            plugin.getMessageService().send(player, "command.start.success", "name", module.getDisplayName());
        } else {
            module.stop();
            plugin.getMessageService().send(player, "command.stop.success");
        }
        player.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        
    }

    
    private static final class GuiHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            throw new UnsupportedOperationException("Holder marker, non usare direttamente.");
        }
    }
}
