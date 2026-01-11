// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import zombie.core.math.PZMath;

public final class ObjectCache<E> {
    final AtomicInteger size = new AtomicInteger(0);
    final ConcurrentLinkedQueue<ObjectCache<E>.ObjectCacheList> queue = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<ObjectCache<E>.ObjectCacheList> pool = new ConcurrentLinkedQueue<>();
    final ThreadLocal<ObjectCache<E>.ObjectCacheList> tl = new ThreadLocal<>();

    public int size() {
        return this.size.get();
    }

    public ObjectCache<E>.ObjectCacheList popList() {
        ObjectCache<E>.ObjectCacheList list = this.pool.poll();
        if (list == null) {
            list = new ObjectCache.ObjectCacheList();
        }

        list.clear();
        return list;
    }

    public void push(E object) {
        ObjectCache<E>.ObjectCacheList list = this.queue.poll();
        if (list == null) {
            list = this.pool.poll();
        }

        if (list == null) {
            list = new ObjectCache.ObjectCacheList();
        }

        list.add(object);
        this.size.getAndAdd(1);
        this.queue.add(list);
    }

    public void push(List<E> objects) {
        for (int i = 0; i < objects.size(); i += 128) {
            ObjectCache<E>.ObjectCacheList list = this.pool.poll();
            if (list == null) {
                list = new ObjectCache.ObjectCacheList();
            }

            list.clear();
            int max = PZMath.min(128, objects.size() - i);

            for (int j = 0; j < max; j++) {
                list.add(objects.get(i + j));
            }

            this.size.getAndAdd(list.size());
            this.queue.add(list);
        }
    }

    public void push(ObjectCache<E>.ObjectCacheList list) {
        if (list.isEmpty()) {
            this.pool.add(list);
        } else {
            this.size.getAndAdd(list.size());
            this.queue.add(list);
        }
    }

    public E pop() {
        ObjectCache<E>.ObjectCacheList list = this.tl.get();
        if (list == null) {
            list = this.queue.poll();
        }

        if (list == null) {
            return null;
        } else {
            E e = list.remove(list.size() - 1);
            this.size.getAndDecrement();
            if (list.isEmpty()) {
                this.tl.set(null);
                this.pool.add(list);
            } else {
                this.tl.set(list);
            }

            return e;
        }
    }

    public final class ObjectCacheList extends ArrayList<E> {
        public ObjectCacheList() {
            Objects.requireNonNull(ObjectCache.this);
            super();
        }
    }
}
