// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

@UsedFromLua
public final class MannequinScript extends BaseScriptObject {
    private String name;
    private boolean female = true;
    private String modelScriptName;
    private String texture;
    private String animSet;
    private String animState;
    private String pose;
    private String outfit;

    public MannequinScript() {
        super(ScriptType.Mannequin);
    }

    public String getName() {
        return this.name;
    }

    public boolean isFemale() {
        return this.female;
    }

    public void setFemale(boolean b) {
        this.female = b;
    }

    public String getModelScriptName() {
        return this.modelScriptName;
    }

    public void setModelScriptName(String str) {
        this.modelScriptName = StringUtils.discardNullOrWhitespace(str);
    }

    public String getTexture() {
        return this.texture;
    }

    public void setTexture(String str) {
        this.texture = StringUtils.discardNullOrWhitespace(str);
    }

    public String getAnimSet() {
        return this.animSet;
    }

    public void setAnimSet(String str) {
        this.animSet = StringUtils.discardNullOrWhitespace(str);
    }

    public String getAnimState() {
        return this.animState;
    }

    public void setAnimState(String str) {
        this.animState = StringUtils.discardNullOrWhitespace(str);
    }

    public String getPose() {
        return this.pose;
    }

    public void setPose(String str) {
        this.pose = StringUtils.discardNullOrWhitespace(str);
    }

    public String getOutfit() {
        return this.outfit;
    }

    public void setOutfit(String str) {
        this.outfit = StringUtils.discardNullOrWhitespace(str);
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
            if ("female".equalsIgnoreCase(k)) {
                this.female = StringUtils.tryParseBoolean(v);
            } else if ("model".equalsIgnoreCase(k)) {
                this.modelScriptName = StringUtils.discardNullOrWhitespace(v);
            } else if ("texture".equalsIgnoreCase(k)) {
                this.texture = StringUtils.discardNullOrWhitespace(v);
            } else if ("animSet".equalsIgnoreCase(k)) {
                this.animSet = StringUtils.discardNullOrWhitespace(v);
            } else if ("animState".equalsIgnoreCase(k)) {
                this.animState = StringUtils.discardNullOrWhitespace(v);
            } else if ("pose".equalsIgnoreCase(k)) {
                this.pose = StringUtils.discardNullOrWhitespace(v);
            } else if ("outfit".equalsIgnoreCase(k)) {
                this.outfit = StringUtils.discardNullOrWhitespace(v);
            }
        }
    }

    @Override
    public void reset() {
        this.modelScriptName = null;
        this.texture = null;
        this.animSet = null;
        this.animState = null;
        this.pose = null;
        this.outfit = null;
    }
}
