// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public interface HitCharacter extends INetworkPacket {
    boolean isRelevant(UdpConnection var1);

    default void attack() {
    }

    default void react() {
    }

    default void update() {
    }

    void preProcess();

    void process();

    void postProcess();

    default void log(UdpConnection connection) {
    }

    @Override
    default void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.log(connection);
        this.update();
        GameServer.sendHitCharacter(this, packetType, connection);
        this.processClient(connection);
    }

    @Override
    default void processClient(UdpConnection connection) {
        this.preProcess();
        this.process();
        this.postProcess();
        this.attack();
        this.react();
    }
}
