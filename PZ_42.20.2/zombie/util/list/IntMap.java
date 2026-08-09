// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.list;

import zombie.util.Pool;
import zombie.util.PooledObject;

public class IntMap<E> extends PooledObject {
    private int count;
    private int[] keys;
    private Object[] elements;
    private static final Pool<IntMap<?>> s_pool = new Pool<>(IntMap::new);

    public static <ET> IntMap<ET> alloc() {
        return (IntMap<ET>)s_pool.alloc();
    }

    @Override
    public void onReleased() {
        this.count = 0;
        this.keys = PZArrayUtil.arraySet(this.keys, 0);
        this.elements = PZArrayUtil.arraySet(this.elements, null);
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public E get(int key) {
        int indexOf = this.indexOf(key);
        return (E)(indexOf > -1 ? this.elements[indexOf] : null);
    }

    public E set(int key, E element) {
        int indexOf = this.indexOf(key);
        if (indexOf > -1) {
            E oldElement = (E)this.elements[indexOf];
            Pool.tryRelease(oldElement);
            this.elements[indexOf] = element;
        } else {
            indexOf = this.count++;
            if (indexOf == PZArrayUtil.lengthOf(this.keys)) {
                if (indexOf == 0) {
                    this.keys = new int[0];
                    this.elements = new Object[0];
                }

                this.keys = PZArrayUtil.add(this.keys, key);
                this.elements = PZArrayUtil.add(this.elements, element);
            } else {
                this.keys[indexOf] = key;
                this.elements[indexOf] = element;
            }
        }

        return (E)this.elements[indexOf];
    }

    public int count() {
        return this.elements.length;
    }

    public int keyAt(int elementIdx) {
        return this.keys[elementIdx];
    }

    public E elementAt(int elementIdx) {
        return (E)this.elements[elementIdx];
    }

    private int indexOf(int key) {
        for (int i = 0; i < this.count; i++) {
            if (this.keys[i] == key) {
                return i;
            }
        }

        return -1;
    }
}
