// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.utils.triangulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TriangleSoup {
    private final List<Triangle2D> triangleSoup = new ArrayList<>();

    public TriangleSoup() {
    }

    public void add(Triangle2D triangle) {
        this.triangleSoup.add(triangle);
    }

    public void remove(Triangle2D triangle) {
        this.triangleSoup.remove(triangle);
    }

    public List<Triangle2D> getTriangles() {
        return this.triangleSoup;
    }

    public Triangle2D findContainingTriangle(Vector2D point) {
        for (Triangle2D triangle : this.triangleSoup) {
            if (triangle.contains(point)) {
                return triangle;
            }
        }

        return null;
    }

    public Triangle2D findNeighbour(Triangle2D triangle, Edge2D edge) {
        for (Triangle2D triangleFromSoup : this.triangleSoup) {
            if (triangleFromSoup.isNeighbour(edge) && triangleFromSoup != triangle) {
                return triangleFromSoup;
            }
        }

        return null;
    }

    public Triangle2D findOneTriangleSharing(Edge2D edge) {
        for (Triangle2D triangle : this.triangleSoup) {
            if (triangle.isNeighbour(edge)) {
                return triangle;
            }
        }

        return null;
    }

    public Edge2D findNearestEdge(Vector2D point) {
        List<EdgeDistancePack> edgeList = new ArrayList<>();

        for (Triangle2D triangle : this.triangleSoup) {
            edgeList.add(triangle.findNearestEdge(point));
        }

        EdgeDistancePack[] edgeDistancePacks = new EdgeDistancePack[edgeList.size()];
        edgeList.toArray(edgeDistancePacks);
        Arrays.sort((Object[])edgeDistancePacks);
        return edgeDistancePacks[0].edge;
    }

    public void removeTrianglesUsing(Vector2D vertex) {
        List<Triangle2D> trianglesToBeRemoved = new ArrayList<>();

        for (Triangle2D triangle : this.triangleSoup) {
            if (triangle.hasVertex(vertex)) {
                trianglesToBeRemoved.add(triangle);
            }
        }

        this.triangleSoup.removeAll(trianglesToBeRemoved);
    }
}
