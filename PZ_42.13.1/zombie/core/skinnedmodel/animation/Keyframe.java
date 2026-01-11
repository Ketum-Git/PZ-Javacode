// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.math.PZMath;

/**
 * Created by LEMMYATI on 03/01/14.
 */
public final class Keyframe {
    public Quaternion rotation;
    public Vector3f position;
    public Vector3f scale = new Vector3f(1.0F, 1.0F, 1.0F);
    public int none;
    public String boneName;
    public float time = -1.0F;

    public Keyframe() {
    }

    public Keyframe(Vector3f pos, Quaternion rotation, Vector3f scale) {
        this.position = new Vector3f(pos);
        this.rotation = new Quaternion(rotation);
        this.scale = new Vector3f(scale);
    }

    public void set(Keyframe keyframe) {
        this.setPosition(keyframe.position);
        this.setRotation(keyframe.rotation);
        this.setScale(keyframe.scale);
        this.time = keyframe.time;
        this.none = keyframe.none;
        this.boneName = keyframe.boneName;
    }

    public void set(Vector3f in_pos, Quaternion in_rot, Vector3f in_scale) {
        this.setPosition(in_pos);
        this.setRotation(in_rot);
        this.setScale(in_scale);
    }

    public void get(Vector3f out_pos, Quaternion out_rot, Vector3f out_scale) {
        setIfNotNull(out_pos, this.position, 0.0F, 0.0F, 0.0F);
        setIfNotNull(out_rot, this.rotation);
        setIfNotNull(out_scale, this.scale, 1.0F, 1.0F, 1.0F);
    }

    public void setScale(Vector3f in_scale) {
        if (in_scale == null) {
            this.scale = null;
        } else {
            if (this.scale == null) {
                this.scale = new Vector3f();
            }

            this.scale.set(in_scale);
        }
    }

    public void setRotation(Quaternion in_rot) {
        if (in_rot == null) {
            this.rotation = null;
        } else {
            if (this.rotation == null) {
                this.rotation = new Quaternion();
            }

            this.rotation.set(in_rot);
        }
    }

    public void setPosition(Vector3f in_pos) {
        if (in_pos == null) {
            this.position = null;
        } else {
            if (this.position == null) {
                this.position = new Vector3f();
            }

            this.position.set(in_pos);
        }
    }

    public void clear() {
        this.time = -1.0F;
        this.position = null;
        this.rotation = null;
    }

    public void setIdentity() {
        setIdentity(this.position, this.rotation, this.scale);
    }

    public static void setIdentity(Vector3f out_pos, Quaternion out_rot, Vector3f out_scale) {
        setIfNotNull(out_pos, 0.0F, 0.0F, 0.0F);
        setIdentityIfNotNull(out_rot);
        setIfNotNull(out_scale, 1.0F, 1.0F, 1.0F);
    }

    public static Keyframe lerp(Keyframe a, Keyframe b, float time, Keyframe out_result) {
        lerp(a, b, time, out_result.position, out_result.rotation, out_result.scale);
        out_result.none = b.none;
        out_result.boneName = b.boneName;
        out_result.time = time;
        return out_result;
    }

    public static void setIfNotNull(Vector3f to, Vector3f val, float default_x, float default_y, float default_z) {
        if (to != null) {
            if (val != null) {
                to.set(val);
            } else {
                to.set(default_x, default_y, default_z);
            }
        }
    }

    public static void setIfNotNull(Vector3f to, float x, float y, float z) {
        if (to != null) {
            to.set(x, y, z);
        }
    }

    public static void setIfNotNull(Quaternion to, Quaternion val) {
        if (to != null) {
            if (val != null) {
                to.set(val);
            } else {
                to.setIdentity();
            }
        }
    }

    public static void setIdentityIfNotNull(Quaternion to) {
        if (to != null) {
            to.setIdentity();
        }
    }

    public static void lerp(Keyframe a, Keyframe b, float time, Vector3f out_pos, Quaternion out_rot, Vector3f out_scale) {
        if (b.time == a.time) {
            b.get(out_pos, out_rot, out_scale);
        } else {
            float del = (time - a.time) / (b.time - a.time);
            if (out_pos != null) {
                PZMath.lerp(out_pos, a.position, b.position, del);
            }

            if (out_rot != null) {
                PZMath.slerp(out_rot, a.rotation, b.rotation, del);
            }

            if (out_scale != null) {
                PZMath.lerp(out_scale, a.scale, b.scale, del);
            }
        }
    }
}
