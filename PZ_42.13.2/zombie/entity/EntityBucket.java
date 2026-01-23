// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.Comparator;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.util.Array;
import zombie.entity.util.BitSet;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectSet;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public abstract class EntityBucket {
    private final Array<GameEntity> entities;
    private final ImmutableArray<GameEntity> immutableEntities;
    private final Array<EntityBucket.BucketListenerData> listeners = new Array<>(true, 16);
    private final ObjectSet<IBucketListener> listenerSet = new ObjectSet<>();
    private final EntityBucket.BucketListenerComparator listenerComparator = new EntityBucket.BucketListenerComparator();
    private final int index;
    private boolean verbose;

    private EntityBucket(int index) {
        this.entities = new Array<>(false, 16);
        this.immutableEntities = new ImmutableArray<>(this.entities);
        this.index = index;
    }

    public final int getIndex() {
        return this.index;
    }

    public final ImmutableArray<GameEntity> getEntities() {
        return this.immutableEntities;
    }

    public final void setVerbose(boolean b) {
        this.verbose = b;
    }

    protected abstract boolean acceptsEntity(GameEntity arg0);

    final void updateMembership(GameEntity entity) {
        BitSet bits = entity.getBucketBits();
        boolean containsEntity = bits.get(this.index);
        boolean acceptsEntity = this.acceptsEntity(entity);
        if (Core.debug && this.verbose) {
            DebugLog.Entity
                .println(
                    "testing entity = "
                        + entity.getEntityNetID()
                        + ", type="
                        + entity.getGameEntityType()
                        + ", contains="
                        + containsEntity
                        + ", accepts="
                        + acceptsEntity
                        + ", removing="
                        + entity.removingFromEngine
                );
        }

        if (!entity.removingFromEngine && !containsEntity && acceptsEntity) {
            if (Core.debug && this.verbose) {
                DebugLog.Entity.println("adding entity = " + entity.getEntityNetID() + ", type=" + entity.getGameEntityType());
            }

            if (Core.debug && GameEntityManager.debugMode && this.entities.contains(entity, true)) {
                throw new RuntimeException("Entity already exists in bucket.");
            }

            this.entities.add(entity);
            bits.set(this.index);
            if (Core.debug && this.verbose) {
                DebugLog.Entity.println("bits = " + bits.get(this.index));
            }

            if (this.listeners.size > 0) {
                for (int i = 0; i < this.listeners.size; i++) {
                    this.listeners.get(i).listener.onBucketEntityAdded(this, entity);
                }
            }
        } else if (containsEntity && (entity.removingFromEngine || !acceptsEntity)) {
            if (Core.debug && this.verbose) {
                DebugLog.Entity.println("removing entity = " + entity.getEntityNetID() + ", type=" + entity.getGameEntityType());
            }

            if (Core.debug && GameEntityManager.debugMode && !this.entities.contains(entity, true)) {
                throw new RuntimeException("Entity should exist in bucket but does not.");
            }

            this.entities.removeValue(entity, true);
            bits.clear(this.index);
            if (this.listeners.size > 0) {
                for (int i = 0; i < this.listeners.size; i++) {
                    this.listeners.get(i).listener.onBucketEntityRemoved(this, entity);
                }
            }
        }
    }

    public final void addListener(int priority, IBucketListener listener) {
        if (!this.listenerSet.contains(listener)) {
            EntityBucket.BucketListenerData data = new EntityBucket.BucketListenerData();
            data.listener = listener;
            data.priority = priority;
            this.listeners.add(data);
            this.listeners.sort(this.listenerComparator);
        }
    }

    public final void removeListener(IBucketListener listener) {
        if (this.listenerSet.remove(listener)) {
            for (int i = 0; i < this.listeners.size; i++) {
                if (this.listeners.get(i).listener == listener) {
                    this.listeners.removeIndex(i);
                    break;
                }
            }
        }
    }

    private static class BucketListenerComparator implements Comparator<EntityBucket.BucketListenerData> {
        public int compare(EntityBucket.BucketListenerData a, EntityBucket.BucketListenerData b) {
            return Integer.compare(a.priority, b.priority);
        }
    }

    private static class BucketListenerData {
        public IBucketListener listener;
        public int priority;
    }

    protected static class CustomBucket extends EntityBucket {
        private final EntityBucket.EntityValidator validator;

        protected CustomBucket(int index, EntityBucket.EntityValidator validator) {
            super(index);
            this.validator = validator;
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return this.validator.acceptsEntity(entity);
        }
    }

    public interface EntityValidator {
        boolean acceptsEntity(GameEntity var1);
    }

    protected static class FamilyBucket extends EntityBucket {
        private final Family family;

        protected FamilyBucket(int index, Family family) {
            super(index);
            this.family = family;
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return this.family.matches(entity);
        }
    }

    protected static class InventoryItemBucket extends EntityBucket {
        protected InventoryItemBucket(int index) {
            super(index);
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return entity instanceof InventoryItem;
        }
    }

    protected static class IsoObjectBucket extends EntityBucket {
        protected IsoObjectBucket(int index) {
            super(index);
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return entity instanceof IsoObject;
        }
    }

    protected static class RendererBucket extends EntityBucket {
        protected RendererBucket(int index) {
            super(index);
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return entity.hasRenderers();
        }
    }

    protected static class VehiclePartBucket extends EntityBucket {
        protected VehiclePartBucket(int index) {
            super(index);
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return entity instanceof VehiclePart;
        }
    }
}
