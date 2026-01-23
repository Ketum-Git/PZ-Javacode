// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.skinnedmodel.animation.AnimationClip;

/**
 * Created by LEMMYATI on 03/01/14.
 */
public final class SkinningData {
    public final SkinningData.Buffers buffers;
    public HashMap<String, AnimationClip> animationClips;
    public List<Matrix4f> bindPose;
    public List<Matrix4f> inverseBindPose;
    public List<Matrix4f> boneOffset = new ArrayList<>();
    public List<Integer> skeletonHierarchy;
    public HashMap<String, Integer> boneIndices;
    private volatile boolean boneHierarchyValid;
    private SkinningBoneHierarchy boneHierarchy;
    private final Object boneHierarchyLock = new Object();
    private SkinningBoneHierarchy skeletonBoneHierarchy;
    private final String skeletonBoneName = "Dummy01";

    public SkinningData(
        HashMap<String, AnimationClip> animationClips,
        List<Matrix4f> bindPose,
        List<Matrix4f> inverseBindPose,
        List<Matrix4f> skinOffset,
        List<Integer> skeletonHierarchy,
        HashMap<String, Integer> boneIndices
    ) {
        this.animationClips = animationClips;
        this.bindPose = bindPose;
        this.inverseBindPose = inverseBindPose;
        this.skeletonHierarchy = skeletonHierarchy;

        for (int n = 0; n < skeletonHierarchy.size(); n++) {
            Matrix4f f = skinOffset.get(n);
            this.boneOffset.add(f);
        }

        this.boneIndices = boneIndices;
        this.buffers = null;
    }

    private void validateBoneHierarchy() {
        if (!this.boneHierarchyValid) {
            synchronized (this.boneHierarchyLock) {
                if (!this.boneHierarchyValid) {
                    this.boneHierarchy = new SkinningBoneHierarchy();
                    this.boneHierarchy.buildBoneHierarchy(this);
                    this.boneHierarchyValid = true;
                }
            }
        }
    }

    public int numBones() {
        return this.skeletonHierarchy.size();
    }

    public int numRootBones() {
        return this.getBoneHierarchy().numRootBones();
    }

    public int getParentBoneIdx(int boneIdx) {
        return this.skeletonHierarchy.get(boneIdx);
    }

    public SkinningBone getBoneAt(int boneIdx) {
        return this.getBoneHierarchy().getBoneAt(boneIdx);
    }

    public SkinningBone getBone(String boneName) {
        Integer boneIdx = this.boneIndices.get(boneName);
        return boneIdx == null ? null : this.getBoneAt(boneIdx);
    }

    public SkinningBone getRootBoneAt(int idx) {
        return this.getBoneHierarchy().getRootBoneAt(idx);
    }

    public SkinningBoneHierarchy getBoneHierarchy() {
        this.validateBoneHierarchy();
        return this.boneHierarchy;
    }

    public SkinningBoneHierarchy getSkeletonBoneHierarchy() {
        if (this.skeletonBoneHierarchy == null) {
            this.skeletonBoneHierarchy = this.getBoneHierarchy().getSubHierarchy("Dummy01");
        }

        return this.skeletonBoneHierarchy;
    }

    public static final class Buffers {
        public FloatBuffer boneMatrices;
        public FloatBuffer boneWeights;
        public ShortBuffer boneIds;

        public Buffers(List<Matrix4f> _boneMatrices, float[] _boneWeights, List<Integer> _boneIDs) {
            this.boneMatrices = BufferUtils.createFloatBuffer(_boneMatrices.size() * 16);

            for (int i = 0; i < _boneMatrices.size(); i++) {
                Matrix4f matrix = _boneMatrices.get(i);
                matrix.store(this.boneMatrices);
            }

            this.boneWeights = BufferUtils.createFloatBuffer(_boneWeights.length);
            this.boneWeights.put(_boneWeights);
            this.boneIds = BufferUtils.createShortBuffer(_boneIDs.size());

            for (int i = 0; i < _boneIDs.size(); i++) {
                this.boneIds.put(_boneIDs.get(i).shortValue());
            }
        }
    }
}
