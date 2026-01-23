// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import java.nio.FloatBuffer;
import org.lwjglx.BufferUtils;

public class Matrix4 {
    private final FloatBuffer matrix = FloatBuffer.allocate(16);
    public static Matrix4 identity = new Matrix4();
    private FloatBuffer direct;

    public Matrix4() {
    }

    public Matrix4(float[] m) {
        this();
        this.put(m);
    }

    public Matrix4(Matrix4 m) {
        this();
        this.put(m);
    }

    public Matrix4 clear() {
        for (int a = 0; a < 16; a++) {
            this.matrix.put(a, 0.0F);
        }

        return this;
    }

    public Matrix4 clearToIdentity() {
        return this.clear().put(0, 1.0F).put(5, 1.0F).put(10, 1.0F).put(15, 1.0F);
    }

    public Matrix4 clearToOrtho(float left, float right, float bottom, float top, float near, float far) {
        return this.clear()
            .put(0, 2.0F / (right - left))
            .put(5, 2.0F / (top - bottom))
            .put(10, -2.0F / (far - near))
            .put(12, -(right + left) / (right - left))
            .put(13, -(top + bottom) / (top - bottom))
            .put(14, -(far + near) / (far - near))
            .put(15, 1.0F);
    }

    public Matrix4 clearToPerspective(float fovRad, float width, float height, float near, float far) {
        float fov = 1.0F / (float)Math.tan(fovRad / 2.0F);
        return this.clear()
            .put(0, fov / (width / height))
            .put(5, fov)
            .put(10, (far + near) / (near - far))
            .put(14, 2.0F * far * near / (near - far))
            .put(11, -1.0F);
    }

    public float get(int index) {
        return this.matrix.get(index);
    }

    public Matrix4 put(int index, float f) {
        this.matrix.put(index, f);
        return this;
    }

    public Matrix4 put(int index, Vector3 v, float w) {
        this.put(index * 4 + 0, v.x());
        this.put(index * 4 + 1, v.y());
        this.put(index * 4 + 2, v.z());
        this.put(index * 4 + 3, w);
        return this;
    }

    public Matrix4 put(float[] m) {
        if (m.length < 16) {
            throw new IllegalArgumentException("float array must have at least 16 values.");
        } else {
            this.matrix.position(0);
            this.matrix.put(m, 0, 16);
            return this;
        }
    }

    public Matrix4 put(Matrix4 m) {
        FloatBuffer b = m.getBuffer();

        while (b.hasRemaining()) {
            this.matrix.put(b.get());
        }

        return this;
    }

    public Matrix4 mult(float[] m) {
        float[] newm = new float[16];

        for (int a = 0; a < 16; a += 4) {
            newm[a + 0] = this.get(0) * m[a] + this.get(4) * m[a + 1] + this.get(8) * m[a + 2] + this.get(12) * m[a + 3];
            newm[a + 1] = this.get(1) * m[a] + this.get(5) * m[a + 1] + this.get(9) * m[a + 2] + this.get(13) * m[a + 3];
            newm[a + 2] = this.get(2) * m[a] + this.get(6) * m[a + 1] + this.get(10) * m[a + 2] + this.get(14) * m[a + 3];
            newm[a + 3] = this.get(3) * m[a] + this.get(7) * m[a + 1] + this.get(11) * m[a + 2] + this.get(15) * m[a + 3];
        }

        this.put(newm);
        return this;
    }

    public Matrix4 mult(Matrix4 m) {
        float[] newm = new float[16];

        for (int a = 0; a < 16; a += 4) {
            newm[a + 0] = this.get(0) * m.get(a) + this.get(4) * m.get(a + 1) + this.get(8) * m.get(a + 2) + this.get(12) * m.get(a + 3);
            newm[a + 1] = this.get(1) * m.get(a) + this.get(5) * m.get(a + 1) + this.get(9) * m.get(a + 2) + this.get(13) * m.get(a + 3);
            newm[a + 2] = this.get(2) * m.get(a) + this.get(6) * m.get(a + 1) + this.get(10) * m.get(a + 2) + this.get(14) * m.get(a + 3);
            newm[a + 3] = this.get(3) * m.get(a) + this.get(7) * m.get(a + 1) + this.get(11) * m.get(a + 2) + this.get(15) * m.get(a + 3);
        }

        this.put(newm);
        return this;
    }

    public Matrix4 transpose() {
        float old = this.get(1);
        this.put(1, this.get(4));
        this.put(4, old);
        old = this.get(2);
        this.put(2, this.get(8));
        this.put(8, old);
        old = this.get(3);
        this.put(3, this.get(12));
        this.put(12, old);
        old = this.get(7);
        this.put(7, this.get(13));
        this.put(13, old);
        old = this.get(11);
        this.put(11, this.get(14));
        this.put(14, old);
        old = this.get(6);
        this.put(6, this.get(9));
        this.put(9, old);
        return this;
    }

    public Matrix4 translate(float x, float y, float z) {
        float[] m = new float[16];
        m[0] = 1.0F;
        m[5] = 1.0F;
        m[10] = 1.0F;
        m[15] = 1.0F;
        m[12] = x;
        m[13] = y;
        m[14] = z;
        return this.mult(m);
    }

    public Matrix4 translate(Vector3 vec) {
        return this.translate(vec.x(), vec.y(), vec.z());
    }

    public Matrix4 scale(float x, float y, float z) {
        float[] m = new float[16];
        m[0] = x;
        m[5] = y;
        m[10] = z;
        m[15] = 1.0F;
        return this.mult(m);
    }

    public Matrix4 scale(Vector3 vec) {
        return this.scale(vec.x(), vec.y(), vec.z());
    }

    public Matrix4 rotate(float angle, float x, float y, float z) {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        float invCos = 1.0F - cos;
        Vector3 v = new Vector3(x, y, z).normalize();
        float[] m = new float[16];
        m[0] = v.x() * v.x() + (1.0F - v.x() * v.x()) * cos;
        m[4] = v.x() * v.y() * invCos - v.z() * sin;
        m[8] = v.x() * v.z() * invCos + v.y() * sin;
        m[1] = v.y() * v.x() * invCos + v.z() * sin;
        m[5] = v.y() * v.y() + (1.0F - v.y() * v.y()) * cos;
        m[9] = v.y() * v.z() * invCos - v.x() * sin;
        m[2] = v.z() * v.x() * invCos - v.y() * sin;
        m[6] = v.z() * v.y() * invCos + v.x() * sin;
        m[10] = v.z() * v.z() + (1.0F - v.z() * v.z()) * cos;
        m[15] = 1.0F;
        return this.mult(m);
    }

    public Matrix4 rotate(float angle, Vector3 vec) {
        return this.rotate(angle, vec.x(), vec.y(), vec.z());
    }

    public FloatBuffer getBuffer() {
        if (this.direct == null) {
            this.direct = BufferUtils.createFloatBuffer(16);
        }

        this.direct.clear();
        this.direct.put(this.matrix.position(16).flip());
        this.direct.flip();
        return this.direct;
    }

    static {
        identity.clearToIdentity();
    }
}
