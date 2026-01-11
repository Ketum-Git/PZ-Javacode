// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.radio.devices.DeviceData;

public final class ParameterDeviceVolume extends FMODLocalParameter {
    private final DeviceData deviceData;

    public ParameterDeviceVolume(DeviceData deviceData) {
        super("DeviceVolume");
        this.deviceData = deviceData;
    }

    @Override
    public float calculateCurrentValue() {
        return this.deviceData.getDeviceVolume();
    }
}
