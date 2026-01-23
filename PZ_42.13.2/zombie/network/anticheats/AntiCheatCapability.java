// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;

public class AntiCheatCapability extends AbstractAntiCheat {
    public static boolean validate(UdpConnection connection, Capability capability) {
        return connection.role.hasCapability(capability);
    }
}
