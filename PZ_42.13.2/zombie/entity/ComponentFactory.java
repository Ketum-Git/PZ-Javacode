// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.reflect.ClassReflection;
import zombie.entity.util.reflect.Constructor;
import zombie.entity.util.reflect.ReflectionException;

public class ComponentFactory {
    private final ObjectMap<Class<?>, ComponentFactory.ComponentPool> pools = new ObjectMap<>();
    private final int initialSize;
    private final int maxSize;

    public ComponentFactory() {
        this(1024, Integer.MAX_VALUE);
    }

    public ComponentFactory(int initialSize, int maxSize) {
        this.initialSize = initialSize;
        this.maxSize = maxSize;
    }

    public <T extends Component> T alloc(Class<T> componentClass) {
        ComponentFactory.ComponentPool pool = this.pools.get(componentClass);
        if (pool == null) {
            pool = new ComponentFactory.ComponentPool<>(componentClass, this.initialSize, this.maxSize);
            this.pools.put(componentClass, pool);
        }

        return (T)pool.obtain();
    }

    public <T extends Component> void release(T component) {
        if (component == null) {
            throw new IllegalArgumentException("component cannot be null.");
        } else {
            ComponentFactory.ComponentPool pool = this.pools.get(component.getClass());
            if (pool != null) {
                assert !Core.debug || !pool.pool.contains(component) : "Object already in pool.";

                if (component.owner != null) {
                    DebugLog.General.error("Owner not removed?");
                    if (Core.debug) {
                        throw new RuntimeException("Owner not removed");
                    }

                    component.owner.removeComponent(component);
                }

                pool.free(component);
            }
        }
    }

    private static class ComponentPool<T extends Component> {
        protected final ConcurrentLinkedDeque<T> pool;
        private final Constructor constructor;
        private final int max;
        private int peak;
        private final AtomicInteger atomicSize = new AtomicInteger();

        public ComponentPool(Class<T> type) {
            this(type, 16, Integer.MAX_VALUE);
        }

        public ComponentPool(Class<T> type, int initialCapacity) {
            this(type, initialCapacity, Integer.MAX_VALUE);
        }

        public ComponentPool(Class<T> type, int initialCapacity, int max) {
            this.max = max;
            this.pool = new ConcurrentLinkedDeque<>();
            this.constructor = this.findConstructor(type);
            if (this.constructor == null) {
                throw new RuntimeException("Class cannot be created (missing no-arg constructor): " + type.getName());
            }
        }

        private @Nullable Constructor findConstructor(Class<T> type) {
            try {
                return ClassReflection.getConstructor(type);
            } catch (Exception var5) {
                try {
                    Constructor constructor = ClassReflection.getDeclaredConstructor(type);
                    constructor.setAccessible(true);
                    return constructor;
                } catch (ReflectionException var4) {
                    return null;
                }
            }
        }

        public T obtain() {
            T o = this.pool.poll();
            if (o == null) {
                o = this.newObject();
            } else {
                this.atomicSize.decrementAndGet();
            }

            return o;
        }

        public void free(T o) {
            if (o == null) {
                throw new IllegalArgumentException("object cannot be null.");
            } else {
                if (this.atomicSize.get() < this.max) {
                    o.reset();
                    this.pool.add(o);
                    this.peak = Math.max(this.peak, this.atomicSize.incrementAndGet());
                } else {
                    o.reset();
                }
            }
        }

        protected T newObject() {
            try {
                return (T)this.constructor.newInstance((Object[])null);
            } catch (Exception var2) {
                throw new RuntimeException("Unable to create new instance: " + this.constructor.getDeclaringClass().getName(), var2);
            }
        }
    }
}
