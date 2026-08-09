// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.debug.DebugType;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

@DebugClassFields
@UsedFromLua
public class StringListScript extends BaseScriptObject {
    private final ArrayList<String> values = new ArrayList<>();

    protected StringListScript() {
        super(ScriptType.StringList);
    }

    public ArrayList<String> getValues() {
        return this.values;
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Value value : block.values) {
            String s = value.string;
            if (!StringUtils.isNullOrWhitespace(s)) {
                s = s.trim();
                if (this.values.contains(s)) {
                    DebugType.General.warn("Stringlist <" + name + "> double string entry: " + s);
                } else {
                    this.values.add(s);
                }
            }
        }
    }

    @Override
    public void reset() {
        this.values.clear();
    }

    @Override
    public void PreReload() {
        this.values.clear();
    }
}
