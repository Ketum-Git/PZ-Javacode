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
import zombie.iso.weather.ClimateManager;

@CommandName(name = "stoprain")
@CommandHelp(helpText = "UI_ServerOptionDesc_StopRain")
@RequiredCapability(requiredCapability = Capability.StartStopRain)
public class StopRainCommand extends CommandBase {
    public StopRainCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        ClimateManager.getInstance().transmitServerStopRain();
        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " stopped rain");
        return "Rain stopped";
    }
}
