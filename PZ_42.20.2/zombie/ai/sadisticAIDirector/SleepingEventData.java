// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.sadisticAIDirector;

import zombie.GameTime;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;

public final class SleepingEventData {
    protected int forceWakeUpTime = -1;
    protected boolean zombiesIntruders = true;
    protected int nightmareWakeUp = -1;
    protected IsoWindow weakestWindow;
    protected IsoDoor openDoor;
    protected boolean raining;
    protected boolean wasRainingAtStart;
    protected double rainTimeStartHours = -1.0;
    protected float sleepingTime = 8.0F;
    protected boolean fastWakeup;

    public void reset() {
        this.forceWakeUpTime = -1;
        this.zombiesIntruders = false;
        this.nightmareWakeUp = -1;
        this.openDoor = null;
        this.weakestWindow = null;
        this.raining = false;
        this.wasRainingAtStart = false;
        this.rainTimeStartHours = -1.0;
        this.sleepingTime = 8.0F;
        this.fastWakeup = false;
    }

    public double getHoursSinceRainStarted() {
        return GameTime.getInstance().getWorldAgeHours() - this.rainTimeStartHours;
    }
}
