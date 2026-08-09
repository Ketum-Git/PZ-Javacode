// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import zombie.util.list.PZArrayUtil;

public enum ControllerButtonStyle {
    XBOX(1, "XBOX"),
    PS4(2, "PS4"),
    STEAMDECK(3, "STEAMDECK");

    private final int optionCode;
    private final String label;
    private static final ControllerButtonStyle[] values = values();

    private ControllerButtonStyle(final int optionCode, final String label) {
        this.optionCode = optionCode;
        this.label = label;
    }

    public int getOptionCode() {
        return this.optionCode;
    }

    public String getLabel() {
        return this.label;
    }

    public boolean isOptionCode(int optionValue) {
        return this.optionCode == optionValue;
    }

    public static ControllerButtonStyle[] allValues() {
        return values;
    }

    public static ControllerButtonStyle fromOptionCode(int value) {
        return PZArrayUtil.findOrDefault(allValues(), value, ControllerButtonStyle::isOptionCode, PS4);
    }
}
