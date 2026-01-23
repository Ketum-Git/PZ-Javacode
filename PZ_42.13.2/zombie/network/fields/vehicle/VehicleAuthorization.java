// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;

public class VehicleAuthorization extends VehicleField implements INetworkPacketField {
    @JSONField
    protected BaseVehicle.Authorization authorization = BaseVehicle.Authorization.Server;
    @JSONField
    protected short authorizationPlayer = -1;

    public VehicleAuthorization(VehicleID vehicleID) {
        super(vehicleID);
    }

    public void set(BaseVehicle vehicle) {
        this.authorization = vehicle.netPlayerAuthorization;
        this.authorizationPlayer = vehicle.netPlayerId;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        try {
            this.authorization = BaseVehicle.Authorization.values()[b.getInt()];
            this.authorizationPlayer = b.getShort();
        } catch (Exception var4) {
            DebugLog.Multiplayer.printException(var4, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.putInt(this.authorization.ordinal());
            b.putShort(this.authorizationPlayer);
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    public BaseVehicle.Authorization getAuthorization() {
        return this.authorization;
    }

    public short getAuthorizationPlayer() {
        return this.authorizationPlayer;
    }
}
