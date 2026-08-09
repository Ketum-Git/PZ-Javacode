// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;

public class VehicleProperties extends VehicleField implements INetworkPacketField {
    public VehicleProperties(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            this.getVehicle().setHotwired(bb.getBoolean());
            this.getVehicle().setHotwiredBroken(bb.getBoolean());
            this.getVehicle().setRegulatorSpeed(bb.getFloat());
            this.getVehicle().setPreviouslyEntered(bb.getBoolean());
            boolean keyInIgnition = bb.getBoolean();
            this.getVehicle().keysContainerId = bb.getInt();
            boolean keyOnDoor = bb.getBoolean();
            InventoryItem key = null;
            if (bb.getBoolean()) {
                key = InventoryItem.loadItem(bb.bb, 249);
                this.getVehicle().ignitionSwitch.addItem(key);
            }

            if (!keyInIgnition) {
                this.getVehicle().ignitionSwitch.removeAllItems();
            }

            this.getVehicle().syncKeyInIgnition(keyInIgnition, keyOnDoor, key);
            this.getVehicle().setRust(bb.getFloat());
            this.getVehicle().setBloodIntensity("Front", bb.getFloat());
            this.getVehicle().setBloodIntensity("Rear", bb.getFloat());
            this.getVehicle().setBloodIntensity("Left", bb.getFloat());
            this.getVehicle().setBloodIntensity("Right", bb.getFloat());
            this.getVehicle().setColorHSV(bb.getFloat(), bb.getFloat(), bb.getFloat());
            this.getVehicle().setSkinIndex(bb.getInt());
            this.getVehicle().updateSkin();
            this.getVehicle().setAlarmed(bb.getBoolean());
        } catch (Exception var6) {
            DebugType.Multiplayer.printException(var6, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.putBoolean(this.getVehicle().isHotwired());
            b.putBoolean(this.getVehicle().isHotwiredBroken());
            b.putFloat(this.getVehicle().getRegulatorSpeed());
            b.putBoolean(this.getVehicle().isPreviouslyEntered());
            b.putBoolean(this.getVehicle().isKeysInIgnition());
            b.putInt(this.getVehicle().keysContainerId);
            b.putBoolean(this.getVehicle().isKeyIsOnDoor());
            InventoryItem key = this.getVehicle().getCurrentKey();
            if (b.putBoolean(key != null)) {
                key.saveWithSize(b.bb, false);
            }

            b.putFloat(this.getVehicle().getRust());
            b.putFloat(this.getVehicle().getBloodIntensity("Front"));
            b.putFloat(this.getVehicle().getBloodIntensity("Rear"));
            b.putFloat(this.getVehicle().getBloodIntensity("Left"));
            b.putFloat(this.getVehicle().getBloodIntensity("Right"));
            b.putFloat(this.getVehicle().getColorHue());
            b.putFloat(this.getVehicle().getColorSaturation());
            b.putFloat(this.getVehicle().getColorValue());
            b.putInt(this.getVehicle().getSkinIndex());
            b.putBoolean(this.getVehicle().isAlarmed());
        } catch (Exception var3) {
            DebugType.Multiplayer.printException(var3, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
