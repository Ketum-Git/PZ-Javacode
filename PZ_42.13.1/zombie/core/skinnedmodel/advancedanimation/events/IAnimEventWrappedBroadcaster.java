// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public interface IAnimEventWrappedBroadcaster extends IAnimEventListener {
    AnimEventBroadcaster getAnimEventBroadcaster();

    default void addAnimEventListener(String in_animEventName, IAnimEventListener in_listener) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener);
    }

    default void addAnimEventListener(String in_animEventName, IAnimEventListenerNoTrack in_listener) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener);
    }

    default void addAnimEventListener(String in_animEventName, IAnimEventListenerNoTrackString in_listener) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener);
    }

    default void addAnimEventListener(String in_animEventName, IAnimEventListenerBoolean in_listener) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener);
    }

    default void addAnimEventListener(String in_animEventName, IAnimEventListenerString in_listener) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener);
    }

    default void addAnimEventListener(String in_animEventName, IAnimEventListenerNoParam in_listener) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener);
    }

    default void addAnimEventListener(String in_animEventName, IAnimEventListenerFloat in_listener) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener);
    }

    default void addAnimEventListener(IAnimEventListenerSetVariableString in_listener) {
        this.getAnimEventBroadcaster().addListener(in_listener);
    }

    default <E extends Enum<E>> void addAnimEventListener(String in_animEventName, IAnimEventListenerEnum<E> in_listener, E in_default) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener, in_default);
    }

    default <E extends Enum<E>> void addAnimEventListener(String in_animEventName, IAnimEventListenerNoTrackEnum<E> in_listener, E in_default) {
        this.getAnimEventBroadcaster().addListener(in_animEventName, in_listener, in_default);
    }

    @Override
    default void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.getAnimEventBroadcaster().animEvent(owner, layer, track, event);
    }
}
