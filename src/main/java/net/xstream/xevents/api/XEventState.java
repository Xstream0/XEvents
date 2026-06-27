package net.xstream.xevents.api;


public enum XEventState {

    
    IDLE,

    
    SETUP,

    
    WAITING,

    
    RUNNING,

    
    ENDING;

    public boolean isActive() {
        return this == WAITING || this == RUNNING || this == ENDING;
    }
}
