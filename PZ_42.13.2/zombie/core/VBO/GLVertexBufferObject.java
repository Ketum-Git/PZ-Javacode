// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.VBO;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.ARBMapBufferRange;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjglx.opengl.OpenGLException;
import zombie.core.skinnedmodel.model.VertexBufferObject;

/**
 * Vertex buffer object wrapper
 */
public class GLVertexBufferObject {
    public static IGLBufferObject funcs;
    private long size;
    private final int type;
    private final int usage;
    private transient int id;
    private transient boolean mapped;
    private transient boolean cleared;
    private transient ByteBuffer buffer;
    private int vertexAttribArray = -1;

    public static void init() {
        if (GL.getCapabilities().OpenGL15) {
            System.out.println("OpenGL 1.5 buffer objects supported");
            funcs = new GLBufferObject15();
        } else {
            if (!GL.getCapabilities().GL_ARB_vertex_buffer_object) {
                throw new RuntimeException("Neither OpenGL 1.5 nor GL_ARB_vertex_buffer_object supported");
            }

            System.out.println("GL_ARB_vertex_buffer_object supported");
            funcs = new GLBufferObjectARB();
        }

        VertexBufferObject.funcs = funcs;
    }

    /**
     * C'tor
     */
    public GLVertexBufferObject(long size, int type, int usage) {
        this.size = size;
        this.type = type;
        this.usage = usage;
    }

    /**
     * C'tor
     */
    public GLVertexBufferObject(int type, int usage) {
        this.size = 0L;
        this.type = type;
        this.usage = usage;
    }

    public void create() {
        this.id = funcs.glGenBuffers();
    }

    /**
     * Tells the driver we don't care about the data in our buffer any more (may improve performance before mapping)
     */
    public void clear() {
        if (!this.cleared) {
            funcs.glBufferData(this.type, this.size, this.usage);
            this.cleared = true;
        }
    }

    protected void doDestroy() {
        if (this.id != 0) {
            this.unmap();
            funcs.glDeleteBuffers(this.id);
            this.id = 0;
        }
    }

    public ByteBuffer map(int size) {
        if (!this.mapped) {
            if (this.size != size) {
                this.size = size;
                this.clear();
            }

            if (this.buffer != null && this.buffer.capacity() < size) {
                this.buffer = null;
            }

            ByteBuffer old = this.buffer;
            if (GL.getCapabilities().OpenGL30) {
                int flags = 38;
                this.buffer = GL30.glMapBufferRange(this.type, 0L, size, 38, this.buffer);
            } else if (GL.getCapabilities().GL_ARB_map_buffer_range) {
                int flags = 38;
                this.buffer = ARBMapBufferRange.glMapBufferRange(this.type, 0L, size, 38, this.buffer);
            } else {
                this.buffer = funcs.glMapBuffer(this.type, funcs.GL_WRITE_ONLY(), size, this.buffer);
            }

            if (this.buffer == null) {
                throw new OpenGLException("Failed to map buffer " + this);
            }

            if (this.buffer != old && old != null) {
            }

            this.buffer.order(ByteOrder.nativeOrder()).clear().limit(size);
            this.mapped = true;
            this.cleared = false;
        }

        return this.buffer;
    }

    public ByteBuffer map() {
        if (!this.mapped) {
            assert this.size > 0L;

            this.clear();
            ByteBuffer old = this.buffer;
            if (GL.getCapabilities().OpenGL30) {
                int flags = 38;
                this.buffer = GL30.glMapBufferRange(this.type, 0L, this.size, 38, this.buffer);
            } else if (GL.getCapabilities().GL_ARB_map_buffer_range) {
                int flags = 38;
                this.buffer = ARBMapBufferRange.glMapBufferRange(this.type, 0L, this.size, 38, this.buffer);
            } else {
                this.buffer = funcs.glMapBuffer(this.type, funcs.GL_WRITE_ONLY(), this.size, this.buffer);
            }

            if (this.buffer == null) {
                throw new OpenGLException("Failed to map a buffer " + this.size + " bytes long");
            }

            if (this.buffer != old && old != null) {
            }

            this.buffer.order(ByteOrder.nativeOrder()).clear().limit((int)this.size);
            this.mapped = true;
            this.cleared = false;
        }

        return this.buffer;
    }

    public void orphan() {
        funcs.glMapBuffer(this.type, this.usage, this.size, null);
    }

    public boolean unmap() {
        if (this.mapped) {
            this.mapped = false;
            return funcs.glUnmapBuffer(this.type);
        } else {
            return true;
        }
    }

    public boolean isMapped() {
        return this.mapped;
    }

    public void bufferData(ByteBuffer data) {
        funcs.glBufferData(this.type, data, this.usage);
    }

    @Override
    public String toString() {
        return "GLVertexBufferObject[" + this.id + ", " + this.size + "]";
    }

    public void bind() {
        funcs.glBindBuffer(this.type, this.id);
    }

    public void bindNone() {
        funcs.glBindBuffer(this.type, 0);
    }

    public int getID() {
        return this.id;
    }

    public void enableVertexAttribArray(int index) {
        if (this.vertexAttribArray != index) {
            this.disableVertexAttribArray();
            if (index >= 0) {
                GL20.glEnableVertexAttribArray(index);
            }

            this.vertexAttribArray = index >= 0 ? index : -1;
        }
    }

    public void disableVertexAttribArray() {
        if (this.vertexAttribArray != -1) {
            GL20.glDisableVertexAttribArray(this.vertexAttribArray);
            this.vertexAttribArray = -1;
        }
    }
}
