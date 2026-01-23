// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandNames({@CommandName(name = "godmod"), @CommandName(name = "godmode")})
@CommandArgs(optional = "(-true|-false)")
@CommandHelp(helpText = "UI_ServerOptionDesc_GodMod")
@RequiredCapability(requiredCapability = Capability.ToggleGodModHimself)
public class GodModeCommand extends CommandBase {
    public GodModeCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String userName = this.getExecutorUsername();
        String firstArg = this.getCommandArg(0);
        IsoPlayer p = GameServer.getPlayerByUserNameForCommand(userName);
        if (p != null) {
            userName = p.getDisplayName();
            if (firstArg != null) {
                p.setGodMod("-true".equals(firstArg));
            } else {
                p.setGodMod(!p.isGodMod());
            }

            GameServer.sendPlayerExtraInfo(p, this.connection);
            if (p.isGodMod()) {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " enabled godmode on " + userName);
                return "User " + userName + " is now invincible.";
            } else {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " disabled godmode on " + userName);
                return "User " + userName + " is no longer invincible.";
            }
        } else {
            return "User " + userName + " not found.";
        }
    }
}
