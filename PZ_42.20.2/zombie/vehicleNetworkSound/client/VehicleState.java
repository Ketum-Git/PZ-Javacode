// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleNetworkSound.client;

import zombie.GameTime;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.FMODParameterUtils;
import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.core.math.PZMath;
import zombie.iso.IsoWorld;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicleNetworkSound.SharedVehicleState;
import zombie.vehicleSound.VehicleSoundOwner;
import zombie.vehicleSound.VehicleSounds;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.LightbarSirenMode;
import zombie.vehicles.VehicleManager;

final class VehicleState extends SharedVehicleState implements VehicleSoundOwner {
    private VehicleScript script;
    private VehicleSounds vehicleSounds;

    VehicleState(short id) {
        this.id = id;
        this.setVehicleSounds(new VehicleSounds());
    }

    void setScript(VehicleScript script) {
        this.script = script;
    }

    void setVehicleSounds(VehicleSounds vehicleSounds) {
        this.vehicleSounds = vehicleSounds;
        vehicleSounds.setOwner(this);
    }

    @Override
    public float getX() {
        return this.x;
    }

    @Override
    public float getY() {
        return this.y;
    }

    @Override
    public float getZ() {
        return this.z;
    }

    @Override
    public int getXi() {
        return PZMath.fastfloor(this.getX());
    }

    @Override
    public int getYi() {
        return PZMath.fastfloor(this.getY());
    }

    @Override
    public int getZi() {
        return PZMath.fastfloor(this.getZ());
    }

    @Override
    public boolean isListenerInRange(float range) {
        float closestListenerDistSq = FMODParameterUtils.getClosestListenerDistanceSquared(this.getX(), this.getY(), this.getZ());
        return closestListenerDistSq < PZMath.squared(range);
    }

    @Override
    public String getScriptName() {
        return this.scriptName;
    }

    @Override
    public VehicleScript getScript() {
        return this.script;
    }

    @Override
    public int getEngineCondition() {
        return this.engineCondition;
    }

    @Override
    public int getEngineQuality() {
        return this.engineQuality;
    }

    @Override
    public BaseVehicle.engineStateTypes getEngineState() {
        return this.engineState;
    }

    @Override
    public boolean isEngineRunning() {
        return this.getEngineState() == BaseVehicle.engineStateTypes.Running;
    }

    @Override
    public boolean isEngineSounding() {
        if (!this.isListenerInRange(200.0F)) {
            return false;
        } else {
            boolean isStartingWithCombinedSound = this.getEngineState() == BaseVehicle.engineStateTypes.StartingSuccess
                && this.getEngineStartSound().equals(this.getEngineSound());
            return isStartingWithCombinedSound ? true : this.getEngineState() == BaseVehicle.engineStateTypes.Running;
        }
    }

    @Override
    public double getEngineSpeed() {
        return this.engineSpeed;
    }

    @Override
    public int getTransmissionNumber() {
        return this.gear;
    }

    @Override
    public float getCurrentSpeedKmHour() {
        return this.currentSpeedKmHour;
    }

    @Override
    public float getMaxSpeed() {
        return this.getScript() == null ? 0.0F : this.getScript().maxSpeed;
    }

    @Override
    public boolean isAlarmActive() {
        return (this.vehicleStateFlags & 512) != 0;
    }

    @Override
    public boolean isAlarmSoundOn() {
        return this.isAlarmActive() && (this.vehicleStateFlags & 1) != 0;
    }

    @Override
    public boolean isAlarmSounding() {
        return !this.isListenerInRange(500.0F) ? false : this.isAlarmSoundOn();
    }

    @Override
    public boolean isBrakePedalPressed() {
        return (this.vehicleStateFlags & 64) != 0;
    }

    @Override
    public boolean isGasPedalPressed() {
        return (this.vehicleStateFlags & 128) != 0;
    }

    @Override
    public ParameterVehicleRoadMaterial.Material getRoadMaterial() {
        return this.roadMaterial;
    }

    @Override
    public String getChosenAlarmSound() {
        return this.chosenAlarmSound;
    }

    @Override
    public boolean isBackupBeeperSounding() {
        return !this.isListenerInRange(150.0F) ? false : (this.vehicleStateFlags & 2) != 0;
    }

    @Override
    public boolean isDoorAlarmSounding() {
        return !this.isListenerInRange(50.0F) ? false : (this.vehicleStateFlags & 8) != 0;
    }

    @Override
    public boolean isHornSounding() {
        return !this.isListenerInRange(500.0F) ? false : (this.vehicleStateFlags & 16) != 0;
    }

    @Override
    public BaseSoundEmitter getVehicleSoundEmitter() {
        return this.vehicleSounds.getVehicleSoundEmitter();
    }

    @Override
    public boolean isAnyListenerInside() {
        return false;
    }

    @Override
    public boolean isSirenActive() {
        return this.hasSiren() && this.getLightbarSirenModeObject().isEnable() && (this.vehicleStateFlags & 4) != 0;
    }

    @Override
    public boolean isSirenSounding() {
        return !this.isListenerInRange(500.0F) ? false : this.isSirenActive();
    }

    @Override
    public double getSirenStartTime() {
        return 0.0;
    }

    @Override
    public void setSirenStartTime(double worldAgeHours) {
    }

    @Override
    public LightbarSirenMode getLightbarSirenModeObject() {
        return this.lightbarSirenMode;
    }

    @Override
    public float getMaxWheelSteering() {
        return this.steering / 100.0F;
    }

    @Override
    public float getMinWheelSkid() {
        return this.minWheelSkid / 100.0F;
    }

    @Override
    public boolean isAnyTireMissing() {
        return (this.vehicleStateFlags & 256) != 0;
    }

    void update() {
        BaseVehicle var2 = this.getVehicleInWorld();
        if (var2 instanceof BaseVehicle) {
            if (this.vehicleSounds.getOwner() != var2) {
                if (this.isAlarmActive() && !var2.isAlarmActive()) {
                    var2.getVehicleAlarmObject().setStartTime(GameTime.instance.getWorldAgeHours());
                }

                if (this.isAlarmSoundOn() && !var2.isAlarmSoundOn()) {
                    var2.onAlarmStart();
                }
            }

            var2.setVehicleSounds(this.vehicleSounds);
        } else {
            this.vehicleSounds.setOwner(this);
        }

        this.vehicleSounds.update();
    }

    void remove() {
        this.vehicleSounds.remove();
    }

    private BaseVehicle getVehicleInWorld() {
        if (IsoWorld.instance != null && IsoWorld.instance.currentCell != null) {
            BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(this.id);
            return !IsoWorld.instance.currentCell.getVehicles().contains(vehicle) ? null : vehicle;
        } else {
            return null;
        }
    }

    private String getEngineStartSound() {
        return this.getScript() != null && this.getScript().getSounds().engineStart != null ? this.getScript().getSounds().engineStart : "VehicleStarted";
    }

    private String getEngineSound() {
        return this.getScript() != null && this.getScript().getSounds().engine != null
            ? this.getScript().getSounds().engine
            : SoundKey.VEHICLE_ENGINE_DEFAULT.getSoundName();
    }
}
