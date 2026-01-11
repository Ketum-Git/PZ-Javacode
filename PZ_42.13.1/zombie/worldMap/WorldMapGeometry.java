// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.MapFiles;
import zombie.vehicles.Clipper;

public final class WorldMapGeometry {
    public WorldMapCell cell;
    public WorldMapGeometry.Type type;
    public final ArrayList<WorldMapPoints> points = new ArrayList<>(1);
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;
    public int firstIndex = -1;
    public short indexCount = -1;
    public ArrayList<WorldMapGeometry.TrianglesPerZoom> trianglesPerZoom;
    public boolean failedToTriangulate;
    public int vboIndex1 = -1;
    public int vboIndex2 = -1;
    public int vboIndex3 = -1;
    public int vboIndex4 = -1;
    private static Clipper clipper;
    private static ByteBuffer vertices;

    public WorldMapGeometry(WorldMapCell cell) {
        this.cell = cell;
    }

    public void calculateBounds() {
        this.minX = this.minY = Integer.MAX_VALUE;
        this.maxX = this.maxY = Integer.MIN_VALUE;

        for (int i = 0; i < this.points.size(); i++) {
            WorldMapPoints pts = this.points.get(i);
            pts.calculateBounds();
            this.minX = PZMath.min(this.minX, pts.minX);
            this.minY = PZMath.min(this.minY, pts.minY);
            this.maxX = PZMath.max(this.maxX, pts.maxX);
            this.maxY = PZMath.max(this.maxY, pts.maxY);
        }
    }

    public boolean containsPoint(float x, float y) {
        return this.type == WorldMapGeometry.Type.Polygon && !this.points.isEmpty()
            ? this.isPointInPolygon_WindingNumber(x, y, 0) != WorldMapGeometry.PolygonHit.Outside
            : false;
    }

    public void triangulate(WorldMapCell cell, double[] delta) {
        if (clipper == null) {
            clipper = new Clipper();
        }

        clipper.clear();
        WorldMapPoints outer = this.points.get(0);
        if (vertices == null || vertices.capacity() < outer.numPoints() * 2 * 50 * 4) {
            vertices = ByteBuffer.allocateDirect(outer.numPoints() * 2 * 50 * 4);
        }

        vertices.clear();
        if (outer.isClockwise()) {
            for (int i = outer.numPoints() - 1; i >= 0; i--) {
                vertices.putFloat(outer.getX(i));
                vertices.putFloat(outer.getY(i));
            }
        } else {
            for (int i = 0; i < outer.numPoints(); i++) {
                vertices.putFloat(outer.getX(i));
                vertices.putFloat(outer.getY(i));
            }
        }

        clipper.addPath(outer.numPoints(), vertices, false);

        for (int i = 1; i < this.points.size(); i++) {
            vertices.clear();
            WorldMapPoints hole = this.points.get(i);
            if (hole.isClockwise()) {
                for (int j = hole.numPoints() - 1; j >= 0; j--) {
                    vertices.putFloat(hole.getX(j));
                    vertices.putFloat(hole.getY(j));
                }
            } else {
                for (int j = 0; j < hole.numPoints(); j++) {
                    vertices.putFloat(hole.getX(j));
                    vertices.putFloat(hole.getY(j));
                }
            }

            clipper.addPath(hole.numPoints(), vertices, true);
        }

        if (this.minX < 0 || this.minY < 0 || this.maxX > 256 || this.maxY > 256) {
            int PAD = 768;
            float x1 = -768.0F;
            float y1 = -768.0F;
            float x2 = 1024.0F;
            float y2 = -768.0F;
            float x3 = 1024.0F;
            float y3 = 1024.0F;
            float x4 = -768.0F;
            float y4 = 1024.0F;
            float x5 = -768.0F;
            float y5 = 0.0F;
            float x6 = 0.0F;
            float y6 = 0.0F;
            float x7 = 0.0F;
            float y7 = 256.0F;
            float x8 = 256.0F;
            float y8 = 256.0F;
            float x9 = 256.0F;
            float y9 = 0.0F;
            float x10 = -768.0F;
            float y10 = 0.0F;
            vertices.clear();
            vertices.putFloat(-768.0F).putFloat(-768.0F);
            vertices.putFloat(1024.0F).putFloat(-768.0F);
            vertices.putFloat(1024.0F).putFloat(1024.0F);
            vertices.putFloat(-768.0F).putFloat(1024.0F);
            vertices.putFloat(-768.0F).putFloat(0.0F);
            vertices.putFloat(0.0F).putFloat(0.0F);
            vertices.putFloat(0.0F).putFloat(256.0F);
            vertices.putFloat(256.0F).putFloat(256.0F);
            vertices.putFloat(256.0F).putFloat(0.0F);
            vertices.putFloat(-768.0F).putFloat(0.0F);
            clipper.addPath(10, vertices, true);
        }

        if (cell.clipHigherPriorityCells == null) {
            int minCell300X = (int)Math.floor(cell.x * 256.0F / 300.0F);
            int minCell300Y = (int)Math.floor(cell.y * 256.0F / 300.0F);
            int maxCell300X = (int)Math.floor((cell.x + 1) * 256.0F / 300.0F);
            int maxCell300Y = (int)Math.floor((cell.y + 1) * 256.0F / 300.0F);
            ArrayList<MapFiles> mapFiles = MapFiles.getCurrentMapFiles();
            vertices.clear();

            for (int i = 0; i < cell.priority; i++) {
                MapFiles mapFiles1 = mapFiles.get(i);

                for (int cell300Y = minCell300Y; cell300Y <= maxCell300Y; cell300Y++) {
                    for (int cell300X = minCell300X; cell300X <= maxCell300X; cell300X++) {
                        if (mapFiles1.hasCell300(cell300X, cell300Y)) {
                            int x1 = cell300X * 300 - cell.x * 256;
                            int y1 = cell300Y * 300 - cell.y * 256;
                            int x2 = x1 + 300;
                            int y2 = y1 + 300;
                            vertices.putFloat(x1).putFloat(y1);
                            vertices.putFloat(x2).putFloat(y2);
                        }
                    }
                }
            }

            cell.clipHigherPriorityCells = new int[vertices.position() / 4];
            int i = 0;

            for (int j = 0; i < vertices.position(); j++) {
                float f = vertices.getFloat(i);
                cell.clipHigherPriorityCells[j] = PZMath.roundToInt(f);
                i += 4;
            }
        }

        for (int i = 0; i < cell.clipHigherPriorityCells.length; i += 4) {
            float x1 = cell.clipHigherPriorityCells[i];
            float y1 = cell.clipHigherPriorityCells[i + 1];
            float x2 = cell.clipHigherPriorityCells[i + 2];
            float y2 = cell.clipHigherPriorityCells[i + 3];
            clipper.clipAABB(x1, y1, x2, y2);
        }

        this.firstIndex = 0;
        this.indexCount = 0;
        int numPolys = clipper.generatePolygons(0.0);
        if (numPolys > 0) {
            this.firstIndex = -1;

            for (int i = 0; i < numPolys; i++) {
                vertices.clear();
                int numIndices = clipper.triangulate2(i, vertices);
                if (numIndices >= 3) {
                    int numPoints = vertices.getShort();
                    FloatBuffer triangleBuffer = this.cell.getTriangleBuffer(numPoints * 2);
                    int firstPoint = triangleBuffer.position() / 2;

                    for (int j = 0; j < numPoints; j++) {
                        triangleBuffer.put(vertices.getFloat());
                        triangleBuffer.put(vertices.getFloat());
                    }

                    ShortBuffer indexBuffer = this.cell.getIndexBuffer(numIndices);
                    if (this.firstIndex == -1) {
                        this.firstIndex = (short)indexBuffer.position();
                    }

                    this.indexCount += (short)numIndices;

                    for (int j = 0; j < numIndices; j++) {
                        indexBuffer.put((short)(firstPoint + vertices.getShort()));
                    }
                }
            }

            if (delta != null) {
                for (int ix = 0; ix < delta.length; ix++) {
                    double delta2 = delta[ix] - (ix == 0 ? 0.0 : delta[ix - 1]);
                    numPolys = clipper.generatePolygons(delta2);
                    if (numPolys > 0) {
                        WorldMapGeometry.TrianglesPerZoom tpz = new WorldMapGeometry.TrianglesPerZoom();
                        tpz.delta = delta[ix];

                        for (int j = 0; j < numPolys; j++) {
                            vertices.clear();
                            int numIndices = clipper.triangulate2(j, vertices);
                            if (numIndices >= 3) {
                                int numPoints = vertices.getShort();
                                FloatBuffer triangleBuffer = this.cell.getTriangleBuffer(numPoints * 2);
                                int firstPoint = triangleBuffer.position() / 2;

                                for (int k = 0; k < numPoints; k++) {
                                    triangleBuffer.put(vertices.getFloat());
                                    triangleBuffer.put(vertices.getFloat());
                                }

                                ShortBuffer indexBuffer = this.cell.getIndexBuffer(numIndices);
                                if (tpz.firstIndex == -1) {
                                    tpz.firstIndex = indexBuffer.position();
                                }

                                tpz.indexCount += (short)numIndices;

                                for (int k = 0; k < numIndices; k++) {
                                    indexBuffer.put((short)(firstPoint + vertices.getShort()));
                                }
                            }
                        }

                        if (tpz.indexCount >= 3) {
                            if (this.trianglesPerZoom == null) {
                                this.trianglesPerZoom = new ArrayList<>(delta.length);
                            }

                            this.trianglesPerZoom.add(tpz);
                        }
                    }
                }

                this.trianglesPerZoom.trimToSize();
            }
        }
    }

    WorldMapGeometry.TrianglesPerZoom findTriangles(double delta) {
        if (this.trianglesPerZoom == null) {
            return null;
        } else {
            for (int i = 0; i < this.trianglesPerZoom.size(); i++) {
                WorldMapGeometry.TrianglesPerZoom tpz = this.trianglesPerZoom.get(i);
                if (tpz.delta == delta) {
                    return tpz;
                }
            }

            return null;
        }
    }

    public void clearTriangles() {
        this.firstIndex = -1;
        this.indexCount = -1;
        if (this.trianglesPerZoom != null) {
            this.trianglesPerZoom.clear();
            this.trianglesPerZoom = null;
        }
    }

    public void dispose() {
        this.points.clear();
        if (this.trianglesPerZoom != null) {
            this.trianglesPerZoom.clear();
            this.trianglesPerZoom = null;
        }
    }

    float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
        return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    }

    WorldMapGeometry.PolygonHit isPointInPolygon_WindingNumber(float x, float y, int flags) {
        int wn = 0;
        WorldMapPoints outer = this.points.get(0);

        for (int i = 0; i < outer.numPoints(); i++) {
            int x1 = outer.getX(i);
            int y1 = outer.getY(i);
            int x2 = outer.getX((i + 1) % outer.numPoints());
            int y2 = outer.getY((i + 1) % outer.numPoints());
            if (y1 <= y) {
                if (y2 > y && this.isLeft(x1, y1, x2, y2, x, y) > 0.0F) {
                    wn++;
                }
            } else if (y2 <= y && this.isLeft(x1, y1, x2, y2, x, y) < 0.0F) {
                wn--;
            }
        }

        return wn == 0 ? WorldMapGeometry.PolygonHit.Outside : WorldMapGeometry.PolygonHit.Inside;
    }

    private static enum PolygonHit {
        OnEdge,
        Inside,
        Outside;
    }

    public static final class TrianglesPerZoom {
        public int firstIndex = -1;
        public short indexCount = -1;
        double delta;
    }

    public static enum Type {
        LineString,
        Point,
        Polygon;
    }
}
