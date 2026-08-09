// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 2, reliability = 0, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ZombieSynchronizationUnreliablePacket extends ZombieSynchronizationPacket {
}
