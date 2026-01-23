// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.IsoRoom;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class MetaGridPacket implements INetworkPacket {
    short cellX = -1;
    short cellY = -1;
    short roomIndex = -1;
    boolean lightsActive;

    public boolean set(int cellX, int cellY, int roomIndex) {
        IsoMetaGrid mg = IsoWorld.instance.metaGrid;
        if (cellX >= mg.getMinX() && cellX <= mg.getMaxX() && cellY >= mg.getMinY() && cellY <= mg.getMaxY()) {
            IsoMetaCell cell = mg.getCellData(cellX, cellY);
            if (cell.info != null && roomIndex >= 0 && roomIndex < cell.roomList.size()) {
                this.cellX = (short)cellX;
                this.cellY = (short)cellY;
                this.roomIndex = (short)roomIndex;
                this.lightsActive = cell.roomList.get(roomIndex).lightsActive;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoMetaGrid mg = IsoWorld.instance.metaGrid;
        if (this.cellX >= mg.getMinX() && this.cellX <= mg.getMaxX() && this.cellY >= mg.getMinY() && this.cellY <= mg.getMaxY()) {
            IsoMetaCell cell = mg.getCellData(this.cellX, this.cellY);
            if (cell.info != null && this.roomIndex >= 0 && this.roomIndex < cell.roomList.size()) {
                RoomDef roomDef = cell.roomList.get(this.roomIndex);
                roomDef.lightsActive = this.lightsActive;
                IsoRoom room = mg.getRoomByID(roomDef.getID());
                if (room != null) {
                    if (!room.lightSwitches.isEmpty()) {
                        room.lightSwitches.get(0).switchLight(roomDef.lightsActive);
                    }
                }
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.cellX = b.getShort();
        this.cellY = b.getShort();
        this.roomIndex = b.getShort();
        this.lightsActive = b.get() != 0;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.cellX);
        b.putShort(this.cellY);
        b.putShort(this.roomIndex);
        b.putBoolean(this.lightsActive);
    }
}
