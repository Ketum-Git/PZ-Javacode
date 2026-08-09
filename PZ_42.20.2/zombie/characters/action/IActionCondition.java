// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action;

import java.util.HashMap;
import org.w3c.dom.Element;

public interface IActionCondition {
    HashMap<String, IActionCondition.IFactory> s_factoryMap = new HashMap<>();

    String getDescription();

    boolean passes(ActionContext arg0, ActionState arg1);

    IActionCondition clone();

    String toString(String arg0);

    static IActionCondition createInstance(Element conditionNode) {
        IActionCondition.IFactory fact = s_factoryMap.get(conditionNode.getNodeName());
        return fact != null ? fact.create(conditionNode) : null;
    }

    static void registerFactory(String elementName, IActionCondition.IFactory factory) {
        s_factoryMap.put(elementName, factory);
    }

    public interface IFactory {
        IActionCondition create(Element conditionNode);
    }
}
