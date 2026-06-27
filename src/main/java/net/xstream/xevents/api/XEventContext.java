package net.xstream.xevents.api;

import net.xstream.xevents.XEvents;
import net.xstream.xevents.config.MessageService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public final class XEventContext {

    private final XEvents plugin;
    private final String moduleId;
    private final MessageService messages;

    public XEventContext(@NotNull XEvents plugin, @NotNull String moduleId, @NotNull MessageService messages) {
        this.plugin = plugin;
        this.moduleId = moduleId;
        this.messages = messages;
    }

    @NotNull
    public XEvents getPlugin() {
        return plugin;
    }

    @Nullable
    public ConfigurationSection getModuleConfig() {
        ConfigurationSection modules = plugin.getConfig().getConfigurationSection("modules");
        if (modules == null) {
            return null;
        }
        return modules.getConfigurationSection(moduleId);
    }

    @NotNull
    public MessageService messages() {
        return messages;
    }

    @NotNull
    public Logger logger() {
        return plugin.getLogger();
    }

    @NotNull
    public String getModuleId() {
        return moduleId;
    }

    @NotNull
    public Plugin asPlugin() {
        return plugin;
    }
}
