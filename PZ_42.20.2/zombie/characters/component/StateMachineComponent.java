// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.characters.IsoGameCharacter;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.ecs.ECSComponent;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.events.GlobalAnimEvent;
import zombie.debug.DebugType;
import zombie.util.list.PZArrayUtil;

public class StateMachineComponent extends ECSComponent {
    private final StateMachine stateMachine;
    private State defaultState;
    private final HashMap<String, State> aiStateMap = new HashMap<>();
    private final Map<Class<?>, Map<State.Param<?>, Object>> stateMachineParams = new IdentityHashMap<>();
    private final AdvancedAnimator advancedAnimator;
    private final ActionContext actionContext;

    public StateMachineComponent(IsoGameCharacter owner) {
        this.stateMachine = new StateMachine(owner);
        this.advancedAnimator = new AdvancedAnimator();
        this.actionContext = new ActionContext(owner);
    }

    public StateMachine getStateMachine() {
        return this.stateMachine;
    }

    public Map<State.Param<?>, Object> getStateMachineParams(Class<?> clazz) {
        return this.stateMachineParams.computeIfAbsent(clazz, k -> new IdentityHashMap<>());
    }

    public AdvancedAnimator getAdvancedAnimator() {
        return this.advancedAnimator;
    }

    public ActionContext getActionContext() {
        return this.actionContext;
    }

    public void invokeGlobalAnimEvent(GlobalAnimEvent animEvent) {
        this.advancedAnimator.invokeGlobalAnimEvent(animEvent);
    }

    public void init(IsoGameCharacter owner) {
        this.advancedAnimator.init(owner, owner.GetAnimSetName());
        this.actionContext.addOnStateChanged(this::actionStateChanged);
        this.actionContext.addOnStateChanged(owner);
    }

    private void actionStateChanged(ActionContext sender) {
        DebugType.AnimationRecorder.debugln("* rootState: %s", sender.getCurrentStateName());
        ArrayList<String> stateNames = StateMachineComponent.L_actionStateChanged.stateNames;
        PZArrayUtil.listConvert(sender.getChildStates(), stateNames, ActionState::getName);

        for (String stateName : stateNames) {
            DebugType.AnimationRecorder.debugln("  +- subState: %s", stateName);
        }

        this.getAdvancedAnimator().setState(sender.getCurrentStateName(), stateNames);

        try {
            this.getStateMachine().activeStateChanged++;
            State aiState = this.tryGetAIState(sender.getCurrentStateName());
            if (aiState == null) {
                aiState = this.defaultState;
            }

            ArrayList<State> childStates = StateMachineComponent.L_actionStateChanged.states;
            PZArrayUtil.listConvert(sender.getChildStates(), childStates, this.aiStateMap, (state2, lLookup) -> lLookup.get(state2.getName().toLowerCase()));
            this.getStateMachine().changeState(aiState, childStates, false);
        } finally {
            this.getStateMachine().activeStateChanged--;
        }
    }

    public void setDefaultState() {
        this.getStateMachine().changeState(this.defaultState, null, false);
    }

    public void setDefaultState(State defaultState) {
        this.defaultState = defaultState;
    }

    public State tryGetAIState(String stateName) {
        return this.aiStateMap.get(stateName.toLowerCase(Locale.ENGLISH));
    }

    public void clearAIStateMap() {
        this.aiStateMap.clear();
    }

    public void registerAIState(String name, State aiState) {
        this.aiStateMap.put(name.toLowerCase(Locale.ENGLISH), aiState);
    }

    public State getDefaultState() {
        return this.defaultState;
    }

    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return this.stateMachine.getMinimumSimulationLevel();
    }

    private static final class L_actionStateChanged {
        private static final ArrayList<String> stateNames = new ArrayList<>();
        private static final ArrayList<State> states = new ArrayList<>();
    }
}
