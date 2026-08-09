// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseNotMember;
import zombie.network.fields.SafeHouseTitle;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.CanSetupSafehouses,
    handlingType = 1,
    anticheats = AntiCheat.SafeHousePlayer
)
public class SafezoneClaimPacket extends SafeHouseTitle implements INetworkPacket, AntiCheatSafeHouseNotMember.IAntiCheat {
    @JSONField
    private int x;
    @JSONField
    private int y;
    @JSONField
    private int w;
    @JSONField
    private int h;

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0], (String)values[5]);
        this.x = (Integer)values[1];
        this.y = (Integer)values[2];
        this.w = (Integer)values[3];
        this.h = (Integer)values[4];
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.x = b.getInt();
        this.y = b.getInt();
        this.w = b.getInt();
        this.h = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putInt(this.w);
        b.putInt(this.h);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        boolean capability = connection.getRole().hasCapability(Capability.CanSetupSafehouses);
        if (ServerOptions.instance.playerSafehouse.getValue() || ServerOptions.instance.adminSafehouse.getValue() && capability) {
            if (!super.isConsistent(connection)) {
                return false;
            } else {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
                if (square == null) {
                    DebugType.Multiplayer.error("square is not found");
                    return false;
                } else {
                    int maxSize = ServerOptions.getInstance().maxSafezoneSize.getValue();
                    if (maxSize > 0 && this.h * this.w > maxSize) {
                        DebugType.Multiplayer.error("safezone is too big");
                        return false;
                    } else {
                        int onlineID = SafeHouse.getOnlineID(this.x, this.y);
                        if (SafeHouse.getSafeHouse(onlineID) != null) {
                            DebugType.Multiplayer.error("safezone is already claimed");
                            return false;
                        } else {
                            boolean intersects = SafeHouse.intersects(this.x, this.y, this.x + this.w, this.y + this.h);
                            if (intersects) {
                                DebugType.Multiplayer.error("safezone intersection");
                                return false;
                            } else {
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            DebugType.Multiplayer.error("safehouse options are disabled");
            return false;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        SafeHouse safehouse = SafeHouse.addSafeHouse(this.x, this.y, this.w, this.h, this.getPlayer().getUsername());
        safehouse.setTitle(this.getTitle());
        INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, safehouse);
    }
}
