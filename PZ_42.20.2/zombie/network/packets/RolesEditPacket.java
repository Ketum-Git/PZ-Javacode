// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.characters.Roles;
import zombie.core.Color;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
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
        b.putEnum(this.command);
        if (this.command == RolesEditPacket.Command.AddRole || this.command == RolesEditPacket.Command.DeleteRole) {
            b.putUTF(this.name);
        }

        if (this.command == RolesEditPacket.Command.SetupRole) {
            b.putUTF(this.name);
            b.putUTF(this.description);
            b.putFloat(this.color.r);
            b.putFloat(this.color.g);
            b.putFloat(this.color.b);
            b.putFloat(this.color.a);
            b.putShort(this.capabilities.size());

            for (Capability c : this.capabilities) {
                b.putUTF(c.name());
            }
        }

        if (this.command == RolesEditPacket.Command.SetDefaultRole) {
            b.putUTF(this.defaultId);
            b.putUTF(this.name);
        }

        if (this.command == RolesEditPacket.Command.MoveRole) {
            b.putByte(this.movingDirection);
            b.putUTF(this.name);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.command = b.getEnum(RolesEditPacket.Command.class);
        if (this.command == RolesEditPacket.Command.AddRole || this.command == RolesEditPacket.Command.DeleteRole) {
            this.name = b.getUTF();
        }

        if (this.command == RolesEditPacket.Command.SetupRole) {
            this.name = b.getUTF();
            this.description = b.getUTF();
            float colorR = b.getFloat();
            float colorG = b.getFloat();
            float colorB = b.getFloat();
            float colorA = b.getFloat();
            this.color = new Color(colorR, colorG, colorB, colorA);
            short capabilityCount = b.getShort();
            this.capabilities = new ArrayList<>();

            for (int i = 0; i < capabilityCount; i++) {
                this.capabilities.add(Capability.valueOf(b.getUTF()));
            }
        }

        if (this.command == RolesEditPacket.Command.SetDefaultRole) {
            this.defaultId = b.getUTF();
            this.name = b.getUTF();
        }

        if (this.command == RolesEditPacket.Command.MoveRole) {
            this.movingDirection = b.getByte();
            this.name = b.getUTF();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.command == RolesEditPacket.Command.AddRole) {
            Roles.addRole(this.name);
        }

        if (this.command == RolesEditPacket.Command.DeleteRole) {
            Roles.deleteRole(this.name, connection.getUserName());
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
