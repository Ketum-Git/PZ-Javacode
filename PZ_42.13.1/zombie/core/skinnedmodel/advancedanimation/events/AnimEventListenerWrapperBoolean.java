// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerWrapperBoolean implements IAnimEventListener, IAnimEventListenerBoolean {
    private final IAnimEventListenerBoolean wrapped;

    private AnimEventListenerWrapperBoolean(IAnimEventListenerBoolean in_wrapped) {
        this.wrapped = in_wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, Boolean.parseBoolean(event.parameterValue));
    }

    @Override
    public void animEvent(IsoGameCharacter owner, boolean param) {
        this.wrapped.animEvent(owner, param);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerBoolean in_wrapped) {
        return (IAnimEventListener)(in_wrapped instanceof IAnimEventListener iAnimEventListener
            ? iAnimEventListener
            : new AnimEventListenerWrapperBoolean(in_wrapped));
    }
}
