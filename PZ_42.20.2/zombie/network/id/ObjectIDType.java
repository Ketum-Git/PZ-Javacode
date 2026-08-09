// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.id;

import astar.datastructures.HashPriorityQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

public enum ObjectIDType {
    Unknown(-1, false, ObjectID.ObjectIDShort.class),
    Player(0, false, ObjectID.ObjectIDShort.class),
    Zombie(1, false, ObjectID.ObjectIDShort.class),
    Item(2, true, ObjectID.ObjectIDInteger.class),
    Container(3, true, ObjectID.ObjectIDInteger.class),
    DeadBody(4, true, ObjectID.ObjectIDShort.class),
    Vehicle(5, true, ObjectID.ObjectIDShort.class);

    static byte permanentObjectIDTypes;
    private static final HashMap<Byte, ObjectIDType> objectIDTypes = new HashMap<>();
    final HashPriorityQueue<Long, IIdentifiable> idToObjectMap = new HashPriorityQueue<>(Comparator.comparingLong(o -> o.getObjectID().getObjectID()));
    final byte index;
    final boolean isPermanent;
    final Class<?> type;
    long lastId;
    long countNewId;

    private ObjectIDType(final int index, final boolean isPermanent, final Class<?> type) {
        this.index = (byte)index;
        this.isPermanent = isPermanent;
        this.type = type;
    }

    static ObjectIDType valueOf(byte index) {
        return objectIDTypes.getOrDefault(index, Unknown);
    }

    long allocateID() {
        this.lastId++;
        this.countNewId++;
        return this.lastId;
    }

    @Override
    public String toString() {
        return String.format("ObjectID type=%s last=%d new=%d", this.name(), this.lastId, this.countNewId);
    }

    public Collection<IIdentifiable> getObjects() {
        return this.idToObjectMap.getHashMap().values();
    }

    static {
        for (ObjectIDType type : values()) {
            objectIDTypes.put(type.index, type);
            if (type.isPermanent) {
                permanentObjectIDTypes++;
            }
        }
    }
}
