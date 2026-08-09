// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.util.ArrayList;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class RolesPacket implements INetworkPacket {
    @JSONField
    protected ArrayList<Role> roles = new ArrayList<>();
    @JSONField
    protected static Role defaultForBanned;
    @JSONField
    protected static Role defaultForNewUser;
    @JSONField
    protected static Role defaultForUser;
    @JSONField
    protected static Role defaultForPriorityUser;
    @JSONField
    protected static Role defaultForObserver;
    @JSONField
    protected static Role defaultForGM;
    @JSONField
    protected static Role defaultForOverseer;
    @JSONField
    protected static Role defaultForModerator;
    @JSONField
    protected static Role defaultForAdmin;

    @Override
    public void setData(Object... values) {
        this.roles.clear();
        this.roles.addAll(Roles.getRoles());
        defaultForBanned = Roles.getDefaultForBanned();
        defaultForNewUser = Roles.getDefaultForNewUser();
        defaultForUser = Roles.getDefaultForUser();
        defaultForPriorityUser = Roles.getDefaultForPriorityUser();
        defaultForObserver = Roles.getDefaultForObserver();
        defaultForGM = Roles.getDefaultForGM();
        defaultForOverseer = Roles.getDefaultForOverseer();
        defaultForModerator = Roles.getDefaultForModerator();
        defaultForAdmin = Roles.getDefaultForAdmin();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.roles.size());

        for (Role r : this.roles) {
            r.send(b);
        }

        b.putUTF(defaultForBanned.getName());
        b.putUTF(defaultForNewUser.getName());
        b.putUTF(defaultForUser.getName());
        b.putUTF(defaultForPriorityUser.getName());
        b.putUTF(defaultForObserver.getName());
        b.putUTF(defaultForGM.getName());
        b.putUTF(defaultForOverseer.getName());
        b.putUTF(defaultForModerator.getName());
        b.putUTF(defaultForAdmin.getName());
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.roles.clear();
        byte size = b.getByte();

        for (int i = 0; i < size; i++) {
            Role r = new Role("");
            r.parse(b);
            this.roles.add(r);
        }

        defaultForBanned = this.getRole(b.getUTF());
        defaultForNewUser = this.getRole(b.getUTF());
        defaultForUser = this.getRole(b.getUTF());
        defaultForPriorityUser = this.getRole(b.getUTF());
        defaultForObserver = this.getRole(b.getUTF());
        defaultForGM = this.getRole(b.getUTF());
        defaultForOverseer = this.getRole(b.getUTF());
        defaultForModerator = this.getRole(b.getUTF());
        defaultForAdmin = this.getRole(b.getUTF());
    }

    @Override
    public void processClient(UdpConnection connection) {
        Roles.setRoles(
            this.roles,
            defaultForBanned,
            defaultForNewUser,
            defaultForUser,
            defaultForPriorityUser,
            defaultForObserver,
            defaultForGM,
            defaultForOverseer,
            defaultForModerator,
            defaultForAdmin
        );
        this.updateRole(connection);
        LuaEventManager.triggerEvent("OnRolesReceived");
    }

    private void updateRole(UdpConnection connection) {
        Role role = Roles.getOrDefault(connection.getRole().getName());
        connection.setRole(role);

        for (IsoPlayer player : GameClient.instance.getPlayers()) {
            if (player != null) {
                role = Roles.getOrDefault(player.getRole().getName());
                player.setRole(role);
            }
        }
    }

    private Role getRole(String name) {
        for (Role r : this.roles) {
            if (r.getName().equals(name)) {
                return r;
            }
        }

        return null;
    }
}
