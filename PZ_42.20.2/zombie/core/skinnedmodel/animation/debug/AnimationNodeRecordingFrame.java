// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import java.util.ArrayList;
import java.util.List;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.iso.Vector3;
import zombie.util.list.PZArrayUtil;

public final class AnimationNodeRecordingFrame extends GenericNameWeightRecordingFrame {
    private String actionGroupName;
    private String actionStateName;
    private final ArrayList<String> actionSubStateNames = new ArrayList<>();
    private String aiStateName;
    private final ArrayList<String> aiSubStateNames = new ArrayList<>();
    private String animStateName;
    private final ArrayList<String> animSubStateNames = new ArrayList<>();
    private final Vector3 characterToPlayerDiff = new Vector3();

    public AnimationNodeRecordingFrame(String fileKey) {
        super(fileKey);
    }

    public void logActionState(ActionGroup group, ActionState state, List<ActionState> childStates) {
        this.actionGroupName = group.getName();
        this.actionStateName = state != null ? state.getName() : null;
        PZArrayUtil.arrayConvert(this.actionSubStateNames, childStates, ActionState::getName);
    }

    public void logActionState(String actionGroupName, String actionStateName) {
        this.actionGroupName = actionGroupName;
        this.actionStateName = actionStateName;
        this.actionSubStateNames.clear();
    }

    public void logAIState(State state, List<StateMachine.SubstateSlot> subStates) {
        this.aiStateName = state != null ? state.getName() : null;
        PZArrayUtil.arrayConvert(this.aiSubStateNames, subStates, subState -> !subState.isEmpty() ? subState.getState().getName() : "");
    }

    public void logAIState(String aiStateName) {
        this.aiStateName = aiStateName;
        this.aiSubStateNames.clear();
    }

    public void logAnimState(AnimLayer layer, AnimState state) {
        int layerIdx = layer.getDepth();
        String animStateName = state != null ? state.name : "";
        if (layerIdx == 0) {
            this.animStateName = animStateName;
        } else {
            while (layerIdx > this.animSubStateNames.size()) {
                this.animSubStateNames.add("");
            }

            this.animSubStateNames.set(layerIdx - 1, animStateName);
        }
    }

    public void logCharacterToPlayerDiff(Vector3 diff) {
        this.characterToPlayerDiff.set(diff);
    }

    @Override
    public void buildHeader(StringBuilder logLine) {
        appendCell(logLine, "toPlayer.x");
        appendCell(logLine, "toPlayer.y");
        appendCell(logLine, "actionGroup");
        appendCell(logLine, "actionState");
        appendCell(logLine, "actionState.sub[0]");
        appendCell(logLine, "actionState.sub[1]");
        appendCell(logLine, "actionState.sub[2]");
        appendCell(logLine, "actionState.sub[3]");
        appendCell(logLine, "actionState.sub[4]");
        appendCell(logLine, "actionState.sub[5]");
        appendCell(logLine, "aiState");
        appendCell(logLine, "aiState.sub[0]");
        appendCell(logLine, "aiState.sub[1]");
        appendCell(logLine, "aiState.sub[2]");
        appendCell(logLine, "aiState.sub[3]");
        appendCell(logLine, "aiState.sub[4]");
        appendCell(logLine, "aiState.sub[5]");
        appendCell(logLine, "animState");
        appendCell(logLine, "animState.sub[0]");
        appendCell(logLine, "animState.sub[1]");
        appendCell(logLine, "animState.sub[2]");
        appendCell(logLine, "animState.sub[3]");
        appendCell(logLine, "animState.sub[4]");
        appendCell(logLine, "animState.sub[5]");
        appendCell(logLine, "nodeWeights.begin");
        super.buildHeader(logLine);
    }

    @Override
    protected void writeData(StringBuilder logLine) {
        appendCell(logLine, this.characterToPlayerDiff.x);
        appendCell(logLine, this.characterToPlayerDiff.y);
        appendCell(logLine, this.actionGroupName);
        appendCell(logLine, this.actionStateName);
        appendCell(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 0, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 1, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 2, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 3, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 4, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 5, ""));
        appendCell(logLine, this.aiStateName);
        appendCell(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 0, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 1, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 2, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 3, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 4, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 5, ""));
        appendCell(logLine, this.animStateName);
        appendCell(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 0, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 1, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 2, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 3, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 4, ""));
        appendCell(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 5, ""));
        appendCell(logLine);
        super.writeData(logLine);
    }

    @Override
    public void reset() {
        this.actionGroupName = "";
        this.actionStateName = "";
        this.actionSubStateNames.clear();
        this.aiStateName = "";
        this.aiSubStateNames.clear();
        this.animStateName = "";
        this.animSubStateNames.clear();
        super.reset();
    }
}
