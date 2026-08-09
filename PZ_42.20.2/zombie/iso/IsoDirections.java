// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;

@UsedFromLua
public enum IsoDirections {
    N(0, -1),
    NW(-1, -1),
    W(-1, 0),
    SW(-1, 1),
    S(0, 1),
    SE(1, 1),
    E(1, 0),
    NE(1, -1);

    private static final IsoDirections[] VALUES = values();
    private static final Vector2 TEMP = new Vector2();
    private final int dx;
    private final int dy;
    private final float angle;
    private final Vector2 vector;

    private IsoDirections(final int dx, final int dy) {
        this.dx = dx;
        this.dy = dy;
        this.vector = new Vector2(dx, dy);
        this.vector.normalize();
        this.angle = this.ordinal() * (float) (Math.PI * 2) / 8.0F;
    }

    public static IsoDirections fromString(String str) {
        return valueOf(str.trim().toUpperCase());
    }

    public static IsoDirections fromIndex(int index) {
        return VALUES[index & 7];
    }

    public IsoDirections RotLeft() {
        return VALUES[this.ordinal() + 1 & 7];
    }

    public IsoDirections RotLeft(int times) {
        return fromIndex(this.ordinal() + times);
    }

    public IsoDirections RotRight() {
        return VALUES[this.ordinal() - 1 & 7];
    }

    public IsoDirections RotRight(int times) {
        return fromIndex(this.ordinal() - times);
    }

    public IsoDirections Rot180() {
        return VALUES[this.ordinal() + 4 & 7];
    }

    public static IsoDirections fromAngle(Vector2 v) {
        return fromAngle(v.x, v.y);
    }

    public static IsoDirections fromAngle(float dx, float dy) {
        return safeFromAngle((float)Math.atan2(dy, dx));
    }

    public static IsoDirections fromAngle(float angleRadians) {
        return safeFromAngle(PZMath.wrap(angleRadians, (float) -Math.PI, (float) Math.PI));
    }

    private static IsoDirections safeFromAngle(float preClampedAngleRadians) {
        return VALUES[6 - (int)(8.5F + 1.2732395F * preClampedAngleRadians) & 7];
    }

    public static IsoDirections cardinalFromAngle(Vector2 v) {
        return cardinalFromAngle(v.x, v.y);
    }

    public static IsoDirections cardinalFromAngle(float dx, float dy) {
        return safeCardinalFromAngle((float)Math.atan2(dy, dx));
    }

    public static IsoDirections cardinalFromAngle(float angleRadians) {
        return safeCardinalFromAngle(PZMath.wrap(angleRadians, (float) -Math.PI, (float) Math.PI));
    }

    private static IsoDirections safeCardinalFromAngle(float preClampedAngleRadians) {
        return VALUES[2 * (3 - (int)(4.5F + 0.63661975F * preClampedAngleRadians) & 3)];
    }

    public int dx() {
        return this.dx;
    }

    public int dy() {
        return this.dy;
    }

    public boolean isCardinal() {
        return this.dx == 0 || this.dy == 0;
    }

    public boolean isDiagonal() {
        return this.dx != 0 && this.dy != 0;
    }

    public Vector2 ToVector() {
        return this.ToVector(TEMP);
    }

    public Vector2 ToVector(Vector2 result) {
        result.set(this.vector);
        return result;
    }

    public Vector2 addToVector(Vector2 addTo, Vector2 result) {
        result.x = addTo.x + this.dx();
        result.y = addTo.y + this.dy();
        return result;
    }

    public float toAngle() {
        return this.angle;
    }

    public float toAngleDegrees() {
        return this.angle * (180.0F / (float)Math.PI);
    }

    public static IsoDirections getRandom() {
        return VALUES[Rand.Next(8)];
    }
}
