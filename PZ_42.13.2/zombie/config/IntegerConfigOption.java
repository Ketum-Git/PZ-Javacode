// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.config;

import zombie.UsedFromLua;
import zombie.debug.DebugLog;

@UsedFromLua
public class IntegerConfigOption extends ConfigOption {
    protected int value;
    protected int defaultValue;
    protected int min;
    protected int max;

    public IntegerConfigOption(String name, int min, int max, int defaultValue) {
        super(name);
        if (defaultValue >= min && defaultValue <= max) {
            this.value = defaultValue;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String getType() {
        return "integer";
    }

    @Override
    public void resetToDefault() {
        this.setValue(this.defaultValue);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    @Override
    public void setDefaultToCurrentValue() {
        this.defaultValue = this.value;
    }

    @Override
    public void parse(String s) {
        try {
            double v = Double.parseDouble(s);
            this.setValue((int)v);
        } catch (NumberFormatException var4) {
            DebugLog.log("ERROR IntegerConfigOption.parse() \"" + this.name + "\" string=\"" + s + "\"");
        }
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(this.value);
    }

    @Override
    public void setValueFromObject(Object o) {
        if (o instanceof Double d) {
            this.setValue(d.intValue());
        } else if (o instanceof String s) {
            this.parse(s);
        }
    }

    @Override
    public Object getValueAsObject() {
        return (double)this.value;
    }

    @Override
    public boolean isValidString(String s) {
        try {
            int v = Integer.parseInt(s);
            return v >= this.min && v <= this.max;
        } catch (NumberFormatException var3) {
            return false;
        }
    }

    public void setValue(int value) {
        if (value < this.min) {
            DebugLog.log("ERROR: IntegerConfigOption.setValue() \"" + this.name + "\" " + value + " is less than min=" + this.min);
        } else if (value > this.max) {
            DebugLog.log("ERROR: IntegerConfigOption.setValue() \"" + this.name + "\" " + value + " is greater than max=" + this.max);
        } else {
            this.value = value;
        }
    }

    public int getValue() {
        return this.value;
    }

    public int getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getTooltip() {
        return String.valueOf(this.value);
    }

    @Override
    public ConfigOption makeCopy() {
        IntegerConfigOption copy = new IntegerConfigOption(this.name, this.min, this.max, this.defaultValue);
        copy.value = this.value;
        return copy;
    }
}
