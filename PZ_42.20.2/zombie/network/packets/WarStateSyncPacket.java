// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.GameTime;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.WarManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class WarStateSyncPacket implements INetworkPacket {
    @JSONField
    protected int onlineId;
    @JSONField
    protected String attacker;
    @JSONField
    protected WarManager.State state;

    public void set(int onlineId, String attacker, WarManager.State state) {
        this.onlineId = onlineId;
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
    public void parse(ByteBufferReader b, IConnection connection) {
        this.onlineId = b.getInt();
        this.attacker = b.getUTF();
        this.state = WarManager.State.valueOf(b.getInt());
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (GameServer.server) {
            if (!ServerOptions.getInstance().war.getValue()) {
                DebugType.Multiplayer.error("war is disabled");
                return false;
            }

            SafeHouse safeHouse = SafeHouse.getSafeHouse(this.onlineId);
            if (safeHouse == null) {
                DebugType.Multiplayer.error("war safehouse error");
                return false;
            }

            WarManager.War war = WarManager.getWar(this.onlineId, this.attacker);
            switch (this.state) {
                case Canceled:
                    if (war == null || !war.isValidState(this.state)) {
                        DebugType.Multiplayer.error("war state error");
                        return false;
                    }
                case Claimed:
                    if (!connection.hasPlayer(this.attacker)) {
                        DebugType.Multiplayer.error("war claim error");
                        return false;
                    }
                    break;
                case Accepted:
                case Refused:
                    if (war == null || !war.isValidState(this.state)) {
                        DebugType.Multiplayer.error("war state error");
                        return false;
                    }

                    if (!connection.hasPlayer(safeHouse.getOwner())) {
                        DebugType.Multiplayer.error("war accept or refuse error");
                        return false;
                    }
                    break;
                case Ended:
                    if (connection.getRole().hasCapability(Capability.CanGoInsideSafehouses)) {
                        return true;
                    }
                case Started:
                case Blocked:
                    if (war == null || !war.isValidState(this.state)) {
                        DebugType.Multiplayer.error("war state error");
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
