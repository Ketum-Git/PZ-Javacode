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

@CommandName(name = "setpassword")
@CommandArgs(required = {"(.+)", "(.+)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_SetPassword")
@RequiredCapability(requiredCapability = Capability.ModifyNetworkUsers)
public class SetPasswordCommand extends CommandBase {
    public SetPasswordCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String newUsername = this.getCommandArg(0);
        String newUserPassword = PZcrypt.hash(ServerWorldDatabase.encrypt(this.getCommandArg(1)));
        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " changing password for the " + newUsername.trim());

        try {
            return ServerWorldDatabase.instance.changePassword(newUsername.trim(), newUserPassword.trim());
        } catch (SQLException var4) {
            var4.printStackTrace();
            return "exception occurs";
        }
    }
}
