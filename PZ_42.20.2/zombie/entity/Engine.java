// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.Objects;
import zombie.debug.DebugLog;
import zombie.entity.util.ImmutableArray;

public final class Engine {
    private final SystemManager systemManager;
    private final EngineEntityManager entityManager;
    private final EntityBucketManager bucketManager;
    private Engine.EntityListener entityListener;
    private boolean processing;

    public Engine() {
        boolean enableDynamicSystems = false;
        this.systemManager = new SystemManager(this, false);
        this.entityManager = new EngineEntityManager(this, new Engine.EngineDelayedInformer());
        this.bucketManager = this.entityManager.getBucketManager();
    }

    public boolean isProcessing() {
        return this.processing;
    }

    void setEntityListener(Engine.EntityListener listener) {
        this.entityListener = listener;
    }

    public EntityBucket getRendererBucket() {
        return this.bucketManager.getRendererBucket();
    }

    public EntityBucket getIsoObjectBucket() {
        return this.bucketManager.getIsoObjectBucket();
    }

    public EntityBucket getInventoryItemBucket() {
        return this.bucketManager.getInventoryItemBucket();
    }

    public EntityBucket getVehiclePartBucket() {
        return this.bucketManager.getVehiclePartBucket();
    }

    public EntityBucket getBucket(Family family) {
        return this.bucketManager.getBucket(family);
    }

    public EntityBucket registerCustomBucket(String identifier, EntityBucket.EntityValidator validator) {
        return this.bucketManager.registerCustomBucket(identifier, validator);
    }

    public EntityBucket getCustomBucket(String identifier) {
        return this.bucketManager.getCustomBucket(identifier);
    }

    boolean addEntity(GameEntity entity) {
        this.entityManager.addEntity(entity);
        return true;
    }

    void removeEntity(GameEntity entity) {
        this.entityManager.removeEntity(entity);
    }

    void removeAllEntities() {
        this.entityManager.removeAllEntities();
    }

    void onEntityAdded(GameEntity entity) {
        if (this.entityListener != null) {
            this.entityListener.onEntityAddedToEngine(entity);
        }
    }

    void onEntityRemoved(GameEntity entity) {
        if (this.entityListener != null) {
            this.entityListener.onEntityRemovedFromEngine(entity);
        }
    }

    void update() {
        if (this.processing) {
            throw new IllegalStateException("Cannot call update() engine is already processing.");
        } else {
            this.processing = true;

            try {
                while (this.systemManager.hasPendingOperations()) {
                    this.systemManager.processPendingOperations();
                }

                ImmutableArray<EngineSystem> systems = this.systemManager.getUpdaterSystems();

                for (int i = 0; i < systems.size(); i++) {
                    EngineSystem system = systems.get(i);
                    system.update();
                    this.entityManager.updateOperations();
                }
            } finally {
                this.processing = false;
            }
        }
    }

    void updateSimulation() {
        if (this.processing) {
            throw new IllegalStateException("Cannot call simulationUpdate() engine is already processing.");
        } else {
            this.processing = true;

            try {
                while (this.systemManager.hasPendingOperations()) {
                    this.systemManager.processPendingOperations();
                }

                ImmutableArray<EngineSystem> systems = this.systemManager.getSimulationUpdaterSystems();

                for (int i = 0; i < systems.size(); i++) {
                    EngineSystem system = systems.get(i);
                    system.updateSimulation();
                    this.entityManager.updateOperations();
                }
            } finally {
                this.processing = false;
            }
        }
    }

    void renderLast() {
        if (this.processing) {
            throw new IllegalStateException("Cannot call renderLast() engine is already processing.");
        } else {
            this.processing = true;

            try {
                ImmutableArray<EngineSystem> systems = this.systemManager.getRendererSystems();

                for (int i = 0; i < systems.size(); i++) {
                    EngineSystem system = systems.get(i);
                    system.renderLast();
                    this.entityManager.updateOperations();
                }
            } finally {
                this.processing = false;
            }
        }
    }

    ImmutableArray<GameEntity> getEntities() {
        return this.entityManager.getEntities();
    }

    <T extends EngineSystem> T addSystem(T system) {
        this.systemManager.addSystem(system);
        return system;
    }

    void removeSystem(EngineSystem system) {
        this.systemManager.removeSystem(system);
    }

    void removeAllSystems() {
        this.systemManager.removeAllSystems();
    }

    public <T extends EngineSystem> T getSystem(Class<T> systemType) {
        return this.systemManager.getSystem(systemType);
    }

    public ImmutableArray<EngineSystem> getSystems() {
        return this.systemManager.getSystems();
    }

    protected void printSystems() {
        DebugLog.log("=== Engine Registered Systems ===");
        ImmutableArray<EngineSystem> systems = this.systemManager.getSystems();

        for (int i = 0; i < systems.size(); i++) {
            DebugLog.log("[" + i + "] = " + systems.get(i).getClass().getSimpleName());
        }

        DebugLog.log("");
        DebugLog.log("- UPDATERS -");
        systems = this.systemManager.getUpdaterSystems();

        for (int i = 0; i < systems.size(); i++) {
            DebugLog.log("[" + i + "] = " + systems.get(i).getClass().getSimpleName());
        }

        DebugLog.log("");
        DebugLog.log("- RENDERERS -");
        systems = this.systemManager.getRendererSystems();

        for (int i = 0; i < systems.size(); i++) {
            DebugLog.log("[" + i + "] = " + systems.get(i).getClass().getSimpleName());
        }

        DebugLog.log("---------------------------------");
    }

    private class EngineDelayedInformer implements IBooleanInformer {
        private EngineDelayedInformer() {
            Objects.requireNonNull(Engine.this);
            super();
        }

        @Override
        public boolean value() {
            return Engine.this.processing;
        }
    }

    interface EntityListener {
        void onEntityAddedToEngine(GameEntity var1);

        void onEntityRemovedFromEngine(GameEntity var1);
    }
}
