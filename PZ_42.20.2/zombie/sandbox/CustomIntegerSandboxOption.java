// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.sandbox;

import zombie.scripting.ScriptParser;

public final class CustomIntegerSandboxOption extends CustomSandboxOption {
    public final int min;
    public final int max;
    public final int defaultValue;

    CustomIntegerSandboxOption(String id, int min, int max, int defaultValue) {
        super(id);
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    static CustomIntegerSandboxOption parse(ScriptParser.Block block) {
        int min = getValueInt(block, "min", Integer.MIN_VALUE);
        int max = getValueInt(block, "max", Integer.MIN_VALUE);
        int defaultValue = getValueInt(block, "default", Integer.MIN_VALUE);
        if (min != Integer.MIN_VALUE && max != Integer.MIN_VALUE && defaultValue != Integer.MIN_VALUE) {
            CustomIntegerSandboxOption option = new CustomIntegerSandboxOption(block.id, min, max, defaultValue);
            return !option.parseCommon(block) ? null : option;
        } else {
            return null;
        }
    }
}
