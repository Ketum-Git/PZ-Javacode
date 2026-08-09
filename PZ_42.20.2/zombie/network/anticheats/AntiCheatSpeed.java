// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.ServerOptions;
import zombie.network.fields.IMovable;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSpeed extends AbstractAntiCheat {
    private static final int MAX_SPEED = 20;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (packet instanceof AntiCheatSpeed.IAntiCheat field) {
            int movableCount = field.getMovableCount();

            for (int i = 0; i < movableCount; i++) {
                IMovable movable = field.getMovable(i);
                if (movable != null) {
                    field.resetMovable();
                    if (!connection.getRole().hasCapability(Capability.TeleportToPlayer)
                        && !connection.getRole().hasCapability(Capability.TeleportToCoordinates)
                        && !connection.getRole().hasCapability(Capability.TeleportPlayerToAnotherPlayer)
                        && !connection.getRole().hasCapability(Capability.UseFastMoveCheat)) {
                        float limit = movable.isVehicle() ? (float)ServerOptions.instance.speedLimit.getValue() : 20.0F;
                        if (movable.getSpeed() > limit) {
                            return String.format("speed=%f > limit=%f", movable.getSpeed(), limit);
                        }
                    }
                }
            }

            return result;
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        default void resetMovable() {
        }

        IMovable getMovable(int var1);

        int getMovableCount();
    }
}
