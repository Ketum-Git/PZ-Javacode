// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.Roles;
import zombie.core.Color;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.RolesWrite, handlingType = 1)
public class RolesEditPacket implements INetworkPacket {
    @JSONField
    public RolesEditPacket.Command command;
    @JSONField
    public String name;
    @JSONField
    String description;
    @JSONField
    Color color;
    @JSONField
    ArrayList<Capability> capabilities;
    @JSONField
    String defaultId;
    @JSONField
    byte movingDirection;

    @Override
    public void setData(Object... values) {
        this.command = (RolesEditPacket.Command)values[0];
        if (this.command == RolesEditPacket.Command.AddRole || this.command == RolesEditPacket.Command.DeleteRole) {
            this.name = (String)values[1];
        }

        if (this.command == RolesEditPacket.Command.SetupRole) {
            this.name = (String)values[1];
            this.description = (String)values[2];
            this.color = (Color)values[3];
            this.capabilities = (ArrayList<Capability>)values[4];
        }

        if (this.command == RolesEditPacket.Command.SetDefaultRole) {
            this.defaultId = (String)values[1];
            this.name = (String)values[2];
        }

        if (this.command == RolesEditPacket.Command.MoveRole) {
            this.movingDirection = (Byte)values[1];
            this.name = (String)values[2];
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)this.command.ordinal());
        if (this.command == RolesEditPacket.Command.AddRole || this.command == RolesEditPacket.Command.DeleteRole) {
            GameWindow.WriteStringUTF(b.bb, this.name);
        }

        if (this.command == RolesEditPacket.Command.SetupRole) {
            GameWindow.WriteStringUTF(b.bb, this.name);
            GameWindow.WriteStringUTF(b.bb, this.description);
            b.bb.putFloat(this.color.r);
            b.bb.putFloat(this.color.g);
            b.bb.putFloat(this.color.b);
            b.bb.putFloat(this.color.a);
            b.bb.putShort((short)this.capabilities.size());

            for (Capability c : this.capabilities) {
                GameWindow.WriteStringUTF(b.bb, c.name());
            }
        }

        if (this.command == RolesEditPacket.Command.SetDefaultRole) {
            GameWindow.WriteStringUTF(b.bb, this.defaultId);
            GameWindow.WriteStringUTF(b.bb, this.name);
        }

        if (this.command == RolesEditPacket.Command.MoveRole) {
            b.bb.put(this.movingDirection);
            GameWindow.WriteStringUTF(b.bb, this.name);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.command = RolesEditPacket.Command.values()[b.get()];
        if (this.command == RolesEditPacket.Command.AddRole || this.command == RolesEditPacket.Command.DeleteRole) {
            this.name = GameWindow.ReadString(b);
        }

        if (this.command == RolesEditPacket.Command.SetupRole) {
            this.name = GameWindow.ReadString(b);
            this.description = GameWindow.ReadString(b);
            float color_r = b.getFloat();
            float color_g = b.getFloat();
            float color_b = b.getFloat();
            float color_a = b.getFloat();
            this.color = new Color(color_r, color_g, color_b, color_a);
            short capability_count = b.getShort();
            this.capabilities = new ArrayList<>();

            for (int i = 0; i < capability_count; i++) {
                this.capabilities.add(Capability.valueOf(GameWindow.ReadString(b)));
            }
        }

        if (this.command == RolesEditPacket.Command.SetDefaultRole) {
            this.defaultId = GameWindow.ReadString(b);
            this.name = GameWindow.ReadString(b);
        }

        if (this.command == RolesEditPacket.Command.MoveRole) {
            this.movingDirection = b.get();
            this.name = GameWindow.ReadString(b);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.command == RolesEditPacket.Command.AddRole) {
            Roles.addRole(this.name);
        }

        if (this.command == RolesEditPacket.Command.DeleteRole) {
            Roles.deleteRole(this.name, connection.username);
        }

        if (this.command == RolesEditPacket.Command.SetupRole) {
            Roles.setupRole(this.name, this.description, this.color, this.capabilities);
        }

        if (this.command == RolesEditPacket.Command.SetDefaultRole) {
            Roles.setDefaultRoleFor(this.defaultId, this.name);
        }

        if (this.command == RolesEditPacket.Command.MoveRole) {
            Roles.moveRole(this.movingDirection, this.name);
        }
    }

    public static enum Command {
        AddRole,
        DeleteRole,
        SetupRole,
        SetDefaultRole,
        MoveRole;
    }
}
