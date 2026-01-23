// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.StringUtils;

public class AnimEventListenerWrapperNoTrackEnum<E extends Enum<E>> implements IAnimEventListener, IAnimEventListenerNoTrackEnum<E> {
    private final IAnimEventListenerNoTrackEnum<E> wrapped;
    private final E defaultValue;
    private final Class<E> enumClass;

    private AnimEventListenerWrapperNoTrackEnum(IAnimEventListenerNoTrackEnum<E> in_wrapped, E in_default) {
        this.wrapped = in_wrapped;
        this.defaultValue = in_default;
        this.enumClass = (Class<E>)this.defaultValue.getClass();
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.animEvent(owner, StringUtils.tryParseEnum(this.enumClass, event.parameterValue, this.defaultValue));
    }

    @Override
    public void animEvent(IsoGameCharacter owner, E param) {
        this.wrapped.animEvent(owner, param);
    }

    public static <E extends Enum<E>> IAnimEventListener wrapper(IAnimEventListenerNoTrackEnum<E> in_wrapped, E in_default) {
        return (IAnimEventListener)(in_wrapped instanceof IAnimEventListener iAnimEventListener
            ? iAnimEventListener
            : new AnimEventListenerWrapperNoTrackEnum<>(in_wrapped, in_default));
    }
}
