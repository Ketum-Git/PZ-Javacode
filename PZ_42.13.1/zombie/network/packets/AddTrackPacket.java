// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.animals.AnimalTracks;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class AddTrackPacket implements INetworkPacket {
    int x;
    int y;
    byte z;
    AnimalTracks tracks;

    @Override
    public void setData(Object... values) {
        if (values.length == 2) {
            this.set((IsoGridSquare)values[0], (AnimalTracks)values[1]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoGridSquare sq, AnimalTracks tracks) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
        this.tracks = tracks;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);

        try {
            this.tracks.save(b.bb);
        } catch (IOException var3) {
            throw new RuntimeException(var3);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.get();
        this.tracks = new AnimalTracks();

        try {
            this.tracks.load(b, 0);
        } catch (IOException var4) {
            throw new RuntimeException(var4);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null) {
            new IsoAnimalTrack(sq, this.tracks.getTrackSprite(), this.tracks);
        }
    }
}
