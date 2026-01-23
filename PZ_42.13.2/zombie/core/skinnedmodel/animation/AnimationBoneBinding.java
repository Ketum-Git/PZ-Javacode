// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.util.StringUtils;

public class AnimationBoneBinding {
    public final String boneName;
    private SkinningBone bone;
    private SkinningData skinningData;

    public AnimationBoneBinding(String boneName) {
        this.boneName = boneName;
    }

    public SkinningData getSkinningData() {
        return this.skinningData;
    }

    public void setSkinningData(SkinningData skinningData) {
        if (this.skinningData != skinningData) {
            this.skinningData = skinningData;
            this.bone = null;
        }
    }

    public SkinningBone getBone() {
        if (this.bone == null) {
            this.initBone();
        }

        return this.bone;
    }

    private void initBone() {
        if (this.skinningData == null) {
            this.bone = null;
        } else {
            this.bone = this.skinningData.getBone(this.boneName);
        }
    }

    @Override
    public String toString() {
        String tab = "\t";
        String endln = System.lineSeparator();
        return this.getClass().getName()
            + endln
            + "{"
            + endln
            + "\tboneName:\""
            + this.boneName
            + "\""
            + endln
            + "\tm_bone:"
            + StringUtils.indent(String.valueOf(this.bone))
            + endln
            + "}";
    }
}
