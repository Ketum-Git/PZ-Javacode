// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.entity.util.Array;
import zombie.entity.util.BitSet;
import zombie.entity.util.SnapshotArray;

public final class ComponentContainer {
    private static final ComponentContainer.ComponentRenderComparator renderComparator = new ComponentContainer.ComponentRenderComparator();
    private static final boolean ENABLE_POOLING = true;
    private static final int MAX_POOL_SIZE = -1;
    private static final ConcurrentLinkedDeque<ComponentContainer> array_pool = new ConcurrentLinkedDeque<>();
    private final Array<Component> componentList = new Array<>(false, 8);
    private final Component[] componentArray = new Component[ComponentType.MAX_ID_INDEX];
    private final SnapshotArray<Component> renderersArray = new SnapshotArray<>(false, 16, Component.class);
    private boolean dirtyRenderers;
    private ComponentOperationHandler componentOperationHandler;
    private final BitSet componentBits = new BitSet();
    private final BitSet bucketBits = new BitSet();
    private GameEntity entity;

    public static ComponentContainer Alloc(GameEntity entity) {
        ComponentContainer o = array_pool.poll();
        if (o == null) {
            o = new ComponentContainer();
        }

        o.entity = entity;
        return o;
    }

    public static void Release(ComponentContainer o) {
        if (o.size() > 0 && Core.debug) {
            throw new RuntimeException("Releasing ComponentContainer which has contents that might not have been properly disposed.");
        } else {
            array_pool.offer(o);
        }
    }

    private ComponentContainer() {
    }

    BitSet getComponentBits() {
        return this.componentBits;
    }

    BitSet getBucketBits() {
        return this.bucketBits;
    }

    int size() {
        return this.componentList.size;
    }

    int getCapacity() {
        return this.componentArray.length;
    }

    boolean isEmpty() {
        return this.componentList.size == 0;
    }

    ComponentOperationHandler getComponentOperationHandler() {
        return this.componentOperationHandler;
    }

    void setComponentOperationHandler(ComponentOperationHandler handler) {
        this.componentOperationHandler = handler;
    }

    boolean isIdenticalTo(ComponentContainer other) {
        if (other == null) {
            return false;
        } else {
            return other == this ? true : this.componentBits.equals(other.componentBits);
        }
    }

    Component get(ComponentType componentType) {
        return this.componentArray[componentType.GetID()];
    }

    Component getForIndex(int index) {
        return this.componentList.get(index);
    }

    Component removeIndex(int index) {
        Component component = this.componentList.get(index);
        if (component != null) {
            this.remove(component.getComponentType());
        }

        return component;
    }

    boolean removeComponent(Component component) {
        if (component == null) {
            return false;
        } else {
            Component stored = this.get(component.getComponentType());
            if (stored == component) {
                this.remove(component.getComponentType());
                return true;
            } else {
                return false;
            }
        }
    }

    Component remove(ComponentType componentType) {
        Component component = this.componentArray[componentType.GetID()];
        if (component != null) {
            this.componentArray[componentType.GetID()] = null;
            this.componentList.removeValue(component, true);
            this.componentBits.clear(componentType.GetID());
            if (component.isRenderLast()) {
                this.renderersArray.removeValue(component, true);
                this.dirtyRenderers = true;
            }

            if (this.componentOperationHandler != null) {
                this.componentOperationHandler.remove(this.entity);
            }
        }

        return component;
    }

    boolean contains(ComponentType componentType) {
        return this.componentArray[componentType.GetID()] != null;
    }

    boolean contains(Component component) {
        return component != null && this.contains(component.getComponentType());
    }

    void add(Component component) {
        if (component != null) {
            Component stored = this.get(component.getComponentType());
            if (stored != null) {
                if (stored == component) {
                    return;
                }

                this.remove(component.getComponentType());
            }

            this.componentArray[component.getComponentType().GetID()] = component;
            this.componentList.add(component);
            this.componentBits.set(component.getComponentType().GetID());
            if (component.isRenderLast()) {
                this.renderersArray.add(component);
                this.dirtyRenderers = true;
            }

            if (this.componentOperationHandler != null) {
                this.componentOperationHandler.add(this.entity);
            }
        }
    }

    void release() {
        if (this.entity == null || !this.entity.addedToEntityManager && !this.entity.addedToEngine) {
            if (!this.bucketBits.isEmpty() && Core.debug) {
                throw new IllegalStateException("Entity is still registered to buckets?");
            } else {
                for (int i = 0; i < this.componentList.size; i++) {
                    Component component = this.componentList.get(i);
                    component.setOwner(null);
                    ComponentType.ReleaseComponent(component);
                }

                Arrays.fill(this.componentArray, null);
                this.componentList.clear();
                this.renderersArray.clear();
                this.componentBits.clear();
                this.bucketBits.clear();
                if (this.componentOperationHandler != null) {
                    this.componentOperationHandler = null;
                    if (Core.debug) {
                        throw new IllegalStateException("ComponentHandler should be null.");
                    }
                }

                this.dirtyRenderers = false;
                this.entity = null;
            }
        } else {
            throw new IllegalStateException("Engine should be removed from engine and manager.");
        }
    }

    boolean hasRenderers() {
        return this.renderersArray.size > 0;
    }

    void render() {
        if (this.dirtyRenderers) {
            this.renderersArray.sort(renderComparator);
            this.dirtyRenderers = false;
        }

        if (this.renderersArray.size == 1) {
            this.renderersArray.items[0].renderlast();
        } else if (this.renderersArray.size > 1) {
            Component[] renderers = this.renderersArray.begin();

            try {
                int size = this.renderersArray.size;

                for (int i = 0; i < size; i++) {
                    if (renderers[i].isValid()) {
                        renderers[i].renderlast();
                    }
                }
            } finally {
                this.renderersArray.end();
            }
        }
    }

    private static class ComponentRenderComparator implements Comparator<Component> {
        public int compare(Component e1, Component e2) {
            return (int)Math.signum((float)(e1.getRenderLastPriority() - e2.getRenderLastPriority()));
        }
    }
}
