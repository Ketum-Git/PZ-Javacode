// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
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
@CommandName(name = "addalltowhitelist")
@CommandHelp(helpText = "UI_ServerOptionDesc_AddAllWhitelist")
@RequiredCapability(requiredCapability = Capability.ManipulateWhitelist)
public class AddAllToWhiteListCommand extends CommandBase {
    public AddAllToWhiteListCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        StringBuilder result = new StringBuilder();

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.password != null && !c.password.equals("")) {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " created user " + c.username + " with password " + c.password);

                try {
                    result.append(ServerWorldDatabase.instance.addUser(c.username, c.password)).append(" <LINE> ");
                } catch (SQLException var5) {
                    var5.printStackTrace();
                }
            } else {
                result.append("User ").append(c.username).append(" doesn't have a password. <LINE> ");
            }
        }

        result.append("Done.");
        return result.toString();
    }
}
