// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.IsoGameCharacter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

public class AntiCheatHitVehicle extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (packet instanceof AntiCheatHitVehicle.IAntiCheat field) {
            BaseVehicle vehicle = field.getVehicle();
            if (vehicle == null) {
                return "vehicle not found";
            } else {
                IsoGameCharacter driver = vehicle.getDriverRegardlessOfTow();
                if (driver == null) {
                    return "driver not found";
                } else {
                    return !connection.hasPlayer(driver.getOnlineID()) ? "driver is not authorized" : result;
                }
            }
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        BaseVehicle getVehicle();
    }
}
