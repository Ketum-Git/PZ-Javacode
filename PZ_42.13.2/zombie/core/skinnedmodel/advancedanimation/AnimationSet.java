// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;

public final class AnimationSet {
    protected static final HashMap<String, AnimationSet> setMap = new HashMap<>();
    public final HashMap<String, AnimState> states = new HashMap<>();
    public String name = "";

    public static AnimationSet GetAnimationSet(String name, boolean reload) {
        AnimationSet s = setMap.get(name);
        if (s != null && !reload) {
            return s;
        } else {
            s = new AnimationSet();
            s.Load(name);
            setMap.put(name, s);
            return s;
        }
    }

    public static void Reset() {
        for (AnimationSet animSet : setMap.values()) {
            animSet.clear();
        }

        setMap.clear();
    }

    public AnimState GetState(String name) {
        AnimState n = this.states.get(name.toLowerCase(Locale.ENGLISH));
        if (n != null) {
            return n;
        } else {
            DebugLog.Animation.warn("AnimState not found: " + name);
            return new AnimState();
        }
    }

    public boolean containsState(String name) {
        return this.states.containsKey(name.toLowerCase(Locale.ENGLISH));
    }

    public boolean Load(String name) {
        DebugLog.Animation.debugln("Loading AnimSet: %s", name);
        this.name = name;
        String[] listOfDirs = ZomboidFileSystem.instance.resolveAllDirectories("media/AnimSets/" + name, dir -> true, false);

        for (String stateDir : listOfDirs) {
            String stateName = new File(stateDir).getName();
            AnimState newState = AnimState.Parse(stateName, stateDir);
            newState.set = this;
            this.states.put(stateName.toLowerCase(Locale.ENGLISH), newState);
        }

        return true;
    }

    private void clear() {
        for (AnimState state : this.states.values()) {
            state.clear();
        }

        this.states.clear();
    }
}
