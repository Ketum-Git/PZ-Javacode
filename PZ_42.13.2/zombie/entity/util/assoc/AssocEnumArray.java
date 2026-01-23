// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util.assoc;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import zombie.UsedFromLua;

@UsedFromLua
public class AssocEnumArray<K extends Enum<K>, V> extends AssocArray<K, V> {
    private final EnumSet<K> keys;

    public AssocEnumArray(Class<K> enumType) {
        this.keys = EnumSet.noneOf(enumType);
    }

    public AssocEnumArray(Class<K> enumType, int initialCapacity) {
        super(initialCapacity);
        this.keys = EnumSet.noneOf(enumType);
    }

    public boolean equalsKeys(AssocEnumArray<K, V> other) {
        return other == this ? true : this.keys.equals(other.keys);
    }

    public Iterator<K> keys() {
        return this.keys.iterator();
    }

    public boolean containsKey(K o) {
        return this.keys.contains(o);
    }

    public V put(K k, V v) {
        V res = super.put(k, v);
        this.keys.add(k);
        return res;
    }

    public boolean add(K k, V v) {
        if (super.add(k, v)) {
            this.keys.add(k);
            return true;
        } else {
            return false;
        }
    }

    public void add(int frontIndex, K k, V v) {
        super.add(frontIndex, k, v);
        this.keys.add(k);
    }

    @Override
    public V removeIndex(int frontIndex) {
        Objects.checkIndex(frontIndex, this.size());
        Object[] es = this.elementData;
        int realKeyIdx = this.realKeyIndex(frontIndex);
        V oldKey = (V)es[realKeyIdx];
        V oldValue = (V)es[realKeyIdx + 1];
        this.fastRemove(es, realKeyIdx);
        this.keys.remove(oldKey);
        return oldValue;
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass() == AssocEnumArray.class && o == this;
    }

    public V remove(K o) {
        V val = super.remove(o);
        if (val != null) {
            this.keys.remove(o);
            return val;
        } else {
            return null;
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.keys.clear();
    }
}
