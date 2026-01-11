// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.events;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.entity.Component;

@UsedFromLua
public class ComponentEvent {
    protected static final ConcurrentLinkedDeque<ComponentEvent> pool = new ConcurrentLinkedDeque<>();
    private ComponentEventType eventType;
    private Component sender;

    public static ComponentEvent Alloc(ComponentEventType type, Component sender) {
        ComponentEvent object = pool.poll();
        if (object == null) {
            object = new ComponentEvent();
        }

        object.eventType = type;
        object.sender = sender;
        return object;
    }

    private ComponentEvent() {
    }

    public ComponentEventType getEventType() {
        return this.eventType;
    }

    public Component getSender() {
        return this.sender;
    }

    protected void reset() {
        this.sender = null;
    }

    public void release() {
        this.reset();

        assert !Core.debug || !pool.contains(this) : "Object already in pool.";

        pool.offer(this);
    }
}
