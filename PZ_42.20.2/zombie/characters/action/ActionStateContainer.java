// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.debug.DebugType;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ActionStateContainer {
    private ActionState currentState;
    private final ArrayList<ActionState> childStates = new ArrayList<>();
    private ActionTransition transitionUsedForThisState;
    private final PerformanceProfileProbe evaluateCurrentStateTransitions = new PerformanceProfileProbe("ActionStateContainer.evaluateCurrentStateTransitions");
    private final PerformanceProfileProbe evaluateSubStateTransitions = new PerformanceProfileProbe("ActionStateContainer.evaluateSubStateTransitions");

    public void evaluateCurrentState(ActionContext actionContext) {
        if (this.currentState != null) {
            try (AbstractPerformanceProfileProbe var2 = this.evaluateCurrentStateTransitions.profile()) {
                this.evaluateCurrentStateTransitions(actionContext);
            }

            try (AbstractPerformanceProfileProbe var9 = this.evaluateSubStateTransitions.profile()) {
                this.evaluateSubStateTransitions(actionContext);
            }
        }
    }

    public ActionState peekNextState(ActionContext actionContext) {
        ActionState result = null;
        ActionGroup actionGroup = actionContext.getGroup();

        for (int i = 0; i < this.currentState.transitions.size(); i++) {
            ActionTransition nextTransition = this.currentState.transitions.get(i);
            String transitionTo = this.getTransitionTo(actionContext, nextTransition);
            if (!StringUtils.isNullOrWhitespace(transitionTo) && nextTransition.passes(actionContext, this.currentState)) {
                ActionState nextState = actionGroup.findState(transitionTo);
                if (nextState != null && !this.hasChildState(nextState) && (!nextTransition.asSubstate || this.currentStateSupportsChildState(nextState))) {
                    result = nextState;
                    break;
                }
            }
        }

        for (int subStateIdx = 0; subStateIdx < this.childStateCount(); subStateIdx++) {
            ActionState nextState = null;
            ActionState subState = this.getChildStateAt(subStateIdx);

            for (int transIdx = 0; transIdx < subState.transitions.size(); transIdx++) {
                ActionTransition subTransition = subState.transitions.get(transIdx);
                if (subTransition.passes(actionContext, subState)) {
                    if (subTransition.transitionOut) {
                        break;
                    }

                    String transitionTo = this.getTransitionTo(actionContext, subTransition);
                    if (!StringUtils.isNullOrWhitespace(transitionTo)) {
                        ActionState nextSubState = actionGroup.findState(transitionTo);
                        if (nextSubState != null && !this.hasChildState(nextSubState)) {
                            if (this.currentStateSupportsChildState(nextSubState)) {
                                break;
                            }

                            if (subTransition.forceParent) {
                                nextState = nextSubState;
                                break;
                            }
                        }
                    }
                }
            }

            if (nextState != this.currentState && nextState != null) {
                result = nextState;
            }
        }

        return result;
    }

    private String getTransitionTo(ActionContext actionContext, ActionTransition transition) {
        return transition.transitionOut ? actionContext.peekPreviousStateName() : transition.transitionTo;
    }

    private void evaluateCurrentStateTransitions(ActionContext actionContext) {
        for (int i = 0; i < this.currentState.transitions.size(); i++) {
            ActionTransition nextTransition = this.currentState.transitions.get(i);
            if (!nextTransition.asSubstate) {
                String transitionTo = this.getTransitionTo(actionContext, nextTransition);
                if (StringUtils.isNullOrWhitespace(transitionTo)) {
                    DebugType.ActionSystem.warn("%s> Transition's target state not specified: \"%s\"", actionContext.getOwner().getUID(), transitionTo);
                } else if (nextTransition.passes(actionContext, this.currentState)) {
                    ActionState nextState = actionContext.getGroup().findState(transitionTo);
                    if (nextState != null) {
                        this.setCurrentState(nextState, nextTransition);
                        break;
                    }

                    DebugType.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", actionContext.getOwner().getUID(), transitionTo);
                }
            }
        }

        for (int ix = 0; ix < this.currentState.transitions.size(); ix++) {
            ActionTransition nextTransition = this.currentState.transitions.get(ix);
            if (nextTransition.asSubstate) {
                String transitionTo = this.getTransitionTo(actionContext, nextTransition);
                if (StringUtils.isNullOrWhitespace(transitionTo)) {
                    DebugType.ActionSystem.warn("%s> Transition's target state not specified: \"%s\"", actionContext.getOwner().getUID(), transitionTo);
                } else if (nextTransition.passes(actionContext, this.currentState)) {
                    ActionState nextSubState = actionContext.getGroup().findState(transitionTo);
                    if (nextSubState == null) {
                        DebugType.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", actionContext.getOwner().getUID(), transitionTo);
                    } else {
                        this.tryInsertChildState(actionContext, nextSubState);
                    }
                }
            }
        }
    }

    private void evaluateSubStateTransitions(ActionContext actionContext) {
        for (int subStateIdx = 0; subStateIdx < this.childStateCount(); subStateIdx++) {
            ActionState nextState = null;
            ActionState subState = this.getChildStateAt(subStateIdx);

            for (int transIdx = 0; transIdx < subState.transitions.size(); transIdx++) {
                ActionTransition subTransition = subState.transitions.get(transIdx);
                if (subTransition.passes(actionContext, subState)) {
                    if (subTransition.transitionOut) {
                        this.removeChildStateAt(subStateIdx);
                        subStateIdx--;
                        break;
                    }

                    if (!StringUtils.isNullOrWhitespace(subTransition.transitionTo)) {
                        ActionState nextSubState = actionContext.getGroup().findState(subTransition.transitionTo);
                        if (nextSubState == null) {
                            DebugType.ActionSystem
                                .warn("%s> Transition's target state not found: \"%s\"", actionContext.getOwner().getUID(), subTransition.transitionTo);
                        } else if (!this.hasChildState(nextSubState)) {
                            if (this.currentStateSupportsChildState(nextSubState)) {
                                ActionState previousSubState = this.childStates.set(subStateIdx, nextSubState);
                                DebugType.ActionSystem
                                    .trace(
                                        "%s> Transition passes. SubState \"%s\" replaced with: \"%s\"",
                                        actionContext.getOwner().getUID(),
                                        previousSubState.getName(),
                                        subTransition.transitionTo
                                    );
                                break;
                            }

                            if (subTransition.forceParent) {
                                nextState = nextSubState;
                                break;
                            }
                        }
                    }
                }
            }

            if (nextState != this.currentState && nextState != null) {
                this.setCurrentState(nextState, null);
            }
        }
    }

    public boolean canTransitionToState(ActionGroup actionGroup, String stateName) {
        return this.canTransitionToState(actionGroup, stateName, true);
    }

    public boolean canTransitionToState(ActionGroup actionGroup, String stateName, boolean allowSubState) {
        ActionState nextState = actionGroup.findState(stateName);
        if (nextState == null) {
            return false;
        } else {
            for (int i = 0; i < this.currentState.transitions.size(); i++) {
                ActionTransition nextTransition = this.currentState.transitions.get(i);
                if (StringUtils.equalsIgnoreCase(stateName, nextTransition.transitionTo)) {
                    return true;
                }
            }

            if (!allowSubState) {
                return false;
            } else if (!this.currentStateSupportsChildState(nextState)) {
                return false;
            } else {
                for (int subStateIdx = 0; subStateIdx < this.childStateCount(); subStateIdx++) {
                    ActionState subState = this.getChildStateAt(subStateIdx);

                    for (int transIdx = 0; transIdx < subState.transitions.size(); transIdx++) {
                        ActionTransition subTransition = subState.transitions.get(transIdx);
                        if (!subTransition.transitionOut && StringUtils.equalsIgnoreCase(stateName, subTransition.transitionTo)) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    protected boolean currentStateSupportsChildState(ActionState child) {
        return this.currentState != null && this.currentState.canHaveSubState(child);
    }

    public boolean hasChildState(ActionState child) {
        int indexOf = this.indexOfChildState(state -> state == child);
        return indexOf > -1;
    }

    public void setPlaybackStateSnapshot(ActionContext actionContext, ActionStateSnapshot snapshot) {
        ActionGroup actionGroup = actionContext.getGroup();
        if (actionGroup != null) {
            if (snapshot.stateName == null) {
                DebugType.General.warn("Snapshot not valid. Missing root state name.");
            } else {
                ActionState rootState = actionGroup.findState(snapshot.stateName);
                this.setCurrentState(rootState, null);
                if (PZArrayUtil.isNullOrEmpty(snapshot.childStateNames)) {
                    while (this.childStateCount() > 0) {
                        this.removeChildStateAt(0);
                    }
                } else {
                    for (int i = 0; i < this.childStateCount(); i++) {
                        String childName = this.getChildStateAt(i).getName();
                        boolean childExists = StringUtils.contains(snapshot.childStateNames, childName, StringUtils::equalsIgnoreCase);
                        if (!childExists) {
                            this.removeChildStateAt(i);
                            i--;
                        }
                    }

                    for (int ix = 0; ix < snapshot.childStateNames.length; ix++) {
                        String childName = snapshot.childStateNames[ix];
                        ActionState childState = actionGroup.findState(childName);
                        this.tryAddChildState(childState);
                    }
                }
            }
        }
    }

    public ActionStateSnapshot getPlaybackStateSnapshot() {
        if (this.currentState == null) {
            return null;
        } else {
            ActionStateSnapshot snapshot = new ActionStateSnapshot();
            snapshot.stateName = this.currentState.getName();
            snapshot.childStateNames = new String[this.childStates.size()];

            for (int i = 0; i < snapshot.childStateNames.length; i++) {
                snapshot.childStateNames[i] = this.childStates.get(i).getName();
            }

            return snapshot;
        }
    }

    public boolean setCurrentState(ActionState nextState, ActionTransition transitionUsed) {
        if (nextState == this.currentState) {
            return false;
        } else {
            this.currentState = nextState;
            this.transitionUsedForThisState = transitionUsed;

            for (int i = 0; i < this.childStates.size(); i++) {
                ActionState subState = this.childStates.get(i);
                if (!this.currentState.canHaveSubState(subState)) {
                    this.removeChildStateAt(i);
                    i--;
                }
            }

            return true;
        }
    }

    private boolean tryAddChildState(ActionState nextState) {
        if (this.hasChildState(nextState)) {
            return false;
        } else {
            this.childStates.add(nextState);
            return true;
        }
    }

    private boolean tryInsertChildState(ActionContext actionContext, ActionState nextState) {
        if (this.hasChildState(nextState)) {
            return false;
        } else if (!this.currentStateSupportsChildState(nextState)) {
            DebugType.ActionSystem
                .trace(
                    "%s> Transition's target state \"%s\" not supported by parent: \"%s\"",
                    actionContext.getOwner().getUID(),
                    nextState.getName(),
                    this.currentState.getName()
                );
            return false;
        } else {
            int insertAt = -1;
            ActionState upperState = this.currentState;

            for (int i = 0; i < this.childStates.size(); i++) {
                ActionState lowerState = this.childStates.get(i);
                if (nextState.canHaveSubState(lowerState)) {
                    insertAt = i;
                    break;
                }

                if (!lowerState.canHaveSubState(nextState)) {
                    DebugType.ActionSystem
                        .trace(
                            "%s> Transition's target state \"%s\" not supported by parent: \"%s\"",
                            actionContext.getOwner().getUID(),
                            nextState.getName(),
                            lowerState.getName()
                        );
                    return false;
                }
            }

            if (insertAt > -1) {
                this.childStates.add(insertAt, nextState);
            } else {
                this.childStates.add(nextState);
            }

            return true;
        }
    }

    public void removeChildStateAt(int subStateIdx) {
        this.childStates.remove(subStateIdx);
    }

    public ActionState getRootState() {
        return this.currentState;
    }

    public boolean hasChildStates() {
        return this.childStateCount() > 0;
    }

    public int childStateCount() {
        return this.childStates.size();
    }

    public void foreachChildState(Consumer<ActionState> consumer) {
        for (int i = 0; i < this.childStateCount(); i++) {
            ActionState child = this.getChildStateAt(i);
            consumer.accept(child);
        }
    }

    public int indexOfChildState(Predicate<ActionState> predicate) {
        int indexOf = -1;

        for (int i = 0; i < this.childStateCount(); i++) {
            ActionState child = this.getChildStateAt(i);
            if (predicate.test(child)) {
                indexOf = i;
                break;
            }
        }

        return indexOf;
    }

    public ActionState getChildStateAt(int idx) {
        if (idx >= 0 && idx < this.childStateCount()) {
            return this.childStates.get(idx);
        } else {
            throw new IndexOutOfBoundsException(String.format("Index %d out of bounds. childCount: %d", idx, this.childStateCount()));
        }
    }

    public List<ActionState> getChildStates() {
        return this.childStates;
    }

    public String getCurrentStateName() {
        return this.currentState == null ? null : this.currentState.getName();
    }

    public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        for (int i = this.childStates.size() - 1; i >= 0; i--) {
            ActionState childState = this.childStates.get(i);
            IAnimationVariableSlot childSlot = childState.getVariable(handle);
            if (childSlot != null) {
                return childSlot;
            }
        }

        ActionState currentState = this.getRootState();
        return currentState == null ? null : currentState.getVariable(handle);
    }

    public boolean hasStateVariables() {
        for (int i = this.childStates.size() - 1; i >= 0; i--) {
            ActionState childState = this.childStates.get(i);
            if (childState.hasStateVariables()) {
                return true;
            }
        }

        ActionState currentState = this.getRootState();
        return currentState == null ? false : currentState.hasStateVariables();
    }

    public void set(ActionStateContainer actionStateContainer) {
        this.currentState = actionStateContainer.currentState;
        this.transitionUsedForThisState = actionStateContainer.transitionUsedForThisState;
        PZArrayUtil.copy(this.childStates, actionStateContainer.childStates);
    }

    public void clear() {
        this.currentState = null;
        this.transitionUsedForThisState = null;
        this.childStates.clear();
    }

    public boolean equalTo(ActionStateContainer rhs) {
        return this.currentState == rhs.currentState && this.subStatesEqual(rhs);
    }

    public boolean subStatesEqual(ActionStateContainer rhs) {
        return PZArrayUtil.sequenceEqual(this.childStates, rhs.childStates, PZArrayUtil.Comparators::referencesEqual);
    }

    public ActionTransition getTransitionUsedForThisState() {
        return this.transitionUsedForThisState;
    }
}
