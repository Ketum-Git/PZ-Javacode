// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.chat.ChatManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.radio.ZomboidRadio;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class WaveSignalPacket implements INetworkPacket {
    int sourceX;
    int sourceY;
    int channel;
    String msg;
    String guid;
    String codes;
    float r;
    float g;
    float b;
    int signalStrength;
    boolean isTv;

    public void set(int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV) {
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.channel = channel;
        this.msg = msg;
        this.guid = guid;
        this.codes = codes;
        this.r = r;
        this.g = g;
        this.b = b;
        this.signalStrength = signalStrength;
        this.isTv = isTV;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.sourceX = b.getInt();
        this.sourceY = b.getInt();
        this.channel = b.getInt();
        this.msg = null;
        if (b.getBoolean()) {
            this.msg = b.getUTF();
        }

        this.guid = null;
        if (b.getBoolean()) {
            this.guid = b.getUTF();
        }

        this.codes = null;
        if (b.getBoolean()) {
            this.codes = b.getUTF();
        }

        this.r = b.getFloat();
        this.g = b.getFloat();
        this.b = b.getFloat();
        this.signalStrength = b.getInt();
        this.isTv = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.sourceX);
        b.putInt(this.sourceY);
        b.putInt(this.channel);
        if (b.putBoolean(this.msg != null)) {
            b.putUTF(this.msg);
        }

        if (b.putBoolean(this.guid != null)) {
            b.putUTF(this.guid);
        }

        if (b.putBoolean(this.codes != null)) {
            b.putUTF(this.codes);
        }

        b.putFloat(this.r);
        b.putFloat(this.g);
        b.putFloat(this.b);
        b.putInt(this.signalStrength);
        b.putBoolean(this.isTv);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (ChatManager.getInstance().isWorking()) {
            ZomboidRadio.getInstance()
                .DistributeTransmission(
                    this.sourceX, this.sourceY, this.channel, this.msg, this.guid, this.codes, this.r, this.g, this.b, this.signalStrength, this.isTv
                );
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        ZomboidRadio.getInstance()
            .SendTransmission(
                connection.getConnectedGUID(),
                this.sourceX,
                this.sourceY,
                this.channel,
                this.msg,
                this.guid,
                this.codes,
                this.r,
                this.g,
                this.b,
                this.signalStrength,
                this.isTv
            );
    }
}
