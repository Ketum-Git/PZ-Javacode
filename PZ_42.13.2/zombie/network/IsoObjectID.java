// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import zombie.core.random.Rand;

public class IsoObjectID<T> implements Iterable<T> {
    public static final short incorrect = -1;
    private final ConcurrentHashMap<Short, T> idToObjectMap;
    private final String objectType;
    private short nextId;
    private final ArrayList<T> temp = new ArrayList<>();

    public IsoObjectID(Class<T> cls) {
        this.idToObjectMap = new ConcurrentHashMap<>();
        this.nextId = (short)Rand.Next(32766);
        this.objectType = cls.getSimpleName();
    }

    public void put(short id, T obj) {
        if (id != -1) {
            this.idToObjectMap.put(id, obj);
        }
    }

    public void remove(short id) {
        this.idToObjectMap.remove(id);
    }

    public void remove(T obj) {
        this.idToObjectMap.values().remove(obj);
    }

    public T get(short id) {
        return this.idToObjectMap.get(id);
    }

    public int size() {
        return this.idToObjectMap.size();
    }

    public void clear() {
        this.idToObjectMap.clear();
    }

    public short allocateID() {
        this.nextId++;
        if (this.nextId == -1) {
            this.nextId++;
        }

        return this.nextId;
    }

    @Override
    public Iterator<T> iterator() {
        return this.idToObjectMap.values().iterator();
    }

    public void getObjects(Collection<T> out) {
        out.addAll(this.idToObjectMap.values());
    }

    public ArrayList<T> asList() {
        this.temp.clear();
        this.temp.addAll(this.idToObjectMap.values());
        return this.temp;
    }
}
