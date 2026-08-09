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
import zombie.core.znet.SteamUtils;
import zombie.network.BanSystem;

@CommandName(name = "unbanid")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_UnBanSteamId")
@RequiredCapability(requiredCapability = Capability.BanUnbanUser)
public class UnbanSteamIDCommand extends CommandBase {
    public UnbanSteamIDCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String steamID = this.getCommandArg(0);
        if (!SteamUtils.isSteamModeEnabled()) {
            return "Server is not in Steam mode";
        } else if (!SteamUtils.isValidSteamID(steamID)) {
            return "Expected SteamID but got \"" + steamID + "\"";
        } else {
            BanSystem.BanUserBySteamID(steamID, this.connection, "", false);
            return "SteamID " + steamID + " is now unbanned";
        }
    }
}
