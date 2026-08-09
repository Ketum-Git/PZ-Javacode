// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.sandbox;

import zombie.scripting.ScriptParser;

public final class CustomStringSandboxOption extends CustomSandboxOption {
    public final String defaultValue;

    CustomStringSandboxOption(String id, String defaultValue) {
        super(id);
        this.defaultValue = defaultValue;
    }

    static CustomStringSandboxOption parse(ScriptParser.Block block) {
        ScriptParser.Value vDefaultValue = block.getValue("default");
        if (vDefaultValue == null) {
            return null;
        } else {
            CustomStringSandboxOption option = new CustomStringSandboxOption(block.id, vDefaultValue.getValue().trim());
            return !option.parseCommon(block) ? null : option;
        }
    }
}
