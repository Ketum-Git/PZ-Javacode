// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.model.VertexPositionNormalTangentTextureSkin;
import zombie.popman.ObjectPool;

public final class HelperFunctions {
    private static final Vector3f s_zero3 = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Quaternion s_identityQ = new Quaternion();
    private static final Stack<Matrix4f> MatrixStack = new Stack<>();
    private static final AtomicBoolean MatrixLock = new AtomicBoolean(false);
    private static final ObjectPool<Vector3f> VectorPool = new ObjectPool<>(Vector3f::new);
    private static final ObjectPool<Quaternion> QuaternionPool = new ObjectPool<>(Quaternion::new);

    public static int ToRgba(Color color) {
        return (int)color.a << 24 | (int)color.b << 16 | (int)color.g << 8 | (int)color.r;
    }

    public static void returnMatrix(Matrix4f mat) {
        while (!MatrixLock.compareAndSet(false, true)) {
            Thread.onSpinWait();
        }

        assert !MatrixStack.contains(mat);

        MatrixStack.push(mat);
        MatrixLock.set(false);
    }

    public static Matrix4f getMatrix() {
        Matrix4f result = null;

        while (!MatrixLock.compareAndSet(false, true)) {
            Thread.onSpinWait();
        }

        if (MatrixStack.isEmpty()) {
            result = new Matrix4f();
        } else {
            result = MatrixStack.pop();
        }

        MatrixLock.set(false);
        return result;
    }

    public static Matrix4f getMatrix(Matrix4f copyFrom) {
        Matrix4f result = getMatrix();
        result.load(copyFrom);
        return result;
    }

    public static Vector3f allocVector3f(float x, float y, float z) {
        Vector3f result = allocVector3f();
        result.set(x, y, z);
        return result;
    }

    public static Vector3f allocVector3f() {
        while (!MatrixLock.compareAndSet(false, true)) {
            Thread.onSpinWait();
        }

        Vector3f result = VectorPool.alloc();
        MatrixLock.set(false);
        return result;
    }

    public static void releaseVector3f(Vector3f v) {
        while (!MatrixLock.compareAndSet(false, true)) {
            Thread.onSpinWait();
        }

        VectorPool.release(v);
        MatrixLock.set(false);
    }

    public static Quaternion allocQuaternion(float x, float y, float z, float w) {
        Quaternion result = allocQuaternion();
        result.set(x, y, z, w);
        return result;
    }

    public static Quaternion allocQuaternion() {
        while (!MatrixLock.compareAndSet(false, true)) {
            Thread.onSpinWait();
        }

        Quaternion result = QuaternionPool.alloc();
        MatrixLock.set(false);
        return result;
    }

    public static void releaseQuaternion(Quaternion q) {
        while (!MatrixLock.compareAndSet(false, true)) {
            Thread.onSpinWait();
        }

        QuaternionPool.release(q);
        MatrixLock.set(false);
    }

    public static Matrix4f CreateFromQuaternion(Quaternion q) {
        Matrix4f result = getMatrix();
        CreateFromQuaternion(q, result);
        return result;
    }

    public static Matrix4f CreateFromQuaternion(Quaternion q, Matrix4f result) {
        result.setIdentity();
        float qLenSq = q.lengthSquared();
        if (qLenSq > 0.0F && qLenSq < 0.99999F || qLenSq > 1.00001F) {
            float qLen = (float)Math.sqrt(qLenSq);
            float invQLen = 1.0F / qLen;
            q.scale(invQLen);
        }

        float xx = q.x * q.x;
        float xy = q.x * q.y;
        float xz = q.x * q.z;
        float wx = q.x * q.w;
        float yy = q.y * q.y;
        float yz = q.y * q.z;
        float wy = q.y * q.w;
        float zz = q.z * q.z;
        float wz = q.z * q.w;
        result.m00 = 1.0F - 2.0F * (yy + zz);
        result.m10 = 2.0F * (xy - wz);
        result.m20 = 2.0F * (xz + wy);
        result.m30 = 0.0F;
        result.m01 = 2.0F * (xy + wz);
        result.m11 = 1.0F - 2.0F * (xx + zz);
        result.m21 = 2.0F * (yz - wx) * 1.0F;
        result.m31 = 0.0F;
        result.m02 = 2.0F * (xz - wy);
        result.m12 = 2.0F * (yz + wx);
        result.m22 = 1.0F - 2.0F * (xx + yy);
        result.m32 = 0.0F;
        result.m03 = 0.0F;
        result.m13 = 0.0F;
        result.m23 = 0.0F;
        result.m33 = 1.0F;
        result.m30 = 0.0F;
        result.m31 = 0.0F;
        result.m32 = 0.0F;
        result.transpose();
        return result;
    }

    public static Matrix4f CreateFromQuaternionPositionScale(Vector3f position, Quaternion rotation, Vector3f scale, Matrix4f result) {
        Matrix4f scl = getMatrix();
        Matrix4f rot = getMatrix();
        CreateFromQuaternionPositionScale(position, rotation, scale, result, rot, scl);
        returnMatrix(scl);
        returnMatrix(rot);
        return result;
    }

    public static float getAngle(float v1x, float v1y, float v2x, float v2y) {
        float crossz = v1x * v2y - v1y * v2x;
        float dotab = v1x * v2x + v1y * v2y;
        float v1length = PZMath.sqrt(v1x * v1x + v1y * v1y);
        float v2length = PZMath.sqrt(v2x * v2x + v2y * v2y);
        float cosAngle = dotab / (v1length * v2length);
        if (cosAngle < -1.0F) {
            cosAngle = -1.0F;
        } else if (cosAngle > 1.0F) {
            cosAngle = 1.0F;
        }

        return (float)Math.acos(cosAngle) * PZMath.sign(crossz);
    }

    public static void CreateFromQuaternionPositionScale(
        Vector3f position, Quaternion rotation, Vector3f scale, HelperFunctions.TransformResult_QPS transformResult
    ) {
        CreateFromQuaternionPositionScale(position, rotation, scale, transformResult.result, transformResult.rot, transformResult.scl);
    }

    private static void CreateFromQuaternionPositionScale(
        Vector3f position, Quaternion rotation, Vector3f scale, Matrix4f result, Matrix4f reusable_rotation, Matrix4f reusable_scale
    ) {
        reusable_scale.setIdentity();
        reusable_scale.scale(scale);
        CreateFromQuaternion(rotation, reusable_rotation);
        Matrix4f.mul(reusable_scale, reusable_rotation, result);
        setPosition(result, position);
    }

    public static void TransformVertices(VertexPositionNormalTangentTextureSkin[] vertices, List<Matrix4f> boneTransforms) {
        Vector3 newPos = new Vector3();
        Vector3 newNorm = new Vector3();

        for (VertexPositionNormalTangentTextureSkin vert : vertices) {
            newPos.reset();
            newNorm.reset();
            Vector3 vertPos = vert.position;
            Vector3 vertNorm = vert.normal;
            ApplyBlendBone(vert.blendWeights.x, boneTransforms.get(vert.blendIndices.x), vertPos, vertNorm, newPos, newNorm);
            ApplyBlendBone(vert.blendWeights.y, boneTransforms.get(vert.blendIndices.y), vertPos, vertNorm, newPos, newNorm);
            ApplyBlendBone(vert.blendWeights.z, boneTransforms.get(vert.blendIndices.z), vertPos, vertNorm, newPos, newNorm);
            ApplyBlendBone(vert.blendWeights.w, boneTransforms.get(vert.blendIndices.w), vertPos, vertNorm, newPos, newNorm);
            vertPos.set(newPos);
            vertNorm.set(newNorm);
        }
    }

    public static void ApplyBlendBone(float weight, Matrix4f transform, Vector3 vertPos, Vector3 vertNorm, Vector3 newPos, Vector3 newNorm) {
        if (weight > 0.0F) {
            float inX = vertPos.x();
            float inY = vertPos.y();
            float inZ = vertPos.z();
            float x = transform.m00 * inX + transform.m01 * inY + transform.m02 * inZ + transform.m03;
            float y = transform.m10 * inX + transform.m11 * inY + transform.m12 * inZ + transform.m13;
            float z = transform.m20 * inX + transform.m21 * inY + transform.m22 * inZ + transform.m23;
            newPos.add(x * weight, y * weight, z * weight);
            inX = vertNorm.x();
            inY = vertNorm.y();
            inZ = vertNorm.z();
            x = transform.m00 * inX + transform.m01 * inY + transform.m02 * inZ;
            y = transform.m10 * inX + transform.m11 * inY + transform.m12 * inZ;
            z = transform.m20 * inX + transform.m21 * inY + transform.m22 * inZ;
            newNorm.add(x * weight, y * weight, z * weight);
        }
    }

    public static Vector3f getXAxis(Matrix4f matrix, Vector3f out_axis) {
        out_axis.set(matrix.m00, matrix.m10, matrix.m20);
        return out_axis;
    }

    public static void setXAxis(Matrix4f matrix, Vector3f in_axis) {
        matrix.m00 = in_axis.x;
        matrix.m10 = in_axis.y;
        matrix.m20 = in_axis.z;
    }

    public static Vector3f getYAxis(Matrix4f matrix, Vector3f out_axis) {
        out_axis.set(matrix.m01, matrix.m11, matrix.m21);
        return out_axis;
    }

    public static void setYAxis(Matrix4f matrix, Vector3f in_axis) {
        matrix.m01 = in_axis.x;
        matrix.m11 = in_axis.y;
        matrix.m21 = in_axis.z;
    }

    public static Vector3f getZAxis(Matrix4f matrix, Vector3f out_axis) {
        out_axis.set(matrix.m02, matrix.m12, matrix.m22);
        return out_axis;
    }

    public static void setZAxis(Matrix4f matrix, Vector3f in_axis) {
        matrix.m02 = in_axis.x;
        matrix.m12 = in_axis.y;
        matrix.m22 = in_axis.z;
    }

    public static Vector3f getPosition(Matrix4f matrix, Vector3f out_pos) {
        out_pos.set(matrix.m03, matrix.m13, matrix.m23);
        return out_pos;
    }

    public static void setPosition(Matrix4f matrix, Vector3f pos) {
        matrix.m03 = pos.x;
        matrix.m13 = pos.y;
        matrix.m23 = pos.z;
    }

    public static void setPosition(Matrix4f matrix, float x, float y, float z) {
        matrix.m03 = x;
        matrix.m13 = y;
        matrix.m23 = z;
    }

    public static Quaternion getRotation(Matrix4f matrix, Quaternion out_rot) {
        return Quaternion.setFromMatrix(matrix, out_rot);
    }

    public static Vector3f transform(Quaternion rotation, Vector3f in_vector, Vector3f out_result) {
        rotation.normalise();
        float s = rotation.w;
        float vX = rotation.x;
        float vY = rotation.y;
        float vZ = rotation.z;
        float s_2 = s * s;
        float v_2 = vX * vX + vY * vY + vZ * vZ;
        float pX = in_vector.x;
        float pY = in_vector.y;
        float pZ = in_vector.z;
        float v_cross_Px = vY * pZ - vZ * pY;
        float v_cross_Py = vZ * pX - vX * pZ;
        float v_cross_Pz = vX * pY - vY * pX;
        float v_dot_P = pX * vX + pY * vY + pZ * vZ;
        float rX = (s_2 - v_2) * pX + 2.0F * s * v_cross_Px + 2.0F * vX * v_dot_P;
        float rY = (s_2 - v_2) * pY + 2.0F * s * v_cross_Py + 2.0F * vY * v_dot_P;
        float rZ = (s_2 - v_2) * pZ + 2.0F * s * v_cross_Pz + 2.0F * vZ * v_dot_P;
        out_result.set(rX, rY, rZ);
        return out_result;
    }

    public static Vector4f transform(Matrix4f matrix, Vector4f in_vector, Vector4f out_result) {
        float x = matrix.m00 * in_vector.x + matrix.m01 * in_vector.y + matrix.m02 * in_vector.z + matrix.m03 * in_vector.w;
        float y = matrix.m10 * in_vector.x + matrix.m11 * in_vector.y + matrix.m12 * in_vector.z + matrix.m13 * in_vector.w;
        float z = matrix.m20 * in_vector.x + matrix.m21 * in_vector.y + matrix.m22 * in_vector.z + matrix.m23 * in_vector.w;
        float w = matrix.m30 * in_vector.x + matrix.m31 * in_vector.y + matrix.m32 * in_vector.z + matrix.m33 * in_vector.w;
        out_result.x = x;
        out_result.y = y;
        out_result.z = z;
        out_result.w = w;
        return out_result;
    }

    public static Vector3f transformVector(Matrix4f matrix, Vector3f in_vector, Vector3f out_result) {
        float x = matrix.m00 * in_vector.x + matrix.m01 * in_vector.y + matrix.m02 * in_vector.z;
        float y = matrix.m10 * in_vector.x + matrix.m11 * in_vector.y + matrix.m12 * in_vector.z;
        float z = matrix.m20 * in_vector.x + matrix.m21 * in_vector.y + matrix.m22 * in_vector.z;
        out_result.x = x;
        out_result.y = y;
        out_result.z = z;
        return out_result;
    }

    public static float getRotationY(Matrix4f matrix) {
        float xaxisX = 1.0F;
        float xaxisY = 0.0F;
        float xaxisZ = 0.0F;
        float rxaxisX = matrix.m00 * 1.0F + matrix.m01 * 0.0F + matrix.m02 * 0.0F;
        float rxaxisZ = matrix.m20 * 1.0F + matrix.m21 * 0.0F + matrix.m22 * 0.0F;
        xaxisZ = 0.0F;
        float zaxisY = 0.0F;
        float zaxisZ = 1.0F;
        xaxisX = matrix.m00 * 0.0F + matrix.m01 * 0.0F + matrix.m02 * 1.0F;
        xaxisY = matrix.m20 * 0.0F + matrix.m21 * 0.0F + matrix.m22 * 1.0F;
        xaxisZ = (rxaxisX + xaxisY) / 2.0F;
        zaxisY = (rxaxisZ - xaxisX) / 2.0F;
        zaxisZ = PZMath.sqrt(xaxisZ * xaxisZ + zaxisY * zaxisY);
        float normC = xaxisZ / zaxisZ;
        float normS = zaxisY / zaxisZ;
        return (float)Math.atan2(-normS, normC);
    }

    public static float getRotationY(Quaternion rotation) {
        rotation.normalise();
        float qs = rotation.w;
        float vX = rotation.x;
        float vY = rotation.y;
        float vZ = rotation.z;
        float s_2 = qs * qs;
        float v_2 = vX * vX + vY * vY + vZ * vZ;
        float xaxisX = 1.0F;
        float xaxisY = 0.0F;
        float xaxisZ = 0.0F;
        float v_cross_Px = vY * 0.0F - vZ * 0.0F;
        float v_cross_Pz = vX * 0.0F - vY * 1.0F;
        float v_dot_P = 1.0F * vX + 0.0F * vY + 0.0F * vZ;
        float rxaxisX = (s_2 - v_2) * 1.0F + 2.0F * qs * v_cross_Px + 2.0F * vX * v_dot_P;
        float rxaxisZ = (s_2 - v_2) * 0.0F + 2.0F * qs * v_cross_Pz + 2.0F * vZ * v_dot_P;
        xaxisZ = 0.0F;
        v_cross_Px = 0.0F;
        v_cross_Pz = 1.0F;
        v_dot_P = vY * 1.0F - vZ * 0.0F;
        float v_cross_Pzx = vX * 0.0F - vY * 0.0F;
        float v_dot_Px = 0.0F * vX + 0.0F * vY + 1.0F * vZ;
        xaxisX = (s_2 - v_2) * 0.0F + 2.0F * qs * v_dot_P + 2.0F * vX * v_dot_Px;
        xaxisY = (s_2 - v_2) * 1.0F + 2.0F * qs * v_cross_Pzx + 2.0F * vZ * v_dot_Px;
        xaxisZ = (rxaxisX + xaxisY) / 2.0F;
        v_cross_Px = (rxaxisZ - xaxisX) / 2.0F;
        v_cross_Pz = PZMath.sqrt(xaxisZ * xaxisZ + v_cross_Px * v_cross_Px);
        v_dot_P = xaxisZ / v_cross_Pz;
        v_cross_Pzx = v_cross_Px / v_cross_Pz;
        return (float)Math.atan2(-v_cross_Pzx, v_dot_P);
    }

    public static float getRotationZ(Quaternion rotation) {
        float s = rotation.w;
        float vX = rotation.x;
        float vY = rotation.y;
        float vZ = rotation.z;
        float s_2 = s * s;
        float v_2 = vX * vX + vY * vY + vZ * vZ;
        float pX = 1.0F;
        float v_cross_Py = vZ * 1.0F;
        float v_dot_P = 1.0F * vX;
        float rX = (s_2 - v_2) * 1.0F + 2.0F * vX * v_dot_P;
        float rY = 2.0F * s * v_cross_Py + 2.0F * vY * v_dot_P;
        return (float)Math.atan2(rY, rX);
    }

    public static Vector3f ToEulerAngles(Quaternion rot, Vector3f out_angles) {
        double sinr_cosp = 2.0 * (rot.w * rot.x + rot.y * rot.z);
        double cosr_cosp = 1.0 - 2.0 * (rot.x * rot.x + rot.y * rot.y);
        out_angles.x = (float)Math.atan2(sinr_cosp, cosr_cosp);
        double sinp = 2.0 * (rot.w * rot.y - rot.z * rot.x);
        if (Math.abs(sinp) >= 1.0) {
            out_angles.y = (float)Math.copySign((float) (Math.PI / 2), sinp);
        } else {
            out_angles.y = (float)Math.asin(sinp);
        }

        double siny_cosp = 2.0 * (rot.w * rot.z + rot.x * rot.y);
        double cosy_cosp = 1.0 - 2.0 * (rot.y * rot.y + rot.z * rot.z);
        out_angles.z = (float)Math.atan2(siny_cosp, cosy_cosp);
        return out_angles;
    }

    public static Quaternion ToQuaternion(double roll, double pitch, double yaw, Quaternion out_result) {
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);
        out_result.w = (float)(cy * cp * cr + sy * sp * sr);
        out_result.x = (float)(cy * cp * sr - sy * sp * cr);
        out_result.y = (float)(sy * cp * sr + cy * sp * cr);
        out_result.z = (float)(sy * cp * cr - cy * sp * sr);
        return out_result;
    }

    public static Vector3f getZero3() {
        s_zero3.set(0.0F, 0.0F, 0.0F);
        return s_zero3;
    }

    public static Quaternion getIdentityQ() {
        s_identityQ.setIdentity();
        return s_identityQ;
    }

    public static Quaternion setFromAxisAngle(float axisX, float axisY, float axisZ, float angleRads, Quaternion inout_result) {
        inout_result.x = axisX;
        inout_result.y = axisY;
        inout_result.z = axisZ;
        float n = (float)Math.sqrt(inout_result.x * inout_result.x + inout_result.y * inout_result.y + inout_result.z * inout_result.z);
        float s = (float)(Math.sin(0.5 * angleRads) / n);
        inout_result.x *= s;
        inout_result.y *= s;
        inout_result.z *= s;
        inout_result.w = (float)Math.cos(0.5 * angleRads);
        return inout_result;
    }

    public static class TransformResult_QPS {
        public final Matrix4f result;
        final Matrix4f rot;
        final Matrix4f scl;

        public TransformResult_QPS() {
            this.result = new Matrix4f();
            this.rot = new Matrix4f();
            this.scl = new Matrix4f();
        }

        public TransformResult_QPS(Matrix4f result) {
            this.result = result;
            this.rot = new Matrix4f();
            this.scl = new Matrix4f();
        }
    }
}
