// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.player;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public class PlayerShearAnimalState extends State {
    private static final PlayerShearAnimalState INSTANCE = new PlayerShearAnimalState();
    public static final State.Param<Boolean> SHEAR_ANIMAL = State.Param.ofBool("shear_animal", false);
    public static final State.Param<String> ANIMAL = State.Param.ofString("animal", "");
    public static final State.Param<Float> ANIMAL_SIZE = State.Param.ofFloat("animal_size", 0.01F);

    public static PlayerShearAnimalState instance() {
        return INSTANCE;
    }

    private PlayerShearAnimalState() {
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
            owner.set(SHEAR_ANIMAL, owner.getVariableBoolean("shearanimal"));
            owner.set(ANIMAL, owner.getVariableString("animal"));
            owner.set(ANIMAL_SIZE, owner.getVariableFloat("AnimalSizeY", 0.01F));
        } else {
            owner.setVariable("shearanimal", owner.get(SHEAR_ANIMAL));
            owner.setVariable("animal", owner.get(ANIMAL));
            owner.setVariable("AnimalSizeY", owner.get(ANIMAL_SIZE));
        }

        super.setParams(owner, stage);
    }
}
