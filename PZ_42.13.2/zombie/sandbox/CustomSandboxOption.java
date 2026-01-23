// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.sandbox;

import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public class CustomSandboxOption {
    public final String id;
    public String page;
    public String translation;

    CustomSandboxOption(String id) {
        this.id = id;
    }

    static double getValueDouble(ScriptParser.Block block, String key, double defaultValue) {
        ScriptParser.Value value = block.getValue(key);
        return value == null ? defaultValue : PZMath.tryParseDouble(value.getValue().trim(), defaultValue);
    }

    static float getValueFloat(ScriptParser.Block block, String key, float defaultValue) {
        ScriptParser.Value value = block.getValue(key);
        return value == null ? defaultValue : PZMath.tryParseFloat(value.getValue().trim(), defaultValue);
    }

    static int getValueInt(ScriptParser.Block block, String key, int defaultValue) {
        ScriptParser.Value value = block.getValue(key);
        return value == null ? defaultValue : PZMath.tryParseInt(value.getValue().trim(), defaultValue);
    }

    boolean parseCommon(ScriptParser.Block block) {
        ScriptParser.Value vPage = block.getValue("page");
        if (vPage != null) {
            this.page = StringUtils.discardNullOrWhitespace(vPage.getValue().trim());
        }

        ScriptParser.Value vTranslation = block.getValue("translation");
        if (vTranslation != null) {
            this.translation = StringUtils.discardNullOrWhitespace(vTranslation.getValue().trim());
        }

        return true;
    }
}
