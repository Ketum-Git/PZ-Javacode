// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import zombie.util.Pool;
import zombie.util.list.PZArrayUtil;

public class TwistableBoneTransform extends BoneTransform {
    public float blendWeight;
    public float twist;
    private static final Pool<TwistableBoneTransform> s_pool = new Pool<>(TwistableBoneTransform::new);

    protected TwistableBoneTransform() {
    }

    @Override
    public void reset() {
        super.reset();
        this.blendWeight = 0.0F;
        this.twist = 0.0F;
    }

    @Override
    public void set(BoneTransform rhs) {
        super.set(rhs);
        if (rhs instanceof TwistableBoneTransform transform) {
            this.blendWeight = transform.blendWeight;
            this.twist = transform.twist;
        }
    }

    public static TwistableBoneTransform alloc() {
        return s_pool.alloc();
    }

    public static TwistableBoneTransform[] allocArray(int count) {
        TwistableBoneTransform[] newArray = new TwistableBoneTransform[count];
        PZArrayUtil.arrayPopulate(newArray, TwistableBoneTransform::alloc);
        return newArray;
    }
}
