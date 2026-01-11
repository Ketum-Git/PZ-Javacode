// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.util.Lambda;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;

public final class ActionTransition implements Cloneable {
    String transitionTo;
    boolean asSubstate;
    boolean transitionOut;
    boolean forceParent;
    int conditionPriority;
    final List<IActionCondition> conditions = new ArrayList<>();

    public static boolean parse(Element root, String srcInfo, List<ActionTransition> out_transitions) {
        if (root.getNodeName().equals("transitions")) {
            parseTransitions(root, srcInfo, out_transitions);
            return true;
        } else if (root.getNodeName().equals("transition")) {
            parseTransition(root, out_transitions);
            return true;
        } else {
            return false;
        }
    }

    public static void parseTransition(Element root, List<ActionTransition> out_transitions) {
        out_transitions.clear();
        ActionTransition trans = new ActionTransition();
        if (trans.load(root)) {
            out_transitions.add(trans);
        }
    }

    public static void parseTransitions(Element root, String srcInfo, List<ActionTransition> out_transitions) {
        out_transitions.clear();
        Lambda.forEachFrom(PZXmlUtil::forEachElement, root, srcInfo, out_transitions, (child, l_srcInfo, l_out_transitions) -> {
            if (!child.getNodeName().equals("transition")) {
                DebugLog.ActionSystem.warn("Warning: Unrecognised element '" + child.getNodeName() + "' in " + l_srcInfo);
            } else {
                ActionTransition trans = new ActionTransition();
                if (trans.load(child)) {
                    l_out_transitions.add(trans);
                }
            }
        });
    }

    private boolean load(Element transitionElement) {
        try {
            PZXmlUtil.forEachElement(transitionElement, child -> {
                try {
                    String s = child.getNodeName();
                    if ("transitionTo".equalsIgnoreCase(s)) {
                        this.transitionTo = child.getTextContent();
                    } else if ("transitionOut".equalsIgnoreCase(s)) {
                        this.transitionOut = StringUtils.tryParseBoolean(child.getTextContent());
                    } else if ("forceParent".equalsIgnoreCase(s)) {
                        this.forceParent = StringUtils.tryParseBoolean(child.getTextContent());
                    } else if ("asSubstate".equalsIgnoreCase(s)) {
                        this.asSubstate = StringUtils.tryParseBoolean(child.getTextContent());
                    } else if ("conditionPriority".equalsIgnoreCase(s)) {
                        this.conditionPriority = PZMath.tryParseInt(child.getTextContent(), 0);
                    } else if ("conditions".equalsIgnoreCase(s)) {
                        PZXmlUtil.forEachElement(child, conditionNode -> {
                            IActionCondition condition = IActionCondition.createInstance(conditionNode);
                            if (condition != null) {
                                this.conditions.add(condition);
                            }
                        });
                    }
                } catch (Exception var3x) {
                    DebugLog.ActionSystem.error("Error while parsing xml element: " + child.getNodeName());
                    DebugLog.ActionSystem.error(var3x);
                }
            });
            return true;
        } catch (Exception var3) {
            DebugLog.ActionSystem.error("Error while loading an ActionTransition element");
            DebugLog.ActionSystem.error(var3);
            return false;
        }
    }

    public String getTransitionTo() {
        return this.transitionTo;
    }

    public boolean passes(ActionContext context, ActionState state) {
        for (int i = 0; i < this.conditions.size(); i++) {
            IActionCondition cond = this.conditions.get(i);
            if (!cond.passes(context, state)) {
                return false;
            }
        }

        return true;
    }

    public ActionTransition clone() {
        ActionTransition cloned = new ActionTransition();
        cloned.transitionTo = this.transitionTo;
        cloned.asSubstate = this.asSubstate;
        cloned.transitionOut = this.transitionOut;
        cloned.forceParent = this.forceParent;
        cloned.conditionPriority = this.conditionPriority;

        for (IActionCondition cond : this.conditions) {
            cloned.conditions.add(cond.clone());
        }

        return cloned;
    }

    @Override
    public String toString() {
        return this.toString("");
    }

    public String toString(String in_indent) {
        StringBuilder result = new StringBuilder();
        result.append(in_indent).append(this.getClass().getName()).append("\r\n");
        result.append(in_indent).append("{").append("\r\n");
        result.append(in_indent).append("\t").append("transitionTo:").append(this.transitionTo).append("\r\n");
        result.append(in_indent).append("\t").append("asSubstate:").append(this.asSubstate).append("\r\n");
        result.append(in_indent).append("\t").append("transitionOut:").append(this.transitionOut).append("\r\n");
        result.append(in_indent).append("\t").append("forceParent:").append(this.forceParent).append("\r\n");
        result.append(in_indent).append("\t").append("conditionPriority:").append(this.forceParent).append("\r\n");
        result.append(in_indent).append("\t").append("transitions:").append("\r\n");
        result.append(in_indent).append("\t{").append("\r\n");

        for (int icondition = 0; icondition < this.conditions.size(); icondition++) {
            result.append(in_indent).append(this.conditions.get(icondition).toString(in_indent + "\t")).append(",").append("\r\n");
        }

        result.append(in_indent).append("\t}").append("\r\n");
        result.append(in_indent).append("}");
        return result.toString();
    }
}
