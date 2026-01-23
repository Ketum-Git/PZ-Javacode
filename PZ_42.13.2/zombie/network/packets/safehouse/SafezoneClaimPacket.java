// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.areas.SafeHouse;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseSurvivor;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.CanSetupSafehouses,
    handlingType = 1,
    anticheats = AntiCheat.SafeHouseSurviving
)
public class SafezoneClaimPacket extends PlayerID implements INetworkPacket, AntiCheatSafeHouseSurvivor.IAntiCheat {
    @JSONField
    public int x;
    @JSONField
    public int y;
    @JSONField
    public int w;
    @JSONField
    public int h;
    @JSONField
    private String title;

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
        this.x = (Integer)values[1];
        this.y = (Integer)values[2];
        this.w = (Integer)values[3];
        this.h = (Integer)values[4];
        this.title = (String)values[5];
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.x = b.getInt();
        this.y = b.getInt();
        this.w = b.getInt();
        this.h = b.getInt();
        this.title = GameWindow.ReadString(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putInt(this.w);
        b.putInt(this.h);
        b.putUTF(this.title);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        boolean capability = connection.role.hasCapability(Capability.CanSetupSafehouses);
        if (ServerOptions.instance.playerSafehouse.getValue() || ServerOptions.instance.adminSafehouse.getValue() && capability) {
            if (!super.isConsistent(connection)) {
                DebugLog.Multiplayer.error("player is not found");
                return false;
            } else if (StringUtils.isNullOrEmpty(this.title)) {
                DebugLog.Multiplayer.error("title is not set");
                return false;
            } else {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
                if (square == null) {
                    DebugLog.Multiplayer.error("square is not found");
                    return false;
                } else {
                    int maxSize = ServerOptions.getInstance().maxSafezoneSize.getValue();
                    if (maxSize > 0 && this.h * this.w > maxSize) {
                        DebugLog.Multiplayer.error("safezone is too big");
                        return false;
                    } else {
                        int onlineID = SafeHouse.getOnlineID(this.x, this.y);
                        if (SafeHouse.getSafeHouse(onlineID) != null) {
                            DebugLog.Multiplayer.error("safehouse is already claimed");
                            return false;
                        } else {
                            boolean intersects = SafeHouse.intersects(this.x, this.y, this.x + this.w, this.y + this.h);
                            if (intersects) {
                                DebugLog.Multiplayer.error("can't be safezone");
                                return false;
                            } else {
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            DebugLog.Multiplayer.error("safehouse options are disabled");
            return false;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        SafeHouse safehouse = SafeHouse.addSafeHouse(this.x, this.y, this.w, this.h, this.getPlayer().getUsername());
        safehouse.setTitle(this.title);
        INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, safehouse, false);
    }

    @Override
    public IsoPlayer getSurvivor() {
        return this.getPlayer();
    }
}
