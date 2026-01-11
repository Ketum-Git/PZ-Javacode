// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.util.Pool;
import zombie.util.PooledObject;

public class AnimationVariableWhileAliveFlagCounter extends PooledObject {
    private AnimationVariableReference variableReference;
    private int counter;
    private static final Pool<AnimationVariableWhileAliveFlagCounter> s_pool = new Pool<>(AnimationVariableWhileAliveFlagCounter::new);

    private AnimationVariableWhileAliveFlagCounter() {
    }

    public static AnimationVariableWhileAliveFlagCounter alloc(AnimationVariableReference in_variableReference) {
        AnimationVariableWhileAliveFlagCounter newCounter = s_pool.alloc();
        newCounter.variableReference = in_variableReference;
        newCounter.counter = 0;
        return newCounter;
    }

    @Override
    public void onReleased() {
        this.variableReference = null;
        this.counter = 0;
        super.onReleased();
    }

    public AnimationVariableReference getVariableRerefence() {
        return this.variableReference;
    }

    public int increment() {
        this.counter++;
        return this.counter;
    }

    public int decrement() {
        if (this.counter == 0) {
            throw new IndexOutOfBoundsException("Too many decrements. var: " + this.variableReference);
        } else {
            this.counter--;
            return this.counter;
        }
    }

    public int getCount() {
        return this.counter;
    }
}
