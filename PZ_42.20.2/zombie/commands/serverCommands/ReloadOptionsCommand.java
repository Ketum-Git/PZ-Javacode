// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.characters.SafetySystemManager;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.popman.ZombiePopulationManager;

@CommandName(name = "reloadoptions")
@CommandHelp(helpText = "UI_ServerOptionDesc_ReloadOptions")
@RequiredCapability(requiredCapability = Capability.ChangeAndReloadServerOptions)
public class ReloadOptionsCommand extends CommandBase {
    public ReloadOptionsCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        ServerOptions.instance.init();
        GameServer.initClientCommandFilter();
        ZombiePopulationManager.instance.onConfigReloaded();
        GameServer.sendOptionsToClients();
        SafetySystemManager.updateOptions();
        GameServer.udpEngine.SetServerPassword(GameServer.udpEngine.hashServerPassword(ServerOptions.instance.password.getValue()));
        if (SteamUtils.isSteamModeEnabled()) {
            GameServer.setupSteamGameServer();
        }

        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " reloaded options");
        return "Options reloaded";
    }
}
