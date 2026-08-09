// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;
import zombie.scripting.objects.VehicleScript;

public interface VehiclePartOwner extends IVehicleEngineListener {
    float getX();

    float getY();

    float getZ();

    int getXi();

    int getYi();

    int getZi();

    IsoGridSquare getSquare();

    default IsoGameCharacter getDriver() {
        return null;
    }

    default IsoGameCharacter getDriverRegardlessOfTow() {
        return null;
    }

    VehicleParts getParts();

    default int getPartCount() {
        return this.getParts().size();
    }

    default VehiclePart getPartByIndex(int index) {
        return this.getParts().getPartByIndex(index);
    }

    default VehiclePart getPartByPartId(zombie.scripting.objects.VehiclePart id) {
        return id == null ? null : this.getPartById(id.toString());
    }

    default VehiclePart getPartById(String id) {
        return this.getParts().getPartById(id);
    }

    default int getPartIndex(String id) {
        return this.getParts().getPartIndex(id);
    }

    default VehiclePart getTrunkDoorPart() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUNK_DOOR);
    }

    default VehiclePart getTrunkPart() {
        VehiclePart truckBedPart = this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUCK_BED);
        return truckBedPart != null ? truckBedPart : this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUCK_BED_OPEN);
    }

    default VehiclePart getTrailerTrunkPart() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRAILER_TRUNK);
    }

    default int getNumberOfPartsWithContainers() {
        return this.getParts().getNumberOfPartsWithContainers();
    }

    default VehiclePart getHeater() {
        return this.getPartById("Heater");
    }

    VehicleScript getScript();

    default VehiclePart getBattery() {
        return this.getParts().getBattery();
    }

    default VehiclePart getEngine() {
        return this.getParts().getEngine();
    }

    default VehiclePart getGasTank() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.GAS_TANK);
    }

    default float getGasRemaining() {
        VehiclePart gasTank = this.getGasTank();
        return gasTank == null ? 0.0F : gasTank.getContainerContentAmount();
    }

    LightbarLightsMode getLightbarLightsModeObject();

    default int getLightbarLightsMode() {
        return this.getLightbarLightsModeObject().get();
    }

    default void setLightbarLightsMode(int mode) {
        this.getLightbarLightsModeObject().set(mode);
    }

    LightbarSirenMode getLightbarSirenModeObject();

    default void setLightbarSirenMode(int mode) {
        this.getLightbarSirenModeObject().set(mode);
    }

    default float getBatteryCharge() {
        return this.getParts().getBatteryCharge();
    }

    boolean getHeadlightsOn();

    default int windowsOpen() {
        int result = 0;

        for (int i = 0; i < this.getPartCount(); i++) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.window != null && part.window.open) {
                result++;
            }
        }

        return result;
    }

    void setEngineFeature(int var1, int var2, int var3);

    boolean isEngineWorking();

    float getBrakeSpeedBetweenUpdate();

    BaseVehicle.ModelInfo setModelVisible(VehiclePart var1, VehicleScript.Model var2, boolean var3);

    void updateDamageOverlayLater();

    void updateTotalMass();

    void updateBulletStats();

    void updatePartStats();

    void transmitEngine();

    void transmitPartCondition(VehiclePart var1);

    void transmitPartDoor(VehiclePart var1);

    void transmitPartItem(VehiclePart var1);

    void transmitPartLight(VehiclePart var1);

    void transmitPartModData(VehiclePart var1);

    void transmitPartUsedDelta(VehiclePart var1);

    void transmitPartWindow(VehiclePart var1);
}
