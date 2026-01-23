// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import java.util.ArrayDeque;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.iso.IsoGridSquare;

public abstract class GlobalObjectSystem {
    private static final ArrayDeque<ArrayList<GlobalObject>> objectListPool = new ArrayDeque<>();
    protected final String name;
    protected final KahluaTable modData;
    protected final ArrayList<GlobalObject> objects = new ArrayList<>();
    protected final GlobalObjectLookup lookup = new GlobalObjectLookup(this);

    GlobalObjectSystem(String name) {
        this.name = name;
        this.modData = LuaManager.platform.newTable();
    }

    public String getName() {
        return this.name;
    }

    public final KahluaTable getModData() {
        return this.modData;
    }

    protected abstract GlobalObject makeObject(int arg0, int arg1, int arg2);

    public final GlobalObject newObject(int x, int y, int z) {
        if (this.getObjectAt(x, y, z) != null) {
            throw new IllegalStateException("already an object at " + x + "," + y + "," + z);
        } else {
            GlobalObject object = this.makeObject(x, y, z);
            this.objects.add(object);
            this.lookup.addObject(object);
            return object;
        }
    }

    public final void removeObject(GlobalObject object) throws IllegalArgumentException, IllegalStateException {
        if (object == null) {
            throw new NullPointerException("object is null");
        } else if (object.system != this) {
            throw new IllegalStateException("object not in this system");
        } else {
            this.objects.remove(object);
            this.lookup.removeObject(object);
            object.Reset();
        }
    }

    public final GlobalObject getObjectAt(int x, int y, int z) {
        return this.lookup.getObjectAt(x, y, z);
    }

    public final GlobalObject getObjectAt(IsoGridSquare sq) {
        return this.lookup.getObjectAt(sq.getX(), sq.getY(), sq.getZ());
    }

    public final boolean hasObjectsInChunk(int wx, int wy) {
        return this.lookup.hasObjectsInChunk(wx, wy);
    }

    public final ArrayList<GlobalObject> getObjectsInChunk(int wx, int wy) {
        return this.lookup.getObjectsInChunk(wx, wy, this.allocList());
    }

    public final ArrayList<GlobalObject> getObjectsAdjacentTo(int x, int y, int z) {
        return this.lookup.getObjectsAdjacentTo(x, y, z, this.allocList());
    }

    public final int getObjectCount() {
        return this.objects.size();
    }

    public final GlobalObject getObjectByIndex(int index) {
        return index >= 0 && index < this.objects.size() ? this.objects.get(index) : null;
    }

    public final ArrayList<GlobalObject> allocList() {
        return objectListPool.isEmpty() ? new ArrayList<>() : objectListPool.pop();
    }

    public final void finishedWithList(ArrayList<GlobalObject> list) {
        if (list != null && !objectListPool.contains(list)) {
            list.clear();
            objectListPool.add(list);
        }
    }

    public void Reset() {
        for (int i = 0; i < this.objects.size(); i++) {
            GlobalObject object = this.objects.get(i);
            object.Reset();
        }

        this.objects.clear();
        this.modData.wipe();
    }
}
