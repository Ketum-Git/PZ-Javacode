// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import java.util.ArrayList;
import java.util.List;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.iso.Vector3;
import zombie.util.list.PZArrayUtil;

public final class AnimationNodeRecordingFrame extends GenericNameWeightRecordingFrame {
    private String actionGroupName;
    private String actionStateName;
    private final ArrayList<String> actionSubStateNames = new ArrayList<>();
    private String aiStateName;
    private String animStateName;
    private final ArrayList<String> animSubStateNames = new ArrayList<>();
    private final ArrayList<String> aiSubStateNames = new ArrayList<>();
    private final Vector3 characterToPlayerDiff = new Vector3();

    public AnimationNodeRecordingFrame(String fileKey) {
        super(fileKey);
    }

    public void logActionState(ActionGroup group, ActionState state, List<ActionState> childStates) {
        this.actionGroupName = group.getName();
        this.actionStateName = state != null ? state.getName() : null;
        PZArrayUtil.arrayConvert(this.actionSubStateNames, childStates, ActionState::getName);
    }

    public void logAIState(State state, List<StateMachine.SubstateSlot> subStates) {
        this.aiStateName = state != null ? state.getName() : null;
        PZArrayUtil.arrayConvert(this.aiSubStateNames, subStates, subState -> !subState.isEmpty() ? subState.getState().getName() : "");
    }

    public void logAnimState(AnimState state) {
        this.animStateName = state != null ? state.name : null;
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
        appendCell(logLine, "aiState");
        appendCell(logLine, "aiState.sub[0]");
        appendCell(logLine, "aiState.sub[1]");
        appendCell(logLine, "animState");
        appendCell(logLine, "animState.sub[0]");
        appendCell(logLine, "animState.sub[1]");
        appendCell(logLine, "nodeWeights.begin");
        super.buildHeader(logLine);
    }

    @Override
    protected void writeData(StringBuilder logLine) {
        appendCell(logLine, this.characterToPlayerDiff.x);
        appendCell(logLine, this.characterToPlayerDiff.y);
        appendCellQuot(logLine, this.actionGroupName);
        appendCellQuot(logLine, this.actionStateName);
        appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 0, ""));
        appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 1, ""));
        appendCellQuot(logLine, this.aiStateName);
        appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 0, ""));
        appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 1, ""));
        appendCellQuot(logLine, this.animStateName);
        appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 0, ""));
        appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 1, ""));
        appendCell(logLine);
        super.writeData(logLine);
    }
}
