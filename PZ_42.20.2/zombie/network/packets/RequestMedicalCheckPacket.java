// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class RequestMedicalCheckPacket implements INetworkPacket {
    private static final int MAX_CHECK_DISTANCE = 2;
    private RequestMedicalCheckPacket.RequestType type;
    private final PlayerID target = new PlayerID();
    private final PlayerID requester = new PlayerID();

    @Override
    public void setData(Object... values) {
        this.type = (RequestMedicalCheckPacket.RequestType)values[0];
        this.target.set((IsoPlayer)values[1]);
        this.requester.set((IsoPlayer)values[2]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.type = b.getEnum(RequestMedicalCheckPacket.RequestType.class);
        this.target.parse(b, connection);
        this.requester.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.type);
        this.target.write(b);
        this.requester.write(b);
    }

    @Override
    public void processClient(UdpConnection connection) {
        switch (this.type) {
            case ASK:
                LuaEventManager.triggerEvent("RequestMedicalCheck", this.target.getPlayer(), this.requester.getPlayer());
                break;
            case ACCEPT:
                LuaEventManager.triggerEvent("AcceptedMedicalCheck", this.target.getPlayer(), this.requester.getPlayer());
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        UdpConnection targetConnection = GameServer.getConnectionFromPlayer(
            this.type == RequestMedicalCheckPacket.RequestType.ASK ? this.target.getPlayer() : this.requester.getPlayer()
        );
        if (targetConnection != null && !isCheckForbidden(this.target.getPlayer(), this.requester.getPlayer())) {
            this.sendToClient(PacketTypes.PacketType.RequestMedicalCheck, targetConnection);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.target.getPlayer() != null && this.requester.getPlayer() != null;
    }

    public static boolean isCheckForbidden(IsoPlayer playerOne, IsoPlayer playerTwo) {
        return Math.abs(playerOne.getX() - playerTwo.getX()) > 2.0F || Math.abs(playerOne.getY() - playerTwo.getY()) > 2.0F;
    }

    public static enum RequestType {
        ASK,
        ACCEPT;
    }
}
