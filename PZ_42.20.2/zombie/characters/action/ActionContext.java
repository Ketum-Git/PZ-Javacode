// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import zombie.ai.states.StateManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.action.conditions.CharacterVariableCondition;
import zombie.characters.action.conditions.EventNotOccurred;
import zombie.characters.action.conditions.EventOccurred;
import zombie.characters.action.conditions.LuaCall;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.debug.DebugType;
import zombie.network.GameClient;
import zombie.util.list.PZArrayUtil;

public final class ActionContext {
    private final IAnimatable owner;
    private ActionGroup actionGroup;
    private final int actionStateHistoryMaxSize = 6;
    private final Stack<ActionState> actionStateHistory = new Stack<>();
    private final ActionStateContainer previousActionStateContainer = new ActionStateContainer();
    private final ActionStateContainer actionStateContainer = new ActionStateContainer();
    private final ActionStateContainer nextActionStateContainer = new ActionStateContainer();
    private boolean statesChanged;
    private final Set<IActionStateChanged> onStateChanged = new HashSet<>();
    private final ActionContextEvents occurredAnimEvents = new ActionContextEvents();
    private final PerformanceProfileProbe updateInternal = new PerformanceProfileProbe("ActionContext.update");
    private final PerformanceProfileProbe postUpdateInternal = new PerformanceProfileProbe("ActionContext.postUpdate");

    public ActionContext(IAnimatable owner) {
        this.owner = owner;
    }

    public IAnimatable getOwner() {
        return this.owner;
    }

    public void update() {
        try (AbstractPerformanceProfileProbe var1 = this.updateInternal.profile()) {
            this.updateInternal();
        }

        try (AbstractPerformanceProfileProbe var8 = this.postUpdateInternal.profile()) {
            this.postUpdateInternal();
        }
    }

    private void updateInternal() {
        this.nextActionStateContainer.set(this.actionStateContainer);
        this.nextActionStateContainer.evaluateCurrentState(this);
        this.transferActionState(this.nextActionStateContainer);
    }

    private void transferActionState(ActionStateContainer nextActionStateContainer) {
        if (!this.actionStateContainer.equalTo(nextActionStateContainer)) {
            this.previousActionStateContainer.set(this.actionStateContainer);
            this.actionStateContainer.set(nextActionStateContainer);
            this.updateHistory(
                this.previousActionStateContainer.getRootState(),
                this.actionStateContainer.getRootState(),
                this.actionStateContainer.getTransitionUsedForThisState()
            );
            boolean rootStateChanged = this.previousActionStateContainer.getRootState() != nextActionStateContainer.getRootState();
            boolean subStatesChanged = !this.previousActionStateContainer.subStatesEqual(nextActionStateContainer);
            if (rootStateChanged) {
                DebugType.ActionSystem
                    .trace("%s>  State changed from \"%s\" to \"%s\",", this.getOwner().getUID(), this.peekPreviousStateName(), this.getCurrentStateName());
                if (GameClient.client) {
                    StateManager.exitState(this.owner, this.previousActionStateContainer.getRootState());
                    StateManager.enterState(this.owner, this.actionStateContainer.getRootState());
                }
            }

            if (subStatesChanged) {
                for (int subStatei = 0; subStatei < this.previousActionStateContainer.childStateCount(); subStatei++) {
                    ActionState oldSubState = this.previousActionStateContainer.getChildStateAt(subStatei);
                    if (!nextActionStateContainer.hasChildState(oldSubState)) {
                        DebugType.ActionSystem.trace("%s> SubState exited. \"%s\"", this.getOwner().getUID(), oldSubState.getName());
                        if (GameClient.client) {
                            StateManager.exitSubState(this.owner, oldSubState);
                        }
                    }
                }

                for (int subStateix = 0; subStateix < nextActionStateContainer.childStateCount(); subStateix++) {
                    ActionState nextSubState = nextActionStateContainer.getChildStateAt(subStateix);
                    if (!this.previousActionStateContainer.hasChildState(nextSubState)) {
                        ActionState upperState = subStateix > 0
                            ? nextActionStateContainer.getChildStateAt(subStateix - 1)
                            : nextActionStateContainer.getRootState();
                        DebugType.ActionSystem
                            .trace(
                                "%s> Transition passes. SubState \"%s\" added to parent state: \"%s\"",
                                this.getOwner().getUID(),
                                nextSubState.getName(),
                                upperState.getName()
                            );
                        if (GameClient.client) {
                            StateManager.enterSubState(this.owner, nextSubState);
                        }
                    }
                }
            }

            this.onStatesChanged();
        }
    }

    private void updateHistory(ActionState previousState, ActionState currentState, ActionTransition transitionUsed) {
        if (currentState == null) {
            DebugType.ActionSystem.error("Current state is null.");
        } else if (previousState != currentState) {
            if (previousState == null) {
                DebugType.ActionSystem.debugln("Previous state null. Resetting history. Entering state: %s", currentState.getName());
                this.actionStateHistory.clear();
            } else {
                if (!this.actionStateHistory.isEmpty() && transitionUsed != null && transitionUsed.transitionOut) {
                    ActionState previousStateInHistory = this.peekPreviousState();
                    if (previousStateInHistory == currentState) {
                        this.popPreviousState();
                        DebugType.ActionSystem.debugln("TransitionOut success. Returning from: %s to: %s", previousState.getName(), currentState.getName());
                        return;
                    }

                    DebugType.ActionSystem
                        .error("TransitionOut mismatch. Previous state \"%s\" != inHistory \"%s\"", currentState.getName(), previousStateInHistory.getName());
                    this.actionStateHistory.clear();
                }

                this.pushPreviousState(previousState);
            }
        }
    }

    private void postUpdateInternal() {
        this.clearActionContextEvents();
        this.invokeAnyStateChangedEvents();
        this.logCurrentState();
    }

    public ActionState peekNextState() {
        return this.actionStateContainer.peekNextState(this);
    }

    public boolean canTransitionToState(String stateName) {
        return this.canTransitionToState(stateName, true);
    }

    public boolean canTransitionToState(String stateName, boolean allowSubState) {
        return this.actionStateContainer.canTransitionToState(this.getGroup(), stateName, allowSubState);
    }

    public void setPlaybackStateSnapshot(ActionStateSnapshot snapshot) {
        this.nextActionStateContainer.clear();
        this.nextActionStateContainer.setPlaybackStateSnapshot(this, snapshot);
        this.transferActionState(this.nextActionStateContainer);
    }

    public ActionStateSnapshot getPlaybackStateSnapshot() {
        return this.actionStateContainer.getPlaybackStateSnapshot();
    }

    public void setCurrentState(ActionState nextState) {
        this.nextActionStateContainer.clear();
        this.nextActionStateContainer.setCurrentState(nextState, null);
        this.transferActionState(this.nextActionStateContainer);
    }

    private void onStatesChanged() {
        this.statesChanged = true;
    }

    public void logCurrentState() {
        if (this.owner.isAnimationRecorderActive()) {
            this.owner
                .getAnimationRecorder()
                .logActionState(this.actionGroup, this.actionStateContainer.getRootState(), this.actionStateContainer.getChildStates());
            this.owner
                .getAnimationRecorder()
                .logVariable("actionStateHistory", PZArrayUtil.arrayToString(this.actionStateHistory, ActionState::getName, "", "", ";"));
        }
    }

    private void invokeAnyStateChangedEvents() {
        if (this.statesChanged) {
            this.statesChanged = false;

            for (IActionStateChanged callback : this.onStateChanged) {
                callback.actionStateChanged(this);
            }

            if (this.owner instanceof IsoZombie isoZombie) {
                isoZombie.getNetworkCharacterAI().extraUpdate();
            }
        }
    }

    public void clearActionContextEvents() {
        this.occurredAnimEvents.clear();
    }

    public ActionState getCurrentState() {
        return this.actionStateContainer.getRootState();
    }

    public void setGroup(ActionGroup group) {
        this.actionGroup = group;
        this.setCurrentState(group.getInitialState());
    }

    public ActionGroup getGroup() {
        return this.actionGroup;
    }

    public void reportEvent(String event) {
        this.reportEvent(null, event);
    }

    public void reportEvent(String state, String event) {
        this.occurredAnimEvents.add(event, state);
        if (state == null && GameClient.client && this.owner instanceof IsoPlayer player && player.isLocalPlayer()) {
            player.getNetworkCharacterAI().getState().reportEvent(state, event);
        }
    }

    public ActionState getChildStateAt(int idx) {
        return this.actionStateContainer.getChildStateAt(idx);
    }

    public List<ActionState> getChildStates() {
        return this.actionStateContainer.getChildStates();
    }

    public String getCurrentStateName() {
        return this.actionStateContainer.getRootState() != null
            ? this.actionStateContainer.getCurrentStateName()
            : this.actionGroup.getDefaultState().getName();
    }

    public String peekPreviousStateName() {
        return this.getStateNameOrDefault(this.peekPreviousState());
    }

    public ActionState popPreviousState() {
        return !this.actionStateHistory.isEmpty() ? this.actionStateHistory.pop() : null;
    }

    public ActionState peekPreviousState() {
        return !this.actionStateHistory.isEmpty() ? this.actionStateHistory.peek() : null;
    }

    private void pushPreviousState(ActionState currentState) {
        if (currentState != null && this.peekPreviousState() != currentState) {
            this.actionStateHistory.push(currentState);
        }

        while (this.actionStateHistory.size() >= 6) {
            this.actionStateHistory.removeFirst();
        }
    }

    private String getStateNameOrDefault(ActionState previousState) {
        return previousState != null ? previousState.getName() : this.actionGroup.getDefaultState().getName();
    }

    /**
     * Returns TRUE if an event has occurred on any layer.
     */
    public boolean hasEventOccurred(String eventName) {
        return this.hasEventOccurred(eventName, null);
    }

    public boolean hasEventOccurred(String eventName, String stateName) {
        return this.occurredAnimEvents.contains(eventName, stateName);
    }

    public void clearEvent(String eventName) {
        this.occurredAnimEvents.clearEvent(eventName);
    }

    public void getEvents(HashMap<String, String> events) {
        this.occurredAnimEvents.get(events);
    }

    public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        return this.actionStateContainer.getVariable(handle);
    }

    public boolean hasStateVariables() {
        return this.actionStateContainer.hasStateVariables();
    }

    public void addOnStateChanged(IActionStateChanged callback) {
        this.onStateChanged.add(callback);
    }

    static {
        CharacterVariableCondition.Factory factory = new CharacterVariableCondition.Factory();
        IActionCondition.registerFactory("isTrue", factory);
        IActionCondition.registerFactory("isFalse", factory);
        IActionCondition.registerFactory("compare", factory);
        IActionCondition.registerFactory("gtr", factory);
        IActionCondition.registerFactory("less", factory);
        IActionCondition.registerFactory("equals", factory);
        IActionCondition.registerFactory("lessEqual", factory);
        IActionCondition.registerFactory("gtrEqual", factory);
        IActionCondition.registerFactory("notEquals", factory);
        IActionCondition.registerFactory("eventOccurred", new EventOccurred.Factory());
        IActionCondition.registerFactory("eventNotOccurred", new EventNotOccurred.Factory());
        IActionCondition.registerFactory("lua", new LuaCall.Factory());
    }
}
