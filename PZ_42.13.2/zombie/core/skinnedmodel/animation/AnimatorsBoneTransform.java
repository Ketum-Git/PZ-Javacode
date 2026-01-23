// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import zombie.util.Pool;
import zombie.util.list.PZArrayUtil;

public class AnimatorsBoneTransform extends TwistableBoneTransform {
    private float timeDelta = -1.0F;
    private final TwistableBoneTransform previousTransform = new TwistableBoneTransform();
    private static final Pool<AnimatorsBoneTransform> s_pool = new Pool<>(AnimatorsBoneTransform::new);

    @Override
    public void set(BoneTransform in_rhs) {
        super.set(in_rhs);
        if (in_rhs instanceof AnimatorsBoneTransform rhs) {
            this.timeDelta = rhs.timeDelta;
            this.previousTransform.set(rhs.previousTransform);
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.timeDelta = -1.0F;
        this.previousTransform.reset();
    }

    public <T extends BoneTransform> T getPreviousTransform(T out_result) {
        out_result.set(this.previousTransform);
        return out_result;
    }

    public float getTimeDelta() {
        return this.timeDelta;
    }

    public void nextFrame(float in_timeDelta) {
        this.timeDelta = in_timeDelta;
        this.previousTransform.set(this);
    }

    public static AnimatorsBoneTransform alloc() {
        return s_pool.alloc();
    }

    public static TwistableBoneTransform[] allocArray(int in_count) {
        AnimatorsBoneTransform[] newArray = new AnimatorsBoneTransform[in_count];
        PZArrayUtil.arrayPopulate(newArray, AnimatorsBoneTransform::alloc);
        return newArray;
    }
}
