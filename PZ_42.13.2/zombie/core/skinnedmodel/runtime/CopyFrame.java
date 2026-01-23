// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.runtime;

import java.util.List;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.scripting.ScriptParser;

public final class CopyFrame implements IRuntimeAnimationCommand {
    protected int frame;
    protected int fps = 30;
    protected String source;
    protected int sourceFrame;
    protected int sourceFps = 30;

    @Override
    public void parse(ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("source".equalsIgnoreCase(k)) {
                this.source = v;
            } else if ("frame".equalsIgnoreCase(k)) {
                this.frame = PZMath.tryParseInt(v, 1);
            } else if ("sourceFrame".equalsIgnoreCase(k)) {
                this.sourceFrame = PZMath.tryParseInt(v, 1);
            }
        }
    }

    @Override
    public void exec(List<Keyframe> keyframes) {
        AnimationClip clip = ModelManager.instance.getAnimationClip(this.source);

        for (int i = 0; i < 60; i++) {
            Keyframe[] keyframes1 = clip.getBoneFramesAt(i);
            if (keyframes1.length != 0) {
                Keyframe keyframe1 = keyframes1[0];
                Keyframe newKeyframe = new Keyframe();
                newKeyframe.none = keyframe1.none;
                newKeyframe.boneName = keyframe1.boneName;
                newKeyframe.time = (float)(this.frame - 1) / this.fps;
                newKeyframe.position = KeyframeUtil.GetKeyFramePosition(keyframes1, (float)(this.sourceFrame - 1) / this.sourceFps, clip.getDuration());
                newKeyframe.rotation = KeyframeUtil.GetKeyFrameRotation(keyframes1, (float)(this.sourceFrame - 1) / this.sourceFps, clip.getDuration());
                newKeyframe.scale = keyframe1.scale;
                keyframes.add(newKeyframe);
            }
        }
    }
}
