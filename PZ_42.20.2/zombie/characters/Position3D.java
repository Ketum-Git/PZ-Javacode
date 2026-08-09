// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.UsedFromLua;

@UsedFromLua
public class Position3D {
    public float x;
    public float y;
    public float z;

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float z() {
        return this.z;
    }

    public Position3D() {
    }

    public Position3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position3D set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
}
