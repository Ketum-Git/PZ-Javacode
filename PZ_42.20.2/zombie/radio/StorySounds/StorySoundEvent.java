// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.StorySounds;

import java.util.ArrayList;
import zombie.UsedFromLua;

/**
 * Turbo
 */
@UsedFromLua
public final class StorySoundEvent {
    protected String name;
    protected ArrayList<EventSound> eventSounds = new ArrayList<>();

    public StorySoundEvent() {
        this("Unnamed");
    }

    public StorySoundEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<EventSound> getEventSounds() {
        return this.eventSounds;
    }

    public void setEventSounds(ArrayList<EventSound> eventSounds) {
        this.eventSounds = eventSounds;
    }
}
