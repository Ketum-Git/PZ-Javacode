// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 7, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class AnimalUpdateReliablePacket extends AnimalUpdatePacket {
}
