// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.NetworkCharacterAI;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerOptions;
import zombie.network.fields.IMovable;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.PlayerPacket;

public class AntiCheatSpeed extends AbstractAntiCheat {
    private static final int MAX_SPEED = 10;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatSpeed.IAntiCheat field = (AntiCheatSpeed.IAntiCheat)packet;
        if (packet instanceof PlayerPacket playerPacket && !playerPacket.getPlayer().isDead()) {
            ((NetworkCharacterAI.SpeedChecker)field.getMovable())
                .set(playerPacket.prediction.position.x, playerPacket.prediction.position.y, playerPacket.getPlayer().isSeatedInVehicle());
        }

        if (!connection.role.hasCapability(Capability.TeleportToPlayer)
            && !connection.role.hasCapability(Capability.TeleportToCoordinates)
            && !connection.role.hasCapability(Capability.TeleportPlayerToAnotherPlayer)
            && !connection.role.hasCapability(Capability.UseFastMoveCheat)) {
            float limit = field.getMovable().isVehicle() ? (float)ServerOptions.instance.speedLimit.getValue() : 10.0F;
            return field.getMovable().getSpeed() > limit ? String.format("speed=%f > limit=%f", field.getMovable().getSpeed(), limit) : result;
        } else {
            return result;
        }
    }

    public interface IAntiCheat {
        IMovable getMovable();
    }
}
