// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class DeadAnimalPacket extends DeadCharacterPacket implements INetworkPacket {
}
