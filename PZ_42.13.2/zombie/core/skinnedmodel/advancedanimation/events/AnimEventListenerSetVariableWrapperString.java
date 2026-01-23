// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimEventSetVariable;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventListenerSetVariableWrapperString implements IAnimEventListener, IAnimEventListenerSetVariableString {
    private final IAnimEventListenerSetVariableString wrapped;

    private AnimEventListenerSetVariableWrapperString(IAnimEventListenerSetVariableString in_wrapped) {
        this.wrapped = in_wrapped;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event instanceof AnimEventSetVariable eventsv) {
            this.animEvent(owner, eventsv.variableReference, eventsv.setVariableValue);
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimationVariableReference variableReference, String variableValue) {
        this.wrapped.animEvent(owner, variableReference, variableValue);
    }

    public static IAnimEventListener wrapper(IAnimEventListenerSetVariableString in_wrapped) {
        return (IAnimEventListener)(in_wrapped instanceof IAnimEventListener iAnimEventListener
            ? iAnimEventListener
            : new AnimEventListenerSetVariableWrapperString(in_wrapped));
    }
}
