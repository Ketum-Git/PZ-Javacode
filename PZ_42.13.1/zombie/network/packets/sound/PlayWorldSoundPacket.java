// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.sound;

import java.nio.ByteBuffer;
import zombie.GameSounds;
import zombie.GameWindow;
import zombie.SoundManager;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTrap;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class PlayWorldSoundPacket implements INetworkPacket {
    String name;
    int x;
    int y;
    byte z;
    int index;

    public void set(String name, int x, int y, byte z, int index) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.index = index;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection)) {
            int radius = 70;
            GameSound gameSound = GameSounds.getSound(this.getName());
            if (gameSound != null) {
                for (int i = 0; i < gameSound.clips.size(); i++) {
                    GameSoundClip clip = gameSound.clips.get(i);
                    if (clip.hasMaxDistance()) {
                        radius = Math.max(radius, (int)clip.distanceMax);
                    }
                }
            }

            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID() && c.isFullyConnected()) {
                    IsoPlayer p = GameServer.getAnyPlayerFromConnection(c);
                    if (p != null && c.RelevantTo(this.getX(), this.getY(), radius)) {
                        ByteBufferWriter b2 = c.startPacket();
                        PacketTypes.PacketType.PlayWorldSound.doPacket(b2);
                        this.write(b2);
                        PacketTypes.PacketType.PlayWorldSound.send(c);
                    }
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.index == -1) {
            SoundManager.instance.PlayWorldSoundImpl(this.name, false, this.x, this.y, this.z, 1.0F, 20.0F, 2.0F, false);
        } else {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
            if (square != null && square.getObjects().get(this.index) instanceof IsoTrap trap) {
                trap.playExplosionSound();
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.get();
        this.name = GameWindow.ReadString(b);
        this.index = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        b.putUTF(this.name);
        b.putInt(this.index);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.name != null && !this.name.isEmpty();
    }

    @Override
    public int getPacketSizeBytes() {
        return 12 + this.name.length();
    }

    @Override
    public String getDescription() {
        return "\n\tPlayWorldSoundPacket [name=" + this.name + " | x=" + this.x + " | y=" + this.y + " | z=" + this.z + " ]";
    }
}
