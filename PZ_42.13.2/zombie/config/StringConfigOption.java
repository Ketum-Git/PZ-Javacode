// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;

@UsedFromLua
public class StringConfigOption extends ConfigOption {
    protected String value;
    protected String defaultValue;
    protected int maxLength;
    protected String[] values;
    private final Set<String> valueCacheSet = new HashSet<>();

    public StringConfigOption(String name, String defaultValue, int maxLength) {
        super(name);
        if (defaultValue == null) {
            defaultValue = "";
        }

        this.setValueInternal(defaultValue);
        this.defaultValue = defaultValue;
        this.maxLength = maxLength;
    }

    public StringConfigOption(String name, String defaultValue, String[] values) {
        super(name);

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(defaultValue)) {
                this.setValueInternal(defaultValue);
                break;
            }
        }

        if (this.value == null) {
            defaultValue = values[0];
            this.setValueInternal(defaultValue);
        }

        this.defaultValue = defaultValue;
        this.values = values;
    }

    @Override
    public String getType() {
        return "string";
    }

    @Override
    public void resetToDefault() {
        this.setValueInternal(this.defaultValue);
    }

    @Override
    public void setDefaultToCurrentValue() {
        this.defaultValue = this.value;
    }

    @Override
    public void parse(String s) {
        this.setValueFromObject(s);
    }

    @Override
    public String getValueAsString() {
        return this.value;
    }

    @Override
    public String getValueAsLuaString() {
        return String.format("\"%s\"", this.value.replace("\\", "\\\\").replace("\"", "\\\""));
    }

    @Override
    public void setValueFromObject(Object o) {
        if (this.values != null) {
            for (int i = 0; i < this.values.length; i++) {
                if (this.values[i].equals(o)) {
                    this.setValueInternal(this.values[i]);
                    return;
                }
            }

            DebugLog.General.println("ERROR: StringConfigOption.setValueFromObject() \"%s\" value \"%s\" is unknown", this.getName(), o);
        } else {
            if (o == null) {
                this.setValueInternal("");
            } else if (o instanceof String s) {
                this.setValueInternal(s);
            } else {
                this.setValueInternal(o.toString());
            }
        }
    }

    @Override
    public Object getValueAsObject() {
        return this.value;
    }

    @Override
    public boolean isValidString(String s) {
        if (this.values != null) {
            for (int i = 0; i < this.values.length; i++) {
                if (this.values[i].equals(s)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public void setValue(String value) {
        if (this.values != null) {
            if (this.isValidString(value)) {
                this.setValueInternal(value);
            } else {
                DebugLog.General.println("ERROR StringConfigOption.setValue() \"%s\" string=\"%s\"", this.getName(), value);
            }
        } else {
            if (value == null) {
                value = "";
            }

            if (this.maxLength > 0 && value.length() > this.maxLength) {
                value = value.substring(0, this.maxLength);
            }

            this.setValueInternal(value);
        }
    }

    public String getValue() {
        return this.value;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getTooltip() {
        return this.value;
    }

    @Override
    public ConfigOption makeCopy() {
        if (this.values == null) {
            StringConfigOption copy = new StringConfigOption(this.name, this.defaultValue, this.maxLength);
            copy.setValueInternal(this.value);
            return copy;
        } else {
            StringConfigOption copy = new StringConfigOption(this.name, this.defaultValue, this.values);
            copy.setValueInternal(this.value);
            return copy;
        }
    }

    public Set<String> getSplitCSVList() {
        return this.valueCacheSet;
    }

    private void setValueInternal(String value) {
        this.value = value;
        this.valueCacheSet.clear();
        Collections.addAll(this.valueCacheSet, this.value.replaceAll("\\s+", "").split(","));
    }
}
