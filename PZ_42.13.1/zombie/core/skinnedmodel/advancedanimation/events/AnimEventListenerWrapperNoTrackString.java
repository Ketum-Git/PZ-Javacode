// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperNoTrackString implements IAnimEventListener, IAnimEventListenerNoTrackString {
    private final IAnimEventListenerNoTrackString wrapped;

    private AnimEventListenerWrapperNoTrackString(IAnimEventListenerNoTrackString in_wrapped) {
        this.wrapped = in_wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, event.parameterValue);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, String param) {
        this.wrapped.animEvent(owner, param);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerNoTrackString in_wrapped) {
        return (IAnimEventListener)(in_wrapped instanceof IAnimEventListener iAnimEventListener
            ? iAnimEventListener
            : new AnimEventListenerWrapperNoTrackString(in_wrapped));
    }
}
