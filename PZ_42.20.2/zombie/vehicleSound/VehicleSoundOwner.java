// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound;

import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.LightbarSirenMode;

public interface VehicleSoundOwner {
    float getX();

    float getY();

    float getZ();

    int getXi();

    int getYi();

    int getZi();

    boolean isListenerInRange(float var1);

    String getScriptName();

    VehicleScript getScript();

    int getEngineCondition();

    int getEngineQuality();

    BaseVehicle.engineStateTypes getEngineState();

    boolean isEngineRunning();

    boolean isEngineSounding();

    double getEngineSpeed();

    int getTransmissionNumber();

    float getCurrentSpeedKmHour();

    float getMaxSpeed();

    default boolean hasAlarm() {
        return this.getScript().getSounds().alarmEnable;
    }

    boolean isAlarmActive();

    boolean isAlarmSoundOn();

    boolean isAlarmSounding();

    boolean isBrakePedalPressed();

    boolean isGasPedalPressed();

    ParameterVehicleRoadMaterial.Material getRoadMaterial();

    String getChosenAlarmSound();

    boolean isBackupBeeperSounding();

    boolean isDoorAlarmSounding();

    default boolean hasHorn() {
        return this.getScript().getSounds().hornEnable;
    }

    boolean isHornSounding();

    BaseSoundEmitter getVehicleSoundEmitter();

    boolean isAnyListenerInside();

    boolean isSirenActive();

    boolean isSirenSounding();

    default boolean hasSiren() {
        return this.getScript().getLightbar().enable;
    }

    double getSirenStartTime();

    void setSirenStartTime(double var1);

    default boolean sirenShutoffTimeExpired() {
        double shutoffHours = SandboxOptions.instance.sirenShutoffHours.getValue();
        if (shutoffHours <= 0.0) {
            return false;
        } else {
            double worldAge = GameTime.instance.getWorldAgeHours();
            this.setSirenStartTime(GameTime.minHours(this.getSirenStartTime(), worldAge));
            return this.getSirenStartTime() + shutoffHours < worldAge;
        }
    }

    default boolean hasLightbar() {
        return this.getScript().getLightbar().enable;
    }

    LightbarSirenMode getLightbarSirenModeObject();

    default int getLightbarSirenMode() {
        return this.getLightbarSirenModeObject().get();
    }

    default void setLightbarSirenMode(int mode) {
        this.getLightbarSirenModeObject().set(mode);
    }

    float getMaxWheelSteering();

    float getMinWheelSkid();

    boolean isAnyTireMissing();
}
