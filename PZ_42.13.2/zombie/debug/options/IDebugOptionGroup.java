// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.util.StringUtils;

public interface IDebugOptionGroup extends IDebugOption {
    Iterable<IDebugOption> getChildren();

    void addChild(IDebugOption childOption);

    void removeChild(IDebugOption arg0);

    void onChildAdded(IDebugOption newChild);

    void onDescendantAdded(IDebugOption newDescendant);

    default <E extends IDebugOptionGroup> E newOptionGroup(E newGroup) {
        this.addChild(newGroup);
        return newGroup;
    }

    default BooleanDebugOption newOption(String name, boolean defaultValue) {
        return BooleanDebugOption.newOption(this, name, defaultValue);
    }

    default BooleanDebugOption newDebugOnlyOption(String name, boolean defaultValue) {
        return BooleanDebugOption.newDebugOnlyOption(this, name, defaultValue);
    }

    default String getCombinedName(String childName) {
        String parentGroupName = this.getName();
        return StringUtils.isNullOrWhitespace(parentGroupName) ? childName : String.format("%s.%s", parentGroupName, childName);
    }
}
