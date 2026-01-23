// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action.conditions;

import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.IActionCondition;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.util.StringUtils;

public final class CharacterVariableCondition implements IActionCondition {
    private CharacterVariableCondition.Operator op;
    private Object lhsValue;
    private Object rhsValue;

    private static Object parseValue(String value, boolean parseForCharacterVariableLookup) {
        if (value.length() <= 0) {
            return value;
        } else {
            char first = value.charAt(0);
            if (first == '-' || first == '+' || first >= '0' && first <= '9') {
                int intVal = 0;
                if (first >= '0' && first <= '9') {
                    intVal = first - '0';
                }

                int readPos;
                for (readPos = 1; readPos < value.length(); readPos++) {
                    char chr = value.charAt(readPos);
                    if (chr >= '0' && chr <= '9') {
                        intVal = intVal * 10 + chr - 48;
                    } else if (chr != ',') {
                        if (chr != '.') {
                            return value;
                        }

                        readPos++;
                        break;
                    }
                }

                if (readPos == value.length()) {
                    return intVal;
                } else {
                    float floatVal = intVal;

                    for (float divisor = 10.0F; readPos < value.length(); readPos++) {
                        char chr = value.charAt(readPos);
                        if (chr >= '0' && chr <= '9') {
                            floatVal += (chr - '0') / divisor;
                            divisor *= 10.0F;
                        } else if (chr != ',') {
                            return value;
                        }
                    }

                    if (first == '-') {
                        floatVal *= -1.0F;
                    }

                    return floatVal;
                }
            } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
                return true;
            } else if (!value.equalsIgnoreCase("false") && !value.equalsIgnoreCase("no")) {
                if (parseForCharacterVariableLookup) {
                    if (first != '\'' && first != '"') {
                        return new CharacterVariableCondition.CharacterVariableLookup(value);
                    } else {
                        StringBuilder sb = new StringBuilder(value.length() - 2);

                        for (int readPosx = 1; readPosx < value.length(); readPosx++) {
                            char c = value.charAt(readPosx);
                            switch (c) {
                                case '"':
                                case '\'':
                                    if (c == first) {
                                        return sb.toString();
                                    }
                                default:
                                    sb.append(c);
                                    break;
                                case '\\':
                                    sb.append(value.charAt(readPosx));
                            }
                        }

                        return sb.toString();
                    }
                } else {
                    return value;
                }
            } else {
                return false;
            }
        }
    }

    private boolean load(Element node) {
        String var2 = node.getNodeName();
        switch (var2) {
            case "isTrue":
                this.op = CharacterVariableCondition.Operator.Equal;
                this.lhsValue = new CharacterVariableCondition.CharacterVariableLookup(node.getTextContent().trim());
                this.rhsValue = true;
                return true;
            case "isFalse":
                this.op = CharacterVariableCondition.Operator.Equal;
                this.lhsValue = new CharacterVariableCondition.CharacterVariableLookup(node.getTextContent().trim());
                this.rhsValue = false;
                return true;
            case "compare":
                String var4 = node.getAttribute("op").trim();
                switch (var4) {
                    case "=":
                    case "==":
                        this.op = CharacterVariableCondition.Operator.Equal;
                        break;
                    case "!=":
                    case "<>":
                        this.op = CharacterVariableCondition.Operator.NotEqual;
                        break;
                    case "<":
                        this.op = CharacterVariableCondition.Operator.Less;
                        break;
                    case ">":
                        this.op = CharacterVariableCondition.Operator.Greater;
                        break;
                    case "<=":
                        this.op = CharacterVariableCondition.Operator.LessEqual;
                        break;
                    case ">=":
                        this.op = CharacterVariableCondition.Operator.GreaterEqual;
                        break;
                    default:
                        return false;
                }

                this.loadCompareValues(node);
                return true;
            case "gtr":
                this.op = CharacterVariableCondition.Operator.Greater;
                this.loadCompareValues(node);
                return true;
            case "less":
                this.op = CharacterVariableCondition.Operator.Less;
                this.loadCompareValues(node);
                return true;
            case "equals":
                this.op = CharacterVariableCondition.Operator.Equal;
                this.loadCompareValues(node);
                return true;
            case "notEquals":
                this.op = CharacterVariableCondition.Operator.NotEqual;
                this.loadCompareValues(node);
                return true;
            case "lessEqual":
                this.op = CharacterVariableCondition.Operator.LessEqual;
                this.loadCompareValues(node);
                return true;
            case "gtrEqual":
                this.op = CharacterVariableCondition.Operator.GreaterEqual;
                this.loadCompareValues(node);
                return true;
            default:
                return false;
        }
    }

    private void loadCompareValues(Element node) {
        String lhsString = node.getAttribute("a").trim();
        String rhsString = node.getAttribute("b").trim();
        this.lhsValue = parseValue(lhsString, true);
        this.rhsValue = parseValue(rhsString, false);
    }

    private static Object resolveValue(Object value, IAnimationVariableSource owner) {
        if (value instanceof CharacterVariableCondition.CharacterVariableLookup lookUp) {
            String variableValue = lookUp.getValueString(owner);
            return variableValue != null ? parseValue(variableValue, false) : null;
        } else {
            return value;
        }
    }

    private boolean resolveCompareTo(int result) {
        switch (this.op) {
            case Equal:
                return result == 0;
            case NotEqual:
                return result != 0;
            case Less:
                return result < 0;
            case Greater:
                return result > 0;
            case LessEqual:
                return result <= 0;
            case GreaterEqual:
                return result >= 0;
            default:
                return false;
        }
    }

    @Override
    public boolean passes(ActionContext context, ActionState state) {
        IAnimatable owner = context.getOwner();
        Object lhsResolved = resolveValue(this.lhsValue, owner);
        Object rhsResolved = resolveValue(this.rhsValue, owner);
        if (lhsResolved == null && rhsResolved instanceof String string && StringUtils.isNullOrEmpty(string)) {
            if (this.op == CharacterVariableCondition.Operator.Equal) {
                return true;
            }

            if (this.op == CharacterVariableCondition.Operator.NotEqual) {
                return false;
            }

            boolean lhsIsFloat = true;
        }

        if (lhsResolved != null && rhsResolved != null) {
            if (lhsResolved.getClass().equals(rhsResolved.getClass())) {
                if (lhsResolved instanceof String s) {
                    return this.resolveCompareTo(s.compareTo((String)rhsResolved));
                }

                if (lhsResolved instanceof Integer i) {
                    return this.resolveCompareTo(i.compareTo((Integer)rhsResolved));
                }

                if (lhsResolved instanceof Float f) {
                    return this.resolveCompareTo(f.compareTo((Float)rhsResolved));
                }

                if (lhsResolved instanceof Boolean b) {
                    return this.resolveCompareTo(b.compareTo((Boolean)rhsResolved));
                }
            }

            boolean lhsIsInt = lhsResolved instanceof Integer;
            boolean lhsIsFloat = lhsResolved instanceof Float;
            boolean rhsIsInt = rhsResolved instanceof Integer;
            boolean rhsIsFloat = rhsResolved instanceof Float;
            if ((lhsIsInt || lhsIsFloat) && (rhsIsInt || rhsIsFloat)) {
                boolean lhsWasLookup = this.lhsValue instanceof CharacterVariableCondition.CharacterVariableLookup;
                boolean rhsWasLookup = this.rhsValue instanceof CharacterVariableCondition.CharacterVariableLookup;
                if (lhsWasLookup == rhsWasLookup) {
                    float lhsFloat = lhsIsFloat ? (Float)lhsResolved : ((Integer)lhsResolved).intValue();
                    float rhsFloat = rhsIsFloat ? (Float)rhsResolved : ((Integer)rhsResolved).intValue();
                    return this.resolveCompareTo(Float.compare(lhsFloat, rhsFloat));
                } else if (lhsWasLookup) {
                    if (rhsIsFloat) {
                        float lhsFloat = lhsIsFloat ? (Float)lhsResolved : ((Integer)lhsResolved).intValue();
                        float rhsFloat = (Float)rhsResolved;
                        return this.resolveCompareTo(Float.compare(lhsFloat, rhsFloat));
                    } else {
                        int lhsInt = lhsIsFloat ? (int)((Float)lhsResolved).floatValue() : (Integer)lhsResolved;
                        int rhsInt = (Integer)rhsResolved;
                        return this.resolveCompareTo(Integer.compare(lhsInt, rhsInt));
                    }
                } else if (lhsIsFloat) {
                    float lhsFloat = (Float)lhsResolved;
                    float rhsFloat = rhsIsFloat ? (Float)rhsResolved : ((Integer)rhsResolved).intValue();
                    return this.resolveCompareTo(Float.compare(lhsFloat, rhsFloat));
                } else {
                    int lhsInt = (Integer)lhsResolved;
                    int rhsInt = rhsIsFloat ? (int)((Float)rhsResolved).floatValue() : (Integer)rhsResolved;
                    return this.resolveCompareTo(Integer.compare(lhsInt, rhsInt));
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public IActionCondition clone() {
        return this;
    }

    private static String getOpString(CharacterVariableCondition.Operator op) {
        switch (op) {
            case Equal:
                return " == ";
            case NotEqual:
                return " != ";
            case Less:
                return " < ";
            case Greater:
                return " > ";
            case LessEqual:
                return " <= ";
            case GreaterEqual:
                return " >=";
            default:
                return " ?? ";
        }
    }

    private static String valueToString(Object value) {
        return value instanceof String ? "\"" + value + "\"" : value.toString();
    }

    @Override
    public String getDescription() {
        return valueToString(this.lhsValue) + getOpString(this.op) + valueToString(this.rhsValue);
    }

    @Override
    public String toString() {
        return this.toString("");
    }

    @Override
    public String toString(String in_indent) {
        return in_indent + this.getClass().getName() + "{ " + this.getDescription() + " }";
    }

    private static class CharacterVariableLookup {
        private final AnimationVariableReference variableReference;

        public CharacterVariableLookup(String variableName) {
            this.variableReference = AnimationVariableReference.fromRawVariableName(variableName);
        }

        public String getValueString(IAnimationVariableSource owner) {
            IAnimationVariableSlot variableSlot = this.variableReference.getVariable(owner);
            return variableSlot == null ? null : variableSlot.getValueString();
        }

        @Override
        public String toString() {
            return this.variableReference.toString();
        }
    }

    public static class Factory implements IActionCondition.IFactory {
        @Override
        public IActionCondition create(Element conditionNode) {
            CharacterVariableCondition cond = new CharacterVariableCondition();
            return cond.load(conditionNode) ? cond : null;
        }
    }

    static enum Operator {
        Equal,
        NotEqual,
        Less,
        Greater,
        LessEqual,
        GreaterEqual;
    }
}
