// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.sound;

import zombie.WorldSoundManager;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.CharacterID;
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
    @JSONField
    boolean stressZombies;
    @JSONField
    final CharacterID sourceCharacterId = new CharacterID();

    @Override
    public void setData(Object... values) {
        WorldSoundManager.WorldSound worldSound = (WorldSoundManager.WorldSound)values[0];
        this.x = worldSound.x;
        this.y = worldSound.y;
        this.z = worldSound.z;
        this.radius = worldSound.radius;
        this.volume = worldSound.volume;
        this.stressHumans = worldSound.stresshumans;
        this.zombieIgnoreDist = worldSound.zombieIgnoreDist;
        this.stressMod = worldSound.stressMod;
        this.sourceIsZombie = worldSound.sourceIsZombie;
        this.repeating = worldSound.repeating;
        this.stressAnimals = worldSound.stressAnimals;
        this.stressZombies = worldSound.stressZombies;
        if (worldSound.source instanceof IsoGameCharacter sourceGameCharacter) {
            this.sourceCharacterId.set(sourceGameCharacter);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putInt(this.z);
        b.putInt(this.radius);
        b.putInt(this.volume);
        b.putBoolean(this.stressHumans);
        b.putFloat(this.zombieIgnoreDist);
        b.putFloat(this.stressMod);
        b.putBoolean(this.sourceIsZombie);
        b.putBoolean(this.repeating);
        b.putBoolean(this.stressAnimals);
        b.putBoolean(this.stressZombies);
        this.sourceCharacterId.write(b);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getInt();
        this.radius = b.getInt();
        this.volume = b.getInt();
        this.stressHumans = b.getBoolean();
        this.zombieIgnoreDist = b.getFloat();
        this.stressMod = b.getFloat();
        this.sourceIsZombie = b.getBoolean();
        this.repeating = b.getBoolean();
        this.stressAnimals = b.getBoolean();
        this.stressZombies = b.getBoolean();
        this.sourceCharacterId.parse(b, connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        short flags = 0;
        if (this.stressZombies) {
            flags = (short)(flags | 4);
        }

        if (this.stressAnimals) {
            flags = (short)(flags | 1);
        }

        if (this.stressHumans) {
            flags = (short)(flags | 2);
        }

        WorldSoundManager.instance
            .addSound(
                this.sourceCharacterId.getCharacter(),
                this.x,
                this.y,
                this.z,
                this.radius,
                this.volume,
                this.zombieIgnoreDist,
                this.stressMod,
                this.sourceIsZombie,
                false,
                true,
                this.repeating,
                flags
            );
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        DebugType.Sound.noise("x=%d y=%d z=%d, radius=%d", this.x, this.y, this.z, this.radius);
        short flags = 0;
        if (this.stressZombies) {
            flags = (short)(flags | 4);
        }

        if (this.stressAnimals) {
            flags = (short)(flags | 1);
        }

        if (this.stressHumans) {
            flags = (short)(flags | 2);
        }

        WorldSoundManager.WorldSound sound = WorldSoundManager.instance
            .addSound(
                this.sourceCharacterId.getCharacter(),
                this.x,
                this.y,
                this.z,
                this.radius,
                this.volume,
                this.zombieIgnoreDist,
                this.stressMod,
                this.sourceIsZombie,
                false,
                true,
                this.repeating,
                flags
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
