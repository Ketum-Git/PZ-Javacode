// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import zombie.util.StringUtils;

public class AnimationVariableHandlePool {
    private static final Object s_threadLock = "AnimationVariableHandlePool.ThreadLock";
    private static final Map<String, AnimationVariableHandle> s_handlePoolMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final ArrayList<AnimationVariableHandle> s_handlePool = new ArrayList<>();
    private static volatile int globalIndexGenerator;

    public static AnimationVariableHandle getOrCreate(String name) {
        synchronized (s_threadLock) {
            return getOrCreateInternal(name);
        }
    }

    private static AnimationVariableHandle getOrCreateInternal(String in_name) {
        if (StringUtils.isNullOrWhitespace(in_name)) {
            return null;
        } else {
            String key = in_name.trim();
            if (!isVariableNameValid(key)) {
                return null;
            } else {
                AnimationVariableHandle handle = s_handlePoolMap.get(key);
                if (handle != null) {
                    return handle;
                } else {
                    AnimationVariableHandle newHandle = new AnimationVariableHandle();
                    newHandle.setVariableName(key);
                    newHandle.setVariableIndex(generateNewVariableIndex());
                    String keyLc = key.toLowerCase();
                    s_handlePoolMap.put(keyLc, newHandle);
                    s_handlePool.add(newHandle);
                    return newHandle;
                }
            }
        }
    }

    private static boolean isVariableNameValid(String name) {
        return !StringUtils.isNullOrWhitespace(name);
    }

    private static int generateNewVariableIndex() {
        return ++globalIndexGenerator;
    }

    public static Iterable<AnimationVariableHandle> all() {
        return () -> new Iterator<AnimationVariableHandle>() {
            private int currentIndex = -1;

            @Override
            public boolean hasNext() {
                return this.currentIndex + 1 < AnimationVariableHandlePool.globalIndexGenerator;
            }

            public AnimationVariableHandle next() {
                synchronized (AnimationVariableHandlePool.s_threadLock) {
                    if (!this.hasNext()) {
                        throw new NoSuchElementException();
                    } else {
                        this.currentIndex++;
                        return AnimationVariableHandlePool.s_handlePool.get(this.currentIndex);
                    }
                }
            }
        };
    }
}
