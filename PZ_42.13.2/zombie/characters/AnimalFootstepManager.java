// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.characters.animals.IsoAnimal;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public final class AnimalFootstepManager extends BaseAnimalSoundManager {
    public static final AnimalFootstepManager instance = new AnimalFootstepManager();

    public AnimalFootstepManager() {
        super(20, 500);
    }

    @Override
    public void playSound(IsoAnimal chr) {
        LogSeverity severity = DebugLog.Sound.getLogSeverity();
        DebugLog.Sound.setLogSeverity(LogSeverity.General);

        try {
            chr.playNextFootstepSound();
        } finally {
            DebugLog.Sound.setLogSeverity(severity);
        }
    }

    @Override
    public void postUpdate() {
    }
}
