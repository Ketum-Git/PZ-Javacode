// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import java.util.ArrayDeque;
import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.popman.ObjectPool;

public final class MatrixStack {
    private final int mode;
    private final ArrayDeque<Matrix4f> matrices = new ArrayDeque<>();
    private final ObjectPool<Matrix4f> pool = new ObjectPool<>(Matrix4f::new);

    public MatrixStack(int ID) {
        this.mode = ID;
    }

    public Matrix4f alloc() {
        if (Core.debug && Thread.currentThread() != RenderThread.renderThread) {
            boolean var1 = true;
        }

        return this.pool.alloc();
    }

    public void release(Matrix4f matrix) {
        this.pool.release(matrix);
    }

    public void push(Matrix4f m) {
        if (Core.debug && Thread.currentThread() != RenderThread.renderThread) {
            boolean var2 = true;
        }

        this.matrices.push(m);
    }

    public void pop() {
        if (Core.debug && Thread.currentThread() != RenderThread.renderThread) {
            boolean m = true;
        }

        Matrix4f m = this.matrices.pop();
        this.pool.release(m);
    }

    public Matrix4f peek() {
        return this.matrices.peek();
    }

    public boolean isEmpty() {
        return this.matrices.isEmpty();
    }

    public void clear() {
        while (!this.isEmpty()) {
            this.pop();
        }
    }
}
