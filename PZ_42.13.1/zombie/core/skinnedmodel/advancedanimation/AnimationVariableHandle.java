// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.util.StringUtils;

public class AnimationVariableHandle {
    private String name;
    private int varIndex = -1;

    AnimationVariableHandle() {
    }

    public static boolean equals(AnimationVariableHandle lhs, AnimationVariableHandle rhs) {
        return lhs == rhs ? true : lhs != null && lhs.equals(rhs);
    }

    public boolean equals(AnimationVariableHandle rhs) {
        return rhs != null && StringUtils.equalsIgnoreCase(this.name, rhs.name) && this.varIndex == rhs.varIndex;
    }

    public static AnimationVariableHandle alloc(String name) {
        return AnimationVariableHandlePool.getOrCreate(name);
    }

    public String getVariableName() {
        return this.name;
    }

    public int getVariableIndex() {
        return this.varIndex;
    }

    void setVariableName(String name) {
        this.name = name;
    }

    void setVariableIndex(int idx) {
        this.varIndex = idx;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{  variableName:" + this.name + ",  variableIndex:" + this.varIndex + " }";
    }
}
