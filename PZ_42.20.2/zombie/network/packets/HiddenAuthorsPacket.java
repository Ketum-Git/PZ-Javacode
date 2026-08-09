// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.worldMap.network.HiddenAuthors;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class HiddenAuthorsPacket implements INetworkPacket {
    public static final int MAX_USER_NAMES = 127;
    @JSONField
    private boolean hidden;
    @JSONField
    private final List<String> userNames = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        this.hidden = (Boolean)values[0];
        this.userNames.clear();
        if (values[1] instanceof String) {
            this.userNames.add((String)values[1]);
        } else if (values[1] instanceof Collection && ((Collection)values[1]).size() <= 127) {
            this.userNames.addAll((Collection<? extends String>)values[1]);
        } else {
            throw new IllegalArgumentException("Invalid userNames array");
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.hidden = b.getBoolean();
        this.userNames.clear();
        int count = b.getByte();

        for (int i = 0; i < count; i++) {
            this.userNames.add(b.getUTF());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.hidden);
        b.putByte(this.userNames.size());

        for (String userName : this.userNames) {
            b.putUTF(userName);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!GameServer.server) {
            return true;
        } else {
            for (String userName : this.userNames) {
                if (!GameServer.UserNameToPlayerMap.containsKey(userName)) {
                    return false;
                }
            }

            return true;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        for (String userName : this.userNames) {
            HiddenAuthors.serverSetAuthorHidden(connection.getUserName(), userName, this.hidden);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        for (String userName : this.userNames) {
            HiddenAuthors.clientSetAuthorHidden(userName, this.hidden);
        }
    }
}
