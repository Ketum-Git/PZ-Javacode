// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.sandbox;

import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class CustomEnumSandboxOption extends CustomSandboxOption {
    public final int numValues;
    public final int defaultValue;
    public String valueTranslation;

    CustomEnumSandboxOption(String id, int numValues, int defaultValue) {
        super(id);
        this.numValues = numValues;
        this.defaultValue = defaultValue;
    }

    static CustomEnumSandboxOption parse(ScriptParser.Block block) {
        int numValues = getValueInt(block, "numValues", -1);
        int defaultValue = getValueInt(block, "default", -1);
        if (numValues > 0 && defaultValue > 0) {
            CustomEnumSandboxOption option = new CustomEnumSandboxOption(block.id, numValues, defaultValue);
            if (!option.parseCommon(block)) {
                return null;
            } else {
                ScriptParser.Value vValueTranslation = block.getValue("valueTranslation");
                if (vValueTranslation != null) {
                    option.valueTranslation = StringUtils.discardNullOrWhitespace(vValueTranslation.getValue().trim());
                }

                return option;
            }
        } else {
            return null;
        }
    }
}
