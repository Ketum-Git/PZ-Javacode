// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.player;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public class PlayerPetAnimalState extends State {
    private static final PlayerPetAnimalState instance = new PlayerPetAnimalState();
    private static final Integer PARAM_PET_ANIMAL = 0;
    private static final Integer PARAM_ANIMAL = 1;
    private static final Integer PARAM_ANIMAL_SIZE = 2;

    public static PlayerPetAnimalState instance() {
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
            StateMachineParams.put(PARAM_PET_ANIMAL, owner.getVariableBoolean("petanimal"));
            StateMachineParams.put(PARAM_ANIMAL, owner.getVariableString("animal"));
            StateMachineParams.put(PARAM_ANIMAL_SIZE, owner.getVariableFloat("AnimalSizeY", 0.01F));
        } else {
            owner.setVariable("petanimal", (Boolean)StateMachineParams.get(PARAM_PET_ANIMAL));
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
