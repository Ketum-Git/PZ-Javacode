// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.animals.AnimalTracks;
import zombie.characters.animals.AnimalZones;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 7, priority = 2, reliability = 2, requiredCapability = Capability.AnimalCheats, handlingType = 2)
public class ManageAnimalTrackOnWorldMapPacket implements INetworkPacket {
    @JSONField
    protected AnimalTracks tracks;
    @JSONField
    protected boolean isAdded;

    @Override
    public void setData(Object... values) {
        if (values.length == 2 && values[0] instanceof AnimalTracks animalTracks && values[1] instanceof Boolean add) {
            this.tracks = animalTracks;
            this.isAdded = add;
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.isAdded);
        if (this.isAdded) {
            try {
                this.tracks.save(b.bb);
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        } else {
            b.putInt(this.tracks.x);
            b.putInt(this.tracks.y);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.isAdded = b.getBoolean();
        if (this.isAdded) {
            this.tracks = new AnimalTracks();

            try {
                this.tracks.load(b.bb, 0);
            } catch (IOException var7) {
                throw new RuntimeException(var7);
            }
        } else {
            int x = b.getInt();
            int y = b.getInt();

            for (AnimalTracks track : AnimalZones.clientTracks) {
                if (track.x == x && track.y == y) {
                    this.tracks = track;
                    break;
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.isAdded) {
            if (!AnimalZones.clientTracks.contains(this.tracks)) {
                AnimalZones.clientTracks.add(this.tracks);
            }
        } else if (this.tracks != null) {
            AnimalZones.clientTracks.remove(this.tracks);
        }
    }
}
