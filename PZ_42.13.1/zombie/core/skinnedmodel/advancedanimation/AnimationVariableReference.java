// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.Core;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.debug.DebugLog;
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
                    DebugLog.Animation.warn("Error parsing: %s", rawVariableName);
                    DebugLog.Animation.warn("  SubVariableName not specified.");
                    DebugLog.Animation.warn("  Expected: $<subVariableSource>.<subVariableName>");
                } else if (!StringUtils.isValidVariableName(subVariableSourceName)) {
                    DebugLog.Animation.warn("Error parsing: %s", rawVariableName);
                    DebugLog.Animation.warn("  SubVariableSource name not valid. Only AlphaNumeric or underscores '_' allowed.");
                } else {
                    this.subVariableSourceName = subVariableSourceName;
                    this.name = subVariableName;
                }
            }
        }
    }

    public static AnimationVariableReference fromRawVariableName(String in_rawVariableName) {
        if (Core.debug) {
            AnimatorDebugMonitor.registerVariable(in_rawVariableName);
        }

        AnimationVariableReference newReference = new AnimationVariableReference();
        newReference.name = in_rawVariableName;
        newReference.parse();
        return newReference;
    }

    public IAnimationVariableSlot getVariable(IAnimationVariableSource in_varSource) {
        if (this.getName().isBlank()) {
            return null;
        } else {
            AnimationVariableHandle variableHandle = this.getVariableHandle();
            IAnimationVariableSource varSource = this.getAnimationVariableSource(in_varSource);
            return varSource == null ? null : varSource.getVariable(variableHandle);
        }
    }

    private AnimationVariableHandle getVariableHandle() {
        if (this.variableHandle == null) {
            this.variableHandle = AnimationVariableHandle.alloc(this.name);
        }

        return this.variableHandle;
    }

    private IAnimationVariableSource getAnimationVariableSource(IAnimationVariableSource in_varSource) {
        if (this.subVariableSourceName != null) {
            IAnimationVariableSource varSource = in_varSource.getSubVariableSource(this.subVariableSourceName);
            if (varSource == null) {
                DebugLog.Animation.warnOnce("SubVariableSource name \"%s\" does not exist in %s", this.subVariableSourceName, in_varSource);
            }

            return varSource;
        } else {
            return in_varSource;
        }
    }

    public boolean isSubVariableSourceReference() {
        return this.subVariableSourceName != null;
    }

    public void setVariable(IAnimationVariableSource in_owner, String in_variableValue) {
        if (this.getName().isBlank()) {
            DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
        } else {
            IAnimationVariableSlot slot = this.getVariable(in_owner);
            if (slot != null) {
                slot.setValue(in_variableValue);
            } else {
                String variableName = this.getName();
                IAnimationVariableSource variableSource = this.getAnimationVariableSource(in_owner);
                if (variableSource instanceof IAnimationVariableMap iAnimationVariableMap) {
                    iAnimationVariableMap.setVariable(variableName, in_variableValue);
                } else {
                    DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot set %s.%s=%s", variableSource, variableName, in_variableValue);
                }
            }
        }
    }

    public void setVariable(IAnimationVariableSource in_owner, boolean in_variableValue) {
        if (this.getName().isBlank()) {
            DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
        } else {
            IAnimationVariableSlot slot = this.getVariable(in_owner);
            if (slot != null) {
                slot.setValue(in_variableValue);
            } else {
                String variableName = this.getName();
                IAnimationVariableSource variableSource = this.getAnimationVariableSource(in_owner);
                if (variableSource instanceof IAnimationVariableMap iAnimationVariableMap) {
                    iAnimationVariableMap.setVariable(variableName, in_variableValue);
                } else {
                    DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot set %s.%s=%s", variableSource, variableName, in_variableValue);
                }
            }
        }
    }

    public void clearVariable(IAnimationVariableSource in_owner) {
        if (this.getName().isBlank()) {
            DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
        } else {
            IAnimationVariableSlot slot = this.getVariable(in_owner);
            if (slot != null) {
                slot.clear();
            } else {
                String variableName = this.getName();
                IAnimationVariableSource variableSource = this.getAnimationVariableSource(in_owner);
                if (variableSource instanceof IAnimationVariableMap iAnimationVariableMap) {
                    iAnimationVariableMap.clearVariable(variableName);
                } else {
                    DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot clear variable %s.%s", variableSource, variableName);
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
