// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import zombie.UsedFromLua;
import zombie.config.BooleanConfigOption;
import zombie.core.Core;
import zombie.debug.options.IDebugOption;
import zombie.debug.options.IDebugOptionGroup;
import zombie.debug.options.OptionGroup;

@UsedFromLua
public class BooleanDebugOption extends BooleanConfigOption implements IDebugOption {
    private IDebugOptionGroup parent;
    private final boolean debugOnly;
    private String fullPath;

    public BooleanDebugOption(String name, boolean debugOnly, boolean defaultValue) {
        super(name, defaultValue);
        this.fullPath = name;
        this.debugOnly = debugOnly;
    }

    @Override
    public String getName() {
        return this.fullPath;
    }

    @Override
    public boolean getValue() {
        return !Core.debug && this.isDebugOnly() ? this.getDefaultValue() : super.getValue();
    }

    public boolean isDebugOnly() {
        return this.debugOnly;
    }

    @Override
    public IDebugOptionGroup getParent() {
        return this.parent;
    }

    @Override
    public void setParent(IDebugOptionGroup parent) {
        this.parent = parent;
        this.fullPath = OptionGroup.getCombinedName(this.parent, this.name);
    }

    @Override
    public void onFullPathChanged() {
        this.fullPath = OptionGroup.getCombinedName(this.parent, this.name);
    }

    public static BooleanDebugOption newOption(IDebugOptionGroup parentGroup, String name, boolean defaultValue) {
        return newOptionInternal(parentGroup, name, false, defaultValue);
    }

    public static BooleanDebugOption newDebugOnlyOption(IDebugOptionGroup parentGroup, String name, boolean defaultValue) {
        return newOptionInternal(parentGroup, name, true, defaultValue);
    }

    private static BooleanDebugOption newOptionInternal(IDebugOptionGroup parentGroup, String name, boolean debugOnly, boolean defaultValue) {
        BooleanDebugOption newOption = new BooleanDebugOption(name, debugOnly, defaultValue);
        if (parentGroup != null) {
            parentGroup.addChild(newOption);
        }

        return newOption;
    }
}
