// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.scripting.objects.MoodleType;

public final class ParameterMoodlePanic extends FMODGlobalParameter {
    public ParameterMoodlePanic() {
        super("MoodlePanic");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        return character == null ? 0.0F : character.getMoodles().getMoodleLevel(MoodleType.PANIC) / 4.0F;
    }
}
