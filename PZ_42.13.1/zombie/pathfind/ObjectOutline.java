// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;

@Deprecated
final class ObjectOutline {
    int x;
    int y;
    int z;
    boolean nw;
    boolean nwW;
    boolean nwN;
    boolean nwE;
    boolean nwS;
    boolean wW;
    boolean wE;
    boolean wCutoff;
    boolean nN;
    boolean nS;
    boolean nCutoff;
    ArrayList<Node> nodes;
    static final ArrayDeque<ObjectOutline> pool = new ArrayDeque<>();

    ObjectOutline init(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.nw = this.nwW = this.nwN = this.nwE = false;
        this.wW = this.wE = this.wCutoff = false;
        this.nN = this.nS = this.nCutoff = false;
        return this;
    }

    static void setSolid(int x, int y, int z, ObjectOutline[][] oo) {
        setWest(x, y, z, oo);
        setNorth(x, y, z, oo);
        setWest(x + 1, y, z, oo);
        setNorth(x, y + 1, z, oo);
    }

    static void setWest(int x, int y, int z, ObjectOutline[][] oo) {
        ObjectOutline f1 = get(x, y, z, oo);
        if (f1 != null) {
            if (f1.nw) {
                f1.nwS = false;
            } else {
                f1.nw = true;
                f1.nwW = true;
                f1.nwN = true;
                f1.nwE = true;
                f1.nwS = false;
            }

            f1.wW = true;
            f1.wE = true;
        }

        f1 = get(x, y + 1, z, oo);
        if (f1 == null) {
            if (f1 != null) {
                f1.wCutoff = true;
            }
        } else if (f1.nw) {
            f1.nwN = false;
        } else {
            f1.nw = true;
            f1.nwN = false;
            f1.nwW = true;
            f1.nwE = true;
            f1.nwS = true;
        }
    }

    static void setNorth(int x, int y, int z, ObjectOutline[][] oo) {
        ObjectOutline f1 = get(x, y, z, oo);
        if (f1 != null) {
            if (f1.nw) {
                f1.nwE = false;
            } else {
                f1.nw = true;
                f1.nwW = true;
                f1.nwN = true;
                f1.nwE = false;
                f1.nwS = true;
            }

            f1.nN = true;
            f1.nS = true;
        }

        f1 = get(x + 1, y, z, oo);
        if (f1 == null) {
            if (f1 != null) {
                f1.nCutoff = true;
            }
        } else if (f1.nw) {
            f1.nwW = false;
        } else {
            f1.nw = true;
            f1.nwN = true;
            f1.nwW = false;
            f1.nwE = true;
            f1.nwS = true;
        }
    }

    static ObjectOutline get(int x, int y, int z, ObjectOutline[][] oo) {
        if (x < 0 || x >= oo.length) {
            return null;
        } else if (y >= 0 && y < oo[0].length) {
            if (oo[x][y] == null) {
                oo[x][y] = alloc().init(x, y, z);
            }

            return oo[x][y];
        } else {
            return null;
        }
    }

    void trace_NW_N(ObjectOutline[][] oo, Node extend) {
        if (extend != null) {
            extend.setXY(this.x + 0.3F, this.y - 0.3F);
        } else {
            Node node2 = Node.alloc().init(this.x + 0.3F, this.y - 0.3F, this.z);
            this.nodes.add(node2);
        }

        this.nwN = false;
        if (this.nwE) {
            this.trace_NW_E(oo, null);
        } else if (this.nN) {
            this.trace_N_N(oo, this.nodes.get(this.nodes.size() - 1));
        }
    }

    void trace_NW_S(ObjectOutline[][] oo, Node extend) {
        if (extend != null) {
            extend.setXY(this.x - 0.3F, this.y + 0.3F);
        } else {
            Node node2 = Node.alloc().init(this.x - 0.3F, this.y + 0.3F, this.z);
            this.nodes.add(node2);
        }

        this.nwS = false;
        if (this.nwW) {
            this.trace_NW_W(oo, null);
        } else {
            ObjectOutline f1 = get(this.x - 1, this.y, this.z, oo);
            if (f1 == null) {
                return;
            }

            if (f1.nS) {
                f1.nodes = this.nodes;
                f1.trace_N_S(oo, this.nodes.get(this.nodes.size() - 1));
            }
        }
    }

    void trace_NW_W(ObjectOutline[][] oo, Node extend) {
        if (extend != null) {
            extend.setXY(this.x - 0.3F, this.y - 0.3F);
        } else {
            Node node2 = Node.alloc().init(this.x - 0.3F, this.y - 0.3F, this.z);
            this.nodes.add(node2);
        }

        this.nwW = false;
        if (this.nwN) {
            this.trace_NW_N(oo, null);
        } else {
            ObjectOutline f1 = get(this.x, this.y - 1, this.z, oo);
            if (f1 == null) {
                return;
            }

            if (f1.wW) {
                f1.nodes = this.nodes;
                f1.trace_W_W(oo, this.nodes.get(this.nodes.size() - 1));
            }
        }
    }

    void trace_NW_E(ObjectOutline[][] oo, Node extend) {
        if (extend != null) {
            extend.setXY(this.x + 0.3F, this.y + 0.3F);
        } else {
            Node node2 = Node.alloc().init(this.x + 0.3F, this.y + 0.3F, this.z);
            this.nodes.add(node2);
        }

        this.nwE = false;
        if (this.nwS) {
            this.trace_NW_S(oo, null);
        } else if (this.wE) {
            this.trace_W_E(oo, this.nodes.get(this.nodes.size() - 1));
        }
    }

    void trace_W_E(ObjectOutline[][] oo, Node extend) {
        if (extend != null) {
            extend.setXY(this.x + 0.3F, this.y + 1 - 0.3F);
        } else {
            Node node2 = Node.alloc().init(this.x + 0.3F, this.y + 1 - 0.3F, this.z);
            this.nodes.add(node2);
        }

        this.wE = false;
        if (this.wCutoff) {
            Node node2 = this.nodes.get(this.nodes.size() - 1);
            node2.setXY(this.x + 0.3F, this.y + 1 + 0.3F);
            node2 = Node.alloc().init(this.x - 0.3F, this.y + 1 + 0.3F, this.z);
            this.nodes.add(node2);
            node2 = Node.alloc().init(this.x - 0.3F, this.y + 1 - 0.3F, this.z);
            this.nodes.add(node2);
            this.trace_W_W(oo, node2);
        } else {
            ObjectOutline f1 = get(this.x, this.y + 1, this.z, oo);
            if (f1 != null) {
                if (f1.nw && f1.nwE) {
                    f1.nodes = this.nodes;
                    f1.trace_NW_E(oo, this.nodes.get(this.nodes.size() - 1));
                } else if (f1.nN) {
                    f1.nodes = this.nodes;
                    f1.trace_N_N(oo, null);
                }
            }
        }
    }

    void trace_W_W(ObjectOutline[][] oo, Node extend) {
        if (extend != null) {
            extend.setXY(this.x - 0.3F, this.y + 0.3F);
        } else {
            Node node2 = Node.alloc().init(this.x - 0.3F, this.y + 0.3F, this.z);
            this.nodes.add(node2);
        }

        this.wW = false;
        if (this.nwW) {
            this.trace_NW_W(oo, this.nodes.get(this.nodes.size() - 1));
        } else {
            ObjectOutline f1 = get(this.x - 1, this.y, this.z, oo);
            if (f1 == null) {
                return;
            }

            if (f1.nS) {
                f1.nodes = this.nodes;
                f1.trace_N_S(oo, null);
            }
        }
    }

    void trace_N_N(ObjectOutline[][] oo, Node extend) {
        if (extend != null) {
            extend.setXY(this.x + 1 - 0.3F, this.y - 0.3F);
        } else {
            Node node2 = Node.alloc().init(this.x + 1 - 0.3F, this.y - 0.3F, this.z);
            this.nodes.add(node2);
        }

        this.nN = false;
        if (this.nCutoff) {
            Node node2 = this.nodes.get(this.nodes.size() - 1);
            node2.setXY(this.x + 1 + 0.3F, this.y - 0.3F);
            node2 = Node.alloc().init(this.x + 1 + 0.3F, this.y + 0.3F, this.z);
            this.nodes.add(node2);
            node2 = Node.alloc().init(this.x + 1 - 0.3F, this.y + 0.3F, this.z);
            this.nodes.add(node2);
            this.trace_N_S(oo, node2);
        } else {
            ObjectOutline f1 = get(this.x + 1, this.y, this.z, oo);
            if (f1 != null) {
                if (f1.nwN) {
                    f1.nodes = this.nodes;
                    f1.trace_NW_N(oo, this.nodes.get(this.nodes.size() - 1));
                } else {
                    f1 = get(this.x + 1, this.y - 1, this.z, oo);
                    if (f1 == null) {
                        return;
                    }

                    if (f1.wW) {
                        f1.nodes = this.nodes;
                        f1.trace_W_W(oo, null);
                    }
                }
            }
        }
    }

    void trace_N_S(ObjectOutline[][] oo, Node extend) {
        if (extend != null) {
            extend.setXY(this.x + 0.3F, this.y + 0.3F);
        } else {
            Node node2 = Node.alloc().init(this.x + 0.3F, this.y + 0.3F, this.z);
            this.nodes.add(node2);
        }

        this.nS = false;
        if (this.nwS) {
            this.trace_NW_S(oo, this.nodes.get(this.nodes.size() - 1));
        } else if (this.wE) {
            this.trace_W_E(oo, null);
        }
    }

    void trace(ObjectOutline[][] oo, ArrayList<Node> nodes) {
        nodes.clear();
        this.nodes = nodes;
        Node node1 = Node.alloc().init(this.x - 0.3F, this.y - 0.3F, this.z);
        nodes.add(node1);
        this.trace_NW_N(oo, null);
        if (nodes.size() != 2 && node1.x == nodes.get(nodes.size() - 1).x && node1.y == nodes.get(nodes.size() - 1).y) {
            nodes.get(nodes.size() - 1).release();
            nodes.set(nodes.size() - 1, node1);
        } else {
            nodes.clear();
        }
    }

    static ObjectOutline alloc() {
        return pool.isEmpty() ? new ObjectOutline() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
