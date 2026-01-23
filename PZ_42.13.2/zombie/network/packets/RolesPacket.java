// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
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
        b.putByte((byte)this.roles.size());

        for (Role r : this.roles) {
            r.send(b.bb);
        }

        GameWindow.WriteStringUTF(b.bb, defaultForBanned.getName());
        GameWindow.WriteStringUTF(b.bb, defaultForNewUser.getName());
        GameWindow.WriteStringUTF(b.bb, defaultForUser.getName());
        GameWindow.WriteStringUTF(b.bb, defaultForPriorityUser.getName());
        GameWindow.WriteStringUTF(b.bb, defaultForObserver.getName());
        GameWindow.WriteStringUTF(b.bb, defaultForGM.getName());
        GameWindow.WriteStringUTF(b.bb, defaultForOverseer.getName());
        GameWindow.WriteStringUTF(b.bb, defaultForModerator.getName());
        GameWindow.WriteStringUTF(b.bb, defaultForAdmin.getName());
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.roles.clear();
        byte size = b.get();

        for (int i = 0; i < size; i++) {
            Role r = new Role("");
            r.parse(b);
            this.roles.add(r);
        }

        defaultForBanned = this.getRole(GameWindow.ReadString(b));
        defaultForNewUser = this.getRole(GameWindow.ReadString(b));
        defaultForUser = this.getRole(GameWindow.ReadString(b));
        defaultForPriorityUser = this.getRole(GameWindow.ReadString(b));
        defaultForObserver = this.getRole(GameWindow.ReadString(b));
        defaultForGM = this.getRole(GameWindow.ReadString(b));
        defaultForOverseer = this.getRole(GameWindow.ReadString(b));
        defaultForModerator = this.getRole(GameWindow.ReadString(b));
        defaultForAdmin = this.getRole(GameWindow.ReadString(b));
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
        Role role = Roles.getOrDefault(connection.role.getName());
        connection.role = role;

        for (IsoPlayer player : IsoPlayer.players) {
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
