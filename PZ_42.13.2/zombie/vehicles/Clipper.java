// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.nio.ByteBuffer;
import zombie.debug.DebugLog;

public class Clipper {
    private long address;
    final ByteBuffer bb = ByteBuffer.allocateDirect(64);
    public static final int ctNoClip = 0;
    public static final int ctIntersection = 1;
    public static final int ctUnion = 2;
    public static final int ctDifference = 3;
    public static final int ctXor = 4;
    public static final int jtSquare = 0;
    public static final int jtBevel = 1;
    public static final int jtRound = 2;
    public static final int jtMiter = 3;

    public static void init() {
        String libSuffix = "";
        if ("1".equals(System.getProperty("zomboid.debuglibs.clipper"))) {
            DebugLog.log("***** Loading debug version of PZClipper");
            libSuffix = "d";
        }

        if (System.getProperty("os.name").contains("OS X")) {
            System.loadLibrary("PZClipper");
        } else {
            System.loadLibrary("PZClipper64" + libSuffix);
        }

        n_init();
    }

    public Clipper() {
        this.newInstance();
    }

    private native void newInstance();

    public native void clear();

    public void addPath(int numPoints, ByteBuffer points, boolean bClip) {
        this.addPath(numPoints, points, bClip, true);
    }

    public native void addPath(int arg0, ByteBuffer arg1, boolean arg2, boolean arg3);

    public native void addLine(float x1, float y1, float x2, float y2);

    public native void addAABB(float x1, float y1, float x2, float y2);

    public void addAABBBevel(float x1, float y1, float x2, float y2, float RADIUS) {
        this.bb.clear();
        this.bb.putFloat(x1 + RADIUS);
        this.bb.putFloat(y1);
        this.bb.putFloat(x2 - RADIUS);
        this.bb.putFloat(y1);
        this.bb.putFloat(x2);
        this.bb.putFloat(y1 + RADIUS);
        this.bb.putFloat(x2);
        this.bb.putFloat(y2 - RADIUS);
        this.bb.putFloat(x2 - RADIUS);
        this.bb.putFloat(y2);
        this.bb.putFloat(x1 + RADIUS);
        this.bb.putFloat(y2);
        this.bb.putFloat(x1);
        this.bb.putFloat(y2 - RADIUS);
        this.bb.putFloat(x1);
        this.bb.putFloat(y1 + RADIUS);
        this.addPath(this.bb.position() / 4 / 2, this.bb, false);
    }

    public native void addPolygon(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4);

    public native void clipAABB(float x1, float y1, float x2, float y2);

    public int generatePolygons() {
        return this.generatePolygons(0.0);
    }

    public native int generatePolygons(int arg0, double arg1, int arg2);

    public int generatePolygons(double delta, int joinType) {
        return this.generatePolygons(3, delta, joinType);
    }

    public int generatePolygons(double delta) {
        return this.generatePolygons(3, delta, 0);
    }

    public native int getPolygon(int index, ByteBuffer vertices);

    public native int generateTriangulatePolygons(int wx, int wy);

    public native int triangulate(int index, ByteBuffer vertices);

    public native int triangulate2(int arg0, ByteBuffer arg1);

    public static native void n_init();

    private static void writeToStdErr(String message) {
        System.err.println(message);
    }
}
