package net.xstream.xevents.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.xstream.xevents.XEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public final class MessageService {

    private final XEvents plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;
    private String prefix;

    public MessageService(@NotNull XEvents plugin) {
        this.plugin = plugin;
        reload();
    }

    
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);

        
        
        try (InputStream resource = plugin.getResource("messages.yml")) {
            if (resource != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(resource, StandardCharsets.UTF_8));
                this.messages.setDefaults(defaults);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Impossibile caricare i messaggi di default: " + e.getMessage());
        }

        this.prefix = messages.getString("prefix", "<gray>[<gold><b>XEvents</b></gold><gray>]</gray> ");
    }

    
    @NotNull
    public String raw(@NotNull String key) {
        String value = messages.getString(key);
        if (value == null) {
            return "<red>Messaggio mancante: " + key + "</red>";
        }
        return value;
    }

    
    @NotNull
    public Component get(@NotNull String key, @NotNull Object... placeholders) {
        return render(prefix + raw(key), placeholders);
    }

    
    @NotNull
    public Component getRaw(@NotNull String key, @NotNull Object... placeholders) {
        return render(raw(key), placeholders);
    }

    private Component render(String text, Object... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("I placeholder devono essere passati in coppie chiave/valore.");
        }
        List<TagResolver> resolvers = new ArrayList<>();
        for (int i = 0; i < placeholders.length; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String value = String.valueOf(placeholders[i + 1]);
            resolvers.add(Placeholder.parsed(key, value));
        }
        return miniMessage.deserialize(text, TagResolver.resolver(resolvers));
    }

    
    public void send(@NotNull CommandSender target, @NotNull String key, @NotNull Object... placeholders) {
        target.sendMessage(get(key, placeholders));
    }

    @NotNull
    public MiniMessage miniMessage() {
        return miniMessage;
    }

    @NotNull
    public Component prefixComponent() {
        return miniMessage.deserialize(prefix);
    }
}
