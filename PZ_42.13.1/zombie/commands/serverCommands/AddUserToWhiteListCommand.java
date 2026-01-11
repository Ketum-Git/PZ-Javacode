// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.DisabledCommand;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;

@DisabledCommand
@CommandName(name = "addusertowhitelist")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_AddWhitelist")
@RequiredCapability(requiredCapability = Capability.ManipulateWhitelist)
public class AddUserToWhiteListCommand extends CommandBase {
    public AddUserToWhiteListCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String user = this.getCommandArg(0);
        if (!ServerWorldDatabase.isValidUserName(user)) {
            return "Invalid username \"" + user + "\"";
        } else {
            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.username.equals(user)) {
                    if (c.password != null && !c.password.equals("")) {
                        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " created user " + c.username + " with password " + c.password);
                        return ServerWorldDatabase.instance.addUser(c.username, c.password);
                    }

                    return "User " + user + " doesn't have a password.";
                }
            }

            return "User " + user + " not found.";
        }
    }
}
