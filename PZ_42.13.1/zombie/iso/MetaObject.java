// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.UsedFromLua;

@UsedFromLua
public final class MetaObject {
    int type;
    int x;
    int y;
    RoomDef def;
    boolean used;

    public MetaObject(int type, int x, int y, RoomDef def) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.def = def;
    }

    public RoomDef getRoom() {
        return this.def;
    }

    public boolean getUsed() {
        return this.used;
    }

    public void setUsed(boolean bUsed) {
        this.used = bUsed;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getType() {
        return this.type;
    }
}
