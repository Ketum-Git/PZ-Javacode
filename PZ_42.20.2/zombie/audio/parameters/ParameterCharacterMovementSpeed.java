// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterMovementSpeed extends FMODLocalParameter {
    private final IsoGameCharacter character;
    private ParameterCharacterMovementSpeed.MovementType movementType = ParameterCharacterMovementSpeed.MovementType.Walk;

    public ParameterCharacterMovementSpeed(IsoGameCharacter character) {
        super("CharacterMovementSpeed");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.movementType.label;
    }

    public void setMovementType(ParameterCharacterMovementSpeed.MovementType movementType) {
        this.movementType = movementType;
    }

    public static enum MovementType {
        SneakWalk(0),
        SneakRun(1),
        Strafe(2),
        Walk(3),
        Run(4),
        Sprint(5);

        public final int label;

        private MovementType(final int label) {
            this.label = label;
        }
    }
}
