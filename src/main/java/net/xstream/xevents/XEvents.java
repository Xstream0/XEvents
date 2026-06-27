package net.xstream.xevents;

import net.xstream.xevents.api.XEventContext;
import net.xstream.xevents.api.XEventManager;
import net.xstream.xevents.command.XEventsCommand;
import net.xstream.xevents.config.MessageService;
import net.xstream.xevents.events.luckyblockrace.LuckyBlockRaceModule;
import org.bukkit.plugin.java.JavaPlugin;


public final class XEvents extends JavaPlugin {

    private static XEvents instance;

    private XEventManager eventManager;
    private MessageService messageService;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messageService = new MessageService(this);
        this.eventManager = new XEventManager();

        registerModules();

        XEventsCommand command = new XEventsCommand(this);
        var pluginCommand = getCommand("xevents");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        } else {
            getLogger().severe("Comando 'xevents' non trovato: controlla plugin.yml.");
        }

        getLogger().info("XEvents abilitato. Moduli registrati: " + eventManager.getModules().size());
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.unregisterAll();
        }
        getLogger().info("XEvents disabilitato.");
    }

    
    private void registerModules() {
        registerModule(new LuckyBlockRaceModule());
        
        
        
    }

    private void registerModule(net.xstream.xevents.api.XEventModule module) {
        XEventContext context = new XEventContext(this, module.getId(), messageService);
        eventManager.register(module);
        module.onRegister(context);
    }

    public void reloadXEvents() {
        reloadConfig();
        messageService.reload();
    }

    public XEventManager getEventManager() {
        return eventManager;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public static XEvents getInstance() {
        return instance;
    }
}
