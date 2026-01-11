// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import java.util.ArrayList;
import java.util.Collection;
import org.joml.Runtime;
import org.joml.Vector2f;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.LineDrawer;
import zombie.iso.zones.Zone;
import zombie.popman.ObjectPool;

public final class Mesh {
    public MeshList meshList;
    public final ArrayList<Vector2f> polygon = new ArrayList<>();
    public final ArrayList<Vector2f> triangles = new ArrayList<>();
    public float centroidX;
    public float centroidY;
    public final TIntArrayList adjacentTriangles = new TIntArrayList();
    public final TShortArrayList trianglesOnBoundaries = new TShortArrayList();
    public final TShortArrayList edgesOnBoundaries = new TShortArrayList();
    boolean offMeshDone;
    final ArrayList<OffMeshConnection> offMeshConnections = new ArrayList<>();
    public Zone zone;
    static final ObjectPool<Mesh> pool = new ObjectPool<Mesh>(Mesh::new) {
        public void release(Mesh obj) {
            AnimalPathfind.getInstance().vector2fObjectPool.releaseAll(obj.triangles);
            obj.triangles.clear();
            obj.trianglesOnBoundaries.clear();
            obj.edgesOnBoundaries.clear();
            OffMeshConnection.pool.releaseAll(obj.offMeshConnections);
            obj.offMeshConnections.clear();
            super.release(obj);
        }
    };

    void initFrom(Mesh other) {
        this.meshList = other.meshList;
        this.polygon.addAll(other.polygon);
        this.triangles.addAll(other.triangles);
        this.centroidX = other.centroidX;
        this.centroidY = other.centroidY;
        this.adjacentTriangles.addAll(other.adjacentTriangles);
        this.trianglesOnBoundaries.addAll(other.trianglesOnBoundaries);
        this.edgesOnBoundaries.addAll(other.edgesOnBoundaries);
        this.zone = other.zone;
    }

    void initFromZone(Zone zone) {
        this.zone = zone;

        for (int i = 0; i < zone.points.size(); i += 2) {
            this.polygon.add(new Vector2f(zone.points.get(i), zone.points.get(i + 1)));
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        float[] zoneTriangles = zone.getPolygonTriangles();

        for (int i = 0; i < zoneTriangles.length; i += 2) {
            float x1 = zoneTriangles[i];
            float y1 = zoneTriangles[i + 1];
            this.triangles.add(new Vector2f(x1, y1));
            minX = Float.min(minX, x1);
            minY = Float.min(minY, y1);
            maxX = Float.max(maxX, x1);
            maxY = Float.max(maxY, y1);
        }

        this.initEdges();
        this.initAdjacentTriangles();
        this.centroidX = (maxX - minX) / 2.0F;
        this.centroidY = (maxY - minY) / 2.0F;
    }

    void initEdges() {
        this.edgesOnBoundaries.clear();
        this.trianglesOnBoundaries.clear();

        for (int i = 0; i < this.triangles.size(); i += 3) {
            Vector2f v1 = this.triangles.get(i);
            Vector2f v2 = this.triangles.get(i + 1);
            Vector2f v3 = this.triangles.get(i + 2);
            short edges = 0;
            if (this.isEdgeOnBoundary(v1, v2)) {
                edges = (short)(edges | 1);
            }

            if (this.isEdgeOnBoundary(v2, v3)) {
                edges = (short)(edges | 2);
            }

            if (this.isEdgeOnBoundary(v3, v1)) {
                edges = (short)(edges | 4);
            }

            if (edges != 0) {
                this.edgesOnBoundaries.add(edges);
                this.trianglesOnBoundaries.add((short)i);
            }
        }
    }

    void initAdjacentTriangles() {
        for (int tri = 0; tri < this.triangles.size(); tri += 3) {
            this.initAdjacentTriangles(tri);
        }
    }

    void initAdjacentTriangles(int tri1) {
        int listIndex = this.adjacentTriangles.size();
        this.adjacentTriangles.add(-1);
        this.adjacentTriangles.add(-1);
        this.adjacentTriangles.add(-1);

        for (int tri2 = 0; tri2 < this.triangles.size(); tri2 += 3) {
            if (tri2 != tri1) {
                for (int edge1 = 0; edge1 < 3; edge1++) {
                    int edge2 = this.getSharedEdge(tri1, edge1, tri2);
                    if (edge2 != -1) {
                        this.adjacentTriangles.set(listIndex + edge1, tri2 << 16 | edge2);
                        break;
                    }
                }
            }
        }
    }

    int getSharedEdge(int tri1, int edge1, int tri2) {
        Vector2f p1 = this.triangles.get(tri1 + edge1);
        Vector2f p2 = this.triangles.get(tri1 + (edge1 + 1) % 3);

        for (int edge2 = 0; edge2 < 3; edge2++) {
            Vector2f p3 = this.triangles.get(tri2 + edge2);
            Vector2f p4 = this.triangles.get(tri2 + (edge2 + 1) % 3);
            if (this.isSameEdge(p1, p2, p3, p4)) {
                return edge2;
            }
        }

        return -1;
    }

    boolean isSameEdge(Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4) {
        float EPSILON = 0.01F;
        return p1.equals(p3, 0.01F) && p2.equals(p4, 0.01F) || p1.equals(p4, 0.01F) && p2.equals(p3, 0.01F);
    }

    boolean isEdgeOnBoundary(Vector2f v1, Vector2f v2) {
        for (int i = 0; i < this.polygon.size(); i++) {
            Vector2f v3 = this.polygon.get(i);
            Vector2f v4 = this.polygon.get((i + 1) % this.polygon.size());
            if (v1.equals(v3) && v2.equals(v4) || v1.equals(v4) && v2.equals(v3)) {
                return true;
            }
        }

        return false;
    }

    int getTriangleAt(float x, float y) {
        for (int tri = 0; tri < this.triangles.size(); tri += 3) {
            Vector2f p1 = this.triangles.get(tri);
            Vector2f p2 = this.triangles.get(tri + 1);
            Vector2f p3 = this.triangles.get(tri + 2);
            if (testPointInTriangle(x, y, 0.0F, p1.x, p1.y, 0.0F, p2.x, p2.y, 0.0F, p3.x, p3.y, 0.0F)) {
                return tri;
            }
        }

        return -1;
    }

    public Vector2f pickRandomPoint(Vector2f out) {
        int triangleIndex = Rand.Next(this.triangles.size() / 3);
        return this.pickRandomPointInTriangle(triangleIndex * 3, out);
    }

    public Vector2f pickRandomPointInTriangle(int triangleIndex, Vector2f out) {
        Vector2f a = this.triangles.get(triangleIndex);
        Vector2f b = this.triangles.get(triangleIndex + 1);
        Vector2f c = this.triangles.get(triangleIndex + 2);
        float notOnEdge = 0.01F;
        float s = Rand.Next(0.01F, 0.99F);
        float t = Rand.Next(0.01F, 0.99F);
        boolean inTriangle = s + t <= 1.0F;
        float px;
        float py;
        if (inTriangle) {
            px = s * (b.x - a.x) + t * (c.x - a.x);
            py = s * (b.y - a.y) + t * (c.y - a.y);
        } else {
            px = (1.0F - s) * (b.x - a.x) + (1.0F - t) * (c.x - a.x);
            py = (1.0F - s) * (b.y - a.y) + (1.0F - t) * (c.y - a.y);
        }

        px += a.x;
        py += a.y;
        return out.set(px, py);
    }

    public static boolean testPointInTriangle(
        float pX, float pY, float pZ, float v0X, float v0Y, float v0Z, float v1X, float v1Y, float v1Z, float v2X, float v2Y, float v2Z
    ) {
        float e10X = v1X - v0X;
        float e10Y = v1Y - v0Y;
        float e10Z = v1Z - v0Z;
        float e20X = v2X - v0X;
        float e20Y = v2Y - v0Y;
        float e20Z = v2Z - v0Z;
        float a = e10X * e10X + e10Y * e10Y + e10Z * e10Z;
        float b = e10X * e20X + e10Y * e20Y + e10Z * e20Z;
        float c = e20X * e20X + e20Y * e20Y + e20Z * e20Z;
        float ac_bb = a * c - b * b;
        float vpX = pX - v0X;
        float vpY = pY - v0Y;
        float vpZ = pZ - v0Z;
        float d = vpX * e10X + vpY * e10Y + vpZ * e10Z;
        float e = vpX * e20X + vpY * e20Y + vpZ * e20Z;
        float x = d * c - e * b;
        float y = e * a - d * b;
        float z = x + y - ac_bb;
        return (Runtime.floatToIntBits(z) & ~(Runtime.floatToIntBits(x) | Runtime.floatToIntBits(y)) & Long.MIN_VALUE) != 0L;
    }

    int indexOf() {
        return this.meshList.indexOf(this);
    }

    void addConnection(int tri, int edge, Mesh meshTo, int triTo, int edgeTo, Vector2f overlap1, Vector2f overlap2) {
        assert meshTo != this;

        OffMeshConnection omc = OffMeshConnection.pool.alloc();
        omc.triFrom = tri;
        omc.edgeFrom = edge;
        omc.meshTo = meshTo;
        omc.triTo = triTo;
        omc.edgeTo = edgeTo;
        omc.edge1.set(overlap1);
        omc.edge2.set(overlap2);
        this.offMeshConnections.add(omc);
    }

    void gatherConnectedMeshes(Collection<Mesh> added) {
        for (OffMeshConnection omc : this.offMeshConnections) {
            if (!added.contains(omc.meshTo)) {
                added.add(omc.meshTo);
                omc.meshTo.gatherConnectedMeshes(added);
            }
        }
    }

    public float getEdgeMidPointX(int triangleIdx, int edgeIdx) {
        Vector2f p1 = this.triangles.get(triangleIdx + edgeIdx);
        Vector2f p2 = this.triangles.get(triangleIdx + (edgeIdx + 1) % 3);
        return (p1.x + p2.x) / 2.0F;
    }

    public float getEdgeMidPointY(int triangleIdx, int edgeIdx) {
        Vector2f p1 = this.triangles.get(triangleIdx + edgeIdx);
        Vector2f p2 = this.triangles.get(triangleIdx + (edgeIdx + 1) % 3);
        return (p1.y + p2.y) / 2.0F;
    }

    int getTriangleEdgeOnX(int tri, float x) {
        Vector2f p1 = this.triangles.get(tri);
        Vector2f p2 = this.triangles.get(tri + 1);
        Vector2f p3 = this.triangles.get(tri + 2);
        if (PZMath.equal(p1.x, p2.x, 0.001F) && PZMath.equal(p1.x, x, 0.001F)) {
            return 0;
        } else if (PZMath.equal(p2.x, p3.x, 0.001F) && PZMath.equal(p2.x, x, 0.001F)) {
            return 1;
        } else {
            return PZMath.equal(p3.x, p1.x, 0.001F) && PZMath.equal(p3.x, x, 0.001F) ? 2 : -1;
        }
    }

    int getTriangleEdgeOnY(int tri, float y) {
        Vector2f p1 = this.triangles.get(tri);
        Vector2f p2 = this.triangles.get(tri + 1);
        Vector2f p3 = this.triangles.get(tri + 2);
        if (PZMath.equal(p1.y, p2.y, 0.001F) && PZMath.equal(p1.y, y, 0.001F)) {
            return 0;
        } else if (PZMath.equal(p2.y, p3.y, 0.001F) && PZMath.equal(p2.y, y, 0.001F)) {
            return 1;
        } else {
            return PZMath.equal(p3.y, p1.y, 0.001F) && PZMath.equal(p3.y, y, 0.001F) ? 2 : -1;
        }
    }

    void renderTriangleEdges() {
        int z = this.meshList.z;

        for (int j = 0; j < this.triangles.size(); j += 3) {
            Vector2f p0 = this.triangles.get(j);
            Vector2f p1 = this.triangles.get(j + 1);
            Vector2f p2 = this.triangles.get(j + 2);
            LineDrawer.addLine(p0.x, p0.y, z, p1.x, p1.y, z, 1.0F, 1.0F, 1.0F, null, true);
            LineDrawer.addLine(p1.x, p1.y, z, p2.x, p2.y, z, 1.0F, 1.0F, 1.0F, null, true);
            LineDrawer.addLine(p2.x, p2.y, z, p0.x, p0.y, z, 1.0F, 1.0F, 1.0F, null, true);
        }
    }

    void renderOffMeshConnections() {
        int z = this.meshList.z;

        for (int j = 0; j < this.offMeshConnections.size(); j++) {
            OffMeshConnection omc = this.offMeshConnections.get(j);
            if (omc.edgeFrom != -1) {
                Vector2f p1 = this.triangles.get(omc.triFrom + omc.edgeFrom);
                Vector2f p2 = this.triangles.get(omc.triFrom + (omc.edgeFrom + 1) % 3);
                LineDrawer.addLine(p1.x, p1.y, z, p2.x, p2.y, z, 1.0F, 0.0F, 0.0F, null, true);
            }

            Vector2f p1 = this.triangles.get(omc.triFrom);
            Vector2f p2 = this.triangles.get(omc.triFrom + 1);
            Vector2f p3 = this.triangles.get(omc.triFrom + 2);
            float c1x = (p1.x + p2.x + p3.x) / 3.0F;
            float c1y = (p1.y + p2.y + p3.y) / 3.0F;
            Mesh mesh2 = omc.meshTo;
            Vector2f p4 = mesh2.triangles.get(omc.triTo);
            Vector2f p5 = mesh2.triangles.get(omc.triTo + 1);
            Vector2f p6 = mesh2.triangles.get(omc.triTo + 2);
            float c2x = (p4.x + p5.x + p6.x) / 3.0F;
            float c2y = (p4.y + p5.y + p6.y) / 3.0F;
            LineDrawer.addLine(c1x, c1y, z, c2x, c2y, mesh2.meshList.z, 1.0F, 0.0F, 0.0F, null, true);
        }
    }

    void renderOutline() {
        int z = this.meshList.z;

        for (int i = 0; i < this.trianglesOnBoundaries.size(); i++) {
            short tri = this.trianglesOnBoundaries.get(i);
            short edges = this.edgesOnBoundaries.get(i);

            for (int edge = 0; edge < 3; edge++) {
                if ((edges & 1 << edge) != 0) {
                    Vector2f p0 = this.triangles.get(tri + edge);
                    Vector2f p1 = this.triangles.get(tri + (edge + 1) % 3);
                    LineDrawer.addLine(p0.x, p0.y, z, p1.x, p1.y, z, 0.0F, 1.0F, 0.0F, null, true);
                }
            }
        }
    }

    public void renderOutline(IPathRenderer renderer, float r, float g, float b, float a) {
        for (int i = 0; i < this.polygon.size(); i++) {
            float _x1 = this.polygon.get(i).x();
            float _y1 = this.polygon.get(i).y();
            float _x2 = this.polygon.get((i + 1) % this.polygon.size()).x();
            float _y2 = this.polygon.get((i + 1) % this.polygon.size()).y();
            renderer.drawLine(_x1, _y1, _x2, _y2, r, g, b, a);
        }
    }

    public void renderPoints(IPathRenderer renderer, float r, float g, float b, float a) {
        for (int i = 0; i < this.polygon.size(); i++) {
            float _x1 = this.polygon.get(i).x();
            float _y1 = this.polygon.get(i).y();
            renderer.drawRect(_x1 - 0.5F, _y1 - 0.5F, 1.0F, 1.0F, r, g, b, a);
        }
    }

    public void renderTriangles(IPathRenderer renderer, float r, float g, float b, float a) {
        for (int i = 0; i < this.triangles.size(); i += 3) {
            float _x1 = this.triangles.get(i).x();
            float _y1 = this.triangles.get(i).y();
            float _x2 = this.triangles.get(i + 1).x();
            float _y2 = this.triangles.get(i + 1).y();
            float _x3 = this.triangles.get(i + 2).x();
            float _y3 = this.triangles.get(i + 2).y();
            renderer.drawLine(_x1, _y1, _x2, _y2, r, g, b, a);
            renderer.drawLine(_x2, _y2, _x3, _y3, r, g, b, a);
            renderer.drawLine(_x1, _y1, _x3, _y3, r, g, b, a);
        }
    }

    public void renderOffMeshConnections(IPathRenderer renderer, float r, float g, float b, float a) {
        float BOX = 1.0F;

        for (int i = 0; i < this.offMeshConnections.size(); i++) {
            OffMeshConnection omc = this.offMeshConnections.get(i);
            renderer.drawRect((omc.edge1.x + omc.edge2.x) / 2.0F - 0.5F, (omc.edge1.y + omc.edge2.y) / 2.0F - 0.5F, 1.0F, 1.0F, r, g, b, a);
        }
    }
}
