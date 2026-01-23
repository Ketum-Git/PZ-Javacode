// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.rendering;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.Color;

public class ShaderBuffer {
    protected int bufferId;
    protected int binding;
    protected ByteBuffer data;

    protected ShaderBuffer() {
    }

    public ShaderBuffer(int size) {
        this.bufferId = GL15.glGenBuffers();
        this.data = MemoryUtil.memAlloc(size);
        GL15.glBindBuffer(37074, this.bufferId);
        GL45.glNamedBufferStorage(this.bufferId, this.data, 256);
    }

    public int GetBufferID() {
        return this.bufferId;
    }

    public void Release() {
        GL15.glDeleteBuffers(this.bufferId);
        this.data.clear();
    }

    public void SetBinding(int location) {
        if (location >= 0 && this.bufferId >= 0) {
            this.binding = location;
            GL30.glBindBufferBase(37074, location, this.bufferId);
        }
    }

    public int GetBinding() {
        return this.binding;
    }

    public void UpdateData() {
        if (this.bufferId >= 0 && this.data != null) {
            this.PreUpdate();
            this.data.flip();
            this.UpdateBufferData();
            this.data.limit(this.data.capacity());
            this.OnReset();
        }
    }

    protected void PreUpdate() {
    }

    protected void OnReset() {
    }

    protected void UpdateBufferData() {
        GL15.glBindBuffer(37074, this.bufferId);
        GL15.glBufferSubData(37074, 0L, this.data);
        GL30.glBindBufferBase(37074, this.binding, this.bufferId);
    }

    public void Advance(int bytes) {
        this.data.position(this.data.position() + bytes);
    }

    public void SetPosition(int position) {
        this.data.position(position);
    }

    public static void PushBool(ByteBuffer data, boolean b) {
        data.put((byte)(b ? 1 : 0));
    }

    public static void PushInt(ByteBuffer data, int i) {
        data.putInt(i);
    }

    public static void PushFloat(ByteBuffer data, float f) {
        data.putFloat(f);
    }

    public static void PushFloat2(ByteBuffer data, float f1, float f2) {
        data.putFloat(f1);
        data.putFloat(f2);
    }

    public static void PushFloat3(ByteBuffer data, float f1, float f2, float f3) {
        data.putFloat(f1);
        data.putFloat(f2);
        data.putFloat(f3);
    }

    public static void PushFloat4(ByteBuffer data, float f1, float f2, float f3, float f4) {
        data.putFloat(f1);
        data.putFloat(f2);
        data.putFloat(f3);
        data.putFloat(f4);
    }

    public static void PushVector2(ByteBuffer data, Vector2f vec) {
        data.putFloat(vec.x);
        data.putFloat(vec.y);
    }

    public static void PushVector3(ByteBuffer data, Vector3f vec) {
        data.putFloat(vec.x);
        data.putFloat(vec.y);
        data.putFloat(vec.z);
    }

    public static void PushVector4(ByteBuffer data, Vector4f vec) {
        data.putFloat(vec.x);
        data.putFloat(vec.y);
        data.putFloat(vec.z);
        data.putFloat(vec.w);
    }

    public static void PushColor(ByteBuffer data, Color colour) {
        data.putFloat(colour.r);
        data.putFloat(colour.g);
        data.putFloat(colour.b);
        data.putFloat(colour.a);
    }

    public static void PushMatrix3(ByteBuffer data, Matrix3f matrix) {
        data.putFloat(matrix.m00);
        data.putFloat(matrix.m10);
        data.putFloat(matrix.m20);
        data.putFloat(matrix.m01);
        data.putFloat(matrix.m11);
        data.putFloat(matrix.m21);
        data.putFloat(matrix.m02);
        data.putFloat(matrix.m12);
        data.putFloat(matrix.m22);
    }

    public static void PushMatrix4(ByteBuffer data, Matrix4f matrix) {
        data.putFloat(matrix.m00);
        data.putFloat(matrix.m10);
        data.putFloat(matrix.m20);
        data.putFloat(matrix.m30);
        data.putFloat(matrix.m01);
        data.putFloat(matrix.m11);
        data.putFloat(matrix.m21);
        data.putFloat(matrix.m31);
        data.putFloat(matrix.m02);
        data.putFloat(matrix.m12);
        data.putFloat(matrix.m22);
        data.putFloat(matrix.m32);
        data.putFloat(matrix.m03);
        data.putFloat(matrix.m13);
        data.putFloat(matrix.m23);
        data.putFloat(matrix.m33);
    }

    public static void PushIntArray(ByteBuffer data, int[] is) {
        for (int i = 0; i < is.length; i++) {
            data.putInt(i);
        }
    }

    public static void PushFloatArray(ByteBuffer data, float[] fs) {
        for (int i = 0; i < fs.length; i++) {
            data.putFloat(fs[i]);
        }
    }

    public static void PushVector2Array(ByteBuffer data, Vector2f[] vs) {
        for (Vector2f v : vs) {
            PushVector2(data, v);
        }
    }

    public static void PushVector3Array(ByteBuffer data, Vector3f[] vs) {
        for (Vector3f v : vs) {
            PushVector3(data, v);
            PushFloat(data, 0.0F);
        }
    }

    public static void PushVector4Array(ByteBuffer data, Vector4f[] vs) {
        for (Vector4f v : vs) {
            PushVector4(data, v);
        }
    }

    public static void PushMatrix3Array(ByteBuffer data, Matrix3f[] ms) {
        for (Matrix3f m : ms) {
            PushMatrix3(data, m);
        }
    }

    public static void PushMatrix4Array(ByteBuffer data, Matrix4f[] ms) {
        for (Matrix4f m : ms) {
            PushMatrix4(data, m);
        }
    }

    public static void PushTextureArray(ByteBuffer data, int[] ts) {
        for (int i = 0; i < ts.length; i++) {
            data.putInt(i);
        }
    }

    public static void PushColorArray(ByteBuffer data, Color[] cs) {
        for (Color c : cs) {
            PushColor(data, c);
        }
    }
}
