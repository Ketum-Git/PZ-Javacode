// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.function.Predicate;
import zombie.debug.DebugType;
import zombie.util.Lambda;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class SkinningBoneHierarchy {
    private boolean boneHierarchyValid;
    private SkinningBone[] allBones;
    private SkinningBone[] rootBones;

    public boolean isValid() {
        return this.boneHierarchyValid;
    }

    public void buildBoneHierarchy(SkinningData data) {
        this.rootBones = new SkinningBone[0];
        this.allBones = new SkinningBone[data.numBones()];
        PZArrayUtil.arrayPopulate(this.allBones, SkinningBone::new);

        for (Entry<String, Integer> entry : data.boneIndices.entrySet()) {
            int idx = entry.getValue();
            String boneName = entry.getKey();
            SkinningBone bone = this.allBones[idx];
            bone.index = idx;
            bone.name = boneName;
            bone.skeletonBone = StringUtils.tryParseEnum(SkeletonBone.class, boneName, SkeletonBone.None);
            bone.children = new SkinningBone[0];
            if (bone.skeletonBone == SkeletonBone.None) {
                DebugType.Ragdoll.warn("SkeletonBone not resolved for bone: %s, defaulting to SkeletonBone.None", boneName);
            }
        }

        for (int i = 0; i < data.numBones(); i++) {
            SkinningBone bone = this.allBones[i];
            int parentIdx = data.getParentBoneIdx(i);
            if (parentIdx > -1) {
                bone.parent = this.allBones[parentIdx];
                bone.parent.children = PZArrayUtil.add(bone.parent.children, bone);
            } else {
                this.rootBones = PZArrayUtil.add(this.rootBones, bone);
            }
        }

        this.boneHierarchyValid = true;
    }

    public int numRootBones() {
        return this.rootBones.length;
    }

    public SkinningBone getBoneAt(int boneIdx) {
        return this.allBones[boneIdx];
    }

    public SkinningBone getBone(SkeletonBone in_skeletonBone) {
        return this.getBone(Lambda.predicate(in_skeletonBone, (bone, skeletonBone) -> bone.skeletonBone == skeletonBone));
    }

    public SkinningBone getBone(String in_boneName) {
        return this.getBone(Lambda.predicate(in_boneName, (bone, boneName) -> StringUtils.equalsIgnoreCase(bone.name, boneName)));
    }

    public SkinningBone getBone(Predicate<SkinningBone> predicate) {
        return PZArrayUtil.find(this.allBones, predicate);
    }

    public SkinningBone getRootBoneAt(int idx) {
        return this.rootBones[idx];
    }

    public SkinningBoneHierarchy getSubHierarchy(String boneName) {
        SkinningBone foundBone = this.getBone(boneName);
        return getSubHierarchy(foundBone);
    }

    public SkinningBoneHierarchy getSubHierarchy(int boneIdx) {
        SkinningBone foundBone = this.getBoneAt(boneIdx);
        return getSubHierarchy(foundBone);
    }

    public static SkinningBoneHierarchy getSubHierarchy(SkinningBone rootBone) {
        if (rootBone == null) {
            return null;
        } else {
            ArrayList<SkinningBone> allBones = new ArrayList<>();
            populateSubHierarchy(rootBone, allBones);
            SkinningBoneHierarchy out_subHierarchy = new SkinningBoneHierarchy();
            out_subHierarchy.allBones = allBones.toArray(new SkinningBone[0]);
            out_subHierarchy.rootBones = new SkinningBone[]{rootBone.toRoot()};
            out_subHierarchy.boneHierarchyValid = true;
            return out_subHierarchy;
        }
    }

    private static void populateSubHierarchy(SkinningBone rootBone, ArrayList<SkinningBone> inout_allBones) {
        inout_allBones.add(rootBone);

        for (SkinningBone childBone : rootBone.children) {
            populateSubHierarchy(childBone, inout_allBones);
        }
    }

    public int numBones() {
        return PZArrayUtil.lengthOf(this.allBones);
    }
}
