// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.characters.animals.IsoAnimal;

public final class AnimalFootstepManager extends BaseAnimalSoundManager {
    public static final AnimalFootstepManager instance = new AnimalFootstepManager();

    public AnimalFootstepManager() {
        super(20, 500);
    }

    @Override
    public void playSound(IsoAnimal chr) {
        chr.playNextFootstepSound();
    }

    @Override
    public void postUpdate() {
    }
}
