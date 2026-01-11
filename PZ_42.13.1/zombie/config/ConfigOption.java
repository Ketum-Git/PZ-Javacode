// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.config;

import zombie.UsedFromLua;

@UsedFromLua
public abstract class ConfigOption {
    protected final String name;

    public ConfigOption(String name) {
        if (name != null && !name.isEmpty() && !name.contains("=")) {
            this.name = name;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public String getName() {
        return this.name;
    }

    public abstract String getType();

    public abstract void resetToDefault();

    public abstract void setDefaultToCurrentValue();

    public abstract void parse(String s);

    public abstract String getValueAsString();

    public String getValueAsLuaString() {
        return this.getValueAsString();
    }

    public abstract void setValueFromObject(Object o);

    public abstract Object getValueAsObject();

    public abstract boolean isValidString(String s);

    public abstract String getTooltip();

    public abstract ConfigOption makeCopy();
}
