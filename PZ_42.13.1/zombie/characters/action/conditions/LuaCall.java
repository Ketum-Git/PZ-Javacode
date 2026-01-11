// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action.conditions;

import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.IActionCondition;

public final class LuaCall implements IActionCondition {
    @Override
    public String getDescription() {
        return "<luaCheck>";
    }

    @Override
    public boolean passes(ActionContext context, ActionState state) {
        return false;
    }

    @Override
    public IActionCondition clone() {
        return new LuaCall();
    }

    @Override
    public String toString() {
        return this.toString("");
    }

    @Override
    public String toString(String in_indent) {
        return in_indent + this.getClass().getName();
    }

    public static class Factory implements IActionCondition.IFactory {
        @Override
        public IActionCondition create(Element conditionNode) {
            return new LuaCall();
        }
    }
}
