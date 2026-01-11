// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;

@UsedFromLua
public final class MoveDeltaModifiers {
    public float turnDelta = -1.0F;
    public float moveDelta = -1.0F;
    public float twistDelta = -1.0F;

    public float getTurnDelta() {
        return this.turnDelta;
    }

    public float getMoveDelta() {
        return this.moveDelta;
    }

    public float getTwistDelta() {
        return this.twistDelta;
    }

    public void setTurnDelta(float delta) {
        this.turnDelta = delta;
    }

    public void setMoveDelta(float delta) {
        this.moveDelta = delta;
    }

    public void setTwistDelta(float delta) {
        this.twistDelta = delta;
    }

    public void setMaxTurnDelta(float delta) {
        this.turnDelta = PZMath.max(this.turnDelta, delta);
    }

    public void setMaxMoveDelta(float delta) {
        this.moveDelta = PZMath.max(this.moveDelta, delta);
    }

    public void setMaxTwistDelta(float delta) {
        this.twistDelta = PZMath.max(this.twistDelta, delta);
    }
}
