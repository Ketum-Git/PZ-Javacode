// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncVisualsPacket implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    ItemVisuals itemVisuals = new ItemVisuals();
    private int itemVisualsSize;

    @Override
    public void setData(Object... values) {
        if (values.length == 1 && values[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer player) {
        this.playerId.set(player);
        this.itemVisuals.clear();
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.playerId.getPlayer() != null && this.itemVisualsSize == this.itemVisuals.size();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        this.playerId.getPlayer().getItemVisuals(this.itemVisuals);
        this.itemVisualsSize = b.get();
        if (this.itemVisualsSize != this.itemVisuals.size()) {
            DebugLog.General.error("Player has " + this.itemVisuals.size() + " itemVisuals but server tries to sync " + this.itemVisualsSize + " ones");
        } else {
            for (int i = 0; i < this.itemVisualsSize; i++) {
                ItemVisual itemVisual = this.itemVisuals.get(i);

                for (int j = 0; j < BloodBodyPartType.MAX.index(); j++) {
                    BloodBodyPartType part = BloodBodyPartType.FromIndex(j);
                    byte basicPatch = b.get();
                    byte denimPatch = b.get();
                    byte leatherPatch = b.get();
                    byte hole = b.get();
                    float dirt = b.getFloat();
                    float blood = b.getFloat();
                    itemVisual.removePatch(part.index());
                    itemVisual.removeHole(part.index());
                    if (basicPatch != 0) {
                        itemVisual.setBasicPatch(part);
                    }

                    if (denimPatch != 0) {
                        itemVisual.setDenimPatch(part);
                    }

                    if (leatherPatch != 0) {
                        itemVisual.setLeatherPatch(part);
                    }

                    if (hole != 0) {
                        itemVisual.setHole(part);
                    }

                    itemVisual.setDirt(part, dirt);
                    itemVisual.setBlood(part, blood);
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.playerId.getPlayer().resetModelNextFrame();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID() && c.RelevantTo(this.playerId.getX(), this.playerId.getY())) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.SyncVisuals.doPacket(b2);
                this.write(b2);
                PacketTypes.PacketType.SyncVisuals.send(c);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        this.playerId.getPlayer().getItemVisuals(this.itemVisuals);
        b.putByte((byte)this.itemVisuals.size());

        for (int i = 0; i < this.itemVisuals.size(); i++) {
            ItemVisual itemVisual = this.itemVisuals.get(i);

            for (int j = 0; j < BloodBodyPartType.MAX.index(); j++) {
                BloodBodyPartType part = BloodBodyPartType.FromIndex(j);
                b.putByte((byte)(itemVisual.getBasicPatch(part) != 0.0F ? 1 : 0));
                b.putByte((byte)(itemVisual.getDenimPatch(part) != 0.0F ? 1 : 0));
                b.putByte((byte)(itemVisual.getLeatherPatch(part) != 0.0F ? 1 : 0));
                b.putByte((byte)(itemVisual.getHole(part) != 0.0F ? 1 : 0));
                b.putFloat(itemVisual.getDirt(part));
                b.putFloat(itemVisual.getBlood(part));
            }
        }
    }
}
