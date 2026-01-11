// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.UsedFromLua;

@UsedFromLua
public final class Vector3 implements Cloneable {
    /**
     * The horizontal part of this vector
     */
    public float x;
    /**
     * The vertical part of this vector
     */
    public float y;
    public float z;

    /**
     * Create a new vector with zero length
     */
    public Vector3() {
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
    }

    /**
     * Create a new vector which is identical to another vector
     * 
     * @param this The Vector2 to copy
     */
    public Vector3(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    /**
     * Create a new vector with specified horizontal and vertical parts
     * 
     * @param this The horizontal part
     * @param x The vertical part
     * @param y
     */
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Create a new vector with a specified length and direction
     * 
     * @param length The length of the new vector
     * @param direction The direction of the new vector, in radians
     */
    public static Vector2 fromLengthDirection(float length, float direction) {
        Vector2 v = new Vector2();
        v.setLengthAndDirection(direction, length);
        return v;
    }

    public static float dot(float x, float y, float tx, float ty) {
        return x * tx + y * ty;
    }

    public void rotate(float rad) {
        double rx = this.x * Math.cos(rad) - this.y * Math.sin(rad);
        double ry = this.x * Math.sin(rad) + this.y * Math.cos(rad);
        this.x = (float)rx;
        this.y = (float)ry;
    }

    public void rotatey(float rad) {
        double rx = this.x * Math.cos(rad) - this.z * Math.sin(rad);
        double ry = this.x * Math.sin(rad) + this.z * Math.cos(rad);
        this.x = (float)rx;
        this.z = (float)ry;
    }

    /**
     * Add another vector to this one and return as a new vector
     * 
     * @param other The other Vector2 to add to this one
     * @return The result as new Vector2
     */
    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }

    /**
     * Add another vector to this one and store the result in this one
     * 
     * @param other The other Vector2 to add to this one
     * @return This vector, with the other vector added
     */
    public Vector3 addToThis(Vector2 other) {
        this.x = this.x + other.x;
        this.y = this.y + other.y;
        return this;
    }

    public Vector3 addToThis(Vector3 other) {
        this.x = this.x + other.x;
        this.y = this.y + other.y;
        this.z = this.z + other.z;
        return this;
    }

    public Vector3 div(float scalar) {
        this.x /= scalar;
        this.y /= scalar;
        this.z /= scalar;
        return this;
    }

    /**
     * Set the direction of this vector to point to another vector, maintaining the length
     * 
     * @param other The Vector2 to point this one at.
     */
    public Vector3 aimAt(Vector2 other) {
        this.setLengthAndDirection(this.angleTo(other), this.getLength());
        return this;
    }

    /**
     * Calculate the angle between this point and another
     * 
     * @param other The second point as vector
     * @return The angle between them, in radians
     */
    public float angleTo(Vector2 other) {
        return (float)Math.atan2(other.y - this.y, other.x - this.x);
    }

    /**
     * Clone this vector
     */
    public Vector3 clone() {
        return new Vector3(this);
    }

    /**
     * Calculate the distance between this point and another
     * 
     * @param other The second point as vector
     * @return The distance between them
     */
    public float distanceTo(Vector2 other) {
        return (float)Math.sqrt(Math.pow(other.x - this.x, 2.0) + Math.pow(other.y - this.y, 2.0));
    }

    public float distanceTo(Vector3 other) {
        return (float)Math.sqrt(Math.pow(other.x - this.x, 2.0) + Math.pow(other.y - this.y, 2.0) + Math.pow(other.z - this.z, 2.0));
    }

    public float distanceTo(float x, float y, float z) {
        return (float)Math.sqrt(Math.pow(x - this.x, 2.0) + Math.pow(y - this.y, 2.0) + Math.pow(z - this.z, 2.0));
    }

    public float dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }

    public float dot3d(Vector3 other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    /**
     * See if this vector is equal to another
     * 
     * @param other A Vector2 to compare this one to
     * @return true if other is a Vector2 equal to this one
     */
    @Override
    public boolean equals(Object other) {
        return !(other instanceof Vector3 v) ? false : v.x == this.x && v.y == this.y && v.z == this.z;
    }

    /**
     * get the direction in which this vector is pointing
     *  Note: if the length of this vector is 0, then the direction will also be 0
     * @return The direction in which this vector is pointing in radians
     */
    public float getDirection() {
        return (float)Math.atan2(this.x, this.y);
    }

    /**
     * Set the direction of this vector, maintaining the length
     * 
     * @param direction The new direction of this vector, in radians
     */
    public Vector3 setDirection(float direction) {
        this.setLengthAndDirection(direction, this.getLength());
        return this;
    }

    /**
     * get the length of this vector
     * @return The length of this vector
     */
    public float getLength() {
        float lengthSq = this.getLengthSq();
        return (float)Math.sqrt(lengthSq);
    }

    /**
     * get the length squared (L^2) of this vector
     * @return The length squared of this vector
     */
    public float getLengthSq() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    /**
     * Set the length of this vector, maintaining the direction
     * 
     * @param length The length of this vector
     */
    public Vector3 setLength(float length) {
        this.normalize();
        this.x *= length;
        this.y *= length;
        this.z *= length;
        return this;
    }

    public void normalize() {
        float length = this.getLength();
        if (length == 0.0F) {
            this.x = 0.0F;
            this.y = 0.0F;
            this.z = 0.0F;
        } else {
            this.x /= length;
            this.y /= length;
            this.z /= length;
        }

        length = this.getLength();
    }

    /**
     * Make this vector identical to another vector
     * 
     * @param other The Vector2 to copy
     */
    public Vector3 set(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    /**
     * Set the horizontal and vertical parts of this vector
     * 
     * @param x The horizontal part
     * @param y The vertical part
     * @param z
     */
    public Vector3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Set the length and direction of this vector
     * 
     * @param direction The direction of this vector, in radians
     * @param length The length of this vector
     */
    public Vector3 setLengthAndDirection(float direction, float length) {
        this.x = (float)(Math.cos(direction) * length);
        this.y = (float)(Math.sin(direction) * length);
        return this;
    }

    @Override
    public String toString() {
        return String.format("Vector2 (X: %f, Y: %f) (L: %f, D:%f)", this.x, this.y, this.getLength(), this.getDirection());
    }

    public Vector3 sub(Vector3 val, Vector3 out) {
        return sub(this, val, out);
    }

    public static Vector3 sub(Vector3 a, Vector3 b, Vector3 out) {
        out.set(a.x - b.x, a.y - b.y, a.z - b.z);
        return out;
    }
}
