// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public interface IAnimEventListenerEnum<E extends Enum<E>> {
    void animEvent(IsoGameCharacter var1, AnimLayer var2, AnimationTrack var3, E var4);
}
