// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;

@PacketSetting(
    ordering = 5,
    priority = 1,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = {AntiCheat.Power, AntiCheat.Speed, AntiCheat.NoClip, AntiCheat.Player}
)
public class PlayerPacketReliable extends PlayerPacket {
}
