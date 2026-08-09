// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import java.util.ArrayList;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.Group;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class SafehouseSyncPacket extends Group implements INetworkPacket {
    @JSONField
    private int x;
    @JSONField
    private int y;
    @JSONField
    private int w;
    @JSONField
    private int h;
    @JSONField
    private int hitPoints;
    @JSONField
    private final ArrayList<String> membersRespawn = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0]);
    }

    @Override
    public void set(SafeHouse safehouse) {
        super.set(safehouse);
        this.x = safehouse.getX();
        this.y = safehouse.getY();
        this.w = safehouse.getW();
        this.h = safehouse.getH();
        this.hitPoints = safehouse.getHitPoints();
        this.membersRespawn.clear();
        this.membersRespawn.addAll(safehouse.getPlayersRespawn());
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.x = b.getInt();
        this.y = b.getInt();
        this.w = b.getInt();
        this.h = b.getInt();
        this.hitPoints = b.getInt();
        this.membersRespawn.clear();
        short membersRespawnSize = b.getShort();

        for (int i = 0; i < membersRespawnSize; i++) {
            this.membersRespawn.add(b.getUTF());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putInt(this.w);
        b.putInt(this.h);
        b.putInt(this.hitPoints);
        b.putShort(this.membersRespawn.size());

        for (String member : this.membersRespawn) {
            b.putUTF(member);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        SafeHouse safeHouse = SafeHouse.getSafeHouse(this.x, this.y, this.w, this.h);
        if (safeHouse == null) {
            safeHouse = SafeHouse.addSafeHouse(this.x, this.y, this.w, this.h, this.owner);
        }

        safeHouse.setTitle(this.title);
        safeHouse.setOwner(this.owner);
        safeHouse.getPlayers().clear();
        safeHouse.getPlayers().addAll(this.members);
        safeHouse.setHitPoints(this.hitPoints);
        safeHouse.getPlayersRespawn().clear();
        safeHouse.getPlayersRespawn().addAll(this.membersRespawn);
        LuaEventManager.triggerEvent("OnSafehousesChanged");
    }
}
