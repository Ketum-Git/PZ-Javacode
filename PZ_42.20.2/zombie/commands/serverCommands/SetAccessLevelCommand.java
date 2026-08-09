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
import zombie.network.GameServer;

@CommandName(name = "setaccesslevel")
@CommandArgs(required = {"(.+)", "(\\w+)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_SetAccessLevel")
@RequiredCapability(requiredCapability = Capability.ChangeAccessLevel)
public class SetAccessLevelCommand extends CommandBase {
    public SetAccessLevelCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String user = this.getCommandArg(0);
        String newAccessLevel = "none".equals(this.getCommandArg(1)) ? "" : this.getCommandArg(1);
        return update(this.getExecutorUsername(), this.connection, user, newAccessLevel);
    }

    public static String update(String executorName, UdpConnection connection, String user, String newAccessLevelName) throws SQLException {
        return GameServer.changeRole(executorName, connection, user, newAccessLevelName);
    }
}
