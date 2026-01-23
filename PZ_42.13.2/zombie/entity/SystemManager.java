// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.Comparator;
import java.util.Objects;
import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.SingleThreadPool;

public final class SystemManager {
    private final SystemManager.SystemUpdateComparator systemUpdateComparator = new SystemManager.SystemUpdateComparator();
    private final SystemManager.SystemUpdateSimulationComparator systemUpdateSimulationComparator = new SystemManager.SystemUpdateSimulationComparator();
    private final SystemManager.SystemRenderComparator systemRenderComparator = new SystemManager.SystemRenderComparator();
    private final Array<EngineSystem> systems = new Array<>(false, 16);
    private final Array<EngineSystem> updaterSystems = new Array<>(true, 16);
    private final Array<EngineSystem> simulationUpdaterSystems = new Array<>(true, 16);
    private final Array<EngineSystem> rendererSystems = new Array<>(true, 16);
    private final ImmutableArray<EngineSystem> immutableSystems = new ImmutableArray<>(this.systems);
    private final ImmutableArray<EngineSystem> immutableUpdaterSystems = new ImmutableArray<>(this.updaterSystems);
    private final ImmutableArray<EngineSystem> immutableSimulationUpdaterSystems = new ImmutableArray<>(this.simulationUpdaterSystems);
    private final ImmutableArray<EngineSystem> immutableRendererSystems = new ImmutableArray<>(this.rendererSystems);
    private final ObjectMap<Class<?>, EngineSystem> systemsByClass = new ObjectMap<>();
    private final Engine engine;
    private final Array<SystemManager.SystemOperation> pendingOperations = new Array<>(false, 16);
    private final SystemManager.SystemOperationPool systemOperationPool = new SystemManager.SystemOperationPool();
    private final SystemManager.SystemMembershipListener systemMembershipListener = new SystemManager.SystemMembershipListener();
    private final boolean enableDynamicSystems;

    protected SystemManager(Engine engine, boolean enableDynamicSystems) {
        this.engine = engine;
        this.enableDynamicSystems = enableDynamicSystems;
    }

    void addSystem(EngineSystem system) {
        if (this.engine.isProcessing()) {
            if (!this.enableDynamicSystems) {
                throw new UnsupportedOperationException("Cannot modify systems while the Engine is processing.");
            }

            this.addSystemDelayed(system);
        } else {
            this.addSystemInternal(system);
        }
    }

    void addSystemDelayed(EngineSystem system) {
        for (int i = 0; i < this.pendingOperations.size; i++) {
            SystemManager.SystemOperation operation = this.pendingOperations.get(i);
            if (operation.system == system) {
                operation.valid = false;
            }
        }

        SystemManager.SystemOperation operation = this.systemOperationPool.obtain();
        operation.system = system;
        operation.type = SystemManager.SystemOperation.Type.Add;
        this.pendingOperations.add(operation);
    }

    void addSystemInternal(EngineSystem system) {
        Class<? extends EngineSystem> systemType = (Class<? extends EngineSystem>)system.getClass();
        EngineSystem oldSytem = this.getSystem(systemType);
        if (oldSytem == system) {
            this.updateSystemMembership(system);
        } else {
            if (oldSytem != null) {
                this.removeSystem(oldSytem);
            }

            system.membershipListener = this.systemMembershipListener;
            this.systems.add(system);
            this.systemsByClass.put(systemType, system);
            this.updateSystemMembership(system);
            system.addedToEngineInternal(this.engine);
        }
    }

    void removeSystem(EngineSystem system) {
        if (this.engine.isProcessing()) {
            if (!this.enableDynamicSystems) {
                throw new UnsupportedOperationException("Cannot modify systems while the Engine is processing.");
            }

            this.removeSystemDelayed(system);
        } else {
            this.removeSystemInternal(system);
        }
    }

    void removeSystemDelayed(EngineSystem system) {
        for (int i = 0; i < this.pendingOperations.size; i++) {
            SystemManager.SystemOperation operation = this.pendingOperations.get(i);
            if (operation.system == system) {
                operation.valid = false;
            }
        }

        SystemManager.SystemOperation operation = this.systemOperationPool.obtain();
        operation.system = system;
        operation.type = SystemManager.SystemOperation.Type.Remove;
        this.pendingOperations.add(operation);
    }

    void removeSystemInternal(EngineSystem system) {
        if (this.systems.removeValue(system, true)) {
            system.membershipListener = null;
            this.systemsByClass.remove(system.getClass());
            this.updaterSystems.removeValue(system, true);
            this.simulationUpdaterSystems.removeValue(system, true);
            this.rendererSystems.removeValue(system, true);
            system.removedFromEngineInternal(this.engine);
        }
    }

    void removeAllSystems() {
        while (this.systems.size > 0) {
            this.removeSystem(this.systems.first());
        }
    }

    <T extends EngineSystem> T getSystem(Class<T> systemType) {
        return (T)this.systemsByClass.get(systemType);
    }

    ImmutableArray<EngineSystem> getSystems() {
        return this.immutableSystems;
    }

    ImmutableArray<EngineSystem> getUpdaterSystems() {
        return this.immutableUpdaterSystems;
    }

    ImmutableArray<EngineSystem> getSimulationUpdaterSystems() {
        return this.immutableSimulationUpdaterSystems;
    }

    ImmutableArray<EngineSystem> getRendererSystems() {
        return this.immutableRendererSystems;
    }

    boolean hasPendingOperations() {
        return this.pendingOperations.size > 0;
    }

    void processPendingOperations() {
        for (int i = 0; i < this.pendingOperations.size; i++) {
            SystemManager.SystemOperation operation = this.pendingOperations.get(i);
            if (!operation.valid) {
                this.systemOperationPool.free(operation);
            } else {
                switch (operation.type) {
                    case Add:
                        this.addSystemInternal(operation.system);
                        break;
                    case Remove:
                        this.removeSystemInternal(operation.system);
                        break;
                    case UpdateMembership:
                        this.updateSystemMembership(operation.system);
                        break;
                    default:
                        throw new AssertionError("Unexpected SystemOperation type");
                }

                this.systemOperationPool.free(operation);
            }
        }

        this.pendingOperations.clear();
    }

    private void updateSystemMembership(EngineSystem system) {
        boolean enabled = system.isEnabled();
        boolean updater = system.isUpdater();
        if (enabled && updater && !this.updaterSystems.contains(system, true)) {
            this.updaterSystems.add(system);
        } else if ((!enabled || !updater) && this.updaterSystems.contains(system, true)) {
            this.updaterSystems.removeValue(system, true);
        }

        boolean simulationUpdater = system.isSimulationUpdater();
        if (enabled && simulationUpdater && !this.simulationUpdaterSystems.contains(system, true)) {
            this.simulationUpdaterSystems.add(system);
        } else if ((!enabled || !simulationUpdater) && this.simulationUpdaterSystems.contains(system, true)) {
            this.simulationUpdaterSystems.removeValue(system, true);
        }

        boolean renderer = system.isRenderer();
        if (enabled && renderer && !this.rendererSystems.contains(system, true)) {
            this.rendererSystems.add(system);
        } else if ((!enabled || !renderer) && this.rendererSystems.contains(system, true)) {
            this.rendererSystems.removeValue(system, true);
        }

        this.updaterSystems.sort(this.systemUpdateComparator);
        this.simulationUpdaterSystems.sort(this.systemUpdateSimulationComparator);
        this.rendererSystems.sort(this.systemRenderComparator);
    }

    private class SystemMembershipListener implements EngineSystem.MembershipListener {
        private SystemMembershipListener() {
            Objects.requireNonNull(SystemManager.this);
            super();
        }

        @Override
        public void onMembershipPropertyChanged(EngineSystem system) {
            for (int i = 0; i < SystemManager.this.pendingOperations.size; i++) {
                SystemManager.SystemOperation operation = SystemManager.this.pendingOperations.get(i);
                if (operation.system == system && operation.valid) {
                    return;
                }
            }

            SystemManager.SystemOperation operation = SystemManager.this.systemOperationPool.obtain();
            operation.system = system;
            operation.type = SystemManager.SystemOperation.Type.UpdateMembership;
            SystemManager.this.pendingOperations.add(operation);
        }
    }

    private static class SystemOperation implements SingleThreadPool.Poolable {
        SystemManager.SystemOperation.Type type;
        EngineSystem system;
        boolean valid = true;

        @Override
        public void reset() {
            this.system = null;
            this.valid = true;
        }

        public static enum Type {
            Add,
            Remove,
            UpdateMembership;
        }
    }

    private static class SystemOperationPool extends SingleThreadPool<SystemManager.SystemOperation> {
        protected SystemManager.SystemOperation newObject() {
            return new SystemManager.SystemOperation();
        }
    }

    private static class SystemRenderComparator implements Comparator<EngineSystem> {
        public int compare(EngineSystem a, EngineSystem b) {
            return Integer.compare(a.getRenderLastPriority(), b.getRenderLastPriority());
        }
    }

    private static class SystemUpdateComparator implements Comparator<EngineSystem> {
        public int compare(EngineSystem a, EngineSystem b) {
            return Integer.compare(a.getUpdatePriority(), b.getUpdatePriority());
        }
    }

    private static class SystemUpdateSimulationComparator implements Comparator<EngineSystem> {
        public int compare(EngineSystem a, EngineSystem b) {
            return Integer.compare(a.getUpdateSimulationPriority(), b.getUpdateSimulationPriority());
        }
    }
}
