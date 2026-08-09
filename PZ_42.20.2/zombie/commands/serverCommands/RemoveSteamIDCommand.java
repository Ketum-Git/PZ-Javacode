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
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerWorldDatabase;

@CommandName(name = "removesteamid")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_RemoveSteamID")
@RequiredCapability(requiredCapability = Capability.ModifyNetworkUsers)
public class RemoveSteamIDCommand extends CommandBase {
    public RemoveSteamIDCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String steamID = this.getCommandArg(0);
        if (!ServerWorldDatabase.isValidUserName(steamID)) {
            return "Invalid steamID \"" + steamID + "\"";
        } else {
            try {
                if (!ServerWorldDatabase.instance.isSteamIDAllowed(steamID)) {
                    LoggerManager.getLogger("admin")
                        .write(this.getExecutorUsername() + " tried to remove user with SteamID " + steamID + " but it doesn't exists in allowed SteamIDs");
                    return "SteamID " + steamID + " doesn't exists in allowed SteamIDs";
                } else {
                    String result = ServerWorldDatabase.instance.removeSteamID(steamID.trim());
                    LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " removed allowed SteamID " + steamID.trim() + this.getCommandArg(0));
                    return result;
                }
            } catch (SQLException var3) {
                ExceptionLogger.logException(var3);
                return "exception occurs";
            }
        }
    }
}
