// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.sharedskele;

import org.lwjgl.util.vector.Matrix4f;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.animation.AnimTrackSampler;
import zombie.debug.DebugOptions;

public class SharedSkeleAnimationTrack implements AnimTrackSampler {
    private int numFrames;
    private float totalTime;
    private boolean isLooped;
    private SharedSkeleAnimationTrack.BoneTrack[] boneTracks;
    private float currentTime;

    public void set(AnimTrackSampler sampler, float fps) {
        float totalTime = sampler.getTotalTime();
        boolean isLooped = sampler.isLooped();
        int numBones = sampler.getNumBones();
        this.totalTime = totalTime;
        this.numFrames = PZMath.max((int)(totalTime * fps + 0.99F), 1);
        this.isLooped = isLooped;
        this.boneTracks = new SharedSkeleAnimationTrack.BoneTrack[numBones];

        for (int i = 0; i < numBones; i++) {
            this.boneTracks[i] = new SharedSkeleAnimationTrack.BoneTrack();
            this.boneTracks[i].animationData = new float[this.numFrames * 16];
        }

        Matrix4f animationDataFrame = new Matrix4f();
        float timeIncrement = totalTime / (this.numFrames - 1);

        for (int frameIdx = 0; frameIdx < this.numFrames; frameIdx++) {
            float time = timeIncrement * frameIdx;
            sampler.moveToTime(time);

            for (int boneIdx = 0; boneIdx < numBones; boneIdx++) {
                sampler.getBoneMatrix(boneIdx, animationDataFrame);
                int idx = frameIdx * 16;
                SharedSkeleAnimationTrack.BoneTrack boneTrack = this.boneTracks[boneIdx];
                float[] animationData = boneTrack.animationData;
                animationData[idx] = animationDataFrame.m00;
                animationData[idx + 1] = animationDataFrame.m01;
                animationData[idx + 2] = animationDataFrame.m02;
                animationData[idx + 3] = animationDataFrame.m03;
                animationData[idx + 4] = animationDataFrame.m10;
                animationData[idx + 5] = animationDataFrame.m11;
                animationData[idx + 6] = animationDataFrame.m12;
                animationData[idx + 7] = animationDataFrame.m13;
                animationData[idx + 8] = animationDataFrame.m20;
                animationData[idx + 9] = animationDataFrame.m21;
                animationData[idx + 10] = animationDataFrame.m22;
                animationData[idx + 11] = animationDataFrame.m23;
                animationData[idx + 12] = animationDataFrame.m30;
                animationData[idx + 13] = animationDataFrame.m31;
                animationData[idx + 14] = animationDataFrame.m32;
                animationData[idx + 15] = animationDataFrame.m33;
            }
        }
    }

    @Override
    public float getTotalTime() {
        return this.totalTime;
    }

    @Override
    public boolean isLooped() {
        return this.isLooped;
    }

    @Override
    public void moveToTime(float time) {
        this.currentTime = time;
    }

    @Override
    public float getCurrentTime() {
        return this.currentTime;
    }

    @Override
    public void getBoneMatrix(int boneIdx, Matrix4f out_matrix) {
        float totalTime = this.totalTime;
        int numFrames = this.numFrames;
        float t = this.getCurrentTime();
        float alpha = t / totalTime;
        float frameIndexf = alpha * (numFrames - 1);
        if (this.isLooped()) {
            this.sampleAtTime_Looped(out_matrix, boneIdx, frameIndexf);
        } else {
            this.sampleAtTime_NonLooped(out_matrix, boneIdx, frameIndexf);
        }
    }

    @Override
    public int getNumBones() {
        return this.boneTracks != null ? this.boneTracks.length : 0;
    }

    private void sampleAtTime_NonLooped(Matrix4f out_matrix, int boneIdx, float frameIndexf) {
        int rawFrameIndex = (int)frameIndexf;
        float alpha = frameIndexf - rawFrameIndex;
        int frameIndex = PZMath.clamp(rawFrameIndex, 0, this.numFrames - 1);
        int nextFrameIndex = PZMath.clamp(frameIndex + 1, 0, this.numFrames - 1);
        boolean allowLerping = DebugOptions.instance.animation.sharedSkeles.allowLerping.getValue();
        this.sampleBoneData(boneIdx, frameIndex, nextFrameIndex, alpha, allowLerping, out_matrix);
    }

    private void sampleAtTime_Looped(Matrix4f out_matrix, int boneIdx, float frameIndexf) {
        int rawFrameIndex = (int)frameIndexf;
        float alpha = frameIndexf - rawFrameIndex;
        int frameIndex = rawFrameIndex % this.numFrames;
        int nextFrameIndex = (frameIndex + 1) % this.numFrames;
        boolean allowLerping = DebugOptions.instance.animation.sharedSkeles.allowLerping.getValue();
        this.sampleBoneData(boneIdx, frameIndex, nextFrameIndex, alpha, allowLerping, out_matrix);
    }

    private void sampleBoneData(int boneIdx, int frameIndex, int nextFrameIndex, float alpha, boolean allowLerping, Matrix4f out_matrix) {
        int idx = frameIndex * 16;
        SharedSkeleAnimationTrack.BoneTrack boneTrack = this.boneTracks[boneIdx];
        float[] animationData = boneTrack.animationData;
        if (frameIndex != nextFrameIndex && allowLerping) {
            int nextIdx = nextFrameIndex * 16;
            out_matrix.m00 = PZMath.lerp(animationData[idx], animationData[nextIdx], alpha);
            out_matrix.m01 = PZMath.lerp(animationData[idx + 1], animationData[nextIdx + 1], alpha);
            out_matrix.m02 = PZMath.lerp(animationData[idx + 2], animationData[nextIdx + 2], alpha);
            out_matrix.m03 = PZMath.lerp(animationData[idx + 3], animationData[nextIdx + 3], alpha);
            out_matrix.m10 = PZMath.lerp(animationData[idx + 4], animationData[nextIdx + 4], alpha);
            out_matrix.m11 = PZMath.lerp(animationData[idx + 5], animationData[nextIdx + 5], alpha);
            out_matrix.m12 = PZMath.lerp(animationData[idx + 6], animationData[nextIdx + 6], alpha);
            out_matrix.m13 = PZMath.lerp(animationData[idx + 7], animationData[nextIdx + 7], alpha);
            out_matrix.m20 = PZMath.lerp(animationData[idx + 8], animationData[nextIdx + 8], alpha);
            out_matrix.m21 = PZMath.lerp(animationData[idx + 9], animationData[nextIdx + 9], alpha);
            out_matrix.m22 = PZMath.lerp(animationData[idx + 10], animationData[nextIdx + 10], alpha);
            out_matrix.m23 = PZMath.lerp(animationData[idx + 11], animationData[nextIdx + 11], alpha);
            out_matrix.m30 = PZMath.lerp(animationData[idx + 12], animationData[nextIdx + 12], alpha);
            out_matrix.m31 = PZMath.lerp(animationData[idx + 13], animationData[nextIdx + 13], alpha);
            out_matrix.m32 = PZMath.lerp(animationData[idx + 14], animationData[nextIdx + 14], alpha);
            out_matrix.m33 = PZMath.lerp(animationData[idx + 15], animationData[nextIdx + 15], alpha);
        } else {
            out_matrix.m00 = animationData[idx];
            out_matrix.m01 = animationData[idx + 1];
            out_matrix.m02 = animationData[idx + 2];
            out_matrix.m03 = animationData[idx + 3];
            out_matrix.m10 = animationData[idx + 4];
            out_matrix.m11 = animationData[idx + 5];
            out_matrix.m12 = animationData[idx + 6];
            out_matrix.m13 = animationData[idx + 7];
            out_matrix.m20 = animationData[idx + 8];
            out_matrix.m21 = animationData[idx + 9];
            out_matrix.m22 = animationData[idx + 10];
            out_matrix.m23 = animationData[idx + 11];
            out_matrix.m30 = animationData[idx + 12];
            out_matrix.m31 = animationData[idx + 13];
            out_matrix.m32 = animationData[idx + 14];
            out_matrix.m33 = animationData[idx + 15];
        }
    }

    private static class BoneTrack {
        private float[] animationData;
    }
}
