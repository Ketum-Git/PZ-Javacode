// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.vehicles.BaseVehicle;

public class RagdollStateData {
    public float simulationTimeout;
    public boolean isSimulating;
    public boolean isSimulationMovement;
    public boolean isCalculated;
    public boolean isContactingVehicle;
    public float simulationRenderedAngle;
    public float simulationCharacterForwardAngle;
    public final Vector2 simulationDirection = new Vector2();
    public final Vector3 pelvisDirection = new Vector3();
    public BaseVehicle lastCollidedVehicle;

    public RagdollStateData() {
        this.reset();
    }

    public void reset() {
        this.simulationTimeout = 1.5F;
        this.isSimulating = false;
        this.isSimulationMovement = false;
        this.isCalculated = false;
        this.isContactingVehicle = false;
        this.simulationRenderedAngle = 0.0F;
        this.simulationCharacterForwardAngle = 0.0F;
        this.simulationDirection.set(0.0F, 0.0F);
        this.pelvisDirection.set(0.0F, 0.0F, 0.0F);
        this.lastCollidedVehicle = null;
    }
}
