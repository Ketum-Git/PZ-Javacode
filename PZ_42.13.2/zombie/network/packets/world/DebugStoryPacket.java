// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.world;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.util.StringUtils;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.CreateStory, handlingType = 1)
public class DebugStoryPacket implements INetworkPacket {
    @JSONField
    protected final Square square = new Square();
    @JSONField
    protected int type;
    @JSONField
    protected String name;

    @Override
    public void setData(Object... values) {
        this.square.set((IsoGridSquare)values[0]);
        this.type = (Integer)values[1];
        this.name = (String)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.square.write(b);
        b.putInt(this.type);
        b.putUTF(this.name);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.square.parse(b, connection);
        this.type = b.getInt();
        this.name = GameWindow.ReadString(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.square.isConsistent(connection) && !StringUtils.isNullOrEmpty(this.name);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.type == 0) {
            RandomizedVehicleStoryBase rvs = IsoWorld.instance.getRandomizedVehicleStoryByName(this.name);
            if (rvs != null) {
                rvs.randomizeVehicleStory(this.square.getSquare().getZone(), this.square.getSquare().getChunk());
            }
        } else if (this.type == 1) {
            RandomizedZoneStoryBase rzs = IsoWorld.instance.getRandomizedZoneStoryByName(this.name);
            if (rzs != null) {
                Zone zone = new Zone(
                    "debugstoryzone",
                    "debugstoryzone",
                    this.square.getSquare().getX() - 20,
                    this.square.getSquare().getY() - 20,
                    this.square.getSquare().getZ(),
                    this.square.getSquare().getX() + 20,
                    this.square.getSquare().getX() + 20
                );
                zone.setPickedXForZoneStory(this.square.getSquare().getX());
                zone.setPickedYForZoneStory(this.square.getSquare().getY());
                zone.setX(this.square.getSquare().getX() - rzs.getMinimumWidth() / 2);
                zone.setY(this.square.getSquare().getY() - rzs.getMinimumHeight() / 2);
                zone.setW(rzs.getMinimumWidth() + 2);
                zone.setH(rzs.getMinimumHeight() + 2);
                rzs.randomizeZoneStory(zone);
            }
        }
    }
}
