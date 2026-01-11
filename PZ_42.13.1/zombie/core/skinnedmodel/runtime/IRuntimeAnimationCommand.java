// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.runtime;

import java.util.List;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.scripting.ScriptParser;

public interface IRuntimeAnimationCommand {
    void parse(ScriptParser.Block block);

    void exec(List<Keyframe> keyframes);
}
