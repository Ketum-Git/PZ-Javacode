// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

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
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;
import zombie.util.StringUtils;

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
        String newUserPassword = this.getCommandArg(1);
        if (!StringUtils.isNullOrEmpty(newUserPassword)) {
            LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " created user " + newUsername.trim() + " with password " + newUserPassword);
        } else {
            LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " created user " + newUsername.trim() + " without password");
        }

        return GameServer.addUser(newUsername, PZcrypt.hash(ServerWorldDatabase.encrypt(newUserPassword)));
    }
}
