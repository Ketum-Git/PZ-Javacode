// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.player;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public class PlayerShearAnimalState extends State {
    private static final PlayerShearAnimalState instance = new PlayerShearAnimalState();
    private static final Integer PARAM_SHEAR_ANIMAL = 0;
    private static final Integer PARAM_ANIMAL = 1;
    private static final Integer PARAM_ANIMAL_SIZE = 2;

    public static PlayerShearAnimalState instance() {
        return instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_SHEAR_ANIMAL, owner.getVariableBoolean("shearanimal"));
            StateMachineParams.put(PARAM_ANIMAL, owner.getVariableString("animal"));
            StateMachineParams.put(PARAM_ANIMAL_SIZE, owner.getVariableFloat("AnimalSizeY", 0.01F));
        } else {
            owner.setVariable("shearanimal", (Boolean)StateMachineParams.get(PARAM_SHEAR_ANIMAL));
            owner.setVariable("animal", (String)StateMachineParams.get(PARAM_ANIMAL));
            owner.setVariable("AnimalSizeY", (Float)StateMachineParams.get(PARAM_ANIMAL_SIZE));
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isSyncOnEnter() {
        return true;
    }

    @Override
    public boolean isSyncOnExit() {
        return true;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }
}
