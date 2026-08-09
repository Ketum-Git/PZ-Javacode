// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import org.lwjgl.util.vector.Matrix4f;

public interface AnimTrackSampler {
    float getTotalTime();

    boolean isLooped();

    void moveToTime(float time);

    float getCurrentTime();

    void getBoneMatrix(int boneIdx, Matrix4f out_matrix);

    int getNumBones();
}
