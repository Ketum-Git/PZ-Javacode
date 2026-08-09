// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import zombie.core.Core;
import zombie.core.SpriteRenderer;

public final class Anim2DBlendPicker {
    private List<Anim2DBlendTriangle> tris;
    private List<Anim2DBlend> hull;
    private Anim2DBlendPicker.HullComparer hullComparer;

    public void SetPickTriangles(List<Anim2DBlendTriangle> tris) {
        this.tris = tris;
        this.BuildHull();
    }

    private void BuildHull() {
        HashMap<Anim2DBlendPicker.Edge, Anim2DBlendPicker.Counter> edges = new HashMap<>();
        Anim2DBlendPicker.Counter newCounter = new Anim2DBlendPicker.Counter();

        for (Anim2DBlendTriangle tri : this.tris) {
            Anim2DBlendPicker.Counter c = edges.putIfAbsent(new Anim2DBlendPicker.Edge(tri.node1, tri.node2), newCounter);
            if (c == null) {
                c = newCounter;
                newCounter = new Anim2DBlendPicker.Counter();
            }

            c.Increment();
            c = edges.putIfAbsent(new Anim2DBlendPicker.Edge(tri.node2, tri.node3), newCounter);
            if (c == null) {
                c = newCounter;
                newCounter = new Anim2DBlendPicker.Counter();
            }

            c.Increment();
            c = edges.putIfAbsent(new Anim2DBlendPicker.Edge(tri.node3, tri.node1), newCounter);
            if (c == null) {
                c = newCounter;
                newCounter = new Anim2DBlendPicker.Counter();
            }

            c.Increment();
        }

        HashSet<Anim2DBlend> hullNodes = new HashSet<>();
        edges.forEach((e, cx) -> {
            if (cx.count == 1) {
                hullNodes.add(e.a);
                hullNodes.add(e.b);
            }
        });
        ArrayList<Anim2DBlend> hull = new ArrayList<>(hullNodes);
        float centerX = 0.0F;
        float centerY = 0.0F;

        for (Anim2DBlend node : hull) {
            centerX += node.posX;
            centerY += node.posY;
        }

        centerX /= hull.size();
        centerY /= hull.size();
        this.hullComparer = new Anim2DBlendPicker.HullComparer(centerX, centerY);
        hull.sort(this.hullComparer);
        this.hull = hull;
    }

    static <T> int LowerBoundIdx(List<T> list, T searchFor, Comparator<? super T> comparator) {
        int lowerBound = 0;
        int upperBound = list.size();

        while (lowerBound != upperBound) {
            int mid = (lowerBound + upperBound) / 2;
            if (comparator.compare(searchFor, list.get(mid)) < 0) {
                upperBound = mid;
            } else {
                lowerBound = mid + 1;
            }
        }

        return lowerBound;
    }

    private static float ProjectPointToLine(float x, float y, float startX, float startY, float endX, float endY) {
        float lineToPosX = x - startX;
        float lineToPosY = y - startY;
        float lineToEndX = endX - startX;
        float lineToEndY = endY - startY;
        return (lineToEndX * lineToPosX + lineToEndY * lineToPosY) / (lineToEndX * lineToEndX + lineToEndY * lineToEndY);
    }

    public Anim2DBlendPicker.PickResults Pick(float x, float y, Anim2DBlendPicker.PickResults result) {
        result.numNodes = 0;
        result.node1 = null;
        result.node2 = null;
        result.node3 = null;
        result.scale1 = 0.0F;
        result.scale2 = 0.0F;
        result.scale3 = 0.0F;

        for (Anim2DBlendTriangle tri : this.tris) {
            if (tri.Contains(x, y)) {
                result.numNodes = 3;
                result.node1 = tri.node1;
                result.node2 = tri.node2;
                result.node3 = tri.node3;
                float v1x = result.node1.posX;
                float v1y = result.node1.posY;
                float v2x = result.node2.posX;
                float v2y = result.node2.posY;
                float v3x = result.node3.posX;
                float v3y = result.node3.posY;
                result.scale1 = ((v2y - v3y) * (x - v3x) + (v3x - v2x) * (y - v3y)) / ((v2y - v3y) * (v1x - v3x) + (v3x - v2x) * (v1y - v3y));
                result.scale2 = ((v3y - v1y) * (x - v3x) + (v1x - v3x) * (y - v3y)) / ((v2y - v3y) * (v1x - v3x) + (v3x - v2x) * (v1y - v3y));
                result.scale3 = 1.0F - result.scale1 - result.scale2;
                return result;
            }
        }

        x *= 1.1F;
        y *= 1.1F;
        Anim2DBlend dummyNode = new Anim2DBlend();
        dummyNode.posX = x;
        dummyNode.posY = y;
        int idx = LowerBoundIdx(this.hull, dummyNode, this.hullComparer);
        if (idx == this.hull.size()) {
            idx = 0;
        }

        int idx2 = idx > 0 ? idx - 1 : this.hull.size() - 1;
        Anim2DBlend n1 = this.hull.get(idx);
        Anim2DBlend n2 = this.hull.get(idx2);
        float s = ProjectPointToLine(x, y, n1.posX, n1.posY, n2.posX, n2.posY);
        if (s < 0.0F) {
            result.numNodes = 1;
            result.node1 = n1;
            result.scale1 = 1.0F;
        } else if (s > 1.0F) {
            result.numNodes = 1;
            result.node1 = n2;
            result.scale1 = 1.0F;
        } else {
            result.numNodes = 2;
            result.node1 = n1;
            result.node2 = n2;
            result.scale1 = 1.0F - s;
            result.scale2 = s;
        }

        return result;
    }

    void render(float pickX, float pickY) {
        int d = 200;
        int x = Core.getInstance().getScreenWidth() - 200 - 100;
        int y = Core.getInstance().getScreenHeight() - 200 - 100;
        SpriteRenderer.instance.renderi(null, x - 20, y - 20, 240, 240, 1.0F, 1.0F, 1.0F, 1.0F, null);

        for (int i = 0; i < this.tris.size(); i++) {
            Anim2DBlendTriangle tri = this.tris.get(i);
            SpriteRenderer.instance
                .renderline(
                    null,
                    (int)(x + 100 + tri.node1.posX * 200.0F / 2.0F),
                    (int)(y + 100 - tri.node1.posY * 200.0F / 2.0F),
                    (int)(x + 100 + tri.node2.posX * 200.0F / 2.0F),
                    (int)(y + 100 - tri.node2.posY * 200.0F / 2.0F),
                    0.5F,
                    0.5F,
                    0.5F,
                    1.0F
                );
            SpriteRenderer.instance
                .renderline(
                    null,
                    (int)(x + 100 + tri.node2.posX * 200.0F / 2.0F),
                    (int)(y + 100 - tri.node2.posY * 200.0F / 2.0F),
                    (int)(x + 100 + tri.node3.posX * 200.0F / 2.0F),
                    (int)(y + 100 - tri.node3.posY * 200.0F / 2.0F),
                    0.5F,
                    0.5F,
                    0.5F,
                    1.0F
                );
            SpriteRenderer.instance
                .renderline(
                    null,
                    (int)(x + 100 + tri.node3.posX * 200.0F / 2.0F),
                    (int)(y + 100 - tri.node3.posY * 200.0F / 2.0F),
                    (int)(x + 100 + tri.node1.posX * 200.0F / 2.0F),
                    (int)(y + 100 - tri.node1.posY * 200.0F / 2.0F),
                    0.5F,
                    0.5F,
                    0.5F,
                    1.0F
                );
        }

        float size = 8.0F;
        Anim2DBlendPicker.PickResults result = this.Pick(pickX, pickY, new Anim2DBlendPicker.PickResults());
        if (result.node1 != null) {
            SpriteRenderer.instance
                .render(
                    null,
                    x + 100 + result.node1.posX * 200.0F / 2.0F - size / 2.0F,
                    y + 100 - result.node1.posY * 200.0F / 2.0F - size / 2.0F,
                    size,
                    size,
                    0.0F,
                    1.0F,
                    0.0F,
                    1.0F,
                    null
                );
        }

        if (result.node2 != null) {
            SpriteRenderer.instance
                .render(
                    null,
                    x + 100 + result.node2.posX * 200.0F / 2.0F - size / 2.0F,
                    y + 100 - result.node2.posY * 200.0F / 2.0F - size / 2.0F,
                    size,
                    size,
                    0.0F,
                    1.0F,
                    0.0F,
                    1.0F,
                    null
                );
        }

        if (result.node3 != null) {
            SpriteRenderer.instance
                .render(
                    null,
                    x + 100 + result.node3.posX * 200.0F / 2.0F - size / 2.0F,
                    y + 100 - result.node3.posY * 200.0F / 2.0F - size / 2.0F,
                    size,
                    size,
                    0.0F,
                    1.0F,
                    0.0F,
                    1.0F,
                    null
                );
        }

        size = 4.0F;
        SpriteRenderer.instance
            .render(
                null, x + 100 + pickX * 200.0F / 2.0F - size / 2.0F, y + 100 - pickY * 200.0F / 2.0F - size / 2.0F, size, size, 0.0F, 0.0F, 1.0F, 1.0F, null
            );
    }

    static class Counter {
        public int count;

        public int Increment() {
            return ++this.count;
        }
    }

    static class Edge {
        public Anim2DBlend a;
        public Anim2DBlend b;

        public Edge(Anim2DBlend a, Anim2DBlend b) {
            boolean swap;
            if (a.posX != b.posX) {
                swap = a.posX > b.posX;
            } else {
                swap = a.posY > b.posY;
            }

            if (swap) {
                this.a = b;
                this.b = a;
            } else {
                this.a = a;
                this.b = b;
            }
        }

        @Override
        public int hashCode() {
            int h1 = this.a.hashCode();
            int h2 = this.b.hashCode();
            return (h1 << 5) + h1 ^ h2;
        }

        @Override
        public boolean equals(Object obj) {
            return !(obj instanceof Anim2DBlendPicker.Edge edge) ? false : this.a == edge.a && this.b == edge.b;
        }
    }

    static class HullComparer implements Comparator<Anim2DBlend> {
        private final int centerX;
        private final int centerY;

        public HullComparer(float centerX, float centerY) {
            this.centerX = (int)(centerX * 1000.0F);
            this.centerY = (int)(centerY * 1000.0F);
        }

        public boolean isLessThan(Anim2DBlend o1, Anim2DBlend o2) {
            int o1x = (int)(o1.posX * 1000.0F);
            int o1y = (int)(o1.posY * 1000.0F);
            int o2x = (int)(o2.posX * 1000.0F);
            int o2y = (int)(o2.posY * 1000.0F);
            int v1x = o1x - this.centerX;
            int v1y = o1y - this.centerY;
            int v2x = o2x - this.centerX;
            int v2y = o2y - this.centerY;
            if (v1y == 0 && v1x > 0) {
                return true;
            } else if (v2y == 0 && v2x > 0) {
                return false;
            } else if (v1y > 0 && v2y < 0) {
                return true;
            } else if (v1y < 0 && v2y > 0) {
                return false;
            } else {
                int crossProduct = v1x * v2y - v1y * v2x;
                return crossProduct > 0;
            }
        }

        public int compare(Anim2DBlend o1, Anim2DBlend o2) {
            if (this.isLessThan(o1, o2)) {
                return -1;
            } else {
                return this.isLessThan(o2, o1) ? 1 : 0;
            }
        }
    }

    public static class PickResults {
        public int numNodes;
        public Anim2DBlend node1;
        public Anim2DBlend node2;
        public Anim2DBlend node3;
        public float scale1;
        public float scale2;
        public float scale3;
    }
}
