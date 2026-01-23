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
    public void set(BoneTransform in_rhs) {
        super.set(in_rhs);
        if (in_rhs instanceof TwistableBoneTransform rhs) {
            this.blendWeight = rhs.blendWeight;
            this.twist = rhs.twist;
        }
    }

    public static TwistableBoneTransform alloc() {
        return s_pool.alloc();
    }

    public static TwistableBoneTransform[] allocArray(int in_count) {
        TwistableBoneTransform[] newArray = new TwistableBoneTransform[in_count];
        PZArrayUtil.arrayPopulate(newArray, TwistableBoneTransform::alloc);
        return newArray;
    }
}
