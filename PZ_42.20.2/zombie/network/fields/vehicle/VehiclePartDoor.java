// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.io.IOException;
import zombie.Lua.LuaEventManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class VehiclePartDoor extends VehicleField implements INetworkPacketField {
    public VehiclePartDoor(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            BaseVehicle vehicle = this.getVehicle();
            int numParts = bb.getByte() & 255;
            int totalPartBytes = bb.getShort() & '\uffff';
            int positionOfPartsStart = bb.position();
            int expectedPositionOfPartsEnd = positionOfPartsStart + totalPartBytes;

            for (int i = 0; i < numParts; i++) {
                int partIndex = bb.getByte() & 255;
                int lengthOfDoorData = bb.getShort() & '\uffff';
                int positionPartDataStart = bb.position();
                int positionOfPartDataEnd = positionPartDataStart + lengthOfDoorData;
                if (lengthOfDoorData != 0) {
                    VehiclePart part = vehicle.getPartByIndex(partIndex);
                    if (part == null) {
                        bb.position(positionOfPartDataEnd);
                        DebugType.Multiplayer
                            .warn(
                                "%s: could not resolve VehiclePart index: %d. Skipping forward %d bytes.",
                                this.getClass().getSimpleName(),
                                partIndex,
                                lengthOfDoorData
                            );
                    } else {
                        part.getDoor().load(bb.bb, 249);
                        if (bb.position() != positionOfPartDataEnd) {
                            DebugType.Multiplayer
                                .warn(
                                    "%s: unexpected buffer position for part %d. The DoorData.load hasn't loaded the same number of bytes (%d) that was written (%d).",
                                    this.getClass().getSimpleName(),
                                    partIndex,
                                    bb.position() - positionPartDataStart,
                                    positionOfPartDataEnd - positionPartDataStart
                                );
                        }

                        bb.position(positionOfPartDataEnd);
                    }
                }
            }

            int positionOfPartsEnd = bb.position();
            if (positionOfPartsEnd != expectedPositionOfPartsEnd) {
                throw new IOException("Unexpected buffer position at parts end " + positionOfPartsEnd + ". Expected: " + expectedPositionOfPartsEnd);
            }

            int endByte = bb.getByte() & 255;
            if (endByte != 255) {
                throw new IOException("Unexpected value at ModData end: " + endByte + ". Expected 0xFF");
            }

            LuaEventManager.triggerEvent("OnContainerUpdate");
            vehicle.doDamageOverlay();
        } catch (Exception var14) {
            DebugType.Multiplayer.printException(var14, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            int positionOfNumParts = b.position();
            b.putByte(0);
            int positionOfTotalPartBytes = b.position();
            b.putShort(0);
            int numParts = 0;
            int positionOfPartsStart = b.position();

            for (int j = 0; j < this.getVehicle().getPartCount(); j++) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (part.getFlag((short)512)) {
                    b.putByte(j);
                    int positionOfLengthOfDoorData = b.position();
                    b.putShort(0);
                    int positionOfDoorDataStart = b.position();
                    part.getDoor().save(b.bb);
                    int positionOfDoorDataEnd = b.position();
                    int lengthOfDoorData = positionOfDoorDataEnd - positionOfDoorDataStart;
                    b.position(positionOfLengthOfDoorData);
                    b.putShort(lengthOfDoorData);
                    b.position(positionOfDoorDataEnd);
                    numParts++;
                }
            }

            int totalPartBytes = b.position() - positionOfPartsStart;
            b.putByte(255);
            int positionOfEnd = b.position();
            b.position(positionOfNumParts);
            b.putByte(numParts);
            b.position(positionOfTotalPartBytes);
            b.putShort(totalPartBytes);
            b.position(positionOfEnd);
        } catch (Exception var12) {
            DebugType.Multiplayer.printException(var12, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
