// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class BoneTransform extends PooledObject {
    private boolean matrixValid = true;
    private final Matrix4f matrix = new Matrix4f();
    private final HelperFunctions.TransformResult_QPS transformResult = new HelperFunctions.TransformResult_QPS(this.matrix);
    private boolean prsValid = true;
    private final Vector3f pos = new Vector3f();
    private final Quaternion rot = new Quaternion();
    private final Vector3f scale = new Vector3f();
    private static final Pool<BoneTransform> s_pool = new Pool<>(BoneTransform::new);

    protected BoneTransform() {
        this.setIdentity();
    }

    @Override
    public void onReleased() {
        this.reset();
    }

    public void setIdentity() {
        this.matrixValid = true;
        this.matrix.setIdentity();
        this.prsValid = true;
        this.pos.set(0.0F, 0.0F, 0.0F);
        this.rot.setIdentity();
        this.scale.set(1.0F, 1.0F, 1.0F);
    }

    public void reset() {
        this.setIdentity();
    }

    public void set(BoneTransform rhs) {
        this.matrixValid = rhs.matrixValid;
        this.prsValid = rhs.prsValid;
        this.pos.set(rhs.pos);
        this.rot.set(rhs.rot);
        this.scale.set(rhs.scale);
        this.matrix.load(rhs.matrix);
    }

    public void set(Vector3f pos, Quaternion rot, Vector3f scale) {
        if (this.matrixValid || !this.prsValid || !this.pos.equals(pos) || !this.rot.equals(rot) || !this.scale.equals(scale)) {
            this.matrixValid = false;
            this.prsValid = true;
            this.pos.set(pos);
            this.rot.set(rot);
            this.scale.set(scale);
        }
    }

    public void set(Matrix4f matrix) {
        this.matrixValid = true;
        this.matrix.load(matrix);
        this.prsValid = false;
    }

    public void mul(Matrix4f a, Matrix4f b) {
        this.matrixValid = true;
        this.prsValid = false;
        Matrix4f.mul(a, b, this.matrix);
    }

    public void getMatrix(Matrix4f out_result) {
        out_result.load(this.getValidMatrix_Internal());
    }

    public void getPRS(Vector3f out_pos, Quaternion out_rot, Vector3f out_scale) {
        this.validatePRS();
        out_pos.set(this.pos);
        out_rot.set(this.rot);
        out_scale.set(this.scale);
    }

    public void setPosition(Vector3f position) {
        this.validatePRS();
        this.pos.set(position);
    }

    public void getPosition(Vector3f out_pos) {
        this.validatePRS();
        out_pos.set(this.pos);
    }

    public void getRotation(Quaternion out_rot) {
        this.validatePRS();
        out_rot.set(this.rot);
    }

    private Matrix4f getValidMatrix_Internal() {
        this.validateMatrix();
        return this.matrix;
    }

    private void validateMatrix() {
        if (!this.matrixValid) {
            this.validateInternal();
            this.matrixValid = true;
            HelperFunctions.CreateFromQuaternionPositionScale(this.pos, this.rot, this.scale, this.transformResult);
        }
    }

    protected void validatePRS() {
        if (!this.prsValid) {
            this.validateInternal();
            this.prsValid = true;
            HelperFunctions.getPosition(this.matrix, this.pos);
            HelperFunctions.getRotation(this.matrix, this.rot);
            this.scale.set(1.0F, 1.0F, 1.0F);
        }
    }

    protected void validateInternal() {
        if (!this.prsValid && !this.matrixValid) {
            throw new RuntimeException("Neither the matrix nor the PosRotScale values in this object are listed as valid.");
        }
    }

    public static void mul(BoneTransform a, Matrix4f b, Matrix4f out_result) {
        Matrix4f.mul(a.getValidMatrix_Internal(), b, out_result);
    }

    public static void mul(BoneTransform a, Matrix4f b, BoneTransform out_result) {
        out_result.mul(a.getValidMatrix_Internal(), b);
    }

    public static void mul(BoneTransform a, BoneTransform b, BoneTransform out_result) {
        out_result.mul(a.getValidMatrix_Internal(), b.getValidMatrix_Internal());
    }

    public static void mul(Matrix4f a, BoneTransform b, BoneTransform out_result) {
        out_result.mul(a, b.getValidMatrix_Internal());
    }

    public static BoneTransform alloc() {
        return s_pool.alloc();
    }
}
