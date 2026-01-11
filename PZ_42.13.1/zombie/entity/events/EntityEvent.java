// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.events;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.entity.GameEntity;

@UsedFromLua
public class EntityEvent {
    protected static final ConcurrentLinkedDeque<EntityEvent> pool = new ConcurrentLinkedDeque<>();
    private EntityEventType eventType;
    private GameEntity entity;

    public static EntityEvent Alloc(EntityEventType type, GameEntity entity) {
        EntityEvent object = pool.poll();
        if (object == null) {
            object = new EntityEvent();
        }

        object.eventType = type;
        object.entity = entity;
        return object;
    }

    private EntityEvent() {
    }

    public EntityEventType getEventType() {
        return this.eventType;
    }

    public GameEntity getEntity() {
        return this.entity;
    }

    protected void reset() {
        this.entity = null;
    }

    public void release() {
        this.reset();

        assert !Core.debug || !pool.contains(this) : "Object already in pool.";

        pool.offer(this);
    }
}
