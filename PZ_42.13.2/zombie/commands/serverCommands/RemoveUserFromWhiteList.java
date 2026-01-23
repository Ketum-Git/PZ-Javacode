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
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerWorldDatabase;

@CommandName(name = "removeuserfromwhitelist")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_RemoveWhitelist")
@RequiredCapability(requiredCapability = Capability.ManipulateWhitelist)
public class RemoveUserFromWhiteList extends CommandBase {
    public RemoveUserFromWhiteList(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String user = this.getCommandArg(0);
        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " removed user " + user + " from whitelist");
        return ServerWorldDatabase.instance.removeUser(user);
    }
}
