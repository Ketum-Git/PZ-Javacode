// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.sound;

import java.nio.ByteBuffer;
import zombie.WorldSoundManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class WorldSoundPacket implements INetworkPacket {
    @JSONField
    int x;
    @JSONField
    int y;
    @JSONField
    int z;
    @JSONField
    int radius;
    @JSONField
    int volume;
    @JSONField
    boolean stressHumans;
    @JSONField
    float zombieIgnoreDist;
    @JSONField
    float stressMod;
    @JSONField
    boolean sourceIsZombie;
    @JSONField
    boolean repeating;
    @JSONField
    boolean stressAnimals;

    @Override
    public void setData(Object... values) {
        this.x = ((WorldSoundManager.WorldSound)values[0]).x;
        this.y = ((WorldSoundManager.WorldSound)values[0]).y;
        this.z = ((WorldSoundManager.WorldSound)values[0]).z;
        this.radius = ((WorldSoundManager.WorldSound)values[0]).radius;
        this.volume = ((WorldSoundManager.WorldSound)values[0]).volume;
        this.stressHumans = ((WorldSoundManager.WorldSound)values[0]).stresshumans;
        this.zombieIgnoreDist = ((WorldSoundManager.WorldSound)values[0]).zombieIgnoreDist;
        this.stressMod = ((WorldSoundManager.WorldSound)values[0]).stressMod;
        this.sourceIsZombie = ((WorldSoundManager.WorldSound)values[0]).sourceIsZombie;
        this.repeating = ((WorldSoundManager.WorldSound)values[0]).repeating;
        this.stressAnimals = ((WorldSoundManager.WorldSound)values[0]).stressAnimals;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putInt(this.z);
        b.putInt(this.radius);
        b.putInt(this.volume);
        b.putByte((byte)(this.stressHumans ? 1 : 0));
        b.putFloat(this.zombieIgnoreDist);
        b.putFloat(this.stressMod);
        b.putByte((byte)(this.sourceIsZombie ? 1 : 0));
        b.putByte((byte)(this.repeating ? 1 : 0));
        b.putByte((byte)(this.stressAnimals ? 1 : 0));
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getInt();
        this.radius = b.getInt();
        this.volume = b.getInt();
        this.stressHumans = b.get() == 1;
        this.zombieIgnoreDist = b.getFloat();
        this.stressMod = b.getFloat();
        this.sourceIsZombie = b.get() == 1;
        this.repeating = b.get() == 1;
        this.stressAnimals = b.get() == 1;
    }

    @Override
    public void processClient(UdpConnection connection) {
        WorldSoundManager.instance
            .addSound(
                null,
                this.x,
                this.y,
                this.z,
                this.radius,
                this.volume,
                this.stressHumans,
                this.zombieIgnoreDist,
                this.stressMod,
                this.sourceIsZombie,
                false,
                true,
                this.repeating,
                this.stressAnimals
            );
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        DebugLog.Sound.noise("x=%d y=%d z=%d, radius=%d", this.x, this.y, this.z, this.radius);
        WorldSoundManager.WorldSound sound = WorldSoundManager.instance
            .addSound(
                null,
                this.x,
                this.y,
                this.z,
                this.radius,
                this.volume,
                this.stressHumans,
                this.zombieIgnoreDist,
                this.stressMod,
                this.sourceIsZombie,
                false,
                true,
                this.repeating,
                this.stressAnimals
            );
        if (sound != null) {
            if (this.stressAnimals) {
                IsoPlayer player = GameServer.getAnyPlayerFromConnection(connection);
                if (player != null) {
                    player.callOut = true;
                }
            }

            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (connection.getConnectedGUID() != c.getConnectedGUID() && c.isFullyConnected() && c.RelevantTo(this.x, this.y, this.radius)) {
                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.WorldSoundPacket.doPacket(b);
                    this.write(b);
                    PacketTypes.PacketType.WorldSoundPacket.send(c);
                }
            }
        }
    }
}
