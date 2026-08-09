// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;

@CommandName(name = "grantadmin")
@CommandArgs(required = "(.+)")
@RequiredCapability(requiredCapability = Capability.ChangeAccessLevel)
public class GrantAdminCommand extends CommandBase {
    public GrantAdminCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        return SetAccessLevelCommand.update(this.getExecutorUsername(), this.connection, this.getCommandArg(0), "admin");
    }
}
