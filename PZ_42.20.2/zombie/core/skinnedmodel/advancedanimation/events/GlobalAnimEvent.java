// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public enum GlobalAnimEvent {
    Turn180Started("Turn180Started"),
    Turn180TargetChanged("Turn180TargetChanged");

    private final AnimEvent animEvent = new AnimEvent();

    private GlobalAnimEvent(final String eventName) {
        this.animEvent.eventName = eventName;
        this.animEvent.time = AnimEvent.AnimEventTime.END;
    }

    public AnimEvent getAnimEvent() {
        return this.animEvent;
    }
}
