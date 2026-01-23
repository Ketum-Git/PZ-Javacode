// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperFloat implements IAnimEventListener, IAnimEventListenerFloat {
    private final IAnimEventListenerFloat wrapped;

    private AnimEventListenerWrapperFloat(IAnimEventListenerFloat in_wrapped) {
        this.wrapped = in_wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, PZMath.tryParseFloat(event.parameterValue, 0.0F));
    }

    @Override
    public void animEvent(IsoGameCharacter owner, float param) {
        this.wrapped.animEvent(owner, param);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerFloat in_wrapped) {
        return (IAnimEventListener)(in_wrapped instanceof IAnimEventListener iAnimEventListener
            ? iAnimEventListener
            : new AnimEventListenerWrapperFloat(in_wrapped));
    }
}
