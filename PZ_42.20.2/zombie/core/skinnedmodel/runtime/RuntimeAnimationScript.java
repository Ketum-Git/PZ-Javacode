// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.runtime;

import java.util.ArrayList;
import java.util.List;
import zombie.UsedFromLua;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@UsedFromLua
public final class RuntimeAnimationScript extends BaseScriptObject {
    protected String name = this.toString();
    protected final ArrayList<IRuntimeAnimationCommand> commands = new ArrayList<>();

    public RuntimeAnimationScript() {
        super(ScriptType.RuntimeAnimation);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.name = name;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("xxx".equals(k)) {
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("CopyFrame".equals(child.type)) {
                CopyFrame cmd = new CopyFrame();
                cmd.parse(child);
                this.commands.add(cmd);
            } else if ("CopyFrames".equals(child.type)) {
                CopyFrames cmd = new CopyFrames();
                cmd.parse(child);
                this.commands.add(cmd);
            }
        }
    }

    public void exec() {
        List<Keyframe> keyframes = new ArrayList<>();

        for (IRuntimeAnimationCommand cmd : this.commands) {
            cmd.exec(keyframes);
        }

        float duration = 0.0F;

        for (int i = 0; i < keyframes.size(); i++) {
            duration = Math.max(duration, keyframes.get(i).time);
        }

        AnimationClip clip = new AnimationClip(duration, keyframes, this.name, true);
        keyframes.clear();
        ModelManager.instance.addAnimationClip(clip.name, clip);
        keyframes.clear();
    }

    @Override
    public void reset() {
        this.name = this.toString();
        this.commands.clear();
    }
}
