package net.xstream.xevents.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class XEventManager {

    private final Map<String, XEventModule> modules = new LinkedHashMap<>();

    @Nullable
    private String activeModuleId = null;

    public void register(@NotNull XEventModule module) {
        String id = module.getId().toLowerCase();
        if (modules.containsKey(id)) {
            throw new IllegalArgumentException("Un modulo con id '" + id + "' è già registrato.");
        }
        modules.put(id, module);
    }

    public void unregister(@NotNull String id) {
        XEventModule module = modules.remove(id.toLowerCase());
        if (module != null) {
            if (id.equalsIgnoreCase(activeModuleId)) {
                activeModuleId = null;
            }
            module.onUnregister();
        }
    }

    public void unregisterAll() {
        for (XEventModule module : modules.values()) {
            module.onUnregister();
        }
        modules.clear();
        activeModuleId = null;
    }

    @NotNull
    public Optional<XEventModule> getModule(@NotNull String id) {
        return Optional.ofNullable(modules.get(id.toLowerCase()));
    }

    @NotNull
    public Collection<XEventModule> getModules() {
        return modules.values();
    }

    public boolean trySetActive(@NotNull String id) {
        if (activeModuleId != null && !activeModuleId.equalsIgnoreCase(id)) {
            return false;
        }
        if (!modules.containsKey(id.toLowerCase())) {
            return false;
        }
        activeModuleId = id.toLowerCase();
        return true;
    }

    public void clearActiveIfMatches(@NotNull String id) {
        if (activeModuleId != null && activeModuleId.equalsIgnoreCase(id)) {
            activeModuleId = null;
        }
    }

    public boolean hasActiveModule() {
        return activeModuleId != null;
    }

    @NotNull
    public Optional<XEventModule> getActiveModule() {
        if (activeModuleId == null) {
            return Optional.empty();
        }
        return getModule(activeModuleId);
    }

    @NotNull
    public Optional<XEventModule> findModuleWithParticipant(@NotNull Player player) {
        return getActiveModule().filter(module -> module.isParticipant(player));
    }
}
