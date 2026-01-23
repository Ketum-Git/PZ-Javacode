// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;
import zombie.core.Core;
import zombie.core.VBO.IGLBufferObject;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.ShaderProgram;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.util.list.PZArrayUtil;

/**
 * Created by LEMMYATI on 03/01/14.
 */
public final class VertexBufferObject {
    public static IGLBufferObject funcs;
    int[] elements;
    VertexBufferObject.Vbo handle;
    private final VertexBufferObject.VertexFormat vertexFormat;
    private final VertexBufferObject.BeginMode beginMode;
    public boolean isStatic;

    public VertexBufferObject() {
        this.isStatic = false;
        this.vertexFormat = new VertexBufferObject.VertexFormat(4);
        this.vertexFormat.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
        this.vertexFormat.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
        this.vertexFormat.setElement(2, VertexBufferObject.VertexType.ColorArray, 4);
        this.vertexFormat.setElement(3, VertexBufferObject.VertexType.TextureCoordArray, 8);
        this.vertexFormat.calculate();
        this.beginMode = VertexBufferObject.BeginMode.Triangles;
    }

    @Deprecated
    public VertexBufferObject(VertexPositionNormalTangentTexture[] vertices, int[] elements) {
        this.elements = elements;
        this.isStatic = true;
        RenderThread.invokeOnRenderContext(this, vertices, elements, (l_this, l_vertices, l_elements) -> l_this.handle = this.LoadVBO(l_vertices, l_elements));
        this.vertexFormat = new VertexBufferObject.VertexFormat(4);
        this.vertexFormat.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
        this.vertexFormat.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
        this.vertexFormat.setElement(2, VertexBufferObject.VertexType.TangentArray, 12);
        this.vertexFormat.setElement(3, VertexBufferObject.VertexType.TextureCoordArray, 8);
        this.vertexFormat.calculate();
        this.beginMode = VertexBufferObject.BeginMode.Triangles;
    }

    @Deprecated
    public VertexBufferObject(VertexPositionNormalTangentTextureSkin[] vertices, int[] elements, boolean bReverse) {
        this.elements = elements;
        if (bReverse) {
            int[] elements2 = new int[elements.length];
            int ii = 0;

            for (int i = elements.length - 1 - 2; i >= 0; i -= 3) {
                elements2[ii] = elements[i];
                elements2[ii + 1] = elements[i + 1];
                elements2[ii + 2] = elements[i + 2];
                ii += 3;
            }

            elements = elements2;
        }

        this.isStatic = false;
        this.handle = this.LoadVBO(vertices, elements);
        this.vertexFormat = new VertexBufferObject.VertexFormat(6);
        this.vertexFormat.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
        this.vertexFormat.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
        this.vertexFormat.setElement(3, VertexBufferObject.VertexType.TextureCoordArray, 8);
        this.vertexFormat.setElement(4, VertexBufferObject.VertexType.BlendWeightArray, 16);
        this.vertexFormat.setElement(5, VertexBufferObject.VertexType.BlendIndexArray, 16);
        this.vertexFormat.calculate();
        this.beginMode = VertexBufferObject.BeginMode.Triangles;
    }

    public VertexBufferObject(VertexBufferObject.VertexArray vertices, int[] elements) {
        this.vertexFormat = vertices.format;
        this.elements = elements;
        this.isStatic = true;
        RenderThread.invokeOnRenderContext(this, vertices, elements, (l_this, l_vertices, l_elements) -> l_this.handle = this.LoadVBO(l_vertices, l_elements));
        this.beginMode = VertexBufferObject.BeginMode.Triangles;
    }

    public VertexBufferObject(VertexBufferObject.VertexArray vertices, int[] elements, boolean bReverse) {
        this.vertexFormat = vertices.format;
        if (bReverse) {
            int[] elements2 = new int[elements.length];
            int ii = 0;

            for (int i = elements.length - 1 - 2; i >= 0; i -= 3) {
                elements2[ii] = elements[i];
                elements2[ii + 1] = elements[i + 1];
                elements2[ii + 2] = elements[i + 2];
                ii += 3;
            }

            elements = elements2;
        }

        this.elements = elements;
        this.isStatic = false;
        this.handle = this.LoadVBO(vertices, elements);
        this.beginMode = VertexBufferObject.BeginMode.Triangles;
    }

    @Deprecated
    private VertexBufferObject.Vbo LoadVBO(VertexPositionNormalTangentTextureSkin[] vertices, int[] elements) {
        VertexBufferObject.Vbo handle = new VertexBufferObject.Vbo();
        int size = 0;
        int stride = 76;
        handle.faceDataOnly = false;
        ByteBuffer buf = BufferUtils.createByteBuffer(vertices.length * 76);
        ByteBuffer elementsBuffer = BufferUtils.createByteBuffer(elements.length * 4);

        for (int n = 0; n < vertices.length; n++) {
            vertices[n].put(buf);
        }

        for (int n = 0; n < elements.length; n++) {
            elementsBuffer.putInt(elements[n]);
        }

        buf.flip();
        elementsBuffer.flip();
        handle.vboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
        funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), buf, funcs.GL_STATIC_DRAW());
        funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        size = handle.b.get();
        if (vertices.length * 76 != size) {
            throw new RuntimeException("Vertex data not uploaded correctly");
        } else {
            handle.eboId = funcs.glGenBuffers();
            funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
            funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), elementsBuffer, funcs.GL_STATIC_DRAW());
            handle.b.clear();
            funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
            size = handle.b.get();
            if (elements.length * 4 != size) {
                throw new RuntimeException("Element data not uploaded correctly");
            } else {
                handle.numElements = elements.length;
                handle.vertexStride = 76;
                return handle;
            }
        }
    }

    public VertexBufferObject.Vbo LoadSoftwareVBO(ByteBuffer vertices, VertexBufferObject.Vbo vbo, int[] elements) {
        VertexBufferObject.Vbo handle = vbo;
        boolean bNew = false;
        ByteBuffer elBuf = null;
        if (vbo == null) {
            bNew = true;
            handle = new VertexBufferObject.Vbo();
            handle.vboId = funcs.glGenBuffers();
            ByteBuffer elementsBuffer = BufferUtils.createByteBuffer(elements.length * 4);

            for (int n = 0; n < elements.length; n++) {
                elementsBuffer.putInt(elements[n]);
            }

            elementsBuffer.flip();
            elBuf = elementsBuffer;
            handle.vertexStride = 36;
            handle.numElements = elements.length;
        } else {
            vbo.b.clear();
        }

        handle.faceDataOnly = false;
        funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
        funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), vertices, funcs.GL_STATIC_DRAW());
        funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        if (elBuf != null) {
            handle.eboId = funcs.glGenBuffers();
            funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
            funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), elBuf, funcs.GL_STATIC_DRAW());
        }

        return handle;
    }

    @Deprecated
    private VertexBufferObject.Vbo LoadVBO(VertexPositionNormalTangentTexture[] vertices, int[] elements) {
        VertexBufferObject.Vbo handle = new VertexBufferObject.Vbo();
        int size = 0;
        int stride = 44;
        handle.faceDataOnly = false;
        ByteBuffer buf = BufferUtils.createByteBuffer(vertices.length * 44);
        ByteBuffer elementsBuffer = BufferUtils.createByteBuffer(elements.length * 4);

        for (int n = 0; n < vertices.length; n++) {
            vertices[n].put(buf);
        }

        for (int n = 0; n < elements.length; n++) {
            elementsBuffer.putInt(elements[n]);
        }

        buf.flip();
        elementsBuffer.flip();
        handle.vboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
        funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), buf, funcs.GL_STATIC_DRAW());
        funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        size = handle.b.get();
        if (vertices.length * 44 != size) {
            throw new RuntimeException("Vertex data not uploaded correctly");
        } else {
            handle.eboId = funcs.glGenBuffers();
            funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
            funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), elementsBuffer, funcs.GL_STATIC_DRAW());
            handle.b.clear();
            funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
            size = handle.b.get();
            if (elements.length * 4 != size) {
                throw new RuntimeException("Element data not uploaded correctly");
            } else {
                handle.numElements = elements.length;
                handle.vertexStride = 44;
                return handle;
            }
        }
    }

    private VertexBufferObject.Vbo LoadVBO(VertexBufferObject.VertexArray vertices, int[] elements) {
        VertexBufferObject.Vbo handle = new VertexBufferObject.Vbo();
        handle.faceDataOnly = false;
        ByteBuffer elementsBuffer = MemoryUtil.memAlloc(elements.length * 4);

        for (int n = 0; n < elements.length; n++) {
            elementsBuffer.putInt(elements[n]);
        }

        vertices.buffer.position(0);
        vertices.buffer.limit(vertices.numVertices * vertices.format.stride);
        elementsBuffer.flip();
        handle.vboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
        funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), vertices.buffer, funcs.GL_STATIC_DRAW());
        funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        int size = handle.b.get();
        if (vertices.numVertices * vertices.format.stride != size) {
            throw new RuntimeException("Vertex data not uploaded correctly");
        } else {
            handle.eboId = funcs.glGenBuffers();
            funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
            funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), elementsBuffer, funcs.GL_STATIC_DRAW());
            MemoryUtil.memFree(elementsBuffer);
            handle.b.clear();
            funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
            size = handle.b.get();
            if (elements.length * 4 != size) {
                throw new RuntimeException("Element data not uploaded correctly");
            } else {
                handle.numElements = elements.length;
                handle.vertexStride = vertices.format.stride;
                return handle;
            }
        }
    }

    public void clear() {
        if (this.handle != null) {
            if (this.handle.vboId > 0) {
                funcs.glDeleteBuffers(this.handle.vboId);
                this.handle.vboId = -1;
            }

            if (this.handle.eboId > 0) {
                funcs.glDeleteBuffers(this.handle.eboId);
                this.handle.eboId = -1;
            }

            this.handle = null;
        }
    }

    public int BeginInstancedDraw(Shader shader) {
        if (CanDraw(this.handle)) {
            boolean bBlendWeights = BeginDraw(this.handle, this.vertexFormat, shader, 4);
            return bBlendWeights ? 1 : 0;
        } else {
            return -1;
        }
    }

    public void FinishInstancedDraw(Shader shader, boolean bBlendWeights) {
        this.FinishDraw(shader, bBlendWeights);
    }

    public boolean BeginDraw(Shader shader) {
        return BeginDraw(this.handle, this.vertexFormat, shader, 4);
    }

    public void Draw(Shader shader) {
        Draw(this.handle, this.vertexFormat, shader, 4);
    }

    public void DrawInstanced(Shader shader, int instanceCount) {
        DrawInstanced(this.handle, this.vertexFormat, shader, 4, instanceCount);
    }

    public void DrawStrip(Shader shader) {
        Draw(this.handle, this.vertexFormat, shader, 5);
    }

    private static boolean CanDraw(VertexBufferObject.Vbo handle) {
        return handle != null && !DebugOptions.instance.debugDrawSkipVboDraw.getValue();
    }

    private static boolean BeginDraw(VertexBufferObject.Vbo handle, VertexBufferObject.VertexFormat vertexFormat, Shader shader, int vertexType) {
        int textureNumber = 33984;
        boolean bBlendWeights = false;
        if (!handle.faceDataOnly) {
            setModelViewProjection(shader);
            funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);

            for (int i = 0; i < vertexFormat.elements.length; i++) {
                VertexBufferObject.VertexElement element = vertexFormat.elements[i];
                switch (element.type) {
                    case VertexArray:
                        GL20.glVertexAttribPointer(i, 3, 5126, false, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        break;
                    case NormalArray:
                        GL20.glVertexAttribPointer(i, 3, 5126, true, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        break;
                    case ColorArray:
                        GL20.glVertexAttribPointer(i, 3, 5121, true, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                    case IndexArray:
                    case TangentArray:
                    default:
                        break;
                    case TextureCoordArray:
                        GL13.glActiveTexture(textureNumber);
                        GL20.glVertexAttribPointer(i, 2, 5126, false, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        textureNumber++;
                        break;
                    case BlendWeightArray:
                        GL20.glVertexAttribPointer(i, 4, 5126, false, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        bBlendWeights = true;
                        break;
                    case BlendIndexArray:
                        GL20.glVertexAttribPointer(i, 4, 5126, false, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                }
            }
        }

        funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
        return bBlendWeights;
    }

    public void FinishDraw(Shader shader, boolean bBlendWeights) {
        FinishDraw(this.vertexFormat, shader, bBlendWeights);
    }

    public static void FinishDraw(VertexBufferObject.VertexFormat vertexFormat, Shader shader, boolean bBlendWeights) {
        if (bBlendWeights && shader != null) {
            int bw = PZArrayUtil.indexOf(vertexFormat.elements, x -> x.type == VertexBufferObject.VertexType.BlendWeightArray);
            int bi = PZArrayUtil.indexOf(vertexFormat.elements, x -> x.type == VertexBufferObject.VertexType.BlendIndexArray);
            GL20.glDisableVertexAttribArray(bw);
            GL20.glDisableVertexAttribArray(bi);
        }
    }

    private static void Draw(VertexBufferObject.Vbo handle, VertexBufferObject.VertexFormat vertexFormat, Shader shader, int vertexType) {
        if (CanDraw(handle)) {
            boolean bBlendWeights = BeginDraw(handle, vertexFormat, shader, vertexType);
            GL11.glDrawElements(vertexType, handle.numElements, 5125, 0L);
            FinishDraw(vertexFormat, shader, bBlendWeights);
        }
    }

    private static void DrawInstanced(
        VertexBufferObject.Vbo handle, VertexBufferObject.VertexFormat vertexFormat, Shader shader, int vertexType, int instanceCount
    ) {
        if (CanDraw(handle)) {
            boolean bBlendWeights = BeginDraw(handle, vertexFormat, shader, vertexType);
            GL31.glDrawElementsInstanced(vertexType, handle.numElements, 5125, 0L, instanceCount);
            FinishDraw(vertexFormat, shader, bBlendWeights);
        }
    }

    public void PushDrawCall() {
        GL11.glDrawElements(4, this.handle.numElements, 5125, 0L);
    }

    public static void getModelViewProjection(Matrix4f mvp) {
        Core core = Core.getInstance();
        if (!core.projectionMatrixStack.isEmpty() && !core.modelViewMatrixStack.isEmpty()) {
            Matrix4f PRJ = Core.getInstance().projectionMatrixStack.peek();
            Matrix4f MV = Core.getInstance().modelViewMatrixStack.peek();
            PRJ.mul(MV, mvp);
        } else {
            DebugLog.Shader.warn("Matrix stack is empty");
            mvp.identity();
        }
    }

    public static float getDepthValueAt(float x, float y, float z) {
        Matrix4f mvp = VertexBufferObject.L_getModelViewProjection.MVPjoml;
        getModelViewProjection(mvp);
        Vector3f pos = VertexBufferObject.L_getModelViewProjection.vector3f.set(x, y, z);
        mvp.transformPosition(pos);
        return pos.z;
    }

    public static void setModelViewProjection(Shader shader) {
        if (shader != null) {
            setModelViewProjection(shader.getShaderProgram());
        }
    }

    public static void setModelViewProjection(ShaderProgram shaderProgram) {
        if (shaderProgram != null && shaderProgram.isCompiled()) {
            ShaderProgram.Uniform uMVP = shaderProgram.getUniform("ModelViewProjection", 35676);
            if (uMVP != null) {
                Matrix4f PRJ = VertexBufferObject.L_setModelViewProjection.PRJ;
                Matrix4f MV = VertexBufferObject.L_setModelViewProjection.MV;
                if (Core.getInstance().modelViewMatrixStack.isEmpty()) {
                    MV.identity();
                    PRJ.identity();
                } else {
                    MV.set(Core.getInstance().modelViewMatrixStack.peek());
                    PRJ.set(Core.getInstance().projectionMatrixStack.peek());
                }

                if (!MV.equals(shaderProgram.modelView) || !PRJ.equals(shaderProgram.projection)) {
                    shaderProgram.modelView.set(MV);
                    shaderProgram.projection.set(PRJ);
                    PRJ.mul(MV);
                    shaderProgram.setValue("ModelViewProjection", PRJ);
                }
            }
        }
    }

    public static enum BeginMode {
        Triangles;
    }

    private static final class L_getModelViewProjection {
        static final Matrix4f MVPjoml = new Matrix4f();
        static final Vector3f vector3f = new Vector3f();
    }

    private static final class L_setModelViewProjection {
        static final Matrix4f MV = new Matrix4f();
        static final Matrix4f PRJ = new Matrix4f();
    }

    public static final class Vbo {
        public final IntBuffer b = BufferUtils.createIntBuffer(4);
        public int vboId;
        public int eboId;
        public int numElements;
        public int vertexStride;
        public boolean faceDataOnly;
    }

    public static final class VertexArray {
        public final VertexBufferObject.VertexFormat format;
        public final int numVertices;
        public final ByteBuffer buffer;

        public VertexArray(VertexBufferObject.VertexFormat format, int numVertices) {
            this.format = format;
            this.numVertices = numVertices;
            this.buffer = BufferUtils.createByteBuffer(this.numVertices * this.format.stride);
        }

        public void setElement(int vertex, int element, float v1, float v2) {
            int index = vertex * this.format.stride + this.format.elements[element].byteOffset;
            this.buffer.putFloat(index, v1);
            index += 4;
            this.buffer.putFloat(index, v2);
        }

        public void setElement(int vertex, int element, float v1, float v2, float v3) {
            int index = vertex * this.format.stride + this.format.elements[element].byteOffset;
            this.buffer.putFloat(index, v1);
            index += 4;
            this.buffer.putFloat(index, v2);
            index += 4;
            this.buffer.putFloat(index, v3);
        }

        public void setElement(int vertex, int element, float v1, float v2, float v3, float v4) {
            int index = vertex * this.format.stride + this.format.elements[element].byteOffset;
            this.buffer.putFloat(index, v1);
            index += 4;
            this.buffer.putFloat(index, v2);
            index += 4;
            this.buffer.putFloat(index, v3);
            index += 4;
            this.buffer.putFloat(index, v4);
        }

        public float getElementFloat(int vertex, int element, int n) {
            int index = vertex * this.format.stride + this.format.elements[element].byteOffset + n * 4;
            return this.buffer.getFloat(index);
        }
    }

    public static final class VertexElement {
        public VertexBufferObject.VertexType type;
        public int byteSize;
        public int byteOffset;
    }

    public static final class VertexFormat {
        final VertexBufferObject.VertexElement[] elements;
        int stride;

        public VertexFormat(int numElements) {
            this.elements = PZArrayUtil.newInstance(VertexBufferObject.VertexElement.class, numElements, VertexBufferObject.VertexElement::new);
        }

        public void setElement(int index, VertexBufferObject.VertexType type, int byteSize) {
            this.elements[index].type = type;
            this.elements[index].byteSize = byteSize;
        }

        public int getNumElements() {
            return this.elements.length;
        }

        public VertexBufferObject.VertexElement getElement(int index) {
            return this.elements[index];
        }

        public int indexOf(VertexBufferObject.VertexType vertexType) {
            for (int i = 0; i < this.elements.length; i++) {
                VertexBufferObject.VertexElement element = this.elements[i];
                if (element.type == vertexType) {
                    return i;
                }
            }

            return -1;
        }

        public void calculate() {
            this.stride = 0;

            for (int i = 0; i < this.elements.length; i++) {
                this.elements[i].byteOffset = this.stride;
                this.stride = this.stride + this.elements[i].byteSize;
            }
        }

        public int getStride() {
            return this.stride;
        }
    }

    public static enum VertexType {
        VertexArray,
        NormalArray,
        ColorArray,
        IndexArray,
        TextureCoordArray,
        TangentArray,
        BlendWeightArray,
        BlendIndexArray,
        Depth;
    }
}
