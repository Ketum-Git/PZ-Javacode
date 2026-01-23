// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

public final class EngineRPMData {
    public float gearChange;
    public float afterGearChange;

    public EngineRPMData() {
    }

    public EngineRPMData(float gearChange, float afterGearChange) {
        this.gearChange = gearChange;
        this.afterGearChange = afterGearChange;
    }

    public void reset() {
        this.gearChange = 0.0F;
        this.afterGearChange = 0.0F;
    }
}
