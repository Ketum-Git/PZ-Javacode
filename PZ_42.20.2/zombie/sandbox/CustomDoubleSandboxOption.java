// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.sandbox;

import zombie.scripting.ScriptParser;

public final class CustomDoubleSandboxOption extends CustomSandboxOption {
    public final double min;
    public final double max;
    public final double defaultValue;

    CustomDoubleSandboxOption(String id, double min, double max, double defaultValue) {
        super(id);
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    static CustomDoubleSandboxOption parse(ScriptParser.Block block) {
        double min = getValueDouble(block, "min", Double.NaN);
        double max = getValueDouble(block, "max", Double.NaN);
        double defaultValue = getValueDouble(block, "default", Double.NaN);
        if (!Double.isNaN(min) && !Double.isNaN(max) && !Double.isNaN(defaultValue)) {
            CustomDoubleSandboxOption option = new CustomDoubleSandboxOption(block.id, min, max, defaultValue);
            return !option.parseCommon(block) ? null : option;
        } else {
            return null;
        }
    }
}
