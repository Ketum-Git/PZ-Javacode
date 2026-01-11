// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.BanSystem;

@CommandName(name = "unbanuser")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_UnBanUser")
@RequiredCapability(requiredCapability = Capability.BanUnbanUser)
public class UnbanUserCommand extends CommandBase {
    public UnbanUserCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String user = this.getCommandArg(0);
        return BanSystem.BanUser(user, this.connection, "", false);
    }
}
