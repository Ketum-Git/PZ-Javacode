// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;

final class ImmutableRectF {
    private float x;
    private float y;
    private float w;
    private float h;
    static final ArrayDeque<ImmutableRectF> pool = new ArrayDeque<>();

    ImmutableRectF init(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        return this;
    }

    float left() {
        return this.x;
    }

    float top() {
        return this.y;
    }

    float right() {
        return this.x + this.w;
    }

    float bottom() {
        return this.y + this.h;
    }

    float width() {
        return this.w;
    }

    float height() {
        return this.h;
    }

    boolean containsPoint(float x, float y) {
        return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom();
    }

    boolean intersects(ImmutableRectF other) {
        return this.left() < other.right() && this.right() > other.left() && this.top() < other.bottom() && this.bottom() > other.top();
    }

    static ImmutableRectF alloc() {
        return pool.isEmpty() ? new ImmutableRectF() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
