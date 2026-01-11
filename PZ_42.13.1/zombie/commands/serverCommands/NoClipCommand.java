// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandName(name = "noclip")
@AltCommandArgs({@CommandArgs(required = "(.+)", optional = "(-true|-false)"), @CommandArgs(optional = "(-true|-false)")})
@CommandHelp(helpText = "UI_ServerOptionDesc_NoClip")
@RequiredCapability(requiredCapability = Capability.ToggleNoclipHimself)
public class NoClipCommand extends CommandBase {
    public NoClipCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String username = this.getExecutorUsername();
        String firstArg = this.getCommandArg(0);
        String secondArg = this.getCommandArg(1);
        if (this.getCommandArgsCount() == 2 || this.getCommandArgsCount() == 1 && !firstArg.equals("-true") && !firstArg.equals("-false")) {
            username = firstArg;
            if (this.connection != null && !this.connection.role.hasCapability(Capability.ToggleNoclipEveryone)) {
                return "Not enough rights";
            }
        } else if (this.connection != null && !this.connection.role.hasCapability(Capability.ToggleNoclipHimself)) {
            return "Not enough rights";
        }

        boolean haveValue = false;
        boolean value = true;
        if ("-false".equals(secondArg)) {
            value = false;
            haveValue = true;
        } else if ("-true".equals(secondArg)) {
            haveValue = true;
        }

        IsoPlayer p = GameServer.getPlayerByUserNameForCommand(username);
        if (p != null) {
            username = p.getDisplayName();
            if (haveValue) {
                p.setNoClip(value, true);
            } else {
                p.setNoClip(!p.isNoClip(), true);
                value = p.isNoClip();
            }

            GameServer.sendPlayerExtraInfo(p, this.connection, true);
            if (value) {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " enabled noclip on " + username);
                return "User " + username + " won't collide.";
            } else {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " disabled noclip on " + username);
                return "User " + username + " will collide.";
            }
        } else {
            return "User " + username + " not found.";
        }
    }
}
