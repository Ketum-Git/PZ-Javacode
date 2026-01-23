// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.math;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.UsedFromLua;
import zombie.core.math.interpolators.LerpType;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.StringUtils;

@UsedFromLua
public final class PZMath {
    /**
     * The double value that is closer than any other to
     *  pi, the ratio of the circumference of a circle to its
     *  diameter.
     */
    public static final float PI = (float) Math.PI;
    public static final float PI2 = (float) (Math.PI * 2);
    public static final float halfPI = (float) (Math.PI / 2);
    /**
     * Conversion ratios, Degrees to Radians and back
     */
    public static final float degToRads = (float) (Math.PI / 180.0);
    public static final float radToDegs = 180.0F / (float)Math.PI;
    public static final long microsToNanos = 1000L;
    public static final long millisToMicros = 1000L;
    public static final long secondsToMillis = 1000L;
    public static long secondsToNanos = 1000000000L;

    /**
     * Almost Unit Identity
     * 
     *  This is a near-identiy function that maps the unit interval into itself. It is the cousin of smoothstep(), in
     *  that it maps 0 to 0, 1 to 1, and has a 0 derivative at the origin, just like smoothstep. However, instead of
     *  having a 0 derivative at 1, it has a derivative of 1 at that point. It's equivalent to the Almost Identiy above
     *  with n=0 and m=1. Since it's a cubic just like smoothstep() it is very fast to evaluate.
     * 
     *  https://iquilezles.org/www/articles/functions/functions.htm
     * 
     * @param x value in [0..1]
     * @return value in [0..1]
     */
    public static float almostUnitIdentity(float x) {
        return x * x * (2.0F - x);
    }

    /**
     * Almost Identity
     * 
     *  Imagine you don't want to modify a signal unless it's drops to zero or close to it, in which case you want
     *  to replace the value with a small possitive constant. Then, rather than clamping the value and introduce
     *  a discontinuity, you can smoothly blend the signal into the desired clipped value. So, let m be the threshold
     *  (anything above m stays unchanged), and n the value things will take when the signal is zero.
     *  Then, the following function does the soft clipping (in a cubic fashion):
     * 
     *  https://iquilezles.org/www/articles/functions/functions.htm
     * 
     * @param x value in [0..1]
     * @param m
     * @param n
     * @return value in [0..1]
     */
    public static float almostIdentity(float x, float m, float n) {
        if (x > m) {
            return x;
        } else {
            float a = 2.0F * n - m;
            float b = 2.0F * m - 3.0F * n;
            float t = x / m;
            return (a * t + b) * t * t + n;
        }
    }

    /**
     * Gain
     * 
     *  Remapping the unit interval into the unit interval by expanding the sides and compressing the center, and
     *  keeping 1/2 mapped to 1/2, that can be done with the gain() function. This was a common function in RSL tutorials
     *  (the Renderman Shading Language). k=1 is the identity curve, k<1 produces the classic gain() shape, and k>1
     *  produces "s" shaped curces. The curves are symmetric (and inverse) for k=a and k=1/a.
     * 
     *  https://iquilezles.org/www/articles/functions/functions.htm
     */
    public static float gain(float x, float k) {
        float a = (float)(0.5 * Math.pow(2.0F * (x < 0.5F ? x : 1.0F - x), k));
        return x < 0.5F ? a : 1.0F - a;
    }

    /**
     * Result is clamped between min and max.
     * @return min <= val <= max
     */
    public static float clamp(float val, float min, float max) {
        if (val < min) {
            return min;
        } else {
            return val > max ? max : val;
        }
    }

    public static long clamp(long val, long min, long max) {
        if (val < min) {
            return min;
        } else {
            return val > max ? max : val;
        }
    }

    /**
     * Result is clamped between min and max.
     * @return min <= val <= max
     */
    public static int clamp(int val, int min, int max) {
        if (val < min) {
            return min;
        } else {
            return val > max ? max : val;
        }
    }

    public static double clamp(double val, double min, double max) {
        if (val < min) {
            return min;
        } else {
            return val > max ? max : val;
        }
    }

    public static float clampFloat(float val, float min, float max) {
        return clamp(val, min, max);
    }

    public static float clamp_01(float val) {
        return clamp(val, 0.0F, 1.0F);
    }

    public static double clampDouble_01(double val) {
        return clamp(val, 0.0, 1.0);
    }

    public static Quaternion setFromAxisAngle(float ax, float ay, float az, float angleRadians, Quaternion out_result) {
        out_result.x = ax;
        out_result.y = ay;
        out_result.z = az;
        float n = (float)Math.sqrt(out_result.x * out_result.x + out_result.y * out_result.y + out_result.z * out_result.z);
        float s = (float)(Math.sin(0.5 * angleRadians) / n);
        out_result.x *= s;
        out_result.y *= s;
        out_result.z *= s;
        out_result.w = (float)Math.cos(0.5 * angleRadians);
        return out_result;
    }

    public static float lerp(float src, float dest, float alpha) {
        return src + (dest - src) * alpha;
    }

    public static float lerp(float src, float dest, float alpha, LerpType lerpType) {
        return switch (lerpType) {
            case Linear -> lerp(src, dest, alpha);
            case EaseOutQuad -> lerp(src, dest, lerpFunc_EaseOutQuad(alpha));
            case EaseInQuad -> lerp(src, dest, lerpFunc_EaseInQuad(alpha));
            case EaseOutInQuad -> lerp(src, dest, lerpFunc_EaseOutInQuad(alpha));
            default -> throw new UnsupportedOperationException();
        };
    }

    public static float lerpAngle(float src, float dest, float alpha) {
        float diff = getClosestAngle(src, dest);
        float lerped = src + alpha * diff;
        return wrap(lerped, (float) -Math.PI, (float) Math.PI);
    }

    public static Vector3f lerp(Vector3f out, Vector3f a, Vector3f b, float t) {
        out.set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
        return out;
    }

    public static Vector3 lerp(Vector3 out, Vector3 a, Vector3 b, float t) {
        out.set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
        return out;
    }

    public static Vector2 lerp(Vector2 out, Vector2 a, Vector2 b, float t) {
        out.set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
        return out;
    }

    public static float c_lerp(float src, float dest, float alpha) {
        float t2 = (float)(1.0 - Math.cos(alpha * (float) Math.PI)) / 2.0F;
        return src * (1.0F - t2) + dest * t2;
    }

    public static Quaternion slerp(Quaternion result, Quaternion from, Quaternion to, float alpha) {
        double dot = from.x * to.x + from.y * to.y + from.z * to.z + from.w * to.w;
        double absDot = dot < 0.0 ? -dot : dot;
        double scale0 = 1.0F - alpha;
        double scale1 = alpha;
        if (1.0 - absDot > 0.1) {
            double angle = org.joml.Math.acos(absDot);
            double sinAngle = org.joml.Math.sin(angle);
            double invSinAngle = 1.0 / sinAngle;
            scale0 = org.joml.Math.sin(angle * (1.0 - alpha)) * invSinAngle;
            scale1 = org.joml.Math.sin(angle * alpha) * invSinAngle;
        }

        if (dot < 0.0) {
            scale1 = -scale1;
        }

        result.set(
            (float)(scale0 * from.x + scale1 * to.x),
            (float)(scale0 * from.y + scale1 * to.y),
            (float)(scale0 * from.z + scale1 * to.z),
            (float)(scale0 * from.w + scale1 * to.w)
        );
        return result;
    }

    public static float sqrt(float val) {
        return org.joml.Math.sqrt(val);
    }

    public static float lerpFunc_EaseOutQuad(float x) {
        return x * x;
    }

    public static float lerpFunc_EaseInQuad(float x) {
        float revX = 1.0F - x;
        return 1.0F - revX * revX;
    }

    public static float lerpFunc_EaseOutInQuad(float x) {
        return x < 0.5F ? lerpFunc_EaseOutQuad(x) * 2.0F : 0.5F + lerpFunc_EaseInQuad(2.0F * x - 1.0F) / 2.0F;
    }

    public static double tryParseDouble(String varStr, double defaultVal) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return defaultVal;
        } else {
            try {
                return Double.parseDouble(varStr.trim());
            } catch (NumberFormatException var4) {
                return defaultVal;
            }
        }
    }

    public static float tryParseFloat(String varStr, float defaultVal) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return defaultVal;
        } else {
            try {
                return Float.parseFloat(varStr.trim());
            } catch (NumberFormatException var3) {
                return defaultVal;
            }
        }
    }

    public static boolean canParseFloat(String varStr) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return false;
        } else {
            try {
                Float.parseFloat(varStr.trim());
                return true;
            } catch (NumberFormatException var2) {
                return false;
            }
        }
    }

    public static int tryParseInt(String varStr, int defaultVal) {
        if (StringUtils.isNullOrWhitespace(varStr)) {
            return defaultVal;
        } else {
            try {
                return Integer.parseInt(varStr.trim());
            } catch (NumberFormatException var3) {
                return defaultVal;
            }
        }
    }

    public static float pow(float a, float b) {
        return (float)Math.pow(a, b);
    }

    public static float degToRad(float degrees) {
        return (float) (Math.PI / 180.0) * degrees;
    }

    public static float radToDeg(float radians) {
        return (180.0F / (float)Math.PI) * radians;
    }

    public static float getClosestAngle(float in_radsA, float in_radsB) {
        float angleA = wrap(in_radsA, (float) (Math.PI * 2));
        float angleB = wrap(in_radsB, (float) (Math.PI * 2));
        float diff = angleB - angleA;
        return wrap(diff, (float) -Math.PI, (float) Math.PI);
    }

    public static float getClosestAngleDegrees(float in_degsA, float in_degsB) {
        float in_radsA = degToRad(in_degsA);
        float in_radsB = degToRad(in_degsB);
        float closestAngleRads = getClosestAngle(in_radsA, in_radsB);
        return radToDeg(closestAngleRads);
    }

    public static int sign(float val) {
        return val > 0.0F ? 1 : (val < 0.0F ? -1 : 0);
    }

    public static int fastfloor(double val) {
        int xi = (int)val;
        return val < xi ? xi - 1 : xi;
    }

    public static int fastfloor(float val) {
        int xi = (int)val;
        return val < xi ? xi - 1 : xi;
    }

    public static int coorddivision(int value, int divisor) {
        return fastfloor((float)value / divisor);
    }

    public static int coordmodulo(int value, int divisor) {
        return value - fastfloor((float)value / divisor) * divisor;
    }

    public static float coordmodulof(float value, int divisor) {
        return value - fastfloor(value / divisor) * divisor;
    }

    public static float floor(float val) {
        return fastfloor(val);
    }

    public static double floor(double val) {
        return fastfloor(val);
    }

    public static float ceil(float val) {
        return val >= 0.0F ? (int)(val + 0.9999999F) : (int)(val - 1.0E-7F);
    }

    public static float frac(float val) {
        float whole = floor(val);
        return val - whole;
    }

    public static float wrap(float val, float range) {
        if (range == 0.0F) {
            return 0.0F;
        } else if (range < 0.0F) {
            return 0.0F;
        } else if (val < 0.0F) {
            float multipleNegative = -val / range;
            float fracUp = 1.0F - frac(multipleNegative);
            return fracUp * range;
        } else {
            float multiple = val / range;
            float frac = frac(multiple);
            return frac * range;
        }
    }

    public static float wrap(float in_val, float in_min, float in_max) {
        float max = max(in_max, in_min);
        float min = min(in_max, in_min);
        float range = max - min;
        float relVal = in_val - min;
        float wrappedRelVal = wrap(relVal, range);
        return min + wrappedRelVal;
    }

    public static float max(float a, float b) {
        return a > b ? a : b;
    }

    public static float max(float a, float b, float c) {
        return max(a, max(b, c));
    }

    public static float max(float a, float b, float c, float d) {
        return max(a, max(b, max(c, d)));
    }

    public static float max(float a, float b, float c, float d, float e) {
        return max(a, max(b, max(c, max(d, e))));
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static int max(int a, int b, int c) {
        return max(a, max(b, c));
    }

    public static int max(int a, int b, int c, int d) {
        return max(a, b, max(c, d));
    }

    public static int max(int a, int b, int c, int d, int e) {
        return max(a, b, c, max(d, e));
    }

    public static float min(float a, float b) {
        return a > b ? b : a;
    }

    public static float min(float a, float b, float c) {
        return min(a, min(b, c));
    }

    public static float min(float a, float b, float c, float d) {
        return min(a, min(b, min(c, d)));
    }

    public static float min(float a, float b, float c, float d, float e) {
        return min(a, min(b, min(c, min(d, e))));
    }

    public static int min(int a, int b) {
        return a > b ? b : a;
    }

    public static int min(int a, int b, int c) {
        return max(a, max(b, c));
    }

    public static int min(int a, int b, int c, int d) {
        return max(a, b, max(c, d));
    }

    public static int min(int a, int b, int c, int d, int e) {
        return max(a, b, c, max(d, e));
    }

    public static float abs(float val) {
        return val * sign(val);
    }

    public static boolean equal(float a, float b) {
        float delta = 1.0E-7F;
        return equal(a, b, 1.0E-7F);
    }

    public static boolean equal(float a, float b, float delta) {
        float diff = b - a;
        float absDiff = abs(diff);
        return absDiff < delta;
    }

    public static Matrix4f convertMatrix(org.joml.Matrix4f src, Matrix4f dst) {
        if (dst == null) {
            dst = new Matrix4f();
        }

        dst.m00 = src.m00();
        dst.m01 = src.m01();
        dst.m02 = src.m02();
        dst.m03 = src.m03();
        dst.m10 = src.m10();
        dst.m11 = src.m11();
        dst.m12 = src.m12();
        dst.m13 = src.m13();
        dst.m20 = src.m20();
        dst.m21 = src.m21();
        dst.m22 = src.m22();
        dst.m23 = src.m23();
        dst.m30 = src.m30();
        dst.m31 = src.m31();
        dst.m32 = src.m32();
        dst.m33 = src.m33();
        return dst;
    }

    public static org.joml.Matrix4f convertMatrix(Matrix4f src, org.joml.Matrix4f dst) {
        if (dst == null) {
            dst = new org.joml.Matrix4f();
        }

        return dst.set(
            src.m00, src.m01, src.m02, src.m03, src.m10, src.m11, src.m12, src.m13, src.m20, src.m21, src.m22, src.m23, src.m30, src.m31, src.m32, src.m33
        );
    }

    public static float step(float from, float to, float delta) {
        if (from > to) {
            return max(from + delta, to);
        } else {
            return from < to ? min(from + delta, to) : from;
        }
    }

    public static float angleBetween(Vector2 va, Vector2 vb) {
        float in_vax = va.x;
        float in_vay = va.y;
        float in_vbx = vb.x;
        float in_vby = vb.y;
        return angleBetween(in_vax, in_vay, in_vbx, in_vby);
    }

    public static float angleBetween(float in_ax, float in_ay, float in_bx, float in_by) {
        float va_length = sqrt(in_ax * in_ax + in_ay * in_ay);
        if (va_length == 0.0F) {
            return 0.0F;
        } else {
            float vb_length = sqrt(in_bx * in_bx + in_by * in_by);
            if (vb_length == 0.0F) {
                return 0.0F;
            } else {
                float ax = in_ax / va_length;
                float ay = in_ay / va_length;
                float bx = in_bx / vb_length;
                float by = in_by / vb_length;
                return angleBetweenNormalized(ax, bx, ay, by);
            }
        }
    }

    public static float angleBetweenNormalized(float ax, float bx, float ay, float by) {
        float dot = clamp(ax * bx + ay * by, -1.0F, 1.0F);
        float absAngle = acosf(dot);
        float cross = ax * by - ay * bx;
        int angleSign = sign(cross);
        return absAngle * angleSign;
    }

    public static float acosf(float a) {
        return (float)Math.acos(a);
    }

    public static float calculateBearing(Vector3 in_fromPosition, Vector2 in_fromForward, Vector3 in_toPosition) {
        float toTargetX = in_toPosition.x - in_fromPosition.x;
        float toTargetY = in_toPosition.y - in_fromPosition.y;
        return angleBetween(in_fromForward.x, in_fromForward.y, toTargetX, toTargetY) * (180.0F / (float)Math.PI);
    }

    public static Vector3f rotateVector(Vector3f in_vector, Quaternion in_quaternion, Vector3f out_result) {
        float qx = in_quaternion.x;
        float qy = in_quaternion.y;
        float qz = in_quaternion.z;
        float qw = in_quaternion.w;
        float vx = in_vector.x;
        float vy = in_vector.y;
        float vz = in_vector.z;
        return rotateVector(vx, vy, vz, qx, qy, qz, qw, out_result);
    }

    public static Vector3f rotateVector(float vx, float vy, float vz, float qx, float qy, float qz, float qw, Vector3f out_result) {
        float dotUU = qx * qx + qy * qy + qz * qz;
        float dotUV = qx * vx + qy * vy + qz * vz;
        float crossUVx = qy * vz - qz * vy;
        float crossUVy = -(qx * vz - qz * vx);
        float crossUVz = qx * vy - qy * vx;
        out_result.x = 2.0F * dotUV * qx + (qw * qw - dotUU) * vx + 2.0F * qw * crossUVx;
        out_result.y = 2.0F * dotUV * qy + (qw * qw - dotUU) * vy + 2.0F * qw * crossUVy;
        out_result.z = 2.0F * dotUV * qz + (qw * qw - dotUU) * vz + 2.0F * qw * crossUVz;
        return out_result;
    }

    public static Vector2 rotateVector(float vx, float vy, float qx, float qy, float qz, float qw, Vector2 out_result) {
        float dotUU = qx * qx + qy * qy + qz * qz;
        float dotUV = qx * vx + qy * vy;
        float crossUVx = -(qz * vy);
        float crossUVy = qz * vx;
        out_result.x = 2.0F * dotUV * qx + (qw * qw - dotUU) * vx + 2.0F * qw * crossUVx;
        out_result.y = 2.0F * dotUV * qy + (qw * qw - dotUU) * vy + 2.0F * qw * crossUVy;
        return out_result;
    }

    public static PZMath.SideOfLine testSideOfLine(float x1, float y1, float x2, float y2, float px, float py) {
        float d = (px - x1) * (y2 - y1) - (py - y1) * (x2 - x1);
        return d > 0.0F ? PZMath.SideOfLine.Left : (d < 0.0F ? PZMath.SideOfLine.Right : PZMath.SideOfLine.OnLine);
    }

    public static <E> void normalize(List<E> list, PZMath.FloatGet<E> floatGet, PZMath.FloatSet<E> floatSet) {
        float[] values = new float[list.size()];

        for (int i = 0; i < list.size(); i++) {
            values[i] = floatGet.get(list.get(i));
        }

        normalize(values);

        for (int i = 0; i < list.size(); i++) {
            floatSet.set(list.get(i), values[i]);
        }
    }

    public static <E> void normalize(E[] list, PZMath.FloatGet<E> floatGet, PZMath.FloatSet<E> floatSet) {
        float[] values = new float[list.length];

        for (int i = 0; i < list.length; i++) {
            values[i] = floatGet.get(list[i]);
        }

        normalize(values);

        for (int i = 0; i < list.length; i++) {
            floatSet.set(list[i], values[i]);
        }
    }

    public static float[] normalize(float[] weights) {
        int n = weights.length;
        float total = 0.0F;
        int i = n;

        while (--i >= 0) {
            total += weights[i];
        }

        if (total != 0.0F) {
            i = n;

            while (--i >= 0) {
                weights[i] /= total;
            }
        }

        return weights;
    }

    public static ArrayList<Double> normalize(ArrayList<Double> list) {
        float[] values = new float[list.size()];

        for (int i = 0; i < list.size(); i++) {
            values[i] = list.get(i).floatValue();
        }

        normalize(values);
        list.clear();

        for (int i = 0; i < values.length; i++) {
            list.add((double)values[i]);
        }

        return list;
    }

    public static float roundFloatPos(float number, int scale) {
        int pow = 10;

        for (int i = 1; i < scale; i++) {
            pow *= 10;
        }

        float tmp = number * pow;
        return (float)((int)(tmp - (int)tmp >= 0.5F ? tmp + 1.0F : tmp)) / pow;
    }

    public static float roundFloat(float value, int scale) {
        int pow = 10;

        for (int i = 1; i < scale; i++) {
            pow *= 10;
        }

        float tmp = value * pow;
        float tmpSub = tmp - (int)tmp;
        return (float)((int)(value >= 0.0F ? (tmpSub >= 0.5F ? tmp + 1.0F : tmp) : (tmpSub >= -0.5F ? tmp : tmp - 1.0F))) / pow;
    }

    public static int nextPowerOfTwo(int value) {
        if (value == 0) {
            return 1;
        } else {
            value = --value | value >> 1;
            value |= value >> 2;
            value |= value >> 4;
            value |= value >> 8;
            value |= value >> 16;
            return value + 1;
        }
    }

    public static float roundToNearest(float val) {
        int sign = sign(val);
        return floor(val + 0.5F * sign);
    }

    public static int roundToInt(float val) {
        return (int)(roundToNearest(val) + 1.0E-4F);
    }

    public static float roundToIntPlus05(float val) {
        return floor(val) + 0.5F;
    }

    public static float roundFromEdges(float val) {
        float threshold = 0.2F;
        float valInt = fastfloor(val);
        float valfrac = val - valInt;
        if (valfrac < 0.2F) {
            return valInt + 0.2F;
        } else {
            return valfrac > 0.8F ? valInt + 1.0F - 0.2F : val;
        }
    }

    public static Vector3 closestVector3(float lx0, float ly0, float lz0, float lx1, float ly1, float lz1, float x, float y, float z) {
        Vector3 A = new Vector3(lx0, ly0, lz0);
        Vector3 B = new Vector3(lx1, ly1, lz1);
        Vector3 P = new Vector3(x, y, z);
        float ABx = B.x - A.x;
        float ABy = B.y - A.y;
        float ABz = B.z - A.z;
        float APx = P.x - A.x;
        float APy = P.y - A.y;
        float APz = P.z - A.z;
        float dotProductAPAB = APx * ABx + APy * ABy + APz * ABz;
        float dotProductABAB = ABx * ABx + ABy * ABy + ABz * ABz;
        float projectionScalar = dotProductAPAB / dotProductABAB;
        projectionScalar = org.joml.Math.max(0.0F, org.joml.Math.min(1.0F, projectionScalar));
        float closestX = A.x + projectionScalar * ABx;
        float closestY = A.y + projectionScalar * ABy;
        float closestZ = A.z + projectionScalar * ABz;
        return new Vector3(closestX, closestY, closestZ);
    }

    public static float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
        return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    }

    public static boolean intersectLineSegments(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vector2f intersection) {
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (d == 0.0F) {
            return false;
        } else {
            float yd = y1 - y3;
            float xd = x1 - x3;
            float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
            if (!(ua < 0.0F) && !(ua > 1.0F)) {
                float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
                if (!(ub < 0.0F) && !(ub > 1.0F)) {
                    if (intersection != null) {
                        intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
                    }

                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public static double closestPointOnLineSegment(float x1, float y1, float x2, float y2, float px, float py, double epsilon, Vector2f out) {
        double u = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
        if (Double.compare(u, epsilon) <= 0) {
            out.set(x1, y1);
            return (px - x1) * (px - x1) + (py - y1) * (py - y1);
        } else if (Double.compare(u, 1.0 - epsilon) >= 0) {
            out.set(x2, y2);
            return (px - x2) * (px - x2) + (py - y2) * (py - y2);
        } else {
            double xu = x1 + u * (x2 - x1);
            double yu = y1 + u * (y2 - y1);
            out.set((float)xu, (float)yu);
            return (px - xu) * (px - xu) + (py - yu) * (py - yu);
        }
    }

    public static double closestPointsOnLineSegments(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vector2f p1, Vector2f p2) {
        double eta = 1.0E-6;
        float rx = x3 - x1;
        float ry = y3 - y1;
        float ux = x2 - x1;
        float uy = y2 - y1;
        float vx = x4 - x3;
        float vy = y4 - y3;
        float ru = rx * ux + ry * uy;
        float rv = rx * vx + ry * vy;
        float uu = ux * ux + uy * uy;
        float uv = ux * vx + uy * vy;
        float vv = vx * vx + vy * vy;
        float det = uu * vv - uv * uv;
        float s;
        float t;
        if (det < 1.0E-6 * uu * vv) {
            s = clamp_01(ru / uu);
            t = 0.0F;
        } else {
            s = clamp_01((ru * vv - rv * uv) / det);
            t = clamp_01((ru * uv - rv * uu) / det);
        }

        float S = clamp_01((t * uv + ru) / uu);
        float T = clamp_01((s * uv - rv) / vv);
        p1.set(x1 + S * ux, y1 + S * uy);
        p2.set(x3 + T * vx, y3 + T * vy);
        return IsoUtils.DistanceToSquared(p1.x, p1.y, p2.x, p2.y);
    }

    public interface FloatGet<E> {
        float get(E var1);
    }

    public interface FloatSet<E> {
        void set(E var1, float var2);
    }

    public static enum SideOfLine {
        Left,
        OnLine,
        Right;
    }
}
