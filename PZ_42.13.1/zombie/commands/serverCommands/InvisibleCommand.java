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
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandName(name = "invisible")
@CommandArgs(optional = "(-true|-false)")
@CommandHelp(helpText = "UI_ServerOptionDesc_Invisible")
@RequiredCapability(requiredCapability = Capability.ToggleInvisibleHimself)
public class InvisibleCommand extends CommandBase {
    public InvisibleCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String username = this.getExecutorUsername();
        String firstArg = this.getCommandArg(0);
        boolean haveValue = false;
        boolean value = true;
        if ("-false".equals(firstArg)) {
            value = false;
            haveValue = true;
        } else if ("-true".equals(firstArg)) {
            haveValue = true;
        }

        IsoPlayer p = GameServer.getPlayerByUserNameForCommand(username);
        if (p != null) {
            if (!haveValue) {
                value = !p.isInvisible();
            }

            username = p.getDisplayName();
            if (haveValue) {
                p.setInvisible(value);
            } else {
                p.setInvisible(!p.isInvisible());
                value = p.isInvisible();
            }

            p.setGhostMode(value);
            GameServer.sendPlayerExtraInfo(p, this.connection);
            if (value) {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " enabled invisibility on " + username);
                return "User " + username + " is now invisible.";
            } else {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " disabled invisibility on " + username);
                return "User " + username + " is no longer invisible.";
            }
        } else {
            return "User " + username + " not found.";
        }
    }
}
