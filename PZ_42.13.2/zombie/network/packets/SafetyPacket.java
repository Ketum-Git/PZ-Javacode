// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Safety;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PVPLogTool;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.util.Type;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SafetyPacket extends Safety implements INetworkPacket {
    private short id;
    private IsoPlayer player;

    public SafetyPacket(Safety safety) {
        this.enabled = safety.isEnabled();
        this.last = safety.isLast();
        this.cooldown = safety.getCooldown();
        this.toggle = safety.getToggle();
        this.player = Type.tryCastTo(safety.getCharacter(), IsoPlayer.class);
        if (this.player != null) {
            if (GameServer.server) {
                this.id = this.player.getOnlineID();
            } else if (GameClient.client) {
                this.id = (short)this.player.getPlayerNum();
            }
        }
    }

    public SafetyPacket() {
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id = b.getShort();
        this.load(b, IsoWorld.getWorldVersion());
        if (GameServer.server) {
            this.player = GameServer.getPlayerFromConnection(connection, this.id);
        } else if (GameClient.client) {
            this.player = GameClient.IDToPlayerMap.get(this.id);
        } else {
            this.player = null;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.id);
        this.save(b.bb);
    }

    @Override
    public int getPacketSizeBytes() {
        return 12;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.player != null;
    }

    @Override
    public String getDescription() {
        return INetworkPacket.super.getDescription()
            + (this.player == null ? ":" : ": \"" + this.player.getUsername() + "\"")
            + " id="
            + this.id
            + " "
            + super.getDescription();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        PVPLogTool.logSafety(this.player, "set");
        this.processClient(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.isConsistent(connection)) {
            if (GameServer.server) {
                this.player.getSafety().toggleSafety();
            } else if (GameClient.client) {
                this.player.setSafety(this);
            }
        }
    }
}
