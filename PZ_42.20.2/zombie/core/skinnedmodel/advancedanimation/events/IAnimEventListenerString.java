// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public interface IAnimEventListenerString {
    void animEvent(IsoGameCharacter arg0, AnimLayer arg1, AnimationTrack arg2, String arg3);
}
