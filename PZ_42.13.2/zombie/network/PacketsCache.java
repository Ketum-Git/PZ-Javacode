// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.HashMap;
import zombie.debug.DebugLog;
import zombie.network.packets.INetworkPacket;

public abstract class PacketsCache {
    private final HashMap<PacketTypes.PacketType, INetworkPacket> packets = new HashMap<>();

    protected PacketsCache() {
        for (PacketTypes.PacketType packetType : PacketTypes.PacketType.values()) {
            try {
                this.packets.put(packetType, packetType.handler.getDeclaredConstructor().newInstance());
            } catch (Exception var6) {
                DebugLog.Packet.debugln("No packet instance of type \"%s\"", packetType.name());
            }
        }
    }

    public INetworkPacket getPacket(PacketTypes.PacketType packetType) {
        return this.packets.get(packetType);
    }
}
