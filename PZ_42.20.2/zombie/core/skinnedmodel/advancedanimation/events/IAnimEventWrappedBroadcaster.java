// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public interface IAnimEventWrappedBroadcaster extends IAnimEventListener {
    AnimEventBroadcaster getAnimEventBroadcaster();

    default void addAnimEventListener(String animEventName, IAnimEventListener listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default void addAnimEventListener(String animEventName, IAnimEventListenerNoTrack listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default void addAnimEventListener(String animEventName, IAnimEventListenerNoTrackString listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default void addAnimEventListener(String animEventName, IAnimEventListenerBoolean listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default void addAnimEventListener(String animEventName, IAnimEventListenerString listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default void addAnimEventListener(String animEventName, IAnimEventListenerNoParam listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default void addAnimEventListener(String animEventName, IAnimEventListenerFloat listener) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener);
    }

    default void addAnimEventListener(IAnimEventListenerSetVariableString listener) {
        this.getAnimEventBroadcaster().addListener(listener);
    }

    default <E extends Enum<E>> void addAnimEventListener(String animEventName, IAnimEventListenerEnum<E> listener, E defaultValue) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener, defaultValue);
    }

    default <E extends Enum<E>> void addAnimEventListener(String animEventName, IAnimEventListenerNoTrackEnum<E> listener, E defaultValue) {
        this.getAnimEventBroadcaster().addListener(animEventName, listener, defaultValue);
    }

    @Override
    default void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.getAnimEventBroadcaster().animEvent(owner, layer, track, event);
    }
}
