// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.debug.DebugLog;
import zombie.iso.Vector2;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class AnimationVariableRecordingFrame extends GenericNameValueRecordingFrame {
    private String[] variableValues = new String[0];
    private AnimationVariableType[] variableTypes = new AnimationVariableType[0];
    private final Vector2 deferredMovement = new Vector2();
    private final Vector2 deferredMovementFromRagdoll = new Vector2();

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
            case Boolean:
                this.logVariable(name, entry.getValueBool());
        }
    }

    public void logVariable(String in_name, String in_value) {
        int columnIndex = this.getOrCreateColumn(in_name);
        if (this.variableValues[columnIndex] != null) {
            DebugLog.General.error("Value for %s already set: %s, new value: %s", in_name, this.variableValues[columnIndex], in_value);
        }

        this.variableValues[columnIndex] = in_value;
        AnimationVariableType cellType = this.variableTypes[columnIndex];
        AnimationVariableType newCellType = this.checkCellType(cellType, in_value);
        this.setCellType(columnIndex, newCellType);
    }

    public void logVariable(String in_name, float in_value) {
        int columnIndex = this.getOrCreateColumn(in_name);
        if (this.variableValues[columnIndex] != null) {
            DebugLog.General.error("Value for %s already set: %s, new value: %f", in_name, this.variableValues[columnIndex], in_value);
        }

        this.variableValues[columnIndex] = String.valueOf(in_value);
        this.setCellType(columnIndex, AnimationVariableType.Float);
    }

    public void logVariable(String in_name, boolean in_value) {
        int columnIndex = this.getOrCreateColumn(in_name);
        if (this.variableValues[columnIndex] != null) {
            DebugLog.General.error("Value for %s already set: %s, new value: %s", in_name, this.variableValues[columnIndex], in_value ? "1" : "0");
        }

        this.variableValues[columnIndex] = in_value ? "1" : "0";
        this.setCellType(columnIndex, AnimationVariableType.Boolean);
    }

    private void setCellType(int in_columnIndex, AnimationVariableType in_newCellType) {
        AnimationVariableType oldCellType = this.variableTypes[in_columnIndex];
        if (oldCellType != in_newCellType) {
            this.variableTypes[in_columnIndex] = in_newCellType;
            this.headerDirty = true;
        }
    }

    private AnimationVariableType checkCellType(AnimationVariableType in_existingCellType, String in_cellValue) {
        AnimationVariableType out_newCellType = in_existingCellType;
        if (in_existingCellType != null && in_existingCellType != AnimationVariableType.Void) {
            if (in_existingCellType == AnimationVariableType.String) {
                return in_existingCellType;
            } else if (in_existingCellType == AnimationVariableType.Float) {
                boolean isFloat = StringUtils.isNullOrWhitespace(in_cellValue) || StringUtils.isFloat(in_cellValue);
                if (!isFloat) {
                    out_newCellType = AnimationVariableType.String;
                }

                return out_newCellType;
            } else if (in_existingCellType != AnimationVariableType.Boolean) {
                return in_existingCellType;
            } else {
                boolean isBool = StringUtils.isNullOrWhitespace(in_cellValue) || StringUtils.isBoolean(in_cellValue);
                if (!isBool) {
                    out_newCellType = AnimationVariableType.String;
                }

                return out_newCellType;
            }
        } else if (StringUtils.isNullOrWhitespace(in_cellValue)) {
            return in_existingCellType;
        } else {
            if (StringUtils.isFloat(in_cellValue)) {
                out_newCellType = AnimationVariableType.Float;
            } else if (StringUtils.isBoolean(in_cellValue)) {
                out_newCellType = AnimationVariableType.Boolean;
            } else {
                out_newCellType = AnimationVariableType.String;
            }

            return out_newCellType;
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
        this.deferredMovementFromRagdoll.set(0.0F, 0.0F);
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

    public void logDeferredMovement(Vector2 deferredMovement, Vector2 deferredMovementFromRagdoll) {
        this.deferredMovement.set(deferredMovement);
        this.deferredMovementFromRagdoll.set(deferredMovementFromRagdoll);
        this.logVariable("anim_deferredMovement.x", this.deferredMovement.x);
        this.logVariable("anim_deferredMovement.y", this.deferredMovement.y);
        this.logVariable("anim_deferredMovementFromRagdoll.x", this.deferredMovementFromRagdoll.x);
        this.logVariable("anim_deferredMovementFromRagdoll.y", this.deferredMovementFromRagdoll.y);
    }
}
