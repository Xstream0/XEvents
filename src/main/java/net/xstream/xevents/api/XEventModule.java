package net.xstream.xevents.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface XEventModule {

    
    @NotNull String getId();

    @NotNull String getDisplayName();

    
    @NotNull String getDescription();

    
    @NotNull XEventState getState();

    
    void onRegister(@NotNull XEventContext context);

    
    void onUnregister();

    
    boolean isReadyToStart();

    
    @NotNull List<String> getSetupIssues();

    
    boolean start(@NotNull Player initiator);

    
    void stop();

    
    void forceStop();

    
    boolean join(@NotNull Player player);

    
    void leave(@NotNull Player player);

    
    boolean isParticipant(@NotNull Player player);
}
