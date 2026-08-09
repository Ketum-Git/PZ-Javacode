// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.sandbox;

import zombie.scripting.ScriptParser;

public final class CustomBooleanSandboxOption extends CustomSandboxOption {
    public final boolean defaultValue;

    CustomBooleanSandboxOption(String id, boolean defaultValue) {
        super(id);
        this.defaultValue = defaultValue;
    }

    static CustomBooleanSandboxOption parse(ScriptParser.Block block) {
        ScriptParser.Value vDefaultValue = block.getValue("default");
        if (vDefaultValue == null) {
            return null;
        } else {
            boolean defaultValue = Boolean.parseBoolean(vDefaultValue.getValue().trim());
            CustomBooleanSandboxOption option = new CustomBooleanSandboxOption(block.id, defaultValue);
            return !option.parseCommon(block) ? null : option;
        }
    }
}
