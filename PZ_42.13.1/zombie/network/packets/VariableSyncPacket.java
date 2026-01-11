// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.HashSet;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 7)
public class VariableSyncPacket implements INetworkPacket {
    public PlayerID player = new PlayerID();
    public String key;
    public String value;
    public boolean boolValue;
    public float floatValue;
    public VariableSyncPacket.VariableType varType;
    public static HashSet<String> syncedVariables = new HashSet<>();

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putUTF(this.key);
        b.putByte((byte)this.varType.ordinal());
        switch (this.varType) {
            case String:
                b.putUTF(this.value);
                break;
            case Boolean:
                b.putByte((byte)(this.boolValue ? 1 : 0));
                break;
            case Float:
                b.putFloat(this.floatValue);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.player.parse(b, connection);
        this.key = GameWindow.ReadString(b);
        this.varType = VariableSyncPacket.VariableType.values()[b.get()];
        switch (this.varType) {
            case String:
                this.value = GameWindow.ReadString(b);
                break;
            case Boolean:
                this.boolValue = b.get() != 0;
                break;
            case Float:
                this.floatValue = b.getFloat();
        }
    }

    @Override
    public void setData(Object... values) {
        this.player.set((IsoPlayer)values[0]);
        this.key = (String)values[1];
        if (values[2] instanceof String) {
            this.varType = VariableSyncPacket.VariableType.String;
            this.value = (String)values[2];
        }

        if (values[2] instanceof Boolean) {
            this.varType = VariableSyncPacket.VariableType.Boolean;
            this.boolValue = (Boolean)values[2];
        }

        if (values[2] instanceof Float) {
            this.varType = VariableSyncPacket.VariableType.Float;
            this.floatValue = (Float)values[2];
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        switch (this.varType) {
            case String:
                this.player.getPlayer().setVariable(this.key, this.value);
                break;
            case Boolean:
                this.player.getPlayer().setVariable(this.key, this.boolValue);
                break;
            case Float:
                this.player.getPlayer().setVariable(this.key, this.floatValue);
        }

        if (!syncedVariables.contains(this.key)) {
            syncedVariables.add(this.key);
        }

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID() && c.RelevantTo(this.player.getPlayer().getX(), this.player.getPlayer().getY())) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.VariableSync.doPacket(b2);
                this.write(b2);
                PacketTypes.PacketType.VariableSync.send(c);
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        switch (this.varType) {
            case String:
                this.player.getPlayer().setVariable(this.key, this.value);
                break;
            case Boolean:
                this.player.getPlayer().setVariable(this.key, this.boolValue);
                break;
            case Float:
                this.player.getPlayer().setVariable(this.key, this.floatValue);
        }
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        switch (this.varType) {
            case String:
                this.player.getPlayer().setVariable(this.key, this.value);
                break;
            case Boolean:
                this.player.getPlayer().setVariable(this.key, this.boolValue);
                break;
            case Float:
                this.player.getPlayer().setVariable(this.key, this.floatValue);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.player.isConsistent(connection) && !this.key.isEmpty();
    }

    public static enum VariableType {
        String,
        Boolean,
        Float;
    }
}
