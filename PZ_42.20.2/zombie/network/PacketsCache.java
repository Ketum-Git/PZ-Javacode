// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import zombie.core.Core;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.packets.INetworkPacket;

public abstract class PacketsCache {
    private final HashMap<PacketTypes.PacketType, INetworkPacket> packets = new HashMap<>();
    private final HashMap<PacketTypes.PacketType, Integer> hashes = new HashMap<>();
    private final HashMap<PacketTypes.PacketType, List<Long>> limits = new HashMap<>();

    protected PacketsCache() {
        StringBuilder stringBuilder = new StringBuilder();

        for (PacketTypes.PacketType packetType : PacketTypes.PacketType.values()) {
            if (packetType.handler == null) {
                if (!stringBuilder.isEmpty()) {
                    stringBuilder.append(", ");
                }

                stringBuilder.append(packetType.name());
            } else {
                try {
                    this.packets.put(packetType, packetType.handler.getDeclaredConstructor().newInstance());
                } catch (Exception var7) {
                    DebugType.Packet.printException(var7, LogSeverity.Warning, "Error creating packet type: \"%s\"", packetType.name());
                }
            }
        }

        if (!stringBuilder.isEmpty()) {
            stringBuilder.insert(0, "No packet handler for type: ");
            DebugType.Packet.warn(stringBuilder.toString());
        }
    }

    public INetworkPacket getPacket(PacketTypes.PacketType packetType) {
        return this.packets.get(packetType);
    }

    public boolean isHashEquals(PacketTypes.PacketType packetType, Integer hash) {
        return hash.equals(this.hashes.put(packetType, hash));
    }

    public boolean isLimitExceeded(PacketTypes.PacketType packetType) {
        long currentTime = System.currentTimeMillis();

        for (List<Long> limit : this.limits.values()) {
            limit.removeIf(timestamp -> currentTime > timestamp + 1000L);
            limit.removeIf(timestamp -> currentTime < timestamp);
        }

        if (!this.limits.containsKey(packetType)) {
            this.limits.put(packetType, new LinkedList<>());
        }

        List<Long> limit = this.limits.get(packetType);
        if (GameClient.client) {
            limit.add(currentTime);
        }

        if (limit.size() <= ServerOptions.getInstance().maxPacketsPerSecond.getValue()) {
            if (GameServer.server) {
                limit.add(currentTime);
            }

            return false;
        } else {
            DebugType.Multiplayer.warn("Packets limit has exceeded for %s", packetType.name());
            if (Core.debug) {
                StringBuilder stringBuilder = new StringBuilder();

                for (Entry<PacketTypes.PacketType, List<Long>> temp : this.limits.entrySet()) {
                    if (!temp.getValue().isEmpty()) {
                        stringBuilder.append(temp.getKey()).append("=").append(temp.getValue().size()).append(", ");
                    }
                }

                DebugType.Multiplayer.debugln(stringBuilder.toString());
            }

            return true;
        }
    }
}
