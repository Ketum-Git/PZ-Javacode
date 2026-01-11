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

@CommandNames({@CommandName(name = "godmodplayer"), @CommandName(name = "godmodeplayer")})
@CommandArgs(required = "(.+)", optional = "(-true|-false)")
@CommandHelp(helpText = "UI_ServerOptionDesc_GodModPlayer")
@RequiredCapability(requiredCapability = Capability.ToggleGodModEveryone)
public class GodModePlayerCommand extends CommandBase {
    public GodModePlayerCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String userName = "";
        String firstArg = this.getCommandArg(0);
        String secondArg = this.getCommandArg(1);
        if (this.getCommandArgsCount() == 2 || this.getCommandArgsCount() == 1 && !firstArg.equals("-true") && !firstArg.equals("-false")) {
            IsoPlayer p = GameServer.getPlayerByUserNameForCommand(firstArg);
            if (p != null) {
                userName = p.getDisplayName();
                if (secondArg != null) {
                    p.setGodMod("-true".equals(secondArg), true);
                } else {
                    p.setGodMod(!p.isGodMod(), true);
                }

                GameServer.sendPlayerExtraInfo(p, this.connection, true);
                if (p.isGodMod()) {
                    LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " enabled godmode on " + userName);
                    return "User " + userName + " is now invincible.";
                } else {
                    LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " disabled godmode on " + userName);
                    return "User " + userName + " is no longer invincible.";
                }
            } else {
                return "User " + firstArg + " not found.";
            }
        } else {
            return "Wrong arguments!";
        }
    }
}
