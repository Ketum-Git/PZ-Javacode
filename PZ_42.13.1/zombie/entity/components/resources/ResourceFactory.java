// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.util.ObjectMap;

public class ResourceFactory {
    private static final ObjectMap<ResourceType, ResourceFactory.ResourcePool> pools = new ObjectMap<>();
    private static final int initialSize = 512;
    private static final int maxSize = Integer.MAX_VALUE;

    static Resource createResource(String blueprintSerial) {
        try {
            ResourceBlueprint blueprint = ResourceBlueprint.Deserialize(blueprintSerial);
            return createResource(blueprint);
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }

    static Resource createResource(ResourceBlueprint blueprint) {
        Resource resource = alloc(blueprint.getType());
        resource.loadBlueprint(blueprint);
        return resource;
    }

    static Resource createBlancResource(ResourceType resourceType) {
        return alloc(resourceType);
    }

    static void releaseResource(Resource resource) {
        release(resource);
    }

    private static <T extends Resource> T alloc(ResourceType type) {
        ResourceFactory.ResourcePool pool = pools.get(type);
        if (pool == null) {
            DebugLog.General.error("ResourceFactory.alloc can't found pool for '" + type + "'");
            pool = new ResourceFactory.ResourcePool<ResourceItem>(512, Integer.MAX_VALUE) {
                protected ResourceItem newObject() {
                    return new ResourceItem();
                }
            };
            pools.put(type, pool);
        }

        return (T)pool.obtain();
    }

    private static <T extends Resource> void release(T resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource cannot be null.");
        } else {
            ResourceFactory.ResourcePool pool = pools.get(resource.getType());
            if (pool != null) {
                assert !Core.debug || !pool.pool.contains(resource) : "Object already in pool.";

                if (resource.getResourcesComponent() != null) {
                    DebugLog.General.error("Resource not removed from Resources Component");
                    if (Core.debug) {
                    }

                    resource.getResourcesComponent().removeResource(resource);
                }

                pool.free(resource);
            }
        }
    }

    static {
        pools.put(ResourceType.Item, new ResourceFactory.ResourcePool<ResourceItem>(512, Integer.MAX_VALUE) {
            protected ResourceItem newObject() {
                return new ResourceItem();
            }
        });
        pools.put(ResourceType.Fluid, new ResourceFactory.ResourcePool<ResourceFluid>(512, Integer.MAX_VALUE) {
            protected ResourceFluid newObject() {
                return new ResourceFluid();
            }
        });
        pools.put(ResourceType.Energy, new ResourceFactory.ResourcePool<ResourceEnergy>(512, Integer.MAX_VALUE) {
            protected ResourceEnergy newObject() {
                return new ResourceEnergy();
            }
        });
    }

    private abstract static class ResourcePool<T extends Resource> {
        protected final ConcurrentLinkedDeque<T> pool;
        private final int max;
        private int peak;

        public ResourcePool() {
            this(16, Integer.MAX_VALUE);
        }

        public ResourcePool(int initialCapacity) {
            this(initialCapacity, Integer.MAX_VALUE);
        }

        public ResourcePool(int initialCapacity, int max) {
            this.max = max;
            this.pool = new ConcurrentLinkedDeque<>();
        }

        public T obtain() {
            T o = this.pool.poll();
            if (o == null) {
                o = this.newObject();
            }

            return o;
        }

        public void free(T o) {
            if (o == null) {
                throw new IllegalArgumentException("object cannot be null.");
            } else {
                if (this.pool.size() < this.max) {
                    o.reset();
                    this.pool.add(o);
                    this.peak = Math.max(this.peak, this.pool.size());
                } else {
                    o.reset();
                }
            }
        }

        protected abstract T newObject();
    }
}
