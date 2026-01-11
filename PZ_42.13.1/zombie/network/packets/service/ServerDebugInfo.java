// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.MPDebugInfo;

@PacketSetting(ordering = 0, priority = 1, reliability = 0, requiredCapability = Capability.ConnectWithDebug, handlingType = 3)
public class ServerDebugInfo implements INetworkPacket {
    public static final byte PKT_LOADED = 1;
    public static final byte PKT_REPOP = 2;
    public byte packetType;
    public short repopEpoch;

    public void setRequestServerInfo() {
        this.packetType = 1;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.packetType = b.get();
        if (GameServer.server) {
            if (this.packetType == 1) {
                MPDebugInfo.instance.requestTime = System.currentTimeMillis();
                MPDebugInfo.instance.requestPacketReceived = true;
                this.repopEpoch = b.getShort();
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.ServerDebugInfo.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.ServerDebugInfo.send(connection);
                if (this.repopEpoch != MPDebugInfo.instance.repopEpoch) {
                    this.packetType = 2;
                }
            }

            if (this.packetType == 2) {
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.ServerDebugInfo.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.ServerDebugInfo.send(connection);
            }
        } else {
            if (this.packetType == 1) {
                MPDebugInfo.instance.cellPool.release(MPDebugInfo.instance.loadedCells);
                MPDebugInfo.instance.loadedCells.clear();
                short numCells = b.getShort();

                for (int i = 0; i < numCells; i++) {
                    MPDebugInfo.MPCell cell = MPDebugInfo.instance.cellPool.alloc();
                    cell.cx = b.getShort();
                    cell.cy = b.getShort();
                    cell.currentPopulation = b.getShort();
                    cell.desiredPopulation = b.getShort();
                    cell.lastRepopTime = b.getFloat();
                    MPDebugInfo.instance.loadedCells.add(cell);
                }

                MPDebugInfo.instance.loadedAreas.clear();
                short numAreas = b.getShort();

                for (int i = 0; i < numAreas; i++) {
                    int ax = b.getShort();
                    int ay = b.getShort();
                    int aw = b.getShort();
                    int ah = b.getShort();
                    MPDebugInfo.instance.loadedAreas.add(ax, ay, aw, ah);
                }
            }

            if (this.packetType == 2) {
                MPDebugInfo.instance.repopEventPool.release(MPDebugInfo.instance.repopEvents);
                MPDebugInfo.instance.repopEvents.clear();
                MPDebugInfo.instance.repopEpoch = b.getShort();
                short numEvents = b.getShort();

                for (int i = 0; i < numEvents; i++) {
                    MPDebugInfo.MPRepopEvent re = MPDebugInfo.instance.repopEventPool.alloc();
                    re.wx = b.getShort();
                    re.wy = b.getShort();
                    re.worldAge = b.getFloat();
                    MPDebugInfo.instance.repopEvents.add(re);
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.packetType);
        if (GameClient.client) {
            if (this.packetType == 1) {
                b.putShort(MPDebugInfo.instance.repopEpoch);
            }

            if (this.packetType == 2) {
            }
        } else {
            if (this.packetType == 1) {
                b.putShort((short)MPDebugInfo.instance.loadedCells.size());

                for (int i = 0; i < MPDebugInfo.instance.loadedCells.size(); i++) {
                    MPDebugInfo.MPCell cell = MPDebugInfo.instance.loadedCells.get(i);
                    b.putShort(cell.cx);
                    b.putShort(cell.cy);
                    b.putShort(cell.currentPopulation);
                    b.putShort(cell.desiredPopulation);
                    b.putFloat(cell.lastRepopTime);
                }

                b.putShort((short)MPDebugInfo.instance.loadedAreas.count);

                for (int i = 0; i < MPDebugInfo.instance.loadedAreas.count; i++) {
                    int n = i * 4;
                    b.putShort((short)MPDebugInfo.instance.loadedAreas.areas[n++]);
                    b.putShort((short)MPDebugInfo.instance.loadedAreas.areas[n++]);
                    b.putShort((short)MPDebugInfo.instance.loadedAreas.areas[n++]);
                    b.putShort((short)MPDebugInfo.instance.loadedAreas.areas[n++]);
                }
            }

            if (this.packetType == 2) {
                b.putShort(MPDebugInfo.instance.repopEpoch);
                b.putShort((short)MPDebugInfo.instance.repopEvents.size());

                for (int i = 0; i < MPDebugInfo.instance.repopEvents.size(); i++) {
                    MPDebugInfo.MPRepopEvent evt = MPDebugInfo.instance.repopEvents.get(i);
                    b.putShort((short)evt.wx);
                    b.putShort((short)evt.wy);
                    b.putFloat(evt.worldAge);
                }
            }
        }
    }
}
