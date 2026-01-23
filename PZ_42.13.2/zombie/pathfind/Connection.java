// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;

public final class Connection {
    public Node node1;
    public Node node2;
    int flags;
    static final ArrayDeque<Connection> pool = new ArrayDeque<>();

    Connection init(Node node1, Node node2, int flags) {
        this.node1 = node1;
        this.node2 = node2;
        this.flags = flags;
        return this;
    }

    public Node otherNode(Node node) {
        assert node == this.node1 || node == this.node2;

        return node == this.node1 ? this.node2 : this.node1;
    }

    public boolean has(int flags) {
        return (this.flags & flags) != 0;
    }

    static Connection alloc() {
        if (pool.isEmpty()) {
            boolean var0 = false;
        } else {
            boolean var1 = false;
        }

        return pool.isEmpty() ? new Connection() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
