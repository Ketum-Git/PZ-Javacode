// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperString implements IAnimEventListener, IAnimEventListenerString {
    private final IAnimEventListenerString wrapped;

    private AnimEventListenerWrapperString(IAnimEventListenerString wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, layer, track, event.parameterValue);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer animLayer, AnimationTrack animTrack, String param) {
        this.wrapped.animEvent(owner, animLayer, animTrack, param);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerString wrapped) {
        return (IAnimEventListener)(wrapped instanceof IAnimEventListener iAnimEventListener ? iAnimEventListener : new AnimEventListenerWrapperString(wrapped));
    }
}
