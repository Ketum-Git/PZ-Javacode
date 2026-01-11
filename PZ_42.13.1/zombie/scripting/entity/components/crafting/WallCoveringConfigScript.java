// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.crafting;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.objects.SignType;
import zombie.scripting.objects.WallCoveringType;

@UsedFromLua
public class WallCoveringConfigScript extends ComponentScript {
    private WallCoveringType type;
    private String name;
    private SignType sign;

    private WallCoveringConfigScript() {
        super(ComponentType.WallCoveringConfig);
    }

    public WallCoveringType getType() {
        return this.type;
    }

    public String getTypeString() {
        return this.type.toString();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected void copyFrom(ComponentScript other) {
    }

    public Integer getSignIndex() {
        return this.sign.getSpriteIndex();
    }

    public String getSignSpriteName() {
        return this.sign.getSpriteName();
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("type")) {
                    this.type = WallCoveringType.typeOf(val);
                } else if (key.equalsIgnoreCase("name")) {
                    this.name = val;
                } else if (key.equalsIgnoreCase("sign")) {
                    this.sign = SignType.typeOf(Integer.parseInt(val));
                }
            }
        }
    }
}
