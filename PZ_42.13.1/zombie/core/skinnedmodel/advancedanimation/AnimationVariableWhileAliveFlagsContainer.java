// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import zombie.util.IPooledObject;
import zombie.util.list.PZArrayUtil;

public class AnimationVariableWhileAliveFlagsContainer {
    private final ArrayList<AnimationVariableWhileAliveFlagCounter> list = new ArrayList<>();

    private AnimationVariableWhileAliveFlagCounter findCounter(AnimationVariableReference in_variableReference) {
        return PZArrayUtil.find(this.list, item -> item.getVariableRerefence().equals(in_variableReference));
    }

    private AnimationVariableWhileAliveFlagCounter getOrCreateCounter(AnimationVariableReference in_variableReference) {
        AnimationVariableWhileAliveFlagCounter existingCounter = this.findCounter(in_variableReference);
        if (existingCounter != null) {
            return existingCounter;
        } else {
            AnimationVariableWhileAliveFlagCounter newCounter = AnimationVariableWhileAliveFlagCounter.alloc(in_variableReference);
            this.list.add(newCounter);
            return newCounter;
        }
    }

    public boolean incrementWhileAliveFlagOnce(AnimationVariableReference in_variableReference) {
        AnimationVariableWhileAliveFlagCounter foundCounter = this.getOrCreateCounter(in_variableReference);
        return foundCounter.getCount() > 0 ? false : foundCounter.increment() > 0;
    }

    public int incrementWhileAliveFlag(AnimationVariableReference in_variableReference) {
        return this.getOrCreateCounter(in_variableReference).increment();
    }

    public int decrementWhileAliveFlag(AnimationVariableReference in_variableReference) {
        AnimationVariableWhileAliveFlagCounter counter = this.findCounter(in_variableReference);
        if (counter == null) {
            throw new NullPointerException("No counter found for variable: " + in_variableReference);
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
