// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperNoParam implements IAnimEventListener, IAnimEventListenerNoParam {
    private final IAnimEventListenerNoParam wrapped;

    private AnimEventListenerWrapperNoParam(IAnimEventListenerNoParam in_wrapped) {
        this.wrapped = in_wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner);
    }

    @Override
    public void animEvent(IsoGameCharacter owner) {
        this.wrapped.animEvent(owner);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerNoParam in_wrapped) {
        return (IAnimEventListener)(in_wrapped instanceof IAnimEventListener iAnimEventListener
            ? iAnimEventListener
            : new AnimEventListenerWrapperNoParam(in_wrapped));
    }
}
