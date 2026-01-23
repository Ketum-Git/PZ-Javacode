// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.network.fields.INetworkPacketField;

public class VehicleProperties extends VehicleField implements INetworkPacketField {
    public VehicleProperties(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        try {
            this.getVehicle().setHotwired(bb.get() == 1);
            this.getVehicle().setHotwiredBroken(bb.get() == 1);
            this.getVehicle().setRegulatorSpeed(bb.getFloat());
            this.getVehicle().setPreviouslyEntered(bb.get() == 1);
            boolean keyInIgnition = bb.get() == 1;
            this.getVehicle().keysContainerId = bb.getInt();
            boolean keyOnDoor = bb.get() == 1;
            InventoryItem key = null;
            if (bb.get() == 1) {
                key = InventoryItem.loadItem(bb, 241);
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
        } catch (Exception var6) {
            DebugLog.Multiplayer.printException(var6, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.bb.put((byte)(this.getVehicle().isHotwired() ? 1 : 0));
            b.bb.put((byte)(this.getVehicle().isHotwiredBroken() ? 1 : 0));
            b.bb.putFloat(this.getVehicle().getRegulatorSpeed());
            b.bb.put((byte)(this.getVehicle().isPreviouslyEntered() ? 1 : 0));
            b.bb.put((byte)(this.getVehicle().isKeysInIgnition() ? 1 : 0));
            b.bb.putInt(this.getVehicle().keysContainerId);
            b.bb.put((byte)(this.getVehicle().isKeyIsOnDoor() ? 1 : 0));
            InventoryItem key = this.getVehicle().getCurrentKey();
            if (key == null) {
                b.bb.put((byte)0);
            } else {
                b.bb.put((byte)1);
                key.saveWithSize(b.bb, false);
            }

            b.bb.putFloat(this.getVehicle().getRust());
            b.bb.putFloat(this.getVehicle().getBloodIntensity("Front"));
            b.bb.putFloat(this.getVehicle().getBloodIntensity("Rear"));
            b.bb.putFloat(this.getVehicle().getBloodIntensity("Left"));
            b.bb.putFloat(this.getVehicle().getBloodIntensity("Right"));
            b.bb.putFloat(this.getVehicle().getColorHue());
            b.bb.putFloat(this.getVehicle().getColorSaturation());
            b.bb.putFloat(this.getVehicle().getColorValue());
            b.bb.putInt(this.getVehicle().getSkinIndex());
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
