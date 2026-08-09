// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound;

import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.IFMODParameterUpdater;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import zombie.SoundManager;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.FMODParameterUtils;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;
import zombie.vehicleSound.parameters.ParameterVehicleBrake;
import zombie.vehicleSound.parameters.ParameterVehicleEngineCondition;
import zombie.vehicleSound.parameters.ParameterVehicleGear;
import zombie.vehicleSound.parameters.ParameterVehicleLoad;
import zombie.vehicleSound.parameters.ParameterVehicleRPM;
import zombie.vehicleSound.parameters.ParameterVehicleSkid;
import zombie.vehicleSound.parameters.ParameterVehicleSpeed;
import zombie.vehicleSound.parameters.ParameterVehicleSteer;
import zombie.vehicleSound.parameters.ParameterVehicleTireMissing;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.LightbarSirenMode;

public final class VehicleSounds implements VehicleSoundOwner, IFMODParameterUpdater {
    public static float SOUND_VOLUME = 1.0F;
    private VehicleSoundOwner owner;
    private BaseSoundEmitter emitter;
    private final FMODParameterList fmodParameters = new FMODParameterList();
    private final List<VehicleSound> sounds = new ArrayList<>();

    public VehicleSounds() {
        this.sounds.add(new AlarmSound(this));
        this.sounds.add(new BackupBeeperSound(this));
        this.sounds.add(new DoorAlarmSound(this));
        this.sounds.add(new EngineSound(this));
        this.sounds.add(new HornSound(this));
        this.sounds.add(new SirenSound(this));
    }

    public void setOwner(VehicleSoundOwner owner) {
        this.owner = owner;
    }

    public VehicleSoundOwner getOwner() {
        return this.owner;
    }

    @Override
    public float getX() {
        return this.owner.getX();
    }

    @Override
    public float getY() {
        return this.owner.getY();
    }

    @Override
    public float getZ() {
        return this.owner.getZ();
    }

    @Override
    public int getXi() {
        return this.owner.getXi();
    }

    @Override
    public int getYi() {
        return this.owner.getYi();
    }

    @Override
    public int getZi() {
        return this.owner.getZi();
    }

    @Override
    public boolean isListenerInRange(float range) {
        float closestListenerDistSq = FMODParameterUtils.getClosestListenerDistanceSquared(this.getX(), this.getY(), this.getZ());
        return closestListenerDistSq < PZMath.squared(range);
    }

    @Override
    public String getScriptName() {
        return this.owner.getScriptName();
    }

    @Override
    public VehicleScript getScript() {
        return this.owner.getScript();
    }

    @Override
    public int getEngineCondition() {
        return this.owner.getEngineCondition();
    }

    @Override
    public int getEngineQuality() {
        return this.owner.getEngineQuality();
    }

    @Override
    public BaseVehicle.engineStateTypes getEngineState() {
        return this.owner.getEngineState();
    }

    @Override
    public boolean isEngineRunning() {
        return this.owner.isEngineRunning();
    }

    @Override
    public boolean isEngineSounding() {
        return this.owner.isEngineSounding();
    }

    @Override
    public double getEngineSpeed() {
        return this.owner.getEngineSpeed();
    }

    @Override
    public int getTransmissionNumber() {
        return this.owner.getTransmissionNumber();
    }

    @Override
    public float getCurrentSpeedKmHour() {
        return this.owner.getCurrentSpeedKmHour();
    }

    @Override
    public float getMaxSpeed() {
        return this.owner.getMaxSpeed();
    }

    @Override
    public boolean hasAlarm() {
        return this.owner.hasAlarm();
    }

    @Override
    public boolean isAlarmActive() {
        return this.owner.isAlarmActive();
    }

    @Override
    public boolean isAlarmSoundOn() {
        return this.owner.isAlarmSoundOn();
    }

    @Override
    public boolean isAlarmSounding() {
        return this.owner.isAlarmSounding();
    }

    @Override
    public boolean isBrakePedalPressed() {
        return this.owner.isBrakePedalPressed();
    }

    @Override
    public boolean isGasPedalPressed() {
        return this.owner.isGasPedalPressed();
    }

    @Override
    public ParameterVehicleRoadMaterial.Material getRoadMaterial() {
        return this.owner.getRoadMaterial();
    }

    @Override
    public String getChosenAlarmSound() {
        return this.owner.getChosenAlarmSound();
    }

    @Override
    public boolean isBackupBeeperSounding() {
        return this.owner.isBackupBeeperSounding();
    }

    @Override
    public boolean isDoorAlarmSounding() {
        return this.owner.isDoorAlarmSounding();
    }

    @Override
    public boolean hasHorn() {
        return this.owner.hasHorn();
    }

    @Override
    public boolean isHornSounding() {
        return this.owner.isHornSounding();
    }

    @Override
    public BaseSoundEmitter getVehicleSoundEmitter() {
        if (this.emitter == null) {
            if (Core.soundDisabled) {
                this.emitter = new DummySoundEmitter();
            } else {
                FMODSoundEmitter emitter1 = new FMODSoundEmitter();
                emitter1.parameterUpdater = this;
                this.emitter = emitter1;
            }
        }

        return this.emitter;
    }

    @Override
    public boolean isAnyListenerInside() {
        return this.owner.isAnyListenerInside();
    }

    @Override
    public boolean isSirenActive() {
        return this.owner.isSirenActive();
    }

    @Override
    public boolean isSirenSounding() {
        return this.owner.isSirenSounding();
    }

    @Override
    public double getSirenStartTime() {
        return this.owner.getSirenStartTime();
    }

    @Override
    public void setSirenStartTime(double worldAgeHours) {
        this.owner.setSirenStartTime(worldAgeHours);
    }

    @Override
    public boolean hasLightbar() {
        return this.owner.hasLightbar();
    }

    @Override
    public LightbarSirenMode getLightbarSirenModeObject() {
        return this.owner.getLightbarSirenModeObject();
    }

    @Override
    public float getMaxWheelSteering() {
        return this.owner.getMaxWheelSteering();
    }

    @Override
    public float getMinWheelSkid() {
        return this.owner.getMinWheelSkid();
    }

    @Override
    public boolean isAnyTireMissing() {
        return this.owner.isAnyTireMissing();
    }

    public void update() {
        for (VehicleSound sound : this.sounds) {
            sound.update();
        }

        if (this.emitter != null) {
            if (this.getEngineState() != BaseVehicle.engineStateTypes.Idle || !this.emitter.isEmpty()) {
                this.getFMODParameters().update();
                this.emitter.setPos(this.owner.getX(), this.owner.getY(), this.owner.getZ());
                this.emitter.tick();
            }
        }
    }

    public void remove() {
        if (this.emitter != null) {
            for (VehicleSound sound : this.sounds) {
                sound.remove();
            }

            this.emitter.stopAll();
            SoundManager.instance.unregisterEmitter(this.emitter);
            this.emitter = null;
        }
    }

    @Override
    public FMODParameterList getFMODParameters() {
        if (this.fmodParameters.parameterList.isEmpty()) {
            this.fmodParameters.add(new ParameterVehicleBrake(this));
            this.fmodParameters.add(new ParameterVehicleEngineCondition(this));
            this.fmodParameters.add(new ParameterVehicleGear(this));
            this.fmodParameters.add(new ParameterVehicleLoad(this));
            this.fmodParameters.add(new zombie.vehicleSound.parameters.ParameterVehicleRoadMaterial(this));
            this.fmodParameters.add(new ParameterVehicleRPM(this));
            this.fmodParameters.add(new ParameterVehicleSkid(this));
            this.fmodParameters.add(new ParameterVehicleSpeed(this));
            this.fmodParameters.add(new ParameterVehicleSteer(this));
            this.fmodParameters.add(new ParameterVehicleTireMissing(this));
        }

        return this.fmodParameters;
    }

    @Override
    public void startEvent(long eventInstance, GameSoundClip clip, boolean remote, BitSet parameterSet) {
        FMODParameterList myParameters = this.getFMODParameters();
        List<FMOD_STUDIO_PARAMETER_DESCRIPTION> eventParameters = clip.getEventDescription(remote).parameters;

        for (int i = 0; i < eventParameters.size(); i++) {
            FMOD_STUDIO_PARAMETER_DESCRIPTION eventParameter = eventParameters.get(i);
            if (!parameterSet.get(eventParameter.globalIndex)) {
                FMODParameter fmodParameter = myParameters.get(eventParameter);
                if (fmodParameter != null) {
                    fmodParameter.startEventInstance(eventInstance);
                }
            }
        }
    }

    @Override
    public void updateEvent(long eventInstance, GameSoundClip clip) {
    }

    @Override
    public void stopEvent(long eventInstance, GameSoundClip clip, boolean remote, BitSet parameterSet) {
        FMODParameterList myParameters = this.getFMODParameters();
        List<FMOD_STUDIO_PARAMETER_DESCRIPTION> eventParameters = clip.getEventDescription(remote).parameters;

        for (int i = 0; i < eventParameters.size(); i++) {
            FMOD_STUDIO_PARAMETER_DESCRIPTION eventParameter = eventParameters.get(i);
            if (!parameterSet.get(eventParameter.globalIndex)) {
                FMODParameter fmodParameter = myParameters.get(eventParameter);
                if (fmodParameter != null) {
                    fmodParameter.stopEventInstance(eventInstance);
                }
            }
        }
    }

    public String getEngineSound() {
        return this.getScript() != null && this.getScript().getSounds().engine != null ? this.getScript().getSounds().engine : "VehicleEngineDefault";
    }

    public String getEngineStartSound() {
        return this.getScript() != null && this.getScript().getSounds().engineStart != null ? this.getScript().getSounds().engineStart : "VehicleStarted";
    }

    public String getEngineTurnOffSound() {
        return this.getScript() != null && this.getScript().getSounds().engineTurnOff != null ? this.getScript().getSounds().engineTurnOff : "VehicleTurnedOff";
    }

    public String getHandBrakeSound() {
        return this.getScript() != null && this.getScript().getSounds().handBrake != null ? this.getScript().getSounds().handBrake : "VehicleHandBrake";
    }

    public String getIgnitionFailSound() {
        return this.getScript() != null && this.getScript().getSounds().ignitionFail != null
            ? this.getScript().getSounds().ignitionFail
            : "VehicleFailingToStart";
    }

    public String getIgnitionFailNoPowerSound() {
        return this.getScript() != null && this.getScript().getSounds().ignitionFailNoPower != null
            ? this.getScript().getSounds().ignitionFailNoPower
            : SoundKey.VEHICLE_IGNITION_FAIL_DEFAULT.toString();
    }

    public boolean isCombinedWithEngineSound(String sound) {
        return !StringUtils.isNullOrWhitespace(sound) && StringUtils.equals(sound, this.getEngineSound());
    }
}
