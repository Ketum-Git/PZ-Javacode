// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

/**
 * Created by LEMMYATI on 03/01/14.
 */
public final class Vector3 {
    private float x;
    private float y;
    private float z;

    public Vector3() {
        this(0.0F, 0.0F, 0.0F);
    }

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3(Vector3 vec) {
        this.set(vec);
    }

    public float x() {
        return this.x;
    }

    public Vector3 x(float x) {
        this.x = x;
        return this;
    }

    public float y() {
        return this.y;
    }

    public Vector3 y(float y) {
        this.y = y;
        return this;
    }

    public float z() {
        return this.z;
    }

    public Vector3 z(float z) {
        this.z = z;
        return this;
    }

    public Vector3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 set(Vector3 vec) {
        return this.set(vec.x(), vec.y(), vec.z());
    }

    public Vector3 reset() {
        this.x = this.y = this.z = 0.0F;
        return this;
    }

    public float length() {
        return (float)Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public Vector3 normalize() {
        float length = this.length();
        this.x /= length;
        this.y /= length;
        this.z /= length;
        return this;
    }

    public float dot(Vector3 vec) {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }

    public Vector3 cross(Vector3 vec) {
        return new Vector3(this.y() * vec.z() - vec.y() * this.z(), vec.z() * this.x() - this.z() * vec.x(), this.x() * vec.y() - vec.x() * this.y());
    }

    public Vector3 add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3 add(Vector3 vec) {
        return this.add(vec.x(), vec.y(), vec.z());
    }

    public Vector3 sub(float x, float y, float z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vector3 sub(Vector3 vec) {
        return this.sub(vec.x(), vec.y(), vec.z());
    }

    public Vector3 mul(float f) {
        return this.mul(f, f, f);
    }

    public Vector3 mul(float x, float y, float z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public Vector3 mul(Vector3 vec) {
        return this.mul(vec.x(), vec.y(), vec.z());
    }

    public float get(int component) throws IllegalArgumentException {
        return switch (component) {
            case 0 -> this.x;
            case 1 -> this.y;
            case 2 -> this.z;
            default -> throw new IllegalArgumentException();
        };
    }
}
