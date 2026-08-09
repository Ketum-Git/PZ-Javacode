// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.runtime;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.skinnedmodel.animation.Keyframe;

public final class KeyframeUtil {
    static final Quaternion end = new Quaternion();

    public static Vector3f GetKeyFramePosition(Keyframe[] keyframes, float time, double duration) {
        Vector3f pos = new Vector3f();
        if (keyframes.length == 0) {
            return pos;
        } else {
            int frame = 0;

            while (frame < keyframes.length - 1 && !(time < keyframes[frame + 1].time)) {
                frame++;
            }

            int nextFrame = (frame + 1) % keyframes.length;
            Keyframe keyframe1 = keyframes[frame];
            Keyframe keyframe2 = keyframes[nextFrame];
            float t1 = keyframe1.time;
            float t2 = keyframe2.time;
            float diffTime = t2 - t1;
            if (diffTime < 0.0F) {
                diffTime = (float)(diffTime + duration);
            }

            if (diffTime > 0.0F) {
                float r = t2 - t1;
                float s = time - t1;
                s /= r;
                float x1 = keyframe1.position.x;
                float x2 = keyframe2.position.x;
                float x = x1 + s * (x2 - x1);
                float y1 = keyframe1.position.y;
                float y2 = keyframe2.position.y;
                float y = y1 + s * (y2 - y1);
                float z1 = keyframe1.position.z;
                float z2 = keyframe2.position.z;
                float z = z1 + s * (z2 - z1);
                pos.set(x, y, z);
            } else {
                pos.set(keyframe1.position);
            }

            return pos;
        }
    }

    public static Quaternion GetKeyFrameRotation(Keyframe[] keyframes, float time, double duration) {
        Quaternion foundQuat = new Quaternion();
        if (keyframes.length == 0) {
            return foundQuat;
        } else {
            int frame = 0;

            while (frame < keyframes.length - 1 && !(time < keyframes[frame + 1].time)) {
                frame++;
            }

            int nextFrame = (frame + 1) % keyframes.length;
            Keyframe keyframe1 = keyframes[frame];
            Keyframe keyframe2 = keyframes[nextFrame];
            float t1 = keyframe1.time;
            float t2 = keyframe2.time;
            float diffTime = t2 - t1;
            if (diffTime < 0.0F) {
                diffTime = (float)(diffTime + duration);
            }

            if (diffTime > 0.0F) {
                float pFactor = (time - t1) / diffTime;
                Quaternion pStart = keyframe1.rotation;
                Quaternion pEnd = keyframe2.rotation;
                double cosom = pStart.getX() * pEnd.getX() + pStart.getY() * pEnd.getY() + pStart.getZ() * pEnd.getZ() + pStart.getW() * pEnd.getW();
                end.set(pEnd);
                if (cosom < 0.0) {
                    cosom *= -1.0;
                    end.setX(-end.getX());
                    end.setY(-end.getY());
                    end.setZ(-end.getZ());
                    end.setW(-end.getW());
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
                    (float)(sclp * pStart.getX() + sclq * end.getX()),
                    (float)(sclp * pStart.getY() + sclq * end.getY()),
                    (float)(sclp * pStart.getZ() + sclq * end.getZ()),
                    (float)(sclp * pStart.getW() + sclq * end.getW())
                );
            } else {
                foundQuat.set(keyframe1.rotation);
            }

            return foundQuat;
        }
    }
}
