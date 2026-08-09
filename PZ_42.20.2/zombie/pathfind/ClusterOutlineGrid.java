// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayList;

final class ClusterOutlineGrid {
    ClusterOutline[] elements;
    int w;
    int h;

    ClusterOutlineGrid setSize(int w, int h) {
        if (this.elements == null || this.elements.length < w * h) {
            this.elements = new ClusterOutline[w * h];
        }

        this.w = w;
        this.h = h;
        return this;
    }

    void releaseElements() {
        for (int y = 0; y < this.h; y++) {
            for (int x = 0; x < this.w; x++) {
                if (this.elements[x + y * this.w] != null) {
                    this.elements[x + y * this.w].release();
                    this.elements[x + y * this.w] = null;
                }
            }
        }
    }

    void setInner(int x, int y, int z) {
        ClusterOutline f1 = this.get(x, y, z);
        if (f1 != null) {
            f1.inner = true;
        }
    }

    void setWest(int x, int y, int z) {
        ClusterOutline f1 = this.get(x, y, z);
        if (f1 != null) {
            f1.w = true;
        }
    }

    void setNorth(int x, int y, int z) {
        ClusterOutline f1 = this.get(x, y, z);
        if (f1 != null) {
            f1.n = true;
        }
    }

    void setEast(int x, int y, int z) {
        ClusterOutline f1 = this.get(x, y, z);
        if (f1 != null) {
            f1.e = true;
        }
    }

    void setSouth(int x, int y, int z) {
        ClusterOutline f1 = this.get(x, y, z);
        if (f1 != null) {
            f1.s = true;
        }
    }

    boolean canTrace_W(int x, int y, int z) {
        ClusterOutline cell = this.get(x, y, z);
        return cell != null && cell.inner && cell.w && !cell.tw;
    }

    boolean canTrace_N(int x, int y, int z) {
        ClusterOutline cell = this.get(x, y, z);
        return cell != null && cell.inner && cell.n && !cell.tn;
    }

    boolean canTrace_E(int x, int y, int z) {
        ClusterOutline cell = this.get(x, y, z);
        return cell != null && cell.inner && cell.e && !cell.te;
    }

    boolean canTrace_S(int x, int y, int z) {
        ClusterOutline cell = this.get(x, y, z);
        return cell != null && cell.inner && cell.s && !cell.ts;
    }

    boolean isInner(int x, int y, int z) {
        ClusterOutline f1 = this.get(x, y, z);
        return f1 != null && (f1.start || f1.inner);
    }

    ClusterOutline get(int x, int y, int z) {
        if (x < 0 || x >= this.w) {
            return null;
        } else if (y >= 0 && y < this.h) {
            if (this.elements[x + y * this.w] == null) {
                this.elements[x + y * this.w] = ClusterOutline.alloc().init(x, y, z);
            }

            return this.elements[x + y * this.w];
        } else {
            return null;
        }
    }

    void trace_W(ClusterOutline co, ArrayList<Node> nodes, Node extend) {
        int x = co.x;
        int y = co.y;
        int z = co.z;
        if (extend != null) {
            extend.setXY(x, y);
        } else {
            Node node2 = Node.alloc().init(x, y, z);
            nodes.add(node2);
        }

        co.tw = true;
        if (this.canTrace_S(x - 1, y - 1, z)) {
            this.get(x, y - 1, z).innerCorner = true;
            this.trace_S(this.get(x - 1, y - 1, z), nodes, null);
        } else if (this.canTrace_W(x, y - 1, z)) {
            this.trace_W(this.get(x, y - 1, z), nodes, nodes.get(nodes.size() - 1));
        } else if (this.canTrace_N(x, y, z)) {
            this.trace_N(co, nodes, null);
        }
    }

    void trace_N(ClusterOutline co, ArrayList<Node> nodes, Node extend) {
        int x = co.x;
        int y = co.y;
        int z = co.z;
        if (extend != null) {
            extend.setXY(x + 1, y);
        } else {
            Node node2 = Node.alloc().init(x + 1, y, z);
            nodes.add(node2);
        }

        co.tn = true;
        if (this.canTrace_W(x + 1, y - 1, z)) {
            this.get(x + 1, y, z).innerCorner = true;
            this.trace_W(this.get(x + 1, y - 1, z), nodes, null);
        } else if (this.canTrace_N(x + 1, y, z)) {
            this.trace_N(this.get(x + 1, y, z), nodes, nodes.get(nodes.size() - 1));
        } else if (this.canTrace_E(x, y, z)) {
            this.trace_E(co, nodes, null);
        }
    }

    void trace_E(ClusterOutline co, ArrayList<Node> nodes, Node extend) {
        int x = co.x;
        int y = co.y;
        int z = co.z;
        if (extend != null) {
            extend.setXY(x + 1, y + 1);
        } else {
            Node node2 = Node.alloc().init(x + 1, y + 1, z);
            nodes.add(node2);
        }

        co.te = true;
        if (this.canTrace_N(x + 1, y + 1, z)) {
            this.get(x, y + 1, z).innerCorner = true;
            this.trace_N(this.get(x + 1, y + 1, z), nodes, null);
        } else if (this.canTrace_E(x, y + 1, z)) {
            this.trace_E(this.get(x, y + 1, z), nodes, nodes.get(nodes.size() - 1));
        } else if (this.canTrace_S(x, y, z)) {
            this.trace_S(co, nodes, null);
        }
    }

    void trace_S(ClusterOutline co, ArrayList<Node> nodes, Node extend) {
        int x = co.x;
        int y = co.y;
        int z = co.z;
        if (extend != null) {
            extend.setXY(x, y + 1);
        } else {
            Node node2 = Node.alloc().init(x, y + 1, z);
            nodes.add(node2);
        }

        co.ts = true;
        if (this.canTrace_E(x - 1, y + 1, z)) {
            this.get(x - 1, y, z).innerCorner = true;
            this.trace_E(this.get(x - 1, y + 1, z), nodes, null);
        } else if (this.canTrace_S(x - 1, y, z)) {
            this.trace_S(this.get(x - 1, y, z), nodes, nodes.get(nodes.size() - 1));
        } else if (this.canTrace_W(x, y, z)) {
            this.trace_W(co, nodes, null);
        }
    }

    ArrayList<Node> trace(ClusterOutline co) {
        int x = co.x;
        int y = co.y;
        int z = co.z;
        ArrayList<Node> nodes = new ArrayList<>();
        Node node1 = Node.alloc().init(x, y, z);
        nodes.add(node1);
        co.start = true;
        this.trace_N(co, nodes, null);
        Node nodeN = nodes.get(nodes.size() - 1);
        float e = 0.1F;
        if ((int)(node1.x + 0.1F) == (int)(nodeN.x + 0.1F) && (int)(node1.y + 0.1F) == (int)(nodeN.y + 0.1F)) {
            nodeN.release();
            nodes.set(nodes.size() - 1, node1);
        }

        return nodes;
    }
}
