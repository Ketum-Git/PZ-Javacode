// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

@XmlType(name = "AnimCondition")
public final class AnimCondition {
    @XmlElement(name = "m_Name")
    public String name = "";
    @XmlElement(name = "m_Type")
    public AnimCondition.Type type = AnimCondition.Type.STRING;
    @XmlElement(name = "m_Value")
    public String value = "";
    @XmlElement(name = "m_FloatValue")
    public float floatValue;
    @XmlElement(name = "m_BoolValue")
    public boolean boolValue;
    @XmlElement(name = "m_StringValue")
    public String stringValue = "";
    @XmlTransient
    AnimationVariableReference variableReference;

    public void parse(AnimNode in_fromNode, AnimNode in_toNode) {
        this.parseValue();
        this.variableReference = AnimationVariableReference.fromRawVariableName(this.name);
        if (this.isTypeString()) {
            if (this.stringValue.contains("$this")) {
                this.stringValue = this.stringValue.replaceAll("\\$this", in_fromNode.name);
            }

            if (this.stringValue.contains("$source")) {
                this.stringValue = this.stringValue.replaceAll("\\$source", in_fromNode.name);
            }

            if (this.stringValue.contains("$target")) {
                if (in_toNode != null) {
                    this.stringValue = this.stringValue.replaceAll("\\$target", in_toNode.name);
                } else {
                    DebugLog.Animation
                        .error(
                            "$target not supported in conditions that have no toNode specified. Only allowed in AnimTransition. FromNode: %s, ToNode: %s",
                            in_fromNode,
                            in_toNode
                        );
                }
            }
        }
    }

    public void parseValue() {
        if (!this.value.isEmpty()) {
            AnimCondition.Type varType = this.type;
            switch (varType) {
                case STRING:
                case STRNEQ:
                    this.stringValue = this.value;
                    break;
                case BOOL:
                    this.boolValue = StringUtils.tryParseBoolean(this.value);
                    break;
                case EQU:
                case NEQ:
                case LESS:
                case GTR:
                case ABSLESS:
                case ABSGTR:
                    this.floatValue = StringUtils.tryParseFloat(this.value);
                case OR:
            }
        }
    }

    @Override
    public String toString() {
        return String.format("AnimCondition{name:%s type:%s value:%s }", this.name, this.type.toString(), this.getValueString());
    }

    public String getConditionString() {
        return this.type == AnimCondition.Type.OR ? "OR" : String.format("( %s %s %s )", this.name, this.type.toString(), this.getValueString());
    }

    public String getValueString() {
        switch (this.type) {
            case STRING:
            case STRNEQ:
                return this.stringValue;
            case BOOL:
                return this.boolValue ? "true" : "false";
            case EQU:
            case NEQ:
            case LESS:
            case GTR:
            case ABSLESS:
            case ABSGTR:
                return String.valueOf(this.floatValue);
            case OR:
                return " -- OR -- ";
            default:
                throw new RuntimeException("Unexpected internal type:" + this.type);
        }
    }

    public boolean isTypeString() {
        return this.type == AnimCondition.Type.STRING || this.type == AnimCondition.Type.STRNEQ;
    }

    public boolean check(IAnimationVariableSource in_varSource) {
        AnimCondition.Type varType = this.type;
        if (varType == AnimCondition.Type.OR) {
            return false;
        } else {
            IAnimationVariableSlot variableSlot = this.variableReference.getVariable(in_varSource);
            if (variableSlot == null) {
                switch (varType) {
                    case STRING:
                        return StringUtils.equalsIgnoreCase(this.stringValue, "");
                    case STRNEQ:
                        return !StringUtils.equalsIgnoreCase(this.stringValue, "");
                    case BOOL:
                        return !this.boolValue;
                    case EQU:
                    case NEQ:
                    case LESS:
                    case GTR:
                    case ABSLESS:
                    case ABSGTR:
                        DebugLog.Animation.warnOnce("Variable \"%s\" not found in %s", this.variableReference, in_varSource);
                        return false;
                    case OR:
                        return false;
                }
            }

            switch (varType) {
                case STRING:
                    return StringUtils.equalsIgnoreCase(this.stringValue, variableSlot.getValueString());
                case STRNEQ:
                    return !StringUtils.equalsIgnoreCase(this.stringValue, variableSlot.getValueString());
                case BOOL:
                    return variableSlot.getValueBool() == this.boolValue;
                case EQU:
                    return this.floatValue == variableSlot.getValueFloat();
                case NEQ:
                    return this.floatValue != variableSlot.getValueFloat();
                case LESS:
                    return variableSlot.getValueFloat() < this.floatValue;
                case GTR:
                    return variableSlot.getValueFloat() > this.floatValue;
                case ABSLESS:
                    return PZMath.abs(variableSlot.getValueFloat()) < this.floatValue;
                case ABSGTR:
                    return PZMath.abs(variableSlot.getValueFloat()) > this.floatValue;
                case OR:
                    return false;
                default:
                    throw new RuntimeException("Unexpected internal type:" + this.type);
            }
        }
    }

    public static boolean pass(IAnimationVariableSource varSource, AnimCondition[] conditions) {
        boolean valid = true;

        for (AnimCondition condition : conditions) {
            if (condition.type == AnimCondition.Type.OR) {
                if (valid) {
                    break;
                }

                valid = true;
            } else {
                valid = valid && condition.check(varSource);
            }
        }

        return valid;
    }

    @XmlEnum
    @XmlType(name = "Type")
    public static enum Type {
        @XmlEnumValue("STRING")
        STRING,
        @XmlEnumValue("STRNEQ")
        STRNEQ,
        @XmlEnumValue("BOOL")
        BOOL,
        @XmlEnumValue("EQU")
        EQU,
        @XmlEnumValue("NEQ")
        NEQ,
        @XmlEnumValue("LESS")
        LESS,
        @XmlEnumValue("GTR")
        GTR,
        @XmlEnumValue("ABSLESS")
        ABSLESS,
        @XmlEnumValue("ABSGTR")
        ABSGTR,
        @XmlEnumValue("OR")
        OR;
    }
}
