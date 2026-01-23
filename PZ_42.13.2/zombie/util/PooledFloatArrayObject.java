// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import zombie.util.list.PZArrayUtil;

public final class PooledFloatArrayObject extends PooledObject {
    private static final Pool<PooledFloatArrayObject> s_pool = new Pool<>(PooledFloatArrayObject::new);
    private float[] array = PZArrayUtil.emptyFloatArray;

    public static PooledFloatArrayObject alloc(int count) {
        PooledFloatArrayObject newObject = s_pool.alloc();
        newObject.initCapacity(count);
        return newObject;
    }

    public static PooledFloatArrayObject toArray(PooledFloatArrayObject source) {
        if (source == null) {
            return null;
        } else {
            int sourceCount = source.length();
            PooledFloatArrayObject newObject = alloc(sourceCount);
            if (sourceCount > 0) {
                System.arraycopy(source.array(), 0, newObject.array(), 0, sourceCount);
            }

            return newObject;
        }
    }

    private void initCapacity(int count) {
        if (this.array.length != count) {
            this.array = new float[count];
        }
    }

    public float[] array() {
        return this.array;
    }

    public float get(int idx) {
        return this.array[idx];
    }

    public void set(int idx, float val) {
        this.array[idx] = val;
    }

    public int length() {
        return this.array.length;
    }
}
