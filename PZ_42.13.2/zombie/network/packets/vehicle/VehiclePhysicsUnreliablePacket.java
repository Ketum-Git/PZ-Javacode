// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import zombie.characters.Capability;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 8, priority = 2, reliability = 1, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class VehiclePhysicsUnreliablePacket extends VehiclePhysicsPacket {
}
