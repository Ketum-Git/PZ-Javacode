// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.fields.INetworkPacketField;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

public class VehiclePassengers extends VehicleField implements INetworkPacketField {
    public VehiclePassengers(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        for (int i = 0; i < this.getVehicle().getMaxPassengers(); i++) {
            short onlineId = b.getShort();
            if (GameClient.client) {
                if (onlineId == -1) {
                    IsoGameCharacter var6 = this.getVehicle().getCharacter(i);
                    if (var6 instanceof IsoGameCharacter) {
                        if (var6.isLocal()) {
                            continue;
                        }

                        var6.setVehicle(null);
                    }

                    this.getVehicle().clearPassenger(i);
                } else {
                    IsoPlayer player = GameClient.IDToPlayerMap.get(onlineId);
                    if (player == null) {
                        INetworkPacket.send(PacketTypes.PacketType.PlayerDataRequest, onlineId);
                    } else if (!player.isLocalPlayer()) {
                        if (this.getVehicle().enterRSync(i, player, this.getVehicle())) {
                            player.networkAi.parse(this.getVehicle());
                        }

                        GameClient.rememberPlayerPosition(player, this.getVehicle().getX(), this.getVehicle().getY());
                    }
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        for (int i = 0; i < this.getVehicle().getMaxPassengers(); i++) {
            BaseVehicle.Passenger passenger = this.getVehicle().getPassenger(i);
            if (passenger != null && passenger.character instanceof IsoPlayer player) {
                b.putShort(player.getOnlineID());
            } else {
                b.putShort((short)-1);
            }
        }
    }
}
