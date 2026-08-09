// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;

@CommandName(name = "thunder")
@CommandArgs(optional = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_Thunder")
@RequiredCapability(requiredCapability = Capability.StartStopRain)
public class ThunderCommand extends CommandBase {
    public ThunderCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String user;
        if (this.getCommandArgsCount() == 0) {
            if (this.connection == null) {
                return "Pass a username";
            }

            user = this.getExecutorUsername();
        } else {
            user = this.getCommandArg(0);
        }

        IsoPlayer player = GameServer.getPlayerByUserNameForCommand(user);
        if (player == null) {
            return "User \"" + user + "\" not found";
        } else {
            int x = PZMath.fastfloor(player.getX());
            int y = PZMath.fastfloor(player.getY());
            ClimateManager.getInstance().transmitServerTriggerLightning(x, y, false, false, true);
            LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " thunder start");
            return "Thunder triggered";
        }
    }
}
