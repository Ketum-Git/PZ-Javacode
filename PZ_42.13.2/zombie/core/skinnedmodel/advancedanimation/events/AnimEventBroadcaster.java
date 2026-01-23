// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.events;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimEventBroadcaster implements IAnimEventListener {
    private final Map<String, AnimEventListenerList> listeners = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public void addListener(String in_animEventName, IAnimEventListener in_listener) {
        AnimEventListenerList listenerList = this.getOrCreateListenerList(in_animEventName);
        listenerList.listeners.add(in_listener);
    }

    public void addListener(String in_animEventName, IAnimEventListenerNoTrack in_listener) {
        this.addListener(in_animEventName, AnimEventListenerWrapperNoTrack.wrapper(in_listener));
    }

    public void addListener(String in_animEventName, IAnimEventListenerNoTrackString in_listener) {
        this.addListener(in_animEventName, AnimEventListenerWrapperNoTrackString.wrapper(in_listener));
    }

    public void addListener(String in_animEventName, IAnimEventListenerBoolean in_listener) {
        this.addListener(in_animEventName, AnimEventListenerWrapperBoolean.wrapper(in_listener));
    }

    public void addListener(String in_animEventName, IAnimEventListenerString in_listener) {
        this.addListener(in_animEventName, AnimEventListenerWrapperString.wrapper(in_listener));
    }

    public void addListener(String in_animEventName, IAnimEventListenerNoParam in_listener) {
        this.addListener(in_animEventName, AnimEventListenerWrapperNoParam.wrapper(in_listener));
    }

    public void addListener(String in_animEventName, IAnimEventListenerFloat in_listener) {
        this.addListener(in_animEventName, AnimEventListenerWrapperFloat.wrapper(in_listener));
    }

    public <E extends Enum<E>> void addListener(String in_animEventName, IAnimEventListenerEnum<E> in_listener, E in_default) {
        this.addListener(in_animEventName, AnimEventListenerWrapperEnum.wrapper(in_listener, in_default));
    }

    public <E extends Enum<E>> void addListener(String in_animEventName, IAnimEventListenerNoTrackEnum<E> in_listener, E in_default) {
        this.addListener(in_animEventName, AnimEventListenerWrapperNoTrackEnum.wrapper(in_listener, in_default));
    }

    public void addListener(IAnimEventListenerSetVariableString in_listener) {
        this.addListener("SetVariable", AnimEventListenerSetVariableWrapperString.wrapper(in_listener));
    }

    private AnimEventListenerList getOrCreateListenerList(String in_animEventName) {
        AnimEventListenerList listenerList = this.getAnimEventListenerList(in_animEventName);
        if (listenerList == null) {
            listenerList = new AnimEventListenerList();
            this.listeners.put(in_animEventName, listenerList);
        }

        return listenerList;
    }

    private AnimEventListenerList getAnimEventListenerList(String in_animEventName) {
        return this.listeners.get(in_animEventName);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (!this.listeners.isEmpty()) {
            AnimEventListenerList listenerList = this.getAnimEventListenerList(event.eventName);
            if (listenerList != null) {
                List<IAnimEventListener> listeners = listenerList.listeners;

                for (int i = 0; i < listeners.size(); i++) {
                    IAnimEventListener listener = listeners.get(i);
                    listener.animEvent(owner, layer, track, event);
                }
            }
        }
    }
}
