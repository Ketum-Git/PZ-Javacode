// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.ui.SpeedControls;

public final class WalkingOnTheSpot {
    private float x;
    private float y;
    private float time;
    private float seconds;

    public boolean check(IsoGameCharacter chr) {
        float timeCheck = 400.0F;
        if (chr.isAnimal()) {
            timeCheck = 30.0F;
            if (!GameServer.server && SpeedControls.instance.getCurrentGameSpeed() == 4) {
                timeCheck = 150.0F;
            }
        }

        if (IsoUtils.DistanceToSquared(this.x, this.y, chr.getX(), chr.getY()) < 0.010000001F) {
            this.time = this.time + GameTime.getInstance().getMultiplier();
            this.seconds = this.seconds + GameTime.getInstance().getThirtyFPSMultiplier() / 30.0F;
        } else {
            this.x = chr.getX();
            this.y = chr.getY();
            this.time = 0.0F;
            this.seconds = 0.0F;
        }

        return this.time > timeCheck;
    }

    public boolean check(float x1, float y1) {
        if (IsoUtils.DistanceToSquared(this.x, this.y, x1, y1) < 0.010000001F) {
            this.time = this.time + GameTime.getInstance().getMultiplier();
            this.seconds = this.seconds + GameTime.getInstance().getThirtyFPSMultiplier() / 30.0F;
        } else {
            this.x = x1;
            this.y = y1;
            this.time = 0.0F;
            this.seconds = 0.0F;
        }

        return this.time > 400.0F;
    }

    public void reset(float x1, float y1) {
        this.x = x1;
        this.y = y1;
        this.time = 0.0F;
        this.seconds = 0.0F;
    }
}
