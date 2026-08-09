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
import zombie.core.znet.SteamUtils;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

@CommandName(name = "changeoption")
@CommandArgs(required = {"(\\w+)", "(.*)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_ChangeOptions")
@RequiredCapability(requiredCapability = Capability.ChangeAndReloadServerOptions)
public class ChangeOptionCommand extends CommandBase {
    public ChangeOptionCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String option = this.getCommandArg(0);
        String newValue = this.getCommandArg(1);
        String s = ServerOptions.instance.changeOption(option, newValue);
        if (option.equals("Password")) {
            GameServer.udpEngine.SetServerPassword(GameServer.udpEngine.hashServerPassword(ServerOptions.instance.password.getValue()));
        }

        if (option.equals("ClientCommandFilter")) {
            GameServer.initClientCommandFilter();
        }

        if (SteamUtils.isSteamModeEnabled()) {
            GameServer.setupSteamGameServer();
        }

        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " changed option " + option + "=" + newValue);
        if (ServerOptions.getInstance().getOptionByName(option) instanceof ServerOptions.EnumServerOption enumOption) {
            s = s + "(" + enumOption.getValueTranslationByIndex(Integer.parseInt(newValue)) + ")";
        }

        return s;
    }
}
