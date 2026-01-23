// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.sound;

import java.nio.ByteBuffer;
import zombie.GameSounds;
import zombie.GameWindow;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.MovingObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class PlaySoundPacket implements INetworkPacket {
    @JSONField
    String name;
    @JSONField
    MovingObject object = new MovingObject();
    @JSONField
    byte flags;
    static final byte SND_FLAG_LOOP = 1;

    private boolean isLooped() {
        return (this.flags & 1) != 0;
    }

    @Override
    public void setData(Object... values) {
        this.name = (String)values[0];
        if ((Boolean)values[1]) {
            this.flags = (byte)(this.flags | 1);
        }

        this.object.set((IsoMovingObject)values[2]);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoMovingObject object = this.getMovingObject();
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
                    if (p != null && (object == null || c.RelevantTo(object.getX(), object.getY(), radius))) {
                        ByteBufferWriter b2 = c.startPacket();
                        PacketTypes.PacketType.PlaySound.doPacket(b2);
                        this.write(b2);
                        PacketTypes.PacketType.PlaySound.send(c);
                    }
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoMovingObject movingObject = (IsoMovingObject)this.object.getObject();
        if (movingObject instanceof IsoGameCharacter isoGameCharacter) {
            BaseCharacterSoundEmitter emitter = isoGameCharacter.getEmitter();
            if (!this.isLooped()) {
                emitter.playSoundImpl(this.name, null);
            }
        } else if (movingObject != null) {
            BaseSoundEmitter emitter = movingObject.emitter;
            if (emitter == null) {
                emitter = IsoWorld.instance.getFreeEmitter(movingObject.getX(), movingObject.getY(), movingObject.getZ());
                IsoWorld.instance.takeOwnershipOfEmitter(emitter);
                movingObject.emitter = emitter;
            }

            if (!this.isLooped()) {
                emitter.playSoundImpl(this.name, (IsoObject)null);
            } else {
                emitter.playSoundLoopedImpl(this.name);
            }

            emitter.tick();
        }
    }

    public String getName() {
        return this.name;
    }

    public IsoMovingObject getMovingObject() {
        return (IsoMovingObject)this.object.getObject();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.object.parse(b, connection);
        this.name = GameWindow.ReadString(b);
        this.flags = b.get();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.object.write(b);
        b.putUTF(this.name);
        b.putByte(this.flags);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.name != null && !this.name.isEmpty();
    }

    @Override
    public int getPacketSizeBytes() {
        return 12 + this.name.length();
    }
}
