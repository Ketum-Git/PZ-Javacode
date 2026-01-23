// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.VBO;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.ARBVertexBufferObject;

public final class GLBufferObjectARB implements IGLBufferObject {
    @Override
    public int GL_ARRAY_BUFFER() {
        return 34962;
    }

    @Override
    public int GL_ELEMENT_ARRAY_BUFFER() {
        return 34963;
    }

    @Override
    public int GL_STATIC_DRAW() {
        return 35044;
    }

    @Override
    public int GL_STREAM_DRAW() {
        return 35040;
    }

    @Override
    public int GL_BUFFER_SIZE() {
        return 34660;
    }

    @Override
    public int GL_WRITE_ONLY() {
        return 35001;
    }

    @Override
    public int glGenBuffers() {
        return ARBVertexBufferObject.glGenBuffersARB();
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        ARBVertexBufferObject.glBindBufferARB(target, buffer);
    }

    @Override
    public void glDeleteBuffers(int buffers) {
        ARBVertexBufferObject.glDeleteBuffersARB(buffers);
    }

    @Override
    public void glBufferData(int target, ByteBuffer data, int usage) {
        ARBVertexBufferObject.glBufferDataARB(target, data, usage);
    }

    @Override
    public void glBufferData(int target, long data_size, int usage) {
        ARBVertexBufferObject.glBufferDataARB(target, data_size, usage);
    }

    @Override
    public ByteBuffer glMapBuffer(int target, int access, long length, ByteBuffer old_buffer) {
        return ARBVertexBufferObject.glMapBufferARB(target, access, length, old_buffer);
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return ARBVertexBufferObject.glUnmapBufferARB(target);
    }

    @Override
    public void glGetBufferParameter(int target, int pname, IntBuffer params) {
        ARBVertexBufferObject.glGetBufferParameterivARB(target, pname, params);
    }
}
