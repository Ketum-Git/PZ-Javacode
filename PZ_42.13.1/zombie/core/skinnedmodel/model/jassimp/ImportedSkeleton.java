// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model.jassimp;

import gnu.trove.list.array.TFloatArrayList;
import jassimp.AiAnimation;
import jassimp.AiBone;
import jassimp.AiBuiltInWrapperProvider;
import jassimp.AiMatrix4f;
import jassimp.AiMesh;
import jassimp.AiNode;
import jassimp.AiNodeAnim;
import jassimp.AiQuaternion;
import jassimp.AiScene;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ImportedSkeleton {
    final HashMap<String, Integer> boneIndices = new HashMap<>();
    final ArrayList<Integer> skeletonHierarchy = new ArrayList<>();
    final ArrayList<Matrix4f> bindPose = new ArrayList<>();
    final ArrayList<Matrix4f> invBindPose = new ArrayList<>();
    final ArrayList<Matrix4f> skinOffsetMatrices = new ArrayList<>();
    AiNode rootBoneNode;
    final HashMap<String, AnimationClip> clips = new HashMap<>();
    final AiBuiltInWrapperProvider wrapper = new AiBuiltInWrapperProvider();
    final Quaternion end = new Quaternion();

    private ImportedSkeleton() {
    }

    public static ImportedSkeleton process(ImportedSkeletonParams params) {
        ImportedSkeleton processedAiScene = new ImportedSkeleton();
        processedAiScene.processAiScene(params);
        return processedAiScene;
    }

    private void processAiScene(ImportedSkeletonParams params) {
        AiScene scene = params.scene;
        JAssImpImporter.LoadMode mode = params.mode;
        SkinningData skinnedTo = params.skinnedTo;
        float animBonesScaleModifier = params.animBonesScaleModifier;
        Quaternion animBonesRotateModifier = params.animBonesRotateModifier;
        AiMesh mesh = params.mesh;
        AiNode rootnode = scene.getSceneRoot(this.wrapper);
        this.rootBoneNode = JAssImpImporter.FindNode("Dummy01", rootnode);
        boolean bVehicle;
        if (this.rootBoneNode == null) {
            this.rootBoneNode = JAssImpImporter.FindNode("VehicleSkeleton", rootnode);
            bVehicle = true;
        } else {
            bVehicle = false;
        }

        while (this.rootBoneNode != null && this.rootBoneNode.getParent() != null && this.rootBoneNode.getParent() != rootnode) {
            this.rootBoneNode = this.rootBoneNode.getParent();
        }

        if (this.rootBoneNode == null) {
            this.rootBoneNode = rootnode;
        }

        ArrayList<AiNode> boneNodes = new ArrayList<>();
        JAssImpImporter.CollectBoneNodes(boneNodes, this.rootBoneNode);
        AiNode translationBonenode = JAssImpImporter.FindNode("Translation_Data", rootnode);
        if (translationBonenode != null) {
            boneNodes.add(translationBonenode);

            for (AiNode parent = translationBonenode.getParent(); parent != null && parent != rootnode; parent = parent.getParent()) {
                boneNodes.add(parent);
            }
        }

        if (skinnedTo != null) {
            this.boneIndices.putAll(skinnedTo.boneIndices);
            PZArrayUtil.addAll(this.skeletonHierarchy, skinnedTo.skeletonHierarchy);
        }

        for (int i = 0; i < boneNodes.size(); i++) {
            AiNode n = boneNodes.get(i);
            String bonename = n.getName();
            if (!this.boneIndices.containsKey(bonename)) {
                int index = this.boneIndices.size();
                this.boneIndices.put(bonename, index);
                if (n == this.rootBoneNode) {
                    this.skeletonHierarchy.add(-1);
                } else {
                    AiNode parent = n.getParent();

                    while (parent != null && !this.boneIndices.containsKey(parent.getName())) {
                        parent = parent.getParent();
                    }

                    if (parent != null) {
                        this.skeletonHierarchy.add(this.boneIndices.get(parent.getName()));
                    } else {
                        this.skeletonHierarchy.add(0);
                    }
                }
            }
        }

        Matrix4f mi = new Matrix4f();

        for (int ix = 0; ix < this.boneIndices.size(); ix++) {
            this.bindPose.add(mi);
            this.skinOffsetMatrices.add(mi);
        }

        List<AiBone> bones = mesh.getBones();
        Matrix4f skinOffseti = new Matrix4f();
        Matrix4f skinOffsetParent = new Matrix4f();
        Matrix4f skinOffsetParenti = new Matrix4f();

        for (int ix = 0; ix < boneNodes.size(); ix++) {
            AiNode node = boneNodes.get(ix);
            String nodeBoneName = node.getName();
            AiBone bone = JAssImpImporter.FindAiBone(nodeBoneName, bones);
            if (bone != null) {
                AiMatrix4f bonemat = bone.getOffsetMatrix(this.wrapper);
                if (bonemat != null) {
                    Matrix4f skinOffset = JAssImpImporter.getMatrixFromAiMatrix(bonemat);
                    skinOffseti.load(skinOffset);
                    skinOffseti.invert();
                    skinOffsetParent.setIdentity();
                    String parentBoneName = node.getParent().getName();
                    AiBone pbone = JAssImpImporter.FindAiBone(parentBoneName, bones);
                    if (pbone != null) {
                        AiMatrix4f pbonemat = pbone.getOffsetMatrix(this.wrapper);
                        if (pbonemat != null) {
                            JAssImpImporter.getMatrixFromAiMatrix(pbonemat, skinOffsetParent);
                        }
                    }

                    skinOffsetParenti.load(skinOffsetParent);
                    skinOffsetParenti.invert();
                    Matrix4f bind = new Matrix4f();
                    Matrix4f.mul(skinOffseti, skinOffsetParenti, bind);
                    bind.invert();
                    int boneindex = this.boneIndices.get(nodeBoneName);
                    this.bindPose.set(boneindex, bind);
                    this.skinOffsetMatrices.set(boneindex, skinOffset);
                }
            }
        }

        int num = this.bindPose.size();

        for (int ixx = 0; ixx < num; ixx++) {
            Matrix4f ib = new Matrix4f(this.bindPose.get(ixx));
            ib.invert();
            this.invBindPose.add(ixx, ib);
        }

        int numAnims = scene.getNumAnimations();
        if (numAnims > 0) {
            List<AiAnimation> srcAnims = scene.getAnimations();

            for (int ixx = 0; ixx < numAnims; ixx++) {
                AiAnimation srcAnim = srcAnims.get(ixx);
                if (bVehicle) {
                    this.processAnimation(srcAnim, bVehicle, 1.0F, null);
                } else {
                    this.processAnimation(srcAnim, bVehicle, animBonesScaleModifier, animBonesRotateModifier);
                }
            }
        }
    }

    @Deprecated
    void processAnimationOld(AiAnimation srcAnim, boolean bVehicle) {
        ArrayList<Keyframe> frames = new ArrayList<>();
        float numFrames = (float)srcAnim.getDuration();
        float duration = numFrames / (float)srcAnim.getTicksPerSecond();
        ArrayList<Float> frametimes = new ArrayList<>();
        List<AiNodeAnim> channels = srcAnim.getChannels();

        for (int j = 0; j < channels.size(); j++) {
            AiNodeAnim a = channels.get(j);

            for (int k = 0; k < a.getNumPosKeys(); k++) {
                float t = (float)a.getPosKeyTime(k);
                if (!frametimes.contains(t)) {
                    frametimes.add(t);
                }
            }

            for (int kx = 0; kx < a.getNumRotKeys(); kx++) {
                float t = (float)a.getRotKeyTime(kx);
                if (!frametimes.contains(t)) {
                    frametimes.add(t);
                }
            }

            for (int kxx = 0; kxx < a.getNumScaleKeys(); kxx++) {
                float t = (float)a.getScaleKeyTime(kxx);
                if (!frametimes.contains(t)) {
                    frametimes.add(t);
                }
            }
        }

        Collections.sort(frametimes);

        for (int tk = 0; tk < frametimes.size(); tk++) {
            for (int j = 0; j < channels.size(); j++) {
                AiNodeAnim a = channels.get(j);
                Keyframe f = new Keyframe();
                f.clear();
                f.boneName = a.getNodeName();
                Integer boneIdx = this.boneIndices.get(f.boneName);
                if (boneIdx == null) {
                    DebugLog.General.error("Could not find bone index for node name: \"%s\"", f.boneName);
                } else {
                    f.none = boneIdx;
                    f.time = frametimes.get(tk) / (float)srcAnim.getTicksPerSecond();
                    if (!bVehicle) {
                        f.position = JAssImpImporter.GetKeyFramePosition(a, frametimes.get(tk));
                        f.rotation = JAssImpImporter.GetKeyFrameRotation(a, frametimes.get(tk));
                        f.scale = JAssImpImporter.GetKeyFrameScale(a, frametimes.get(tk));
                    } else {
                        f.position = this.GetKeyFramePosition(a, frametimes.get(tk), srcAnim.getDuration());
                        f.rotation = this.GetKeyFrameRotation(a, frametimes.get(tk), srcAnim.getDuration());
                        f.scale = this.GetKeyFrameScale(a, frametimes.get(tk), srcAnim.getDuration());
                    }

                    if (f.none >= 0) {
                        frames.add(f);
                    }
                }
            }
        }

        String animName = srcAnim.getName();
        int p = animName.indexOf(124);
        if (p > 0) {
            animName = animName.substring(p + 1);
        }

        AnimationClip clip = new AnimationClip(duration, frames, animName, true);
        frames.clear();
        this.clips.put(animName, clip);
    }

    private void processAnimation(AiAnimation srcAnim, boolean bVehicle, float animBonesScaleModifier, Quaternion animBonesRotateModifier) {
        ArrayList<Keyframe> frames = new ArrayList<>();
        float duration = (float)srcAnim.getDuration();
        float durationSeconds = duration / (float)srcAnim.getTicksPerSecond();
        TFloatArrayList[] frameTimesPerBone = new TFloatArrayList[this.boneIndices.size()];
        Arrays.fill(frameTimesPerBone, null);
        ArrayList<ArrayList<AiNodeAnim>> channelsPerBone = new ArrayList<>(this.boneIndices.size());

        for (int i = 0; i < this.boneIndices.size(); i++) {
            channelsPerBone.add(null);
        }

        this.collectBoneFrames(srcAnim, frameTimesPerBone, channelsPerBone);
        Quaternion animBonesRotateModifierInv = null;
        boolean applyBoneRotateModifier = animBonesRotateModifier != null;
        if (applyBoneRotateModifier) {
            animBonesRotateModifierInv = new Quaternion();
            Quaternion.mulInverse(animBonesRotateModifierInv, animBonesRotateModifier, animBonesRotateModifierInv);
        }

        for (int boneIdx = 0; boneIdx < this.boneIndices.size(); boneIdx++) {
            ArrayList<AiNodeAnim> boneChannels = channelsPerBone.get(boneIdx);
            if (boneChannels == null) {
                if (boneIdx == 0 && animBonesRotateModifier != null) {
                    String boneName = "RootNode";
                    Quaternion rotation = new Quaternion();
                    rotation.set(animBonesRotateModifier);
                    this.addDefaultAnimTrack("RootNode", boneIdx, rotation, new Vector3f(0.0F, 0.0F, 0.0F), frames, durationSeconds);
                }
            } else {
                TFloatArrayList frameTimes = frameTimesPerBone[boneIdx];
                if (frameTimes != null) {
                    frameTimes.sort();
                    int parentBoneIdx = this.getParentBoneIdx(boneIdx);
                    boolean parentBoneAdjusted = applyBoneRotateModifier
                        && (parentBoneIdx == 0 || this.doesParentBoneHaveAnimFrames(frameTimesPerBone, channelsPerBone, boneIdx));

                    for (int tk = 0; tk < frameTimes.size(); tk++) {
                        float frameTime = frameTimes.get(tk);
                        float frameSeconds = frameTime / (float)srcAnim.getTicksPerSecond();

                        for (int channelIdx = 0; channelIdx < boneChannels.size(); channelIdx++) {
                            AiNodeAnim a = boneChannels.get(channelIdx);
                            Keyframe f = new Keyframe();
                            f.clear();
                            f.boneName = a.getNodeName();
                            f.none = boneIdx;
                            f.time = frameSeconds;
                            if (!bVehicle) {
                                f.position = JAssImpImporter.GetKeyFramePosition(a, frameTime);
                                f.rotation = JAssImpImporter.GetKeyFrameRotation(a, frameTime);
                                f.scale = JAssImpImporter.GetKeyFrameScale(a, frameTime);
                            } else {
                                f.position = this.GetKeyFramePosition(a, frameTime, duration);
                                f.rotation = this.GetKeyFrameRotation(a, frameTime, duration);
                                f.scale = this.GetKeyFrameScale(a, frameTime, duration);
                            }

                            f.position.x *= animBonesScaleModifier;
                            f.position.y *= animBonesScaleModifier;
                            f.position.z *= animBonesScaleModifier;
                            if (applyBoneRotateModifier) {
                                if (parentBoneAdjusted) {
                                    Quaternion.mul(animBonesRotateModifierInv, f.rotation, f.rotation);
                                    boolean isTranslationBone = StringUtils.startsWithIgnoreCase(f.boneName, "Translation_Data");
                                    if (!isTranslationBone) {
                                        HelperFunctions.transform(animBonesRotateModifierInv, f.position, f.position);
                                    }
                                }

                                Quaternion.mul(f.rotation, animBonesRotateModifier, f.rotation);
                            }

                            frames.add(f);
                        }
                    }
                }
            }
        }

        String animName = srcAnim.getName();
        int p = animName.indexOf(124);
        if (p > 0) {
            animName = animName.substring(p + 1);
        }

        animName = animName.trim();
        AnimationClip clip = new AnimationClip(durationSeconds, frames, animName, true);
        frames.clear();
        this.clips.put(animName, clip);
    }

    private void addDefaultAnimTrack(String boneName, int boneIdx, Quaternion rotation, Vector3f position, ArrayList<Keyframe> frames, float durationSeconds) {
        Vector3f one = new Vector3f(1.0F, 1.0F, 1.0F);
        Keyframe firstFrame = new Keyframe();
        firstFrame.clear();
        firstFrame.boneName = boneName;
        firstFrame.none = boneIdx;
        firstFrame.time = 0.0F;
        firstFrame.position = position;
        firstFrame.rotation = rotation;
        firstFrame.scale = one;
        frames.add(firstFrame);
        Keyframe lastFrame = new Keyframe();
        lastFrame.clear();
        lastFrame.boneName = boneName;
        lastFrame.none = boneIdx;
        lastFrame.time = durationSeconds;
        lastFrame.position = position;
        lastFrame.rotation = rotation;
        lastFrame.scale = one;
        frames.add(lastFrame);
    }

    private boolean doesParentBoneHaveAnimFrames(TFloatArrayList[] frameTimesPerBone, ArrayList<ArrayList<AiNodeAnim>> channelsPerBone, int boneIdx) {
        int parentBoneIdx = this.getParentBoneIdx(boneIdx);
        return parentBoneIdx < 0 ? false : this.doesBoneHaveAnimFrames(frameTimesPerBone, channelsPerBone, parentBoneIdx);
    }

    private boolean doesBoneHaveAnimFrames(TFloatArrayList[] frameTimesPerBone, ArrayList<ArrayList<AiNodeAnim>> channelsPerBone, int boneIdx) {
        TFloatArrayList frameTimes = frameTimesPerBone[boneIdx];
        if (frameTimes != null && frameTimes.size() > 0) {
            ArrayList<AiNodeAnim> boneChannels = channelsPerBone.get(boneIdx);
            return boneChannels.size() > 0;
        } else {
            return false;
        }
    }

    private void collectBoneFrames(AiAnimation srcAnim, TFloatArrayList[] frameTimesPerBone, ArrayList<ArrayList<AiNodeAnim>> channelsPerBone) {
        List<AiNodeAnim> channels = srcAnim.getChannels();

        for (int j = 0; j < channels.size(); j++) {
            AiNodeAnim a = channels.get(j);
            String boneName = a.getNodeName();
            Integer boneIdx = this.boneIndices.get(boneName);
            if (boneIdx == null) {
                DebugLog.General.error("Could not find bone index for node name: \"%s\"", boneName);
            } else {
                ArrayList<AiNodeAnim> boneChannels = channelsPerBone.get(boneIdx);
                if (boneChannels == null) {
                    boneChannels = new ArrayList<>();
                    channelsPerBone.set(boneIdx, boneChannels);
                }

                boneChannels.add(a);
                TFloatArrayList frametimes = frameTimesPerBone[boneIdx];
                if (frametimes == null) {
                    frametimes = new TFloatArrayList();
                    frameTimesPerBone[boneIdx] = frametimes;
                }

                for (int k = 0; k < a.getNumPosKeys(); k++) {
                    float t = (float)a.getPosKeyTime(k);
                    if (!frametimes.contains(t)) {
                        frametimes.add(t);
                    }
                }

                for (int kx = 0; kx < a.getNumRotKeys(); kx++) {
                    float t = (float)a.getRotKeyTime(kx);
                    if (!frametimes.contains(t)) {
                        frametimes.add(t);
                    }
                }

                for (int kxx = 0; kxx < a.getNumScaleKeys(); kxx++) {
                    float t = (float)a.getScaleKeyTime(kxx);
                    if (!frametimes.contains(t)) {
                        frametimes.add(t);
                    }
                }
            }
        }
    }

    private int getParentBoneIdx(int boneIdx) {
        return boneIdx > -1 ? this.skeletonHierarchy.get(boneIdx) : -1;
    }

    public int getNumBoneAncestors(int boneIdx) {
        int numAncestors = 0;

        for (int parentBoneIdx = this.getParentBoneIdx(boneIdx); parentBoneIdx > -1; parentBoneIdx = this.getParentBoneIdx(parentBoneIdx)) {
            numAncestors++;
        }

        return numAncestors;
    }

    private Vector3f GetKeyFramePosition(AiNodeAnim animNode, float time, double duration) {
        Vector3f pos = new Vector3f();
        if (animNode.getNumPosKeys() == 0) {
            return pos;
        } else {
            int frame = 0;

            while (frame < animNode.getNumPosKeys() - 1 && !(time < animNode.getPosKeyTime(frame + 1))) {
                frame++;
            }

            int nextFrame = (frame + 1) % animNode.getNumPosKeys();
            float t1 = (float)animNode.getPosKeyTime(frame);
            float t2 = (float)animNode.getPosKeyTime(nextFrame);
            float diffTime = t2 - t1;
            if (diffTime < 0.0F) {
                diffTime = (float)(diffTime + duration);
            }

            if (diffTime > 0.0F) {
                float r = t2 - t1;
                float s = time - t1;
                s /= r;
                float x1 = animNode.getPosKeyX(frame);
                float x2 = animNode.getPosKeyX(nextFrame);
                float x = x1 + s * (x2 - x1);
                float y1 = animNode.getPosKeyY(frame);
                float y2 = animNode.getPosKeyY(nextFrame);
                float y = y1 + s * (y2 - y1);
                float z1 = animNode.getPosKeyZ(frame);
                float z2 = animNode.getPosKeyZ(nextFrame);
                float z = z1 + s * (z2 - z1);
                pos.set(x, y, z);
            } else {
                pos.set(animNode.getPosKeyX(frame), animNode.getPosKeyY(frame), animNode.getPosKeyZ(frame));
            }

            return pos;
        }
    }

    private Quaternion GetKeyFrameRotation(AiNodeAnim animNode, float time, double duration) {
        Quaternion foundQuat = new Quaternion();
        if (animNode.getNumRotKeys() == 0) {
            return foundQuat;
        } else {
            int frame = 0;

            while (frame < animNode.getNumRotKeys() - 1 && !(time < animNode.getRotKeyTime(frame + 1))) {
                frame++;
            }

            int nextFrame = (frame + 1) % animNode.getNumRotKeys();
            float t1 = (float)animNode.getRotKeyTime(frame);
            float t2 = (float)animNode.getRotKeyTime(nextFrame);
            float diffTime = t2 - t1;
            if (diffTime < 0.0F) {
                diffTime = (float)(diffTime + duration);
            }

            if (diffTime > 0.0F) {
                float pFactor = (time - t1) / diffTime;
                AiQuaternion pStart = animNode.getRotKeyQuaternion(frame, this.wrapper);
                AiQuaternion pEnd = animNode.getRotKeyQuaternion(nextFrame, this.wrapper);
                double cosom = pStart.getX() * pEnd.getX() + pStart.getY() * pEnd.getY() + pStart.getZ() * pEnd.getZ() + pStart.getW() * pEnd.getW();
                this.end.set(pEnd.getX(), pEnd.getY(), pEnd.getZ(), pEnd.getW());
                if (cosom < 0.0) {
                    cosom *= -1.0;
                    this.end.setX(-this.end.getX());
                    this.end.setY(-this.end.getY());
                    this.end.setZ(-this.end.getZ());
                    this.end.setW(-this.end.getW());
                }

                double sclp;
                double sclq;
                if (1.0 - cosom > 1.0E-4) {
                    double omega = Math.acos(cosom);
                    double sinom = Math.sin(omega);
                    sclp = Math.sin((1.0 - pFactor) * omega) / sinom;
                    sclq = Math.sin(pFactor * omega) / sinom;
                } else {
                    sclp = 1.0 - pFactor;
                    sclq = pFactor;
                }

                foundQuat.set(
                    (float)(sclp * pStart.getX() + sclq * this.end.getX()),
                    (float)(sclp * pStart.getY() + sclq * this.end.getY()),
                    (float)(sclp * pStart.getZ() + sclq * this.end.getZ()),
                    (float)(sclp * pStart.getW() + sclq * this.end.getW())
                );
            } else {
                float x = animNode.getRotKeyX(frame);
                float y = animNode.getRotKeyY(frame);
                float z = animNode.getRotKeyZ(frame);
                float w = animNode.getRotKeyW(frame);
                foundQuat.set(x, y, z, w);
            }

            return foundQuat;
        }
    }

    private Vector3f GetKeyFrameScale(AiNodeAnim animNode, float time, double duration) {
        Vector3f scale = new Vector3f(1.0F, 1.0F, 1.0F);
        if (animNode.getNumScaleKeys() == 0) {
            return scale;
        } else {
            int frame = 0;

            while (frame < animNode.getNumScaleKeys() - 1 && !(time < animNode.getScaleKeyTime(frame + 1))) {
                frame++;
            }

            scale.set(animNode.getScaleKeyX(frame), animNode.getScaleKeyY(frame), animNode.getScaleKeyZ(frame));
            return scale;
        }
    }
}
