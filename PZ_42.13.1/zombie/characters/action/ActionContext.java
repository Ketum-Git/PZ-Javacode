// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ActionContext {
    private final IAnimatable owner;
    private ActionGroup actionGroup;
    private ActionState currentState;
    private final ArrayList<ActionState> childStates = new ArrayList<>();
    private String previousStateName;
    private boolean statesChanged;
    public final ArrayList<IActionStateChanged> onStateChanged = new ArrayList<>();
    private final ActionContextEvents occurredAnimEvents = new ActionContextEvents();
    private final PerformanceProfileProbe updateInternal = new PerformanceProfileProbe("ActionContext.update");
    private final PerformanceProfileProbe postUpdateInternal = new PerformanceProfileProbe("ActionContext.postUpdate");
    private final PerformanceProfileProbe evaluateCurrentStateTransitions = new PerformanceProfileProbe("ActionContext.evaluateCurrentStateTransitions");
    private final PerformanceProfileProbe evaluateSubStateTransitions = new PerformanceProfileProbe("ActionContext.evaluateSubStateTransitions");

    public ActionContext(IAnimatable owner) {
        this.owner = owner;
    }

    public IAnimatable getOwner() {
        return this.owner;
    }

    public void update() {
        try (AbstractPerformanceProfileProbe ignored = this.updateInternal.profile()) {
            this.updateInternal();
        }

        try (AbstractPerformanceProfileProbe ignored = this.postUpdateInternal.profile()) {
            this.postUpdateInternal();
        }
    }

    private void updateInternal() {
        if (this.currentState != null) {
            try (AbstractPerformanceProfileProbe ignored = this.evaluateCurrentStateTransitions.profile()) {
                this.evaluateCurrentStateTransitions();
            }

            try (AbstractPerformanceProfileProbe ignored = this.evaluateSubStateTransitions.profile()) {
                this.evaluateSubStateTransitions();
            }
        }
    }

    private void postUpdateInternal() {
        this.clearActionContextEvents();
        this.invokeAnyStateChangedEvents();
        this.logCurrentState();
    }

    public ActionState peekNextState() {
        ActionState result = null;

        for (int i = 0; i < this.currentState.transitions.size(); i++) {
            ActionTransition nextTransition = this.currentState.transitions.get(i);
            String transitionTo = this.getTransitionTo(nextTransition);
            if (!StringUtils.isNullOrWhitespace(transitionTo) && nextTransition.passes(this, this.currentState)) {
                ActionState nextState = this.actionGroup.findState(transitionTo);
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
                if (subTransition.passes(this, subState)) {
                    if (subTransition.transitionOut) {
                        break;
                    }

                    String transitionTo = this.getTransitionTo(subTransition);
                    if (!StringUtils.isNullOrWhitespace(transitionTo)) {
                        ActionState nextSubState = this.actionGroup.findState(transitionTo);
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

    private String getTransitionTo(ActionTransition in_transition) {
        return in_transition.transitionOut ? this.getPreviousStateName() : in_transition.transitionTo;
    }

    private void evaluateCurrentStateTransitions() {
        for (int i = 0; i < this.currentState.transitions.size(); i++) {
            ActionTransition nextTransition = this.currentState.transitions.get(i);
            if (!nextTransition.asSubstate) {
                String transitionTo = this.getTransitionTo(nextTransition);
                if (StringUtils.isNullOrWhitespace(transitionTo)) {
                    DebugLog.ActionSystem.warn("%s> Transition's target state not specified: \"%s\"", this.getOwner().getUID(), transitionTo);
                } else if (nextTransition.passes(this, this.currentState)) {
                    ActionState nextState = this.actionGroup.findState(transitionTo);
                    if (nextState != null) {
                        if (this.owner instanceof IsoPlayer isoPlayer) {
                            DebugLog.DetailedInfo
                                .trace("Player '%s' transits from %s to %s", isoPlayer.getUsername(), this.currentState.getName(), transitionTo);
                        }

                        this.setCurrentState(nextState);
                        break;
                    }

                    DebugLog.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", this.getOwner().getUID(), transitionTo);
                }
            }
        }

        for (int ix = 0; ix < this.currentState.transitions.size(); ix++) {
            ActionTransition nextTransition = this.currentState.transitions.get(ix);
            if (nextTransition.asSubstate) {
                String transitionTo = this.getTransitionTo(nextTransition);
                if (StringUtils.isNullOrWhitespace(transitionTo)) {
                    DebugLog.ActionSystem.warn("%s> Transition's target state not specified: \"%s\"", this.getOwner().getUID(), transitionTo);
                } else if (nextTransition.passes(this, this.currentState)) {
                    ActionState nextSubState = this.actionGroup.findState(transitionTo);
                    if (nextSubState == null) {
                        DebugLog.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", this.getOwner().getUID(), transitionTo);
                    } else if (!this.hasChildState(nextSubState)) {
                        if (!this.currentStateSupportsChildState(nextSubState)) {
                            DebugLog.ActionSystem
                                .warn(
                                    "%s> Transition's target state \"%s\" not supported by parent: \"%s\"",
                                    this.getOwner().getUID(),
                                    transitionTo,
                                    this.currentState.getName()
                                );
                        } else {
                            this.tryInsertChildState(nextSubState);
                        }
                    }
                }
            }
        }
    }

    private void evaluateSubStateTransitions() {
        for (int subStateIdx = 0; subStateIdx < this.childStateCount(); subStateIdx++) {
            ActionState nextState = null;
            ActionState subState = this.getChildStateAt(subStateIdx);

            for (int transIdx = 0; transIdx < subState.transitions.size(); transIdx++) {
                ActionTransition subTransition = subState.transitions.get(transIdx);
                if (subTransition.passes(this, subState)) {
                    if (subTransition.transitionOut) {
                        this.removeChildStateAt(subStateIdx);
                        subStateIdx--;
                        break;
                    }

                    if (!StringUtils.isNullOrWhitespace(subTransition.transitionTo)) {
                        ActionState nextSubState = this.actionGroup.findState(subTransition.transitionTo);
                        if (nextSubState == null) {
                            DebugLog.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", this.getOwner().getUID(), subTransition.transitionTo);
                        } else if (!this.hasChildState(nextSubState)) {
                            if (this.currentStateSupportsChildState(nextSubState)) {
                                ActionState previousSubState = this.childStates.set(subStateIdx, nextSubState);
                                this.onStatesChanged();
                                if (GameClient.client) {
                                    StateManager.exitSubState(this.owner, previousSubState);
                                    StateManager.enterSubState(this.owner, nextSubState);
                                }

                                DebugLog.ActionSystem
                                    .trace(
                                        "%s> Transition passes. SubState \"%s\" replaced with: \"%s\"",
                                        this.getOwner().getUID(),
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
                this.setCurrentState(nextState);
            }
        }
    }

    public boolean canTransitionToState(String in_stateName) {
        return this.canTransitionToState(in_stateName, true);
    }

    public boolean canTransitionToState(String in_stateName, boolean in_allowSubState) {
        ActionState nextState = this.actionGroup.findState(in_stateName);
        if (nextState == null) {
            return false;
        } else {
            for (int i = 0; i < this.currentState.transitions.size(); i++) {
                ActionTransition nextTransition = this.currentState.transitions.get(i);
                if (StringUtils.equalsIgnoreCase(in_stateName, nextTransition.transitionTo)) {
                    return true;
                }
            }

            if (!in_allowSubState) {
                return false;
            } else if (!this.currentStateSupportsChildState(nextState)) {
                return false;
            } else {
                for (int subStateIdx = 0; subStateIdx < this.childStateCount(); subStateIdx++) {
                    ActionState subState = this.getChildStateAt(subStateIdx);

                    for (int transIdx = 0; transIdx < subState.transitions.size(); transIdx++) {
                        ActionTransition subTransition = subState.transitions.get(transIdx);
                        if (!subTransition.transitionOut && StringUtils.equalsIgnoreCase(in_stateName, subTransition.transitionTo)) {
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

    private boolean hasChildState(ActionState child) {
        int indexOf = this.indexOfChildState(state -> state == child);
        return indexOf > -1;
    }

    public void setPlaybackStateSnapshot(ActionStateSnapshot snapshot) {
        if (this.actionGroup != null) {
            if (snapshot.stateName == null) {
                DebugLog.General.warn("Snapshot not valid. Missing root state name.");
            } else {
                ActionState rootState = this.actionGroup.findState(snapshot.stateName);
                this.setCurrentState(rootState);
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
                        ActionState childState = this.actionGroup.findState(childName);
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

    public boolean setCurrentState(ActionState nextState) {
        if (nextState == this.currentState) {
            return false;
        } else {
            if (GameClient.client) {
                StateManager.exitState(this.owner, this.currentState);
                StateManager.enterState(this.owner, nextState);
            }

            this.previousStateName = this.currentState == null ? "" : this.currentState.getName();
            this.currentState = nextState;
            DebugLog.ActionSystem
                .trace("%s>  State changed from \"%s\" to \"%s\",", this.getOwner().getUID(), this.previousStateName, this.currentState.getName());

            for (int i = 0; i < this.childStates.size(); i++) {
                ActionState subState = this.childStates.get(i);
                if (!this.currentState.canHaveSubState(subState)) {
                    this.removeChildStateAt(i);
                    i--;
                }
            }

            this.onStatesChanged();
            return true;
        }
    }

    protected boolean tryAddChildState(ActionState in_nextState) {
        if (this.hasChildState(in_nextState)) {
            return false;
        } else {
            this.childStates.add(in_nextState);
            this.onStatesChanged();
            return true;
        }
    }

    public boolean tryInsertChildState(ActionState in_nextState) {
        if (this.hasChildState(in_nextState)) {
            return false;
        } else {
            int insertAt = -1;
            ActionState upperState = this.currentState;

            for (int i = 0; i < this.childStates.size(); i++) {
                ActionState lowerState = this.childStates.get(i);
                if (upperState.canHaveSubState(in_nextState) && in_nextState.canHaveSubState(lowerState)) {
                    insertAt = i;
                    break;
                }

                upperState = lowerState;
            }

            if (insertAt > -1) {
                this.childStates.add(insertAt, in_nextState);
            } else {
                this.childStates.add(in_nextState);
            }

            if (GameClient.client) {
                StateManager.enterSubState(this.owner, in_nextState);
            }

            DebugLog.ActionSystem
                .trace(
                    "%s> Transition passes. SubState \"%s\" added to parent state: \"%s\"",
                    this.getOwner().getUID(),
                    in_nextState.getName(),
                    upperState.getName()
                );
            this.onStatesChanged();
            return true;
        }
    }

    public void removeChildStateAt(int subStateIdx) {
        ActionState child = this.childStates.remove(subStateIdx);
        DebugLog.ActionSystem.trace("%s>  SubState \"%s\" removed,", this.getOwner().getUID(), child.getName());
        this.onStatesChanged();
        if (GameClient.client) {
            StateManager.exitSubState(this.owner, child);
        }
    }

    private void onStatesChanged() {
        this.statesChanged = true;
    }

    public void logCurrentState() {
        if (this.owner.isAnimationRecorderActive()) {
            this.owner.getAnimationPlayerRecorder().logActionState(this.actionGroup, this.currentState, this.childStates);
        }
    }

    private void invokeAnyStateChangedEvents() {
        if (this.statesChanged) {
            this.statesChanged = false;

            for (int i = 0; i < this.onStateChanged.size(); i++) {
                IActionStateChanged callback = this.onStateChanged.get(i);
                callback.actionStateChanged(this);
            }

            if (this.owner instanceof IsoZombie isoZombie) {
                isoZombie.networkAi.extraUpdate();
            }
        }
    }

    public void clearActionContextEvents() {
        this.occurredAnimEvents.clear();
    }

    public ActionState getCurrentState() {
        return this.currentState;
    }

    public void setGroup(ActionGroup group) {
        String oldState = this.currentState == null ? null : this.currentState.getName();
        this.actionGroup = group;
        ActionState newState = group.getInitialState();
        if (!StringUtils.equalsIgnoreCase(oldState, newState.getName())) {
            this.setCurrentState(newState);
        } else {
            this.currentState = newState;
        }
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

    public final boolean hasChildStates() {
        return this.childStateCount() > 0;
    }

    public final int childStateCount() {
        return this.childStates != null ? this.childStates.size() : 0;
    }

    public final void foreachChildState(Consumer<ActionState> consumer) {
        for (int i = 0; i < this.childStateCount(); i++) {
            ActionState child = this.getChildStateAt(i);
            consumer.accept(child);
        }
    }

    public final int indexOfChildState(Predicate<ActionState> predicate) {
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

    public final ActionState getChildStateAt(int idx) {
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
        return this.currentState == null ? this.actionGroup.getDefaultState().getName() : this.currentState.getName();
    }

    public String getPreviousStateName() {
        return this.previousStateName;
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

    public IAnimationVariableSlot getVariable(AnimationVariableHandle in_handle) {
        for (int i = this.childStates.size() - 1; i >= 0; i--) {
            ActionState childState = this.childStates.get(i);
            IAnimationVariableSlot childSlot = childState.getVariable(in_handle);
            if (childSlot != null) {
                return childSlot;
            }
        }

        ActionState currentState = this.getCurrentState();
        return currentState == null ? null : currentState.getVariable(in_handle);
    }

    public boolean hasStateVariables() {
        for (int i = this.childStates.size() - 1; i >= 0; i--) {
            ActionState childState = this.childStates.get(i);
            if (childState.hasStateVariables()) {
                return true;
            }
        }

        ActionState currentState = this.getCurrentState();
        return currentState == null ? false : currentState.hasStateVariables();
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
