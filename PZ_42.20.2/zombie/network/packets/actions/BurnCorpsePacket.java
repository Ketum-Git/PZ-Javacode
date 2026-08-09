// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.IsoUtils;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFireManager;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class BurnCorpsePacket implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    protected final ObjectID objectId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);

    @Override
    public void setData(Object... values) {
        if (values.length == 2) {
            this.player.set((IsoPlayer)values[0]);
            this.objectId.set((ObjectID)values[1]);
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.player.parse(b, connection);
        this.objectId.load(b.bb);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        this.objectId.save(b.bb);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection)) {
            IsoDeadBody corpse = (IsoDeadBody)this.objectId.getObject();
            if (corpse == null) {
                DebugType.Multiplayer.warn("Corpse not found by id: %s", this.objectId);
                return;
            }

            float dist = IsoUtils.DistanceTo(this.player.getPlayer().getX(), this.player.getPlayer().getY(), corpse.getX(), corpse.getY());
            if (dist <= 1.8F) {
                IsoFireManager.StartFire(corpse.getCell(), corpse.getSquare(), true, 100);
            } else {
                DebugType.Multiplayer.warn("Distance between player and corpse too big: %s", dist);
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.player.isConsistent(connection);
    }
}
