// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.Square;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.vehicle.VehicleID;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ObjectChangePacket implements INetworkPacket {
    @JSONField
    IsoObject o;
    @JSONField
    String change;
    @JSONField
    KahluaTable tbl;

    @Override
    public void setData(Object... values) {
        this.set((IsoObject)values[0], (String)values[1], (KahluaTable)values[2]);
    }

    private void set(IsoObject o, String change, KahluaTable tbl) {
        this.o = o;
        this.change = change;
        this.tbl = tbl;
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (this.o instanceof IsoPlayer isoPlayer) {
            b.putByte((byte)1);
            PlayerID player = new PlayerID();
            player.set(isoPlayer);
            player.write(b);
        } else if (this.o instanceof BaseVehicle baseVehicle) {
            b.putByte((byte)2);
            VehicleID vehicleID = new VehicleID();
            vehicleID.set(baseVehicle);
            vehicleID.write(b);
        } else if (this.o instanceof IsoWorldInventoryObject isoWorldInventoryObject) {
            b.putByte((byte)3);
            Square square = new Square();
            square.set(this.o.getSquare());
            square.write(b);
            b.putInt(isoWorldInventoryObject.getItem().getID());
        } else if (this.o instanceof IsoDeadBody) {
            b.putByte((byte)4);
            Square square = new Square();
            square.set(this.o.getSquare());
            square.write(b);
            b.putInt(this.o.getStaticMovingObjectIndex());
        } else {
            b.putByte((byte)0);
            Square square = new Square();
            square.set(this.o.getSquare());
            square.write(b);
            b.putInt(this.o.getSquare().getObjects().indexOf(this.o));
        }

        b.putUTF(this.change);
        this.o.saveChange(this.change, this.tbl, b.bb);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        byte type = b.get();
        if (type == 1) {
            PlayerID player = new PlayerID();
            player.parse(b, connection);
            this.o = player.getPlayer();
            String change = GameWindow.ReadString(b);
            if (Core.debug) {
                DebugLog.log("receiveObjectChange " + change);
            }

            if (player.isConsistent(connection)) {
                player.getPlayer().loadChange(change, b);
            } else if (Core.debug) {
                DebugLog.log("receiveObjectChange: player can't be found=" + this.getDescription());
            }
        } else if (type == 2) {
            VehicleID vehicleID = new VehicleID();
            vehicleID.parse(b, connection);
            this.o = vehicleID.getVehicle();
            String changex = GameWindow.ReadString(b);
            if (Core.debug) {
                DebugLog.log("receiveObjectChange " + changex);
            }

            if (vehicleID.isConsistent(connection)) {
                vehicleID.getVehicle().loadChange(changex, b);
            } else if (Core.debug) {
                DebugLog.log("receiveObjectChange: vehicle can't be found=" + this.getDescription());
            }
        } else if (type == 3) {
            Square square = new Square();
            square.parse(b, connection);
            int itemID = b.getInt();
            String changexx = GameWindow.ReadString(b);
            if (Core.debug) {
                DebugLog.log("receiveObjectChange " + changexx);
            }

            if (!square.isConsistent(connection)) {
                GameClient.instance.delayPacket(PZMath.fastfloor(square.getX()), PZMath.fastfloor(square.getY()), PZMath.fastfloor(square.getZ()));
                return;
            }

            for (int i = 0; i < square.getSquare().getWorldObjects().size(); i++) {
                IsoWorldInventoryObject worldItem = square.getSquare().getWorldObjects().get(i);
                if (worldItem.getItem() != null && worldItem.getItem().getID() == itemID) {
                    worldItem.loadChange(changexx, b);
                    return;
                }
            }

            if (Core.debug) {
                DebugLog.log("receiveObjectChange: object can't be found (square=" + square.getDescription() + ", itemID=" + itemID + ")");
            }
        } else if (type == 4) {
            Square squarex = new Square();
            squarex.parse(b, connection);
            int index = b.getInt();
            String changexxx = GameWindow.ReadString(b);
            if (!squarex.isConsistent(connection)) {
                GameClient.instance.delayPacket(PZMath.fastfloor(squarex.getX()), PZMath.fastfloor(squarex.getY()), PZMath.fastfloor(squarex.getZ()));
                return;
            }

            if (index >= 0 && index < squarex.getSquare().getStaticMovingObjects().size()) {
                this.o = squarex.getSquare().getStaticMovingObjects().get(index);
                this.o.loadChange(changexxx, b);
            } else if (Core.debug) {
                DebugLog.log("receiveObjectChange: object can't be found (square=" + squarex.getDescription() + ", index=" + index + ")");
            }
        } else {
            Square squarexx = new Square();
            squarexx.parse(b, connection);
            int indexx = b.getInt();
            String changexxxx = GameWindow.ReadString(b);
            if (Core.debug) {
                DebugLog.log("receiveObjectChange " + changexxxx);
            }

            if (!squarexx.isConsistent(connection)) {
                GameClient.instance.delayPacket(PZMath.fastfloor(squarexx.getX()), PZMath.fastfloor(squarexx.getY()), PZMath.fastfloor(squarexx.getZ()));
                return;
            }

            if (indexx >= 0 && indexx < squarexx.getSquare().getObjects().size()) {
                this.o = squarexx.getSquare().getObjects().get(indexx);
                this.o.loadChange(changexxxx, b);
            } else if (Core.debug) {
                DebugLog.log("receiveObjectChange: object can't be found (square=" + squarexx.getDescription() + ", index=" + indexx + ")");
            }
        }
    }
}
