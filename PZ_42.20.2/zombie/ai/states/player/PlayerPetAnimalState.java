// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.player;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public class PlayerPetAnimalState extends State {
    private static final PlayerPetAnimalState INSTANCE = new PlayerPetAnimalState();
    public static final State.Param<Boolean> PET_ANIMAL = State.Param.ofBool("pet_animal", false);
    public static final State.Param<String> ANIMAL = State.Param.ofString("animal", "");
    public static final State.Param<Float> ANIMAL_SIZE = State.Param.ofFloat("animal_size", 0.01F);

    public static PlayerPetAnimalState instance() {
        return INSTANCE;
    }

    private PlayerPetAnimalState() {
        super(true, true, false, false);
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
        if (owner.isLocal()) {
            owner.set(PET_ANIMAL, owner.getVariableBoolean("petanimal"));
            owner.set(ANIMAL, owner.getVariableString("animal"));
            owner.set(ANIMAL_SIZE, owner.getVariableFloat("AnimalSizeY", 0.01F));
        } else {
            owner.setVariable("petanimal", owner.get(PET_ANIMAL));
            owner.setVariable("animal", owner.get(ANIMAL));
            owner.setVariable("AnimalSizeY", owner.get(ANIMAL_SIZE));
        }

        super.setParams(owner, stage);
    }
}
