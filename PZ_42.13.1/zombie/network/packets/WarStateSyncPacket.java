// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.WarManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class WarStateSyncPacket implements INetworkPacket {
    @JSONField
    protected int onlineId;
    @JSONField
    protected String attacker;
    @JSONField
    protected WarManager.State state;
    WarManager.War war;

    public void set(int onlineID, String attacker, WarManager.State state) {
        this.onlineId = onlineID;
        this.attacker = attacker;
        this.state = state;
    }

    @Override
    public void setData(Object... values) {
        this.set((Integer)values[0], (String)values[1], (WarManager.State)values[2]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.onlineId);
        b.putUTF(this.attacker);
        b.putInt(this.state.ordinal());
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.onlineId = b.getInt();
        this.attacker = GameWindow.ReadString(b);
        this.state = WarManager.State.valueOf(b.getInt());
        this.war = WarManager.getWar(this.onlineId, this.attacker);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        if (GameServer.server) {
            SafeHouse safeHouse = SafeHouse.getSafeHouse(this.onlineId);
            if (safeHouse == null) {
                DebugLog.Multiplayer.error("war safehouse error");
                return false;
            }

            switch (this.state) {
                case Canceled:
                    if (this.war == null || !this.war.isValidState(this.state)) {
                        DebugLog.Multiplayer.error("war state error");
                        return false;
                    }
                case Claimed:
                    if (!connection.havePlayer(this.attacker)) {
                        DebugLog.Multiplayer.error("war claim error");
                        return false;
                    }
                    break;
                case Accepted:
                case Refused:
                    if (this.war == null || !this.war.isValidState(this.state)) {
                        DebugLog.Multiplayer.error("war state error");
                        return false;
                    }

                    if (!connection.havePlayer(safeHouse.getOwner())) {
                        DebugLog.Multiplayer.error("war accept or refuse error");
                        return false;
                    }
                    break;
                case Ended:
                    if (connection.role.hasCapability(Capability.CanGoInsideSafehouses)) {
                        return true;
                    }
                case Started:
                case Blocked:
                    if (this.war == null || !this.war.isValidState(this.state)) {
                        DebugLog.Multiplayer.error("war state error");
                        return false;
                    }
            }
        }

        return true;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        switch (this.state) {
            case Canceled:
            case Accepted:
            case Refused:
            case Ended:
                WarManager.updateWar(this.onlineId, this.attacker, this.state, GameTime.getServerTimeMills());
                break;
            case Claimed:
                WarManager.updateWar(this.onlineId, this.attacker, this.state, GameTime.getServerTimeMills() + WarManager.getStartDelay());
        }
    }
}
