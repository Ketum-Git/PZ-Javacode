// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import zombie.util.IPooledObject;
import zombie.util.list.PZArrayUtil;

public class AnimationVariableWhileAliveFlagsContainer {
    private final ArrayList<AnimationVariableWhileAliveFlagCounter> list = new ArrayList<>();

    private AnimationVariableWhileAliveFlagCounter findCounter(AnimationVariableReference variableReference) {
        return PZArrayUtil.find(this.list, item -> item.getVariableRerefence().equals(variableReference));
    }

    private AnimationVariableWhileAliveFlagCounter getOrCreateCounter(AnimationVariableReference variableReference) {
        AnimationVariableWhileAliveFlagCounter existingCounter = this.findCounter(variableReference);
        if (existingCounter != null) {
            return existingCounter;
        } else {
            AnimationVariableWhileAliveFlagCounter newCounter = AnimationVariableWhileAliveFlagCounter.alloc(variableReference);
            this.list.add(newCounter);
            return newCounter;
        }
    }

    public boolean incrementWhileAliveFlagOnce(AnimationVariableReference variableReference) {
        AnimationVariableWhileAliveFlagCounter foundCounter = this.getOrCreateCounter(variableReference);
        return foundCounter.getCount() > 0 ? false : foundCounter.increment() > 0;
    }

    public int incrementWhileAliveFlag(AnimationVariableReference variableReference) {
        return this.getOrCreateCounter(variableReference).increment();
    }

    public int decrementWhileAliveFlag(AnimationVariableReference variableReference) {
        AnimationVariableWhileAliveFlagCounter counter = this.findCounter(variableReference);
        if (counter == null) {
            throw new NullPointerException("No counter found for variable: " + variableReference);
        } else {
            return counter.decrement();
        }
    }

    public void clear() {
        IPooledObject.release(this.list);
    }

    public int numCounters() {
        return this.list.size();
    }

    public AnimationVariableWhileAliveFlagCounter getCounterAt(int i) {
        return this.list.get(i);
    }
}
