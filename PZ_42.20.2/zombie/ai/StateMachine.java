// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import java.util.ArrayList;
import java.util.List;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.StateManager;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;
import zombie.util.Lambda;
import zombie.util.list.PZArrayUtil;

public final class StateMachine {
    private boolean isLocked;
    public int activeStateChanged;
    private State currentState;
    private State previousState;
    private final IsoGameCharacter owner;
    private final List<StateMachine.SubstateSlot> subStates = new ArrayList<>();

    public StateMachine(IsoGameCharacter owner) {
        this.owner = owner;
    }

    public void changeState(State newState, Iterable<State> subStates, boolean restart) {
        if (this.isLocked) {
            DebugType.Action.warn("StateMachine is locked. Cannot change state to: %s", newState);
        } else {
            this.changeRootState(newState, restart);
            PZArrayUtil.forEach(this.subStates, subStateSlot -> subStateSlot.shouldBeActive = false);
            PZArrayUtil.forEach(subStates, Lambda.consumer(this, (subState, lThis) -> {
                if (subState != null) {
                    lThis.ensureSubstateActive(subState);
                }
            }));
            Lambda.forEachFrom(PZArrayUtil::forEach, this.subStates, this, (subStateSlot, lThis) -> {
                if (!subStateSlot.shouldBeActive && !subStateSlot.isEmpty()) {
                    lThis.removeSubstate(subStateSlot);
                }
            });
        }
    }

    private void changeRootState(State newState, boolean restart) {
        if (this.currentState == newState) {
            if (restart) {
                this.stateEnter(this.currentState);
            }
        } else {
            State previousState = this.currentState;
            if (previousState != null) {
                this.stateExit(previousState);
            }

            this.previousState = previousState;
            this.currentState = newState;
            if (newState != null) {
                this.stateEnter(newState);
            }

            LuaEventManager.triggerEvent("OnAIStateChange", this.getOwner(), this.currentState, this.previousState);
        }
    }

    private void ensureSubstateActive(State subState) {
        if (subState != this.currentState) {
            StateMachine.SubstateSlot existingSlot = this.getExistingSlot(subState);
            if (existingSlot != null) {
                existingSlot.shouldBeActive = true;
            } else {
                StateMachine.SubstateSlot emptySlot = PZArrayUtil.find(this.subStates, StateMachine.SubstateSlot::isEmpty);
                if (emptySlot != null) {
                    emptySlot.setState(subState);
                    emptySlot.shouldBeActive = true;
                } else {
                    StateMachine.SubstateSlot newSlot = new StateMachine.SubstateSlot(subState);
                    this.subStates.add(newSlot);
                }

                this.stateEnter(subState);
            }
        }
    }

    private StateMachine.SubstateSlot getExistingSlot(State subState) {
        return PZArrayUtil.find(this.subStates, Lambda.predicate(subState, (s, lSubState) -> s.getState() == lSubState));
    }

    private void removeSubstate(State substate) {
        StateMachine.SubstateSlot slot = this.getExistingSlot(substate);
        if (slot != null) {
            this.removeSubstate(slot);
        }
    }

    private void removeSubstate(StateMachine.SubstateSlot substateSlot) {
        State subState = substateSlot.getState();
        substateSlot.setState(null);
        this.stateExit(subState);
    }

    public boolean isSubstate(State substate) {
        for (int i = 0; i < this.subStates.size(); i++) {
            StateMachine.SubstateSlot slot = this.subStates.get(i);
            if (slot.getState() == substate) {
                return true;
            }
        }

        return false;
    }

    public State getCurrent() {
        return this.currentState;
    }

    public State getPrevious() {
        return this.previousState;
    }

    public int getSubStateCount() {
        return this.subStates.size();
    }

    public State getSubStateAt(int idx) {
        return this.subStates.get(idx).getState();
    }

    public void revertToPreviousState(State sender) {
        if (this.isSubstate(sender)) {
            this.removeSubstate(sender);
        } else if (this.currentState != sender) {
            DebugType.ActionSystem.warn("The sender %s is not an active state in this state machine.", String.valueOf(sender));
        } else {
            this.changeRootState(this.previousState, false);
        }
    }

    public void update() {
        if (this.currentState != null) {
            this.stateExecute(this.currentState);
        }

        for (int i = 0; i < this.subStates.size(); i++) {
            StateMachine.SubstateSlot subState = this.subStates.get(i);
            if (!subState.isEmpty()) {
                this.stateExecute(subState.state);
            }
        }

        this.logCurrentState();
    }

    private void logCurrentState() {
        if (this.getOwner().isAnimationRecorderActive()) {
            this.getOwner().getAnimationRecorder().logAIState(this.currentState, this.subStates);
        }
    }

    private void stateExecute(State state) {
        try {
            state.execute(this.getOwner());
        } catch (Exception var3) {
            DebugType.Multiplayer.printException(var3, "State execute error: " + state.getName(), LogSeverity.Error);
        }
    }

    private void stateEnter(State state) {
        try {
            state.enter(this.getOwner());
        } catch (Exception var3) {
            DebugType.Multiplayer.printException(var3, "State enter error: " + state.getName(), LogSeverity.Error);
        }

        if (GameClient.client) {
            if (this.getOwner().getStateMachine().isSubstate(state)) {
                StateManager.enterSubState(this.getOwner(), state);
            } else {
                StateManager.enterState(this.getOwner(), state);
            }
        }
    }

    private void stateExit(State state) {
        try {
            state.exit(this.getOwner());
        } catch (Exception var3) {
            DebugType.Multiplayer.printException(var3, "State exit error: " + state.getName(), LogSeverity.Error);
        }

        if (GameClient.client) {
            if (this.getOwner().getStateMachine().isSubstate(state)) {
                StateManager.exitSubState(this.getOwner(), state);
            } else {
                StateManager.exitState(this.getOwner(), state);
            }
        }
    }

    public final void stateAnimEvent(int stateLayer, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (stateLayer <= 0) {
            if (this.currentState != null) {
                this.currentState.animEvent(this.getOwner(), layer, track, event);
            }

            if (stateLayer == 0) {
                return;
            }
        }

        Lambda.forEachFrom(PZArrayUtil::forEach, this.subStates, this.getOwner(), layer, track, event, (subState, lOwner, lLayer, lTrack, lEvent) -> {
            if (!subState.isEmpty()) {
                subState.state.animEvent(lOwner, lLayer, lTrack, lEvent);
            }
        });
    }

    public boolean isLocked() {
        return this.isLocked;
    }

    public void setLocked(boolean lock) {
        this.isLocked = lock;
    }

    public IsoGameCharacter getOwner() {
        return this.owner;
    }

    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        if (this.currentState == null) {
            return UpdateSchedulerSimulationLevel.minimum();
        } else {
            UpdateSchedulerSimulationLevel minLevel = this.currentState.getMinimumSimulationLevel();

            for (StateMachine.SubstateSlot subStateSlot : this.subStates) {
                if (!subStateSlot.isEmpty()) {
                    minLevel = minLevel.max(subStateSlot.getState().getMinimumSimulationLevel());
                }
            }

            return minLevel;
        }
    }

    public static class SubstateSlot {
        private State state;
        boolean shouldBeActive;

        SubstateSlot(State state) {
            this.state = state;
            this.shouldBeActive = true;
        }

        public State getState() {
            return this.state;
        }

        void setState(State state) {
            this.state = state;
        }

        public boolean isEmpty() {
            return this.state == null;
        }
    }
}
