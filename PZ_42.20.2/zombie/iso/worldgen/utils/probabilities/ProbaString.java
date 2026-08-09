// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.utils.probabilities;

import zombie.SandboxOptions;

public class ProbaString implements Probability {
    private final Double value;
    private final String clazz;
    private final String field;

    public ProbaString(String value) {
        this.clazz = value.split("\\.")[0];
        this.field = value.split("\\.")[1];
        if ("Sandbox".equals(this.clazz)) {
            SandboxOptions.SandboxOption option = SandboxOptions.instance.getOptionByName(this.field);
            if (option == null) {
                throw new IllegalArgumentException("Invalid sandbox option: " + this.field);
            } else {
                this.value = ((SandboxOptions.DoubleSandboxOption)option).getValue();
            }
        } else {
            throw new IllegalArgumentException("Unknown class type: " + this.clazz);
        }
    }

    @Override
    public float getValue() {
        return this.value.floatValue();
    }
}
