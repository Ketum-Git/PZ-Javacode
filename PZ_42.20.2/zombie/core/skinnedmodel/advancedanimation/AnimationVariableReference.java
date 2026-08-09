// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.Core;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.debug.DebugType;
import zombie.util.StringUtils;

public class AnimationVariableReference {
    private String subVariableSourceName;
    private String name;
    private AnimationVariableHandle variableHandle;

    private AnimationVariableReference() {
    }

    public boolean equals(AnimationVariableReference rhs) {
        return rhs != null
            && StringUtils.equalsIgnoreCase(this.subVariableSourceName, rhs.subVariableSourceName)
            && StringUtils.equalsIgnoreCase(this.name, rhs.name)
            && AnimationVariableHandle.equals(this.getVariableHandle(), rhs.getVariableHandle());
    }

    public String getName() {
        return this.name;
    }

    public String getSubVariableSourceName() {
        return this.subVariableSourceName;
    }

    private void parse() {
        String rawVariableName = this.name;
        if (rawVariableName.startsWith("$")) {
            if (rawVariableName.indexOf(46) > 1) {
                int indexOfDot = rawVariableName.indexOf(46);
                String subVariableSourceName = rawVariableName.substring(1, indexOfDot);
                String subVariableName = rawVariableName.substring(indexOfDot + 1);
                if (StringUtils.isNullOrWhitespace(subVariableName)) {
                    DebugType.Animation.warn("Error parsing: %s", rawVariableName);
                    DebugType.Animation.warn("  SubVariableName not specified.");
                    DebugType.Animation.warn("  Expected: $<subVariableSource>.<subVariableName>");
                } else if (!StringUtils.isValidVariableName(subVariableSourceName)) {
                    DebugType.Animation.warn("Error parsing: %s", rawVariableName);
                    DebugType.Animation.warn("  SubVariableSource name not valid. Only AlphaNumeric or underscores '_' allowed.");
                } else {
                    this.subVariableSourceName = subVariableSourceName;
                    this.name = subVariableName;
                }
            }
        }
    }

    public static AnimationVariableReference fromRawVariableName(String rawVariableName) {
        if (Core.debug) {
            AnimatorDebugMonitor.registerVariable(rawVariableName);
        }

        AnimationVariableReference newReference = new AnimationVariableReference();
        newReference.name = rawVariableName;
        newReference.parse();
        return newReference;
    }

    public IAnimationVariableSlot getVariable(IAnimationVariableSource varSource) {
        if (this.getName().isBlank()) {
            return null;
        } else {
            AnimationVariableHandle variableHandle = this.getVariableHandle();
            IAnimationVariableSource animationVariableSource = this.getAnimationVariableSource(varSource);
            return animationVariableSource == null ? null : animationVariableSource.getVariable(variableHandle);
        }
    }

    private AnimationVariableHandle getVariableHandle() {
        if (this.variableHandle == null) {
            this.variableHandle = AnimationVariableHandle.alloc(this.name);
        }

        return this.variableHandle;
    }

    private IAnimationVariableSource getAnimationVariableSource(IAnimationVariableSource varSource) {
        if (this.subVariableSourceName != null) {
            IAnimationVariableSource subVarSource = varSource.getSubVariableSource(this.subVariableSourceName);
            if (subVarSource == null) {
                DebugType.Animation.warnOnce("SubVariableSource name \"%s\" does not exist in %s", this.subVariableSourceName, varSource);
            }

            return subVarSource;
        } else {
            return varSource;
        }
    }

    public boolean isSubVariableSourceReference() {
        return this.subVariableSourceName != null;
    }

    public void setVariable(IAnimationVariableSource owner, String variableValue) {
        if (this.getName().isBlank()) {
            DebugType.General.warnOnce("Variable name is blank. Cannot set.");
        } else {
            IAnimationVariableSlot slot = this.getVariable(owner);
            if (slot != null) {
                slot.setValue(variableValue);
            } else {
                String variableName = this.getName();
                IAnimationVariableSource variableSource = this.getAnimationVariableSource(owner);
                if (variableSource instanceof IAnimationVariableMap iAnimationVariableMap) {
                    iAnimationVariableMap.setVariable(variableName, variableValue);
                } else {
                    DebugType.General.warnOnce("Destination VariableSource is read-only. Cannot set %s.%s=%s", variableSource, variableName, variableValue);
                }
            }
        }
    }

    public void setVariable(IAnimationVariableSource owner, boolean variableValue) {
        if (this.getName().isBlank()) {
            DebugType.General.warnOnce("Variable name is blank. Cannot set.");
        } else {
            IAnimationVariableSlot slot = this.getVariable(owner);
            if (slot != null) {
                slot.setValue(variableValue);
            } else {
                String variableName = this.getName();
                IAnimationVariableSource variableSource = this.getAnimationVariableSource(owner);
                if (variableSource instanceof IAnimationVariableMap iAnimationVariableMap) {
                    iAnimationVariableMap.setVariable(variableName, variableValue);
                } else {
                    DebugType.General.warnOnce("Destination VariableSource is read-only. Cannot set %s.%s=%s", variableSource, variableName, variableValue);
                }
            }
        }
    }

    public void clearVariable(IAnimationVariableSource owner) {
        if (this.getName().isBlank()) {
            DebugType.General.warnOnce("Variable name is blank. Cannot set.");
        } else {
            IAnimationVariableSlot slot = this.getVariable(owner);
            if (slot != null) {
                slot.clear();
            } else {
                String variableName = this.getName();
                IAnimationVariableSource variableSource = this.getAnimationVariableSource(owner);
                if (variableSource instanceof IAnimationVariableMap iAnimationVariableMap) {
                    iAnimationVariableMap.clearVariable(variableName);
                } else {
                    DebugType.General.warnOnce("Destination VariableSource is read-only. Cannot clear variable %s.%s", variableSource, variableName);
                }
            }
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
            + "{"
            + (this.isSubVariableSourceReference() ? " sourceName:" + this.getSubVariableSourceName() + ", " : "")
            + " variableName:"
            + this.getName()
            + " }";
    }
}
