// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

@UsedFromLua
public final class SoundTimelineScript extends BaseScriptObject {
    private String eventName;
    private final HashMap<String, Integer> positionByName = new HashMap<>();

    public SoundTimelineScript() {
        super(ScriptType.SoundTimeline);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.eventName = name;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            this.positionByName.put(k, PZMath.tryParseInt(v, 0));
        }
    }

    public String getEventName() {
        return this.eventName;
    }

    public int getPosition(String id) {
        return this.positionByName.containsKey(id) ? this.positionByName.get(id) : -1;
    }

    @Override
    public void reset() {
        this.positionByName.clear();
    }
}
