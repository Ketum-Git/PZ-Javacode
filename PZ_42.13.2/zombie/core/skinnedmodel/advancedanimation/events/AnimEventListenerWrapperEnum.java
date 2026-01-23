// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.StringUtils;

public class AnimEventListenerWrapperEnum<E extends Enum<E>> implements IAnimEventListener, IAnimEventListenerEnum<E> {
    private final IAnimEventListenerEnum<E> wrapped;
    private final E defaultValue;
    private final Class<E> enumClass;

    private AnimEventListenerWrapperEnum(IAnimEventListenerEnum<E> in_wrapped, E in_default) {
        this.wrapped = in_wrapped;
        this.defaultValue = in_default;
        this.enumClass = (Class<E>)this.defaultValue.getClass();
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, layer, track, StringUtils.tryParseEnum(this.enumClass, event.parameterValue, this.defaultValue));
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer animLayer, AnimationTrack track, E param) {
        this.wrapped.animEvent(owner, animLayer, track, param);
    }

    public static <E extends Enum<E>> IAnimEventListener wrapper(IAnimEventListenerEnum<E> in_wrapped, E in_default) {
        return (IAnimEventListener)(in_wrapped instanceof IAnimEventListener iAnimEventListener
            ? iAnimEventListener
            : new AnimEventListenerWrapperEnum<>(in_wrapped, in_default));
    }
}
