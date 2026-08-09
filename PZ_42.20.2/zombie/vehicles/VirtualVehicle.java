// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.FMODParameterUtils;
import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.core.math.PZMath;
import zombie.core.utils.UpdateLimit;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicleSound.VehicleSoundOwner;
import zombie.vehicleSound.VehicleSounds;

@UsedFromLua
public final class VirtualVehicle implements VehiclePartOwner, VehicleSoundOwner, IVehicleAlarmListener {
    private static IsoChunk fakeChunk;
    private BaseVehicle vehicle;
    private short vehicleId;
    private int sqlId;
    private float x;
    private float y;
    private float z;
    private String scriptName;
    private VehicleScript script;
    private boolean headlightsOn;
    private LightbarLightsMode lightbarLightsMode;
    private LightbarSirenMode lightbarSirenMode;
    double sirenStartTime;
    private VehicleAlarm vehicleAlarm;
    private VehiclePart missingEnginePart;
    private VehicleParts parts;
    private VehicleSounds vehicleSounds;
    private final UpdateLimit worldSoundUpdateLimit = new UpdateLimit(1000L);

    public short getId() {
        return this.vehicleId;
    }

    public int getSqlId() {
        return this.sqlId;
    }

    public VehicleSounds getVehicleSounds() {
        return this.vehicleSounds;
    }

    public void setNeedPartsUpdate(boolean needPartsUpdate) {
    }

    public void set(BaseVehicle vehicle) {
        this.vehicle = vehicle;
        this.vehicleId = vehicle.getId();
        this.sqlId = vehicle.getSqlId();
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
        this.scriptName = vehicle.getScriptName();
        this.script = vehicle.getScript();
        this.headlightsOn = vehicle.getHeadlightsOn();
        this.lightbarLightsMode = vehicle.getLightbarLightsModeObject();
        this.lightbarSirenMode = vehicle.getLightbarSirenModeObject();
        this.sirenStartTime = vehicle.getSirenStartTime();
        this.vehicleAlarm = vehicle.getVehicleAlarmObject();
        this.vehicleAlarm.setOwner(this);
        this.vehicleAlarm.setListener(this);
        this.parts = vehicle.getParts();
        this.parts.setOwner(this);
        vehicle.checkVehicleSoundsExists();
        this.vehicleSounds = vehicle.getVehicleSounds();
        this.vehicleSounds.setOwner(this);
        vehicle.setSquare(null);
        vehicle.setCurrent(null);
    }

    public void update() {
        this.getParts().update();
        this.getVehicleEngine().update();
        this.getVehicleSounds().update();
        this.vehicleAlarm.update();
        this.updateWorldSounds();
    }

    private void updateWorldSounds() {
        if (!GameClient.client) {
            if (this.worldSoundUpdateLimit.Check()) {
                if (this.isSirenSounding() && SandboxOptions.instance.sirenEffectsZombies.getValue()) {
                    WorldSoundManager.instance.addSoundRepeating(this, this.getXi(), this.getYi(), this.getZi(), 100, 60, false, true);
                }
            }
        }
    }

    public boolean shouldUpdateInMeta() {
        boolean batteryOk = this.getBatteryCharge() > 0.0F;
        boolean activeAlarm = batteryOk && this.hasAlarm() && this.vehicleAlarm.isActive();
        boolean activeSiren = batteryOk && this.hasSiren() && this.getLightbarSirenModeObject().isEnable();
        return activeAlarm || activeSiren || this.isEngineRunning();
    }

    public void stopUpdatingInMeta() {
        this.removeFromMeta(this.vehicle);
        this.save();
        this.getVehicleSounds().remove();
    }

    public void removeFromMeta(BaseVehicle vehicle) {
        if (vehicle == null) {
            this.getVehicleSounds().remove();
        } else {
            vehicle.adoptParts(this.getParts());
            vehicle.setHeadlightsOn(this.headlightsOn);
            vehicle.setLightbarLightsMode(this.getLightbarLightsMode());
            vehicle.setLightbarSirenMode(this.getLightbarSirenMode());
            vehicle.setVehicleSounds(this.getVehicleSounds());
            vehicle.setVehicleAlarm(this.vehicleAlarm);
        }
    }

    public void save() {
        this.vehicle.setLightbarLightsMode(this.getLightbarLightsMode());
        this.vehicle.setLightbarSirenMode(this.getLightbarSirenMode());
        if (fakeChunk == null) {
            fakeChunk = new IsoChunk(IsoWorld.instance.currentCell);
        }

        fakeChunk.wx = PZMath.coorddivision(this.getXi(), 8);
        fakeChunk.wy = PZMath.coorddivision(this.getYi(), 8);
        this.vehicle.chunk = fakeChunk;
        VehiclesDB2.instance.updateVehicle(this.vehicle);
        this.vehicle.chunk = null;
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
    public IsoGridSquare getSquare() {
        return null;
    }

    @Override
    public VehicleParts getParts() {
        return this.parts;
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
    public LightbarLightsMode getLightbarLightsModeObject() {
        return this.lightbarLightsMode;
    }

    @Override
    public boolean getHeadlightsOn() {
        return this.headlightsOn;
    }

    private VehicleEngine getVehicleEngine() {
        if (this.getEngine() == null) {
            if (this.missingEnginePart == null) {
                this.missingEnginePart = new VehiclePart(this);
                this.missingEnginePart.engine = new VehicleEngine(this.missingEnginePart);
                this.missingEnginePart.engine.addListener(this);
            }

            return this.missingEnginePart.getVehicleEngine();
        } else {
            this.missingEnginePart = null;
            return this.getEngine().getVehicleEngine();
        }
    }

    @Override
    public void setEngineFeature(int quality, int loudness, int engineForce) {
        this.getVehicleEngine().setFeatures(quality, loudness, engineForce);
    }

    @Override
    public boolean isEngineWorking() {
        return this.getParts().isEngineWorking();
    }

    @Override
    public float getBrakeSpeedBetweenUpdate() {
        return 0.0F;
    }

    @Override
    public BaseVehicle.ModelInfo setModelVisible(VehiclePart part, VehicleScript.Model scriptModel, boolean visible) {
        return null;
    }

    @Override
    public void updateDamageOverlayLater() {
    }

    @Override
    public void updateTotalMass() {
    }

    @Override
    public void updateBulletStats() {
    }

    @Override
    public void updatePartStats() {
    }

    @Override
    public void transmitEngine() {
    }

    @Override
    public void transmitPartCondition(VehiclePart part) {
    }

    @Override
    public void transmitPartDoor(VehiclePart part) {
    }

    @Override
    public void transmitPartItem(VehiclePart part) {
    }

    @Override
    public void transmitPartLight(VehiclePart part) {
    }

    @Override
    public void transmitPartModData(VehiclePart part) {
    }

    @Override
    public void transmitPartUsedDelta(VehiclePart part) {
    }

    @Override
    public void transmitPartWindow(VehiclePart part) {
    }

    @Override
    public int getEngineCondition() {
        return this.parts.getEngineCondition();
    }

    @Override
    public int getEngineQuality() {
        return this.getVehicleEngine().getQuality();
    }

    @Override
    public BaseVehicle.engineStateTypes getEngineState() {
        return this.getVehicleEngine().getState();
    }

    @Override
    public boolean isEngineRunning() {
        return this.getVehicleEngine().isRunning();
    }

    @Override
    public boolean isEngineSounding() {
        if (!this.isListenerInRange(200.0F)) {
            return false;
        } else {
            boolean isStartingWithCombinedSound = this.getEngineState() == BaseVehicle.engineStateTypes.StartingSuccess
                && this.getVehicleSounds().isCombinedWithEngineSound(this.getVehicleSounds().getEngineStartSound());
            return isStartingWithCombinedSound ? true : this.getEngineState() == BaseVehicle.engineStateTypes.Running;
        }
    }

    @Override
    public double getEngineSpeed() {
        return this.getVehicleEngine().getSpeed();
    }

    @Override
    public int getTransmissionNumber() {
        return TransmissionNumber.N.getIndex();
    }

    @Override
    public float getCurrentSpeedKmHour() {
        return 0.0F;
    }

    @Override
    public float getMaxSpeed() {
        return this.getScript() == null ? 0.0F : this.getScript().maxSpeed;
    }

    @Override
    public boolean isAlarmActive() {
        return this.hasAlarm() && this.vehicleAlarm.isActive() && this.getBatteryCharge() > 0.0F;
    }

    @Override
    public boolean isAlarmSoundOn() {
        return this.isAlarmActive() && this.vehicleAlarm.isSoundOn();
    }

    @Override
    public boolean isAlarmSounding() {
        return !this.isListenerInRange(500.0F) ? false : this.isAlarmSoundOn();
    }

    @Override
    public boolean isBrakePedalPressed() {
        return false;
    }

    @Override
    public boolean isGasPedalPressed() {
        return false;
    }

    @Override
    public ParameterVehicleRoadMaterial.Material getRoadMaterial() {
        return ParameterVehicleRoadMaterial.Material.Concrete;
    }

    @Override
    public String getChosenAlarmSound() {
        return this.vehicleAlarm.getChosenSound();
    }

    @Override
    public boolean isBackupBeeperSounding() {
        return false;
    }

    @Override
    public boolean isDoorAlarmSounding() {
        return false;
    }

    @Override
    public boolean isHornSounding() {
        return false;
    }

    @Override
    public BaseSoundEmitter getVehicleSoundEmitter() {
        return this.getVehicleSounds().getVehicleSoundEmitter();
    }

    @Override
    public boolean isAnyListenerInside() {
        return false;
    }

    @Override
    public boolean isSirenActive() {
        return this.hasSiren() && this.getLightbarSirenModeObject().isEnable() && this.getBatteryCharge() > 0.0F;
    }

    @Override
    public boolean isSirenSounding() {
        return !this.isListenerInRange(500.0F) ? false : this.isSirenActive();
    }

    @Override
    public double getSirenStartTime() {
        return this.sirenStartTime;
    }

    @Override
    public void setSirenStartTime(double worldAgeHours) {
        this.sirenStartTime = worldAgeHours;
    }

    @Override
    public LightbarSirenMode getLightbarSirenModeObject() {
        return this.lightbarSirenMode;
    }

    @Override
    public void setLightbarSirenMode(int mode) {
        this.getLightbarSirenModeObject().set(mode);
    }

    @Override
    public float getMaxWheelSteering() {
        return 0.0F;
    }

    @Override
    public float getMinWheelSkid() {
        return 0.0F;
    }

    @Override
    public boolean isAnyTireMissing() {
        return false;
    }

    @Override
    public void onEngineStateChanged(BaseVehicle.engineStateTypes oldState, BaseVehicle.engineStateTypes newState, VehicleEngineStateChangeReason reason) {
    }

    @Override
    public void onVehicleAlarmEvent(VehicleAlarmEvent event) {
        switch (event) {
            case ACTIVATED:
            case DEACTIVATED:
            default:
                break;
            case LIGHTS_ON:
                this.headlightsOn = true;
                break;
            case LIGHTS_OFF:
                this.headlightsOn = false;
        }
    }
}
