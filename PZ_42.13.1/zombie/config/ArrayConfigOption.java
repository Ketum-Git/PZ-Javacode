// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.config;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;

public class ArrayConfigOption extends ConfigOption {
    protected final ArrayList<ConfigOption> value = new ArrayList<>();
    protected final ArrayList<ConfigOption> defaultValue = new ArrayList<>();
    protected final String separator;
    protected final ConfigOption elementHandler;
    protected int fixedSize = -1;
    protected boolean multiLine;

    public ArrayConfigOption(String name, ConfigOption elementHandler, String separator, String defaultValue) {
        super(name);
        this.elementHandler = elementHandler;
        this.separator = separator == null ? "," : separator;
        this.parse(defaultValue);
        this.setDefaultToCurrentValue();
    }

    public ArrayConfigOption setFixedSize(int size) {
        if (size > 0) {
            this.resize(this.value, size);
            this.resize(this.defaultValue, size);
        } else {
            this.fixedSize = -1;
        }

        return this;
    }

    public ArrayConfigOption setMultiLine(boolean bMultiLine) {
        this.multiLine = bMultiLine;
        return this;
    }

    public boolean isMultiLine() {
        return this.multiLine;
    }

    public void clear() {
        this.value.clear();
    }

    @Override
    public String getType() {
        return "array";
    }

    @Override
    public void resetToDefault() {
        this.copyValue(this.defaultValue, this.value);
    }

    @Override
    public void setDefaultToCurrentValue() {
        this.copyValue(this.value, this.defaultValue);
    }

    @Override
    public void parse(String s) {
        this.setValueFromObject(s);
    }

    @Override
    public String getValueAsString() {
        return this.getValueAsString(this.value);
    }

    @Override
    public void setValueFromObject(Object o) {
        if (!(o instanceof String str)) {
            DebugLog.log("ERROR ArrayConfigOption.setValueFromObject() \"" + this.getName() + "\" string=" + o + "\"");
        } else if (!this.isValidString(str)) {
            DebugLog.log("ERROR ArrayConfigOption.setValueFromObject() \"" + this.getName() + "\" string=" + str + "\"");
        } else {
            if (!this.isMultiLine()) {
                this.value.clear();
            }

            if (!str.trim().isEmpty()) {
                String[] ss = str.split(this.separator);

                for (int i = 0; i < ss.length; i++) {
                    this.elementHandler.resetToDefault();
                    this.elementHandler.parse(ss[i]);
                    this.value.add(this.elementHandler.makeCopy());
                }
            }

            if (this.fixedSize > 0) {
                this.resize(this.value, this.fixedSize);
            }
        }
    }

    @Override
    public Object getValueAsObject() {
        return this.getValueAsString();
    }

    @Override
    public boolean isValidString(String s) {
        if (s.trim().isEmpty()) {
            return true;
        } else {
            String[] ss = s.split(this.separator);

            for (int i = 0; i < ss.length; i++) {
                if (!this.elementHandler.isValidString(ss[i])) {
                    return false;
                }
            }

            return true;
        }
    }

    @Override
    public String getTooltip() {
        return this.getValueAsString();
    }

    @Override
    public ConfigOption makeCopy() {
        String defaultValueStr = this.getValueAsString(this.defaultValue);
        ArrayConfigOption copy = new ArrayConfigOption(this.name, this.elementHandler, this.separator, defaultValueStr);
        this.copyValue(this.value, copy.value);
        return copy;
    }

    public int size() {
        return this.value.size();
    }

    public ConfigOption getElement(int index) {
        return this.value.get(index);
    }

    public ArrayConfigOption setValueVarArgs(Object... args) {
        int size = args.length;
        if (this.fixedSize > 0) {
            size = PZMath.min(size, this.fixedSize);
        } else {
            this.resize(this.value, args.length);
        }

        for (int i = 0; i < size; i++) {
            this.value.get(i).setValueFromObject(args[i]);
        }

        return this;
    }

    private void copyValue(ArrayList<ConfigOption> from, ArrayList<ConfigOption> to) {
        to.clear();

        for (int i = 0; i < from.size(); i++) {
            ConfigOption element = from.get(i).makeCopy();
            to.add(element);
        }
    }

    private String getValueAsString(ArrayList<ConfigOption> value) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < value.size(); i++) {
            ConfigOption element = value.get(i);
            stringBuilder.append(element.getValueAsString());
            if (i < value.size() - 1) {
                stringBuilder.append(this.separator);
            }
        }

        return stringBuilder.toString();
    }

    private void resize(ArrayList<ConfigOption> value, int newSize) {
        this.fixedSize = newSize;
        this.elementHandler.resetToDefault();

        for (int i = value.size(); i < this.fixedSize; i++) {
            ConfigOption element = this.elementHandler.makeCopy();
            value.add(element);
        }

        while (value.size() > this.fixedSize) {
            value.remove(value.size() - 1);
        }
    }

    public ColorInfo getValueAsColorInfo(ColorInfo colorInfo) {
        return colorInfo.set(
            (float)this.getElementAsDouble(0, 1.0F),
            (float)this.getElementAsDouble(1, 1.0F),
            (float)this.getElementAsDouble(2, 1.0F),
            (float)this.getElementAsDouble(3, 1.0F)
        );
    }

    public double getElementAsDouble(int index, float defaultValue) {
        return index >= 0 && index < this.value.size() && this.elementHandler instanceof DoubleConfigOption
            ? ((DoubleConfigOption)this.getElement(index)).getValue()
            : defaultValue;
    }
}
