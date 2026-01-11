// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

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
import zombie.iso.weather.ClimateManager;

@CommandName(name = "startrain")
@CommandArgs(optional = "(\\d+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_StartRain")
@RequiredCapability(requiredCapability = Capability.StartStopRain)
public class StartRainCommand extends CommandBase {
    public StartRainCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        float intensity = 1.0F;
        if (this.getCommandArgsCount() == 1) {
            try {
                intensity = Float.parseFloat(this.getCommandArg(0)) / 100.0F;
            } catch (Throwable var3) {
                ExceptionLogger.logException(var3);
                return "Invalid intensity value";
            }
        }

        ClimateManager.getInstance().transmitServerStartRain(intensity);
        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " started rain");
        return "Rain started";
    }
}
