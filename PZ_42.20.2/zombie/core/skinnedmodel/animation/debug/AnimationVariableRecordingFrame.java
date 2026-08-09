// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.debug.DebugType;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class AnimationVariableRecordingFrame extends GenericNameValueRecordingFrame {
    private String[] variableValues = new String[0];
    private AnimationVariableType[] variableTypes = new AnimationVariableType[0];
    private final Vector2 deferredMovement = new Vector2();
    private final Vector3 deferredMovementFromRagdoll = new Vector3();

    public AnimationVariableRecordingFrame(String fileKey) {
        super(fileKey, "_values");
    }

    public void logVariables(IAnimationVariableSource varSource) {
        for (IAnimationVariableSlot entry : varSource.getGameVariables()) {
            this.logVariable(entry);
        }
    }

    @Override
    protected void onColumnAdded() {
        this.variableValues = PZArrayUtil.add(this.variableValues, null);
        this.variableTypes = PZArrayUtil.add(this.variableTypes, AnimationVariableType.Void);
    }

    public void logVariable(IAnimationVariableSlot entry) {
        String name = entry.getKey();
        AnimationVariableType type = entry.getType();
        switch (type) {
            case Void:
                this.logVariable(name, entry.getValueString());
                break;
            case String:
                this.logVariable(name, entry.getValueString());
                break;
            case Float:
                this.logVariable(name, entry.getValueFloat());
                break;
            case Int:
                this.logVariable(name, entry.getValueInt());
                break;
            case Boolean:
                this.logVariable(name, entry.getValueBool());
        }
    }

    public void logVariable(String name, String value) {
        int columnIndex = this.getOrCreateColumn(name);
        if (this.variableValues[columnIndex] != null) {
            DebugType.General.error("Value for %s already set: %s, new value: %s", name, this.variableValues[columnIndex], value);
        }

        this.variableValues[columnIndex] = !StringUtils.isNullOrWhitespace(value) ? value.replaceAll(",", "%2C") : "";
        AnimationVariableType cellType = this.variableTypes[columnIndex];
        AnimationVariableType newCellType = this.checkCellType(cellType, value);
        this.setCellType(columnIndex, newCellType);
    }

    public void logVariable(String name, float value) {
        int columnIndex = this.getOrCreateColumn(name);
        if (this.variableValues[columnIndex] != null) {
            DebugType.General.error("Value for %s already set: %s, new value: %f", name, this.variableValues[columnIndex], value);
        }

        this.variableValues[columnIndex] = String.valueOf(value);
        this.setCellType(columnIndex, AnimationVariableType.Float);
    }

    public void logVariable(String name, int value) {
        int columnIndex = this.getOrCreateColumn(name);
        if (this.variableValues[columnIndex] != null) {
            DebugType.General.error("Value for %s already set: %s, new value: %d", name, this.variableValues[columnIndex], value);
        }

        this.variableValues[columnIndex] = String.valueOf(value);
        this.setCellType(columnIndex, AnimationVariableType.Int);
    }

    public void logVariable(String name, boolean value) {
        int columnIndex = this.getOrCreateColumn(name);
        if (this.variableValues[columnIndex] != null) {
            DebugType.General.error("Value for %s already set: %s, new value: %s", name, this.variableValues[columnIndex], value ? "1" : "0");
        }

        this.variableValues[columnIndex] = value ? "1" : "0";
        this.setCellType(columnIndex, AnimationVariableType.Boolean);
    }

    private void setCellType(int columnIndex, AnimationVariableType newCellType) {
        AnimationVariableType oldCellType = this.variableTypes[columnIndex];
        if (oldCellType != newCellType) {
            this.variableTypes[columnIndex] = newCellType;
            this.headerDirty = true;
        }
    }

    private AnimationVariableType checkCellType(AnimationVariableType existingCellType, String cellValue) {
        AnimationVariableType newCellType = existingCellType;
        if (existingCellType != null && existingCellType != AnimationVariableType.Void) {
            if (existingCellType == AnimationVariableType.String) {
                return existingCellType;
            } else if (existingCellType == AnimationVariableType.Float) {
                boolean isFloat = StringUtils.isNullOrWhitespace(cellValue) || StringUtils.isFloat(cellValue);
                if (!isFloat) {
                    newCellType = AnimationVariableType.String;
                }

                return newCellType;
            } else if (existingCellType == AnimationVariableType.Int) {
                boolean isInt = StringUtils.isNullOrWhitespace(cellValue) || StringUtils.isInt(cellValue);
                if (!isInt) {
                    newCellType = AnimationVariableType.String;
                }

                return newCellType;
            } else if (existingCellType != AnimationVariableType.Boolean) {
                return existingCellType;
            } else {
                boolean isBool = StringUtils.isNullOrWhitespace(cellValue) || StringUtils.isBoolean(cellValue);
                if (!isBool) {
                    newCellType = AnimationVariableType.String;
                }

                return newCellType;
            }
        } else if (StringUtils.isNullOrWhitespace(cellValue)) {
            return existingCellType;
        } else {
            if (StringUtils.isFloat(cellValue)) {
                newCellType = AnimationVariableType.Float;
            } else if (StringUtils.isInt(cellValue)) {
                newCellType = AnimationVariableType.Int;
            } else if (StringUtils.isBoolean(cellValue)) {
                newCellType = AnimationVariableType.Boolean;
            } else {
                newCellType = AnimationVariableType.String;
            }

            return newCellType;
        }
    }

    @Override
    public String getValueAt(int i) {
        return this.variableValues[i];
    }

    @Override
    public void reset() {
        int i = 0;

        for (int weightCount = this.variableValues.length; i < weightCount; i++) {
            this.variableValues[i] = null;
        }

        this.deferredMovement.set(0.0F, 0.0F);
        this.deferredMovementFromRagdoll.set(0.0F, 0.0F, 0.0F);
    }

    @Override
    protected void writeHeaderToMemory() {
        super.writeHeaderToMemory();
        StringBuilder logLine = new StringBuilder();

        for (int i = 0; i < this.variableTypes.length; i++) {
            if (i > 0) {
                logLine.append(",");
            }

            if (this.variableTypes[i] == null) {
                logLine.append("String");
            } else {
                logLine.append(this.variableTypes[i]);
            }
        }

        this.outHeader.println(logLine);
    }

    public void logDeferredMovement(Vector2 deferredMovement, Vector3 deferredMovementFromRagdoll) {
        this.deferredMovement.set(deferredMovement);
        this.deferredMovementFromRagdoll.set(deferredMovementFromRagdoll);
        this.logVariable("anim_deferredMovement.x", this.deferredMovement.x);
        this.logVariable("anim_deferredMovement.y", this.deferredMovement.y);
        this.logVariable("anim_deferredMovementFromRagdoll.x", this.deferredMovementFromRagdoll.x);
        this.logVariable("anim_deferredMovementFromRagdoll.y", this.deferredMovementFromRagdoll.y);
        this.logVariable("anim_deferredMovementFromRagdoll.z", this.deferredMovementFromRagdoll.z);
    }
}
