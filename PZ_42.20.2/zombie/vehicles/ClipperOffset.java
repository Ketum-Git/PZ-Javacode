// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.nio.ByteBuffer;

public final class ClipperOffset {
    private final long address = this.newInstance();

    private native long newInstance();

    public native void clear();

    public native void addPath(int numPoints, ByteBuffer points, int joinType, int endType);

    public native void execute(double delta);

    public native int getPolygonCount();

    public native int getPolygon(int index, ByteBuffer vertices);

    public static enum EndType {
        Polygon,
        Joined,
        Butt,
        Square,
        Round;
    }

    public static enum JoinType {
        Square,
        Bevel,
        Round,
        Miter;
    }
}
