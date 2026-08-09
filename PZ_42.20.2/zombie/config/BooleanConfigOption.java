// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.config;

import zombie.UsedFromLua;
import zombie.debug.DebugLog;

@UsedFromLua
public class BooleanConfigOption extends ConfigOption {
    protected boolean value;
    protected boolean defaultValue;

    public BooleanConfigOption(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getType() {
        return "boolean";
    }

    @Override
    public void resetToDefault() {
        this.setValue(this.defaultValue);
    }

    @Override
    public void setDefaultToCurrentValue() {
        this.defaultValue = this.value;
    }

    @Override
    public void parse(String s) {
        if (this.isValidString(s)) {
            this.setValue(s.equalsIgnoreCase("true") || s.equalsIgnoreCase("1"));
        } else {
            DebugLog.log("ERROR BooleanConfigOption.parse() \"" + this.getName() + "\" string=" + s + "\"");
        }
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(this.value);
    }

    @Override
    public void setValueFromObject(Object o) {
        if (o instanceof Boolean b) {
            this.setValue(b);
        } else if (o instanceof Double d) {
            this.setValue(d != 0.0);
        } else if (o instanceof String s) {
            this.parse(s);
        }
    }

    @Override
    public Object getValueAsObject() {
        return this.value;
    }

    @Override
    public boolean isValidString(String s) {
        return s != null && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false") || s.equalsIgnoreCase("1") || s.equalsIgnoreCase("0"));
    }

    public boolean getValue() {
        return this.value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getTooltip() {
        return String.valueOf(this.value);
    }

    @Override
    public ConfigOption makeCopy() {
        BooleanConfigOption copy = new BooleanConfigOption(this.name, this.defaultValue);
        copy.value = this.value;
        return copy;
    }
}
