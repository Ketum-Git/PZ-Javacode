// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;

@UsedFromLua
public final class SGlobalObject extends GlobalObject {
    private static KahluaTable tempTable;

    SGlobalObject(SGlobalObjectSystem system, int x, int y, int z) {
        super(system, x, y, z);
    }

    public void load(ByteBuffer bb, int WorldVersion) throws IOException {
        boolean empty = bb.get() == 0;
        if (!empty) {
            this.modData.load(bb, WorldVersion);
        }
    }

    public void save(ByteBuffer bb) throws IOException {
        bb.putInt(this.x);
        bb.putInt(this.y);
        bb.put((byte)this.z);
        if (tempTable == null) {
            tempTable = LuaManager.platform.newTable();
        }

        tempTable.wipe();
        KahluaTableIterator iterator = this.modData.iterator();

        while (iterator.advance()) {
            Object key = iterator.getKey();
            if (((SGlobalObjectSystem)this.system).objectModDataKeys.contains(key)) {
                tempTable.rawset(key, this.modData.rawget(key));
            }
        }

        if (tempTable.isEmpty()) {
            bb.put((byte)0);
        } else {
            bb.put((byte)1);
            tempTable.save(bb);
            tempTable.wipe();
        }
    }
}
