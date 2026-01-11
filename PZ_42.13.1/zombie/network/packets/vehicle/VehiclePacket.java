// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.JSONField;
import zombie.network.fields.Position;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleCache;
import zombie.vehicles.VehicleManager;

public abstract class VehiclePacket implements INetworkPacket {
    @JSONField
    protected final VehicleID vehicleID = new VehicleID();
    @JSONField
    protected final Position position = new Position();
    protected IsoGridSquare square;

    public void set(BaseVehicle vehicle) {
        this.vehicleID.set(vehicle);
        this.position.set(vehicle.getX(), vehicle.getY(), vehicle.jniTransform.origin.y);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.vehicleID.isConsistent(connection) || this.square != null;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.vehicleID.parse(b, connection);
        this.position.parse(b, connection);
        this.square = IsoWorld.instance.currentCell.getGridSquare((double)this.position.getX(), (double)this.position.getY(), 0.0);
        VehicleCache.vehicleUpdate(this.vehicleID.getID(), this.position.getX(), this.position.getY(), 0.0F);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.vehicleID.write(b);
        this.position.write(b);
    }

    protected boolean doRequest(UdpConnection connection) {
        if (!this.vehicleID.isConsistent(connection) && this.square != null) {
            VehicleManager.instance.sendVehicleRequest(this.vehicleID.getID(), (short)1);
            return true;
        } else {
            return false;
        }
    }

    protected boolean doRemove(UdpConnection connection) {
        if (this.vehicleID.isConsistent(connection) && this.square == null) {
            boolean needDelete = true;

            for (int n = 0; n < IsoPlayer.numPlayers; n++) {
                IsoPlayer player = IsoPlayer.players[n];
                if (player != null && player.getVehicle() == this.vehicleID.getVehicle()) {
                    needDelete = false;
                    player.setPosition(this.position.getX(), this.position.getY(), 0.0F);
                    VehicleManager.instance.sendVehicleRequest(this.vehicleID.getID(), (short)2);
                }
            }

            if (needDelete) {
                this.vehicleID.getVehicle().removeFromWorld();
                this.vehicleID.getVehicle().removeFromSquare();
            }

            return true;
        } else {
            return false;
        }
    }
}
