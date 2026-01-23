// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperNoTrack implements IAnimEventListener, IAnimEventListenerNoTrack {
    private final IAnimEventListenerNoTrack wrapped;

    private AnimEventListenerWrapperNoTrack(IAnimEventListenerNoTrack in_wrapped) {
        this.wrapped = in_wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, event);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimEvent event) {
        this.wrapped.animEvent(owner, event);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerNoTrack in_wrapped) {
        return (IAnimEventListener)(in_wrapped instanceof IAnimEventListener iAnimEventListener
            ? iAnimEventListener
            : new AnimEventListenerWrapperNoTrack(in_wrapped));
    }
}
