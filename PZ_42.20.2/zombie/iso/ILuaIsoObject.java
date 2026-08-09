// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

public interface ILuaIsoObject {
    default void setDir(IsoDirections directions) {
        this.setForwardIsoDirection(directions);
    }

    void setForwardIsoDirection(IsoDirections arg0);
}
