// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.ai.states.FitnessState;
import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterExercising extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterExercising(IsoGameCharacter character) {
        super("Exercising");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        if (!this.character.isCurrentState(FitnessState.instance())) {
            return 0.0F;
        } else {
            return !this.character.getVariableBoolean("ExerciseStarted") ? 0.0F : 1.0F;
        }
    }
}
