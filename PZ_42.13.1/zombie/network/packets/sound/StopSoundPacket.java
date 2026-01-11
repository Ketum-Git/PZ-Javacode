// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.sound;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.MovingObject;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class StopSoundPacket implements INetworkPacket {
    MovingObject object = new MovingObject();
    String name;
    boolean trigger;

    public void set(IsoMovingObject obj, String name, boolean trigger) {
        this.object.set(obj);
        this.name = name;
        this.trigger = trigger;
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoObject obj = this.object.getObject();
        if (obj instanceof IsoGameCharacter gameCharacter) {
            if (this.trigger) {
                gameCharacter.getEmitter().stopOrTriggerSoundByName(this.name);
            } else {
                gameCharacter.getEmitter().stopSoundByName(this.name);
            }
        } else if (obj instanceof BaseVehicle baseVehicle) {
            if (this.trigger) {
                baseVehicle.getEmitter().stopOrTriggerSoundByName(this.name);
            } else {
                baseVehicle.getEmitter().stopSoundByName(this.name);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToClients(packetType, connection);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.trigger = b.get() == 1;
        this.object.parse(b, connection);
        this.name = GameWindow.ReadString(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)(this.trigger ? 1 : 0));
        this.object.write(b);
        b.putUTF(this.name);
    }

    @Override
    public int getPacketSizeBytes() {
        return this.object.getPacketSizeBytes() + 2 + this.name.length();
    }

    @Override
    public String getDescription() {
        return "\n\tStopSoundPacket [name=" + this.name + " | object=" + this.object.getDescription() + "]";
    }
}
