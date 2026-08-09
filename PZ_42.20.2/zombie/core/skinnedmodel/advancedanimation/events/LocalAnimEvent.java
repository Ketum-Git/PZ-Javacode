// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public enum LocalAnimEvent {
    ActiveAnimLoopedEvent("ActiveAnimLooped", 1.0F),
    ActiveNonLoopedAnimFadeOutEvent("NonLoopedAnimFadeOut", 1.0F),
    ActiveAnimFinishingEvent("ActiveAnimFinishing", AnimEvent.AnimEventTime.END),
    ActiveNonLoopedAnimFinishedEvent("ActiveAnimFinished", AnimEvent.AnimEventTime.END),
    NoAnimConditionsPass("NoAnimConditionsPass", AnimEvent.AnimEventTime.END);

    private final AnimEvent animEvent = new AnimEvent();

    private LocalAnimEvent(final String eventName, final float timePc) {
        this.animEvent.timePc = timePc;
        this.animEvent.eventName = eventName;
    }

    private LocalAnimEvent(final String eventName, final AnimEvent.AnimEventTime time) {
        this.animEvent.time = time;
        this.animEvent.eventName = eventName;
    }

    public AnimEvent getAnimEvent() {
        return this.animEvent;
    }
}
