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
import zombie.core.secure.PZcrypt;
import zombie.network.ServerWorldDatabase;

@CommandName(name = "adduser")
@CommandArgs(required = "(.+)", optional = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_AddUser")
@RequiredCapability(requiredCapability = Capability.ModifyNetworkUsers)
public class AddUserCommand extends CommandBase {
    public AddUserCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String newUsername = this.getCommandArg(0);
        String newUserPassword = PZcrypt.hash(ServerWorldDatabase.encrypt(this.getCommandArg(1)));
        if (!ServerWorldDatabase.isValidUserName(newUsername)) {
            return "Invalid username \"" + newUsername + "\"";
        } else {
            if (!newUserPassword.isEmpty()) {
                LoggerManager.getLogger("admin")
                    .write(this.getExecutorUsername() + " created user " + newUsername.trim() + " with password " + this.getCommandArg(1));
            } else {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " created user " + newUsername.trim() + " without password");
            }

            try {
                return ServerWorldDatabase.instance.addUser(newUsername.trim(), newUserPassword.trim());
            } catch (SQLException var4) {
                var4.printStackTrace();
                return "exception occurs";
            }
        }
    }
}
