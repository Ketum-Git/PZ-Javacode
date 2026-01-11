// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.util.StringUtils;

public final class AnimationBoneBindingPair {
    public final AnimationBoneBinding boneBindingA;
    public final AnimationBoneBinding boneBindingB;

    public AnimationBoneBindingPair(String boneA, String boneB) {
        this.boneBindingA = new AnimationBoneBinding(boneA);
        this.boneBindingB = new AnimationBoneBinding(boneB);
    }

    public void setSkinningData(SkinningData skinningData) {
        this.boneBindingA.setSkinningData(skinningData);
        this.boneBindingB.setSkinningData(skinningData);
    }

    public SkinningBone getBoneA() {
        return this.boneBindingA.getBone();
    }

    public SkinningBone getBoneB() {
        return this.boneBindingB.getBone();
    }

    public boolean isValid() {
        return this.getBoneA() != null && this.getBoneB() != null;
    }

    public boolean matches(String boneA, String boneB) {
        return StringUtils.equalsIgnoreCase(this.boneBindingA.boneName, boneA) && StringUtils.equalsIgnoreCase(this.boneBindingB.boneName, boneB);
    }

    public int getBoneIdxA() {
        return getBoneIdx(this.getBoneA());
    }

    public int getBoneIdxB() {
        return getBoneIdx(this.getBoneB());
    }

    private static int getBoneIdx(SkinningBone bone) {
        return bone != null ? bone.index : -1;
    }

    @Override
    public String toString() {
        String endln = System.lineSeparator();
        String tab = "\t";
        return this.getClass().getName()
            + endln
            + "{"
            + endln
            + "\tboneBindingA:"
            + StringUtils.indent(String.valueOf(this.boneBindingA))
            + endln
            + "\tboneBindingB:"
            + StringUtils.indent(String.valueOf(this.boneBindingB))
            + endln
            + "}";
    }
}
