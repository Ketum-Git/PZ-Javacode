// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.utils;

import java.util.HashMap;

public class CacheMap<K, V> extends HashMap<K, V> {
    private final int size;

    public CacheMap(int size) {
        this.size = size;
    }

    @Override
    public V put(K key, V value) {
        if (this.size() > this.size) {
            this.clear();
        }

        return super.put(key, value);
    }
}
