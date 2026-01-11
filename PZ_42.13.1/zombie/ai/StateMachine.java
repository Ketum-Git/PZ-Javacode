// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import java.util.ArrayList;
import java.util.List;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.StateManager;
import zombie.ai.states.SwipeStatePlayer;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
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

    public void changeState(State newState, Iterable<State> subStates) {
        this.changeState(newState, subStates, false);
    }

    public void changeState(State newState, Iterable<State> subStates, boolean restart) {
        if (!this.isLocked) {
            this.changeRootState(newState, restart);
            PZArrayUtil.forEach(this.subStates, subStateSlot -> subStateSlot.shouldBeActive = false);
            PZArrayUtil.forEach(subStates, Lambda.consumer(this, (subState, l_this) -> {
                if (subState != null) {
                    l_this.ensureSubstateActive(subState);
                }
            }));
            Lambda.forEachFrom(PZArrayUtil::forEach, this.subStates, this, (subStateSlot, l_this) -> {
                if (!subStateSlot.shouldBeActive && !subStateSlot.isEmpty()) {
                    l_this.removeSubstate(subStateSlot);
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

            LuaEventManager.triggerEvent("OnAIStateChange", this.owner, this.currentState, this.previousState);
        }
    }

    private void ensureSubstateActive(State subState) {
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

    private StateMachine.SubstateSlot getExistingSlot(State subState) {
        return PZArrayUtil.find(this.subStates, Lambda.predicate(subState, (s, l_subState) -> s.getState() == l_subState));
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
        if (subState != this.currentState || subState != SwipeStatePlayer.instance()) {
            this.stateExit(subState);
        }
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
            DebugLog.ActionSystem.warn("The sender $s is not an active state in this state machine.", String.valueOf(sender));
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
        if (this.owner.isAnimationRecorderActive()) {
            this.owner.getAnimationPlayerRecorder().logAIState(this.currentState, this.subStates);
        }
    }

    private void stateExecute(State state) {
        try {
            state.execute(this.owner);
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, "State execute error: " + state.getName(), LogSeverity.Error);
        }
    }

    private void stateEnter(State state) {
        try {
            state.enter(this.owner);
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, "State enter error: " + state.getName(), LogSeverity.Error);
        }

        if (GameClient.client) {
            if (this.owner.getStateMachine().isSubstate(state)) {
                StateManager.enterSubState(this.owner, state);
            } else {
                StateManager.enterState(this.owner, state);
            }
        }
    }

    private void stateExit(State state) {
        try {
            state.exit(this.owner);
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, "State exit error: " + state.getName(), LogSeverity.Error);
        }

        if (this.owner.getStateMachine().isSubstate(state)) {
            StateManager.exitSubState(this.owner, state);
        } else {
            StateManager.exitState(this.owner, state);
        }
    }

    public final void stateAnimEvent(int stateLayer, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (stateLayer <= 0) {
            if (this.currentState != null) {
                this.currentState.animEvent(this.owner, layer, track, event);
            }

            if (stateLayer == 0) {
                return;
            }
        }

        Lambda.forEachFrom(PZArrayUtil::forEach, this.subStates, this.owner, layer, track, event, (subState, l_owner, l_layer, l_track, l_event) -> {
            if (!subState.isEmpty()) {
                subState.state.animEvent(l_owner, l_layer, l_track, l_event);
            }
        });
    }

    public boolean isLocked() {
        return this.isLocked;
    }

    public void setLocked(boolean lock) {
        this.isLocked = lock;
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
