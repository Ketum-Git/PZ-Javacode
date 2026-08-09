// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandles;

public class AnimalStateVariables {
    public static byte getVariables(IsoAnimal animal) {
        byte value = 0;
        value = (byte)(value | (animal.isOnFloor() ? AnimalStateVariables.Flags.isOnFloor : 0));
        value = (byte)(value | (animal.isDead() ? AnimalStateVariables.Flags.isDead : 0));
        value = (byte)(value | (animal.getVariableBoolean(AnimationVariableHandles.animalRunning) ? AnimalStateVariables.Flags.isRunning : 0));
        return (byte)(value | (animal.isAnimalAttacking() ? AnimalStateVariables.Flags.isAttacking : 0));
    }

    public static void setVariables(IsoAnimal animal, byte value) {
        animal.setOnFloor((value & AnimalStateVariables.Flags.isOnFloor) != 0);
        animal.setVariable(AnimationVariableHandles.animalRunning, (value & AnimalStateVariables.Flags.isRunning) != 0);
        animal.setAnimalAttackingOnClient((value & AnimalStateVariables.Flags.isAttacking) != 0);
    }

    public static class Flags {
        public static byte isOnFloor = 1;
        public static byte isDead = 2;
        public static byte isRunning = 4;
        public static byte isAttacking = 8;
    }
}
