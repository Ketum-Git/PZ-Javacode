// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import zombie.util.Pool;
import zombie.util.list.PZArrayUtil;

public class AnimatorsBoneTransform extends TwistableBoneTransform {
    private float timeDelta = -1.0F;
    private final TwistableBoneTransform previousTransform = new TwistableBoneTransform();
    private static final Pool<AnimatorsBoneTransform> s_pool = new Pool<>(AnimatorsBoneTransform::new);

    @Override
    public void set(BoneTransform rhs) {
        super.set(rhs);
        if (rhs instanceof AnimatorsBoneTransform transform) {
            this.timeDelta = transform.timeDelta;
            this.previousTransform.set(transform.previousTransform);
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.timeDelta = -1.0F;
        this.previousTransform.reset();
    }

    public <T extends BoneTransform> T getPreviousTransform(T result) {
        result.set(this.previousTransform);
        return result;
    }

    public float getTimeDelta() {
        return this.timeDelta;
    }

    public void nextFrame(float timeDelta) {
        this.timeDelta = timeDelta;
        this.previousTransform.set(this);
    }

    public static AnimatorsBoneTransform alloc() {
        return s_pool.alloc();
    }

    public static TwistableBoneTransform[] allocArray(int count) {
        AnimatorsBoneTransform[] newArray = new AnimatorsBoneTransform[count];
        PZArrayUtil.arrayPopulate(newArray, AnimatorsBoneTransform::alloc);
        return newArray;
    }
}
