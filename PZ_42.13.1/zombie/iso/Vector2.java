// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;

@UsedFromLua
public final class Vector2 implements Cloneable {
    /**
     * The horizontal part of this vector
     */
    public float x;
    /**
     * The vertical part of this vector
     */
    public float y;

    /**
     * Create a new vector with zero length
     */
    public Vector2() {
        this.x = 0.0F;
        this.y = 0.0F;
    }

    /**
     * Create a new vector which is identical to another vector
     * 
     * @param this The Vector2 to copy
     */
    public Vector2(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
    }

    /**
     * Create a new vector with specified horizontal and vertical parts
     * 
     * @param this The horizontal part
     * @param x The vertical part
     */
    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
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

    /**
     * Result = a + b * scale
     * @return The supplied result vector.
     */
    public static Vector2 addScaled(Vector2 a, Vector2 b, float scale, Vector2 result) {
        result.set(a.x + b.x * scale, a.y + b.y * scale);
        return result;
    }

    public void rotate(float radians) {
        double rx = this.x * Math.cos(radians) - this.y * Math.sin(radians);
        double ry = this.x * Math.sin(radians) + this.y * Math.cos(radians);
        this.x = (float)rx;
        this.y = (float)ry;
    }

    /**
     * Add another vector to this one and return this
     * 
     * @param other The other Vector2 to add to this one
     * @return this
     */
    public Vector2 add(Vector2 other) {
        this.x = this.x + other.x;
        this.y = this.y + other.y;
        return this;
    }

    /**
     * Set the direction of this vector to point to another vector, maintaining the length
     * 
     * @param other The Vector2 to point this one at.
     */
    public Vector2 aimAt(Vector2 other) {
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
     * Calculate angle between this and other vectors
     * 
     * @param other The other vector
     * @return The angle in radians in the range [0,PI]
     */
    public float angleBetween(Vector2 other) {
        float dot = this.dot(other) / (this.getLength() * other.getLength());
        if (dot < -1.0F) {
            dot = -1.0F;
        }

        if (dot > 1.0F) {
            dot = 1.0F;
        }

        return (float)Math.acos(dot);
    }

    /**
     * Clone this vector
     */
    public Vector2 clone() {
        return new Vector2(this);
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

    public float dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }

    public float dot(float otherX, float otherY) {
        return this.x * otherX + this.y * otherY;
    }

    /**
     * See if this vector is equal to another
     * 
     * @param other A Vector2 to compare this one to
     * @return true if other is a Vector2 equal to this one
     */
    @Override
    public boolean equals(Object other) {
        return !(other instanceof Vector2 v) ? false : v.x == this.x && v.y == this.y;
    }

    public float getDirection() {
        return getDirection(this.x, this.y);
    }

    public static float getDirection(float x, float y) {
        float angle = (float)Math.atan2(y, x);
        return PZMath.wrap(angle, (float) -Math.PI, (float) Math.PI);
    }

    /**
     * get the direction in which this vector is pointing
     *  Note: if the length of this vector is 0, then the direction will also be 0
     * @return The direction in which this vector is pointing in radians
     */
    @Deprecated
    public float getDirectionNeg() {
        return (float)Math.atan2(this.x, this.y);
    }

    /**
     * Set the direction of this vector, maintaining the length
     * 
     * @param directionRadians The new direction of this vector, in radians
     */
    public Vector2 setDirection(float directionRadians) {
        this.setLengthAndDirection(directionRadians, this.getLength());
        return this;
    }

    /**
     * get the length of this vector
     * @return The length of this vector
     */
    public float getLength() {
        return (float)Math.sqrt(this.x * this.x + this.y * this.y);
    }

    /**
     * get the squared length of this vector
     * @return The squared length of this vector
     */
    public float getLengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    /**
     * Set the length of this vector, maintaining the direction
     * 
     * @param length The length of this vector
     */
    public Vector2 setLength(float length) {
        this.normalize();
        this.x *= length;
        this.y *= length;
        return this;
    }

    public float normalize() {
        float lengthSq = this.getLengthSquared();
        if (PZMath.equal(lengthSq, 1.0F, 1.0E-5F)) {
            return 1.0F;
        } else if (lengthSq == 0.0F) {
            this.x = 0.0F;
            this.y = 0.0F;
            return 0.0F;
        } else {
            float length = (float)Math.sqrt(lengthSq);
            this.x /= length;
            this.y /= length;
            return length;
        }
    }

    /**
     * Make this vector identical to another vector
     * 
     * @param other The Vector2 to copy
     */
    public Vector2 set(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
        return this;
    }

    /**
     * Set the horizontal and vertical parts of this vector
     * 
     * @param x The horizontal part
     * @param y The vertical part
     */
    public Vector2 set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Set the length and direction of this vector
     * 
     * @param direction The direction of this vector, in radians
     * @param length The length of this vector
     */
    public Vector2 setLengthAndDirection(float direction, float length) {
        this.x = (float)(Math.cos(direction) * length);
        this.y = (float)(Math.sin(direction) * length);
        return this;
    }

    @Override
    public String toString() {
        return String.format("Vector2 (X: %f, Y: %f) (L: %f, D:%f)", this.x, this.y, this.getLength(), this.getDirection());
    }

    /**
     * @return the x
     */
    public float getX() {
        return this.x;
    }

    /**
     * 
     * @param x the x to set
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public float getY() {
        return this.y;
    }

    /**
     * 
     * @param y the y to set
     */
    public void setY(float y) {
        this.y = y;
    }

    public int floorX() {
        return PZMath.fastfloor(this.getX());
    }

    public int floorY() {
        return PZMath.fastfloor(this.getY());
    }

    public void tangent() {
        double nx = this.x * Math.cos(Math.toRadians(90.0)) - this.y * Math.sin(Math.toRadians(90.0));
        double ny = this.x * Math.sin(Math.toRadians(90.0)) + this.y * Math.cos(Math.toRadians(90.0));
        this.x = (float)nx;
        this.y = (float)ny;
    }

    public void scale(float scale) {
        scale(this, scale);
    }

    public static Vector2 scale(Vector2 val, float scale) {
        val.x *= scale;
        val.y *= scale;
        return val;
    }

    public static Vector2 moveTowards(Vector2 currentVector, Vector2 targetVector, float maxDistanceDelta) {
        float toVectorX = targetVector.x - currentVector.x;
        float toVectorY = targetVector.y - currentVector.y;
        float sqDistance = toVectorX * toVectorX + toVectorY * toVectorY;
        if (sqDistance != 0.0F && (!(maxDistanceDelta >= 0.0F) || !(sqDistance <= maxDistanceDelta * maxDistanceDelta))) {
            float distance = (float)Math.sqrt(sqDistance);
            return new Vector2(currentVector.x + toVectorX / distance * maxDistanceDelta, currentVector.y + toVectorY / distance * maxDistanceDelta);
        } else {
            return targetVector;
        }
    }
}
