// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import zombie.characters.Capability;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 8, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class VehiclePhysicsReliablePacket extends VehiclePhysicsPacket {
}
