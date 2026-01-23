// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.ServerMap;

@CommandName(name = "quit")
@CommandHelp(helpText = "UI_ServerOptionDesc_Quit")
@RequiredCapability(requiredCapability = Capability.QuitWorld)
public class QuitCommand extends CommandBase {
    public QuitCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        DebugLog.Multiplayer.debugln(this.description);
        ServerMap.instance.QueueQuit();
        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " closed server");
        return "Quit";
    }
}
