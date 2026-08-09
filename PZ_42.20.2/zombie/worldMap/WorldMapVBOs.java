// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.VBO.IGLBufferObject;

public final class WorldMapVBOs {
    private static final int VERTEX_SIZE = 12;
    private static final int COLOR_SIZE = 16;
    private static final int ELEMENT_SIZE = 28;
    private static final int COLOR_OFFSET = 12;
    public static final int NUM_ELEMENTS = 2340;
    private static final int INDEX_SIZE = 2;
    private static final WorldMapVBOs instance = new WorldMapVBOs();
    private final ArrayList<WorldMapVBOs.WorldMapVBO> vbos = new ArrayList<>();
    private ByteBuffer elements;
    private ByteBuffer indices;

    public static WorldMapVBOs getInstance() {
        return instance;
    }

    public void create() {
        this.elements = BufferUtils.createByteBuffer(65520);
        this.indices = BufferUtils.createByteBuffer(4680);
    }

    private void flush() {
        if (this.vbos.isEmpty()) {
            WorldMapVBOs.WorldMapVBO vbo = new WorldMapVBOs.WorldMapVBO();
            vbo.create();
            this.vbos.add(vbo);
        }

        this.elements.flip();
        this.indices.flip();
        this.vbos.get(this.vbos.size() - 1).flush(this.elements, this.indices);
        this.elements.position(this.elements.limit());
        this.elements.limit(this.elements.capacity());
        this.indices.position(this.indices.limit());
        this.indices.limit(this.indices.capacity());
    }

    private void addVBO() {
        WorldMapVBOs.WorldMapVBO vbo = new WorldMapVBOs.WorldMapVBO();
        vbo.create();
        this.vbos.add(vbo);
        this.elements.clear();
        this.indices.clear();
    }

    public void reserveVertices(int numVertices, int[] indices) {
        if (this.indices == null) {
            this.create();
        }

        int currentCount = this.indices.position() / 2;
        if (currentCount + numVertices > 2340) {
            this.flush();
            this.addVBO();
        }

        indices[0] = this.vbos.isEmpty() ? 0 : this.vbos.size() - 1;
        indices[1] = this.indices.position() / 2;
    }

    public void addElement(float x, float y, float z, float r, float g, float b, float a) {
        this.elements.putFloat(x);
        this.elements.putFloat(y);
        this.elements.putFloat(z);
        this.elements.putFloat(r);
        this.elements.putFloat(g);
        this.elements.putFloat(b);
        this.elements.putFloat(a);
        short index = (short)(this.indices.position() / 2);
        this.indices.putShort(index);
    }

    public void drawElements(int mode, int index1, int index2, int count) {
        if (index1 >= 0 && index1 < this.vbos.size()) {
            WorldMapVBOs.WorldMapVBO vbo = this.vbos.get(index1);
            if (index2 >= 0 && index2 + count <= vbo.elementCount) {
                vbo.vbo.bind();
                vbo.ibo.bind();
                GL11.glEnableClientState(32884);
                GL11.glDisableClientState(32886);
                GL11.glVertexPointer(3, 5126, 28, 0L);

                for (int i = 7; i >= 0; i--) {
                    GL13.glActiveTexture(33984 + i);
                    GL11.glDisable(3553);
                }

                GL11.glDisable(2929);
                GL12.glDrawRangeElements(mode, index2, index2 + count, count, 5123, index2 * 2);
                vbo.vbo.bindNone();
                vbo.ibo.bindNone();
            }
        }
    }

    public void reset() {
    }

    private static final class WorldMapVBO {
        GLVertexBufferObject vbo;
        GLVertexBufferObject ibo;
        int elementCount;

        void create() {
            IGLBufferObject funcs = GLVertexBufferObject.funcs;
            this.vbo = new GLVertexBufferObject(65520L, funcs.GL_ARRAY_BUFFER(), funcs.GL_STREAM_DRAW());
            this.vbo.create();
            this.ibo = new GLVertexBufferObject(4680L, funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_STREAM_DRAW());
            this.ibo.create();
        }

        void flush(ByteBuffer elements, ByteBuffer indices) {
            this.vbo.bind();
            this.vbo.bufferData(elements);
            this.ibo.bind();
            this.ibo.bufferData(indices);
            this.elementCount = indices.limit() / 2;
        }
    }
}
