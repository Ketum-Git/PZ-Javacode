// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action.conditions;

import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.IActionCondition;

public final class EventNotOccurred implements IActionCondition {
    public String eventName;

    @Override
    public String getDescription() {
        return "EventNotOccurred(" + this.eventName + ")";
    }

    private boolean load(Element node) {
        this.eventName = node.getTextContent().toLowerCase();
        return true;
    }

    @Override
    public boolean passes(ActionContext context, ActionState state) {
        return !context.hasEventOccurred(this.eventName, state.getName());
    }

    @Override
    public IActionCondition clone() {
        return null;
    }

    @Override
    public String toString() {
        return this.toString("");
    }

    @Override
    public String toString(String in_indent) {
        return in_indent + this.getClass().getName() + "{ " + this.eventName + " }";
    }

    public static class Factory implements IActionCondition.IFactory {
        @Override
        public IActionCondition create(Element conditionNode) {
            EventNotOccurred cond = new EventNotOccurred();
            return cond.load(conditionNode) ? cond : null;
        }
    }
}
