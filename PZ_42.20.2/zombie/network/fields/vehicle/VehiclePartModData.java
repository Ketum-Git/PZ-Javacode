// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.io.IOException;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class VehiclePartModData extends VehicleField implements INetworkPacketField {
    public VehiclePartModData(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            BaseVehicle vehicle = this.getVehicle();
            int numParts = bb.getByte() & 255;
            int totalPartBytes = bb.getShort() & '\uffff';
            int positionOfPartsStart = bb.bb.position();
            int expectedPositionOfPartsEnd = positionOfPartsStart + totalPartBytes;

            for (int i = 0; i < numParts; i++) {
                int partIndex = bb.getByte() & 255;
                int lengthOfModData = bb.getShort() & '\uffff';
                if (lengthOfModData != 0) {
                    int positionModDataStart = bb.bb.position();
                    int positionModDataEnd = positionModDataStart + lengthOfModData;
                    VehiclePart part = vehicle.getPartByIndex(partIndex);
                    if (part == null) {
                        bb.bb.position(positionModDataEnd);
                        DebugType.Multiplayer
                            .warn(
                                "%s: could not resolve VehiclePart index: %d. Skipping forward %d bytes.",
                                this.getClass().getSimpleName(),
                                partIndex,
                                lengthOfModData
                            );
                    } else {
                        part.getModData().load(bb.bb, 249);
                        if (part.isContainer()) {
                            part.setContainerContentAmount(part.getContainerContentAmount());
                        }

                        if (bb.bb.position() != positionModDataEnd) {
                            DebugType.Multiplayer
                                .warn(
                                    "%s: unexpected buffer position for part %d. The ModData.load hasn't loaded the same number of bytes (%d) that was written (%d).",
                                    this.getClass().getSimpleName(),
                                    partIndex,
                                    bb.bb.position() - positionModDataStart,
                                    positionModDataEnd - positionModDataStart
                                );
                        }

                        bb.bb.position(positionModDataEnd);
                    }
                }
            }

            int positionOfPartsEnd = bb.bb.position();
            if (positionOfPartsEnd != expectedPositionOfPartsEnd) {
                throw new IOException("Unexpected buffer position at parts end " + positionOfPartsEnd + ". Expected: " + expectedPositionOfPartsEnd);
            }

            int endByte = bb.getByte() & 255;
            if (endByte != 255) {
                throw new IOException("Unexpected value at ModData end: " + endByte + ". Expected 0xFF");
            }
        } catch (Exception var14) {
            DebugType.Multiplayer.printException(var14, LogSeverity.Error, "%s: failed", this.getClass().getSimpleName());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            int positionOfNumParts = b.bb.position();
            b.putByte(0);
            int positionOfTotalPartBytes = b.bb.position();
            b.putShort(0);
            int positionOfPartsStart = b.bb.position();
            int numPartsWritten = 0;

            for (int j = 0; j < this.getVehicle().getPartCount(); j++) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (part.getFlag((short)16)) {
                    b.putByte(j);
                    int positionOfLength = b.position();
                    b.putShort(0);
                    int positionModDataStart = b.bb.position();
                    part.getModData().save(b.bb);
                    int positionModDataEnd = b.bb.position();
                    int modDataBytes = positionModDataEnd - positionModDataStart;
                    b.bb.position(positionOfLength);
                    b.putShort((short)modDataBytes);
                    b.bb.position(positionModDataEnd);
                    numPartsWritten++;
                }
            }

            int positionOfPartsEnd = b.bb.position();
            int totalPartBytes = positionOfPartsEnd - positionOfPartsStart;
            b.putByte(255);
            int positionOfEnd = b.bb.position();
            b.bb.position(positionOfNumParts);
            b.putByte(numPartsWritten);
            b.bb.position(positionOfTotalPartBytes);
            b.putShort(totalPartBytes);
            b.bb.position(positionOfEnd);
        } catch (Exception var12) {
            DebugType.Multiplayer.printException(var12, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
