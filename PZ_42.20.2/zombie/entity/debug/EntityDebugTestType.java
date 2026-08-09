// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.debug;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.UsedFromLua;

@UsedFromLua
public enum EntityDebugTestType {
    BaseTest;

    private static final ArrayList<EntityDebugTestType> typeList = new ArrayList<>();

    public static ArrayList<EntityDebugTestType> getValueList() {
        return typeList;
    }

    static {
        typeList.addAll(Arrays.asList(values()));
    }
}
