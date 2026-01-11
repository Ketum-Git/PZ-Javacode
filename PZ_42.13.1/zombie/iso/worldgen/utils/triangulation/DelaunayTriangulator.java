// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.utils.triangulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DelaunayTriangulator {
    private List<Vector2D> pointSet;
    private TriangleSoup triangleSoup;

    public DelaunayTriangulator(List<Vector2D> pointSet) {
        this.pointSet = pointSet;
        this.triangleSoup = new TriangleSoup();
    }

    public void triangulate() throws NotEnoughPointsException {
        this.triangleSoup = new TriangleSoup();
        if (this.pointSet != null && this.pointSet.size() >= 3) {
            double maxOfAnyCoordinate = 0.0;

            for (Vector2D vector : this.getPointSet()) {
                maxOfAnyCoordinate = Math.max(Math.max(vector.x, vector.y), maxOfAnyCoordinate);
            }

            maxOfAnyCoordinate *= 16.0;
            Vector2D p1 = new Vector2D(0.0, 3.0 * maxOfAnyCoordinate);
            Vector2D p2 = new Vector2D(3.0 * maxOfAnyCoordinate, 0.0);
            Vector2D p3 = new Vector2D(-3.0 * maxOfAnyCoordinate, -3.0 * maxOfAnyCoordinate);
            Triangle2D superTriangle = new Triangle2D(p1, p2, p3);
            this.triangleSoup.add(superTriangle);

            for (int i = 0; i < this.pointSet.size(); i++) {
                Triangle2D triangle = this.triangleSoup.findContainingTriangle(this.pointSet.get(i));
                if (triangle == null) {
                    Edge2D edge = this.triangleSoup.findNearestEdge(this.pointSet.get(i));
                    Triangle2D first = this.triangleSoup.findOneTriangleSharing(edge);
                    Triangle2D second = this.triangleSoup.findNeighbour(first, edge);
                    Vector2D firstNoneEdgeVertex = first.getNoneEdgeVertex(edge);
                    Vector2D secondNoneEdgeVertex = second.getNoneEdgeVertex(edge);
                    this.triangleSoup.remove(first);
                    this.triangleSoup.remove(second);
                    Triangle2D triangle1 = new Triangle2D(edge.a, firstNoneEdgeVertex, this.pointSet.get(i));
                    Triangle2D triangle2 = new Triangle2D(edge.b, firstNoneEdgeVertex, this.pointSet.get(i));
                    Triangle2D triangle3 = new Triangle2D(edge.a, secondNoneEdgeVertex, this.pointSet.get(i));
                    Triangle2D triangle4 = new Triangle2D(edge.b, secondNoneEdgeVertex, this.pointSet.get(i));
                    this.triangleSoup.add(triangle1);
                    this.triangleSoup.add(triangle2);
                    this.triangleSoup.add(triangle3);
                    this.triangleSoup.add(triangle4);
                    this.legalizeEdge(triangle1, new Edge2D(edge.a, firstNoneEdgeVertex), this.pointSet.get(i));
                    this.legalizeEdge(triangle2, new Edge2D(edge.b, firstNoneEdgeVertex), this.pointSet.get(i));
                    this.legalizeEdge(triangle3, new Edge2D(edge.a, secondNoneEdgeVertex), this.pointSet.get(i));
                    this.legalizeEdge(triangle4, new Edge2D(edge.b, secondNoneEdgeVertex), this.pointSet.get(i));
                } else {
                    Vector2D a = triangle.a;
                    Vector2D b = triangle.b;
                    Vector2D c = triangle.c;
                    this.triangleSoup.remove(triangle);
                    Triangle2D first = new Triangle2D(a, b, this.pointSet.get(i));
                    Triangle2D second = new Triangle2D(b, c, this.pointSet.get(i));
                    Triangle2D third = new Triangle2D(c, a, this.pointSet.get(i));
                    this.triangleSoup.add(first);
                    this.triangleSoup.add(second);
                    this.triangleSoup.add(third);
                    this.legalizeEdge(first, new Edge2D(a, b), this.pointSet.get(i));
                    this.legalizeEdge(second, new Edge2D(b, c), this.pointSet.get(i));
                    this.legalizeEdge(third, new Edge2D(c, a), this.pointSet.get(i));
                }
            }

            this.triangleSoup.removeTrianglesUsing(superTriangle.a);
            this.triangleSoup.removeTrianglesUsing(superTriangle.b);
            this.triangleSoup.removeTrianglesUsing(superTriangle.c);
        } else {
            throw new NotEnoughPointsException("Less than three points in point set.");
        }
    }

    private void legalizeEdge(Triangle2D triangle, Edge2D edge, Vector2D newVertex) {
        Triangle2D neighbourTriangle = this.triangleSoup.findNeighbour(triangle, edge);
        if (neighbourTriangle != null && neighbourTriangle.isPointInCircumcircle(newVertex)) {
            this.triangleSoup.remove(triangle);
            this.triangleSoup.remove(neighbourTriangle);
            Vector2D noneEdgeVertex = neighbourTriangle.getNoneEdgeVertex(edge);
            Triangle2D firstTriangle = new Triangle2D(noneEdgeVertex, edge.a, newVertex);
            Triangle2D secondTriangle = new Triangle2D(noneEdgeVertex, edge.b, newVertex);
            this.triangleSoup.add(firstTriangle);
            this.triangleSoup.add(secondTriangle);
            this.legalizeEdge(firstTriangle, new Edge2D(noneEdgeVertex, edge.a), newVertex);
            this.legalizeEdge(secondTriangle, new Edge2D(noneEdgeVertex, edge.b), newVertex);
        }
    }

    public void shuffle() {
        Collections.shuffle(this.pointSet);
    }

    public void shuffle(int[] permutation) {
        List<Vector2D> temp = new ArrayList<>();

        for (int i = 0; i < permutation.length; i++) {
            temp.add(this.pointSet.get(permutation[i]));
        }

        this.pointSet = temp;
    }

    public List<Vector2D> getPointSet() {
        return this.pointSet;
    }

    public List<Triangle2D> getTriangles() {
        return this.triangleSoup.getTriangles();
    }

    public List<Edge2D> getEdges() {
        Set<Edge2D> edges = new HashSet<>();

        for (Triangle2D triangle : this.triangleSoup.getTriangles()) {
            edges.add(new Edge2D(triangle.a, triangle.b));
            edges.add(new Edge2D(triangle.b, triangle.c));
            edges.add(new Edge2D(triangle.a, triangle.c));
        }

        return edges.stream().toList();
    }
}
