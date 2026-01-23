// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.function.Consumer;
import zombie.util.list.PZArrayUtil;

public final class SkinningBone {
    public SkinningBone parent;
    public String name;
    public int index;
    public SkinningBone[] children;
    public SkeletonBone skeletonBone = SkeletonBone.None;

    public void forEachDescendant(Consumer<SkinningBone> consumer) {
        forEachDescendant(this, consumer);
    }

    private static void forEachDescendant(SkinningBone bone, Consumer<SkinningBone> consumer) {
        if (bone.children != null && bone.children.length != 0) {
            for (SkinningBone child : bone.children) {
                consumer.accept(child);
            }

            for (SkinningBone child : bone.children) {
                forEachDescendant(child, consumer);
            }
        }
    }

    @Override
    public String toString() {
        String tab = " ";
        String endln = "";
        return this.getClass().getName() + "{ Name:\"" + this.name + "\", Index:" + this.index + ", SkeletonBone:" + this.skeletonBone + ",}";
    }

    public int getParentBoneIndex() {
        return this.parent != null ? this.parent.index : -1;
    }

    public SkeletonBone getParentSkeletonBone() {
        return this.parent != null ? this.parent.skeletonBone : SkeletonBone.None;
    }

    public SkinningBone toRoot() {
        if (this.parent == null) {
            return this;
        } else {
            SkinningBone out_newRoot = new SkinningBone();
            out_newRoot.name = this.name;
            out_newRoot.index = this.index;
            out_newRoot.skeletonBone = this.skeletonBone;
            out_newRoot.children = PZArrayUtil.shallowClone(this.children);
            out_newRoot.parent = null;
            return out_newRoot;
        }
    }

    public boolean isRoot() {
        return this.parent == null;
    }
}
