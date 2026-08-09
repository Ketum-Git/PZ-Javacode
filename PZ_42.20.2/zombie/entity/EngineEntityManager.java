// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.Objects;
import zombie.core.Core;
import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectSet;
import zombie.entity.util.SingleThreadPool;

public final class EngineEntityManager {
    private final EntityBucketManager bucketManager;
    private final Array<GameEntity> entities = new Array<>(false, 16);
    private final ObjectSet<GameEntity> entitySet = new ObjectSet<>();
    private final ImmutableArray<GameEntity> immutableEntities = new ImmutableArray<>(this.entities);
    private final Array<EngineEntityManager.EntityOperation> pendingOperations = new Array<>(false, 16);
    private final EngineEntityManager.EntityOperationPool entityOperationPool = new EngineEntityManager.EntityOperationPool();
    private final ComponentOperationHandler componentOperationHandler;
    private final IBooleanInformer delayed;
    private final IBucketInformer bucketsUpdating;
    private final Engine engine;

    protected EngineEntityManager(Engine engine, IBooleanInformer delayed) {
        this.engine = engine;
        this.bucketManager = new EntityBucketManager(this.immutableEntities);
        this.bucketsUpdating = this.bucketManager.getBucketsUpdatingInformer();
        this.delayed = delayed;
        this.componentOperationHandler = new ComponentOperationHandler(this.delayed, this.bucketsUpdating, new EngineEntityManager.ComponentOperationListener());
    }

    EntityBucketManager getBucketManager() {
        return this.bucketManager;
    }

    void addEntity(GameEntity entity) {
        if (!this.delayed.value() && !this.bucketsUpdating.value()) {
            this.addEntityInternal(entity);
        } else {
            if (entity.scheduledForEngineRemoval || entity.removingFromEngine) {
                throw new IllegalArgumentException("Entity is scheduled for removal.");
            }

            if (entity.addedToEngine) {
                if (Core.debug) {
                    throw new IllegalArgumentException("Entity has already been added to Engine.");
                }

                return;
            }

            entity.addedToEngine = true;
            entity.scheduledDelayedAddToEngine = true;
            EngineEntityManager.EntityOperation operation = this.entityOperationPool.obtain();
            operation.entity = entity;
            operation.type = EngineEntityManager.EntityOperation.Type.Add;
            this.pendingOperations.add(operation);
        }
    }

    void removeEntity(GameEntity entity) {
        if (!this.delayed.value() && !this.bucketsUpdating.value()) {
            this.removeEntityInternal(entity);
        } else {
            if (entity.scheduledForEngineRemoval) {
                return;
            }

            entity.scheduledForEngineRemoval = true;
            EngineEntityManager.EntityOperation operation = this.entityOperationPool.obtain();
            operation.entity = entity;
            operation.type = EngineEntityManager.EntityOperation.Type.Remove;
            this.pendingOperations.add(operation);
        }
    }

    void removeAllEntities() {
        this.removeAllEntities(this.immutableEntities);
    }

    void removeAllEntities(ImmutableArray<GameEntity> entities) {
        if (!this.delayed.value() && !this.bucketsUpdating.value()) {
            while (entities.size() > 0) {
                this.removeEntityInternal(entities.first());
            }
        } else {
            for (GameEntity entity : entities) {
                entity.scheduledForEngineRemoval = true;
            }

            EngineEntityManager.EntityOperation operation = this.entityOperationPool.obtain();
            operation.type = EngineEntityManager.EntityOperation.Type.RemoveAll;
            operation.entities = entities;
            this.pendingOperations.add(operation);
        }
    }

    ImmutableArray<GameEntity> getEntities() {
        return this.immutableEntities;
    }

    boolean hasPendingOperations() {
        return this.pendingOperations.size > 0;
    }

    void processPendingOperations() {
        for (int i = 0; i < this.pendingOperations.size; i++) {
            EngineEntityManager.EntityOperation operation = this.pendingOperations.get(i);
            switch (operation.type) {
                case Add:
                    this.addEntityInternal(operation.entity);
                    break;
                case Remove:
                    this.removeEntityInternal(operation.entity);
                    break;
                case RemoveAll:
                    while (operation.entities.size() > 0) {
                        this.removeEntityInternal(operation.entities.first());
                    }
                    break;
                default:
                    throw new AssertionError("Unexpected EntityOperation type");
            }

            this.entityOperationPool.free(operation);
        }

        this.pendingOperations.clear();
    }

    void updateOperations() {
        while (this.componentOperationHandler.hasOperationsToProcess() || this.hasPendingOperations()) {
            this.componentOperationHandler.processOperations();
            this.processPendingOperations();
        }
    }

    void addEntityInternal(GameEntity entity) {
        if (this.entitySet.contains(entity)) {
            throw new IllegalArgumentException("Entity is already registered " + entity);
        } else {
            entity.scheduledDelayedAddToEngine = false;
            this.entities.add(entity);
            this.entitySet.add(entity);
            entity.setComponentOperationHandler(this.componentOperationHandler);
            entity.addedToEngine = true;
            this.bucketManager.updateBucketMembership(entity);
            this.engine.onEntityAdded(entity);
        }
    }

    void removeEntityInternal(GameEntity entity) {
        boolean removed = this.entitySet.remove(entity);
        if (removed) {
            entity.scheduledForEngineRemoval = false;
            entity.removingFromEngine = true;
            this.entities.removeValue(entity, true);
            this.bucketManager.updateBucketMembership(entity);
            entity.setComponentOperationHandler(null);
            entity.removingFromEngine = false;
            entity.addedToEngine = false;
            this.engine.onEntityRemoved(entity);
        }
    }

    private class ComponentOperationListener implements ComponentOperationHandler.OperationListener {
        private ComponentOperationListener() {
            Objects.requireNonNull(EngineEntityManager.this);
            super();
        }

        @Override
        public void componentsChanged(GameEntity entity) {
            EngineEntityManager.this.bucketManager.updateBucketMembership(entity);
        }
    }

    private static class EntityOperation implements SingleThreadPool.Poolable {
        EngineEntityManager.EntityOperation.Type type;
        GameEntity entity;
        ImmutableArray<GameEntity> entities;

        @Override
        public void reset() {
            this.entity = null;
        }

        public static enum Type {
            Add,
            Remove,
            RemoveAll;
        }
    }

    private static class EntityOperationPool extends SingleThreadPool<EngineEntityManager.EntityOperation> {
        protected EngineEntityManager.EntityOperation newObject() {
            return new EngineEntityManager.EntityOperation();
        }
    }
}
