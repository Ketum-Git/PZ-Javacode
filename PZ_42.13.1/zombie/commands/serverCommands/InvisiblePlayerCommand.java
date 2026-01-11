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

@CommandName(name = "invisibleplayer")
@CommandArgs(required = "(.+)", optional = "(-true|-false)")
@CommandHelp(helpText = "UI_ServerOptionDesc_InvisiblePlayer")
@RequiredCapability(requiredCapability = Capability.ToggleInvisibleEveryone)
public class InvisiblePlayerCommand extends CommandBase {
    public InvisiblePlayerCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String username = "";
        String firstArg = this.getCommandArg(0);
        String secondArg = this.getCommandArg(1);
        if (this.getCommandArgsCount() == 2 || this.getCommandArgsCount() == 1 && !firstArg.equals("-true") && !firstArg.equals("-false")) {
            boolean haveValue = false;
            boolean value = true;
            if ("-false".equals(secondArg)) {
                value = false;
                haveValue = true;
            } else if ("-true".equals(secondArg)) {
                haveValue = true;
            }

            IsoPlayer p = GameServer.getPlayerByUserNameForCommand(firstArg);
            if (p != null) {
                if (!haveValue) {
                    value = !p.isInvisible();
                }

                username = p.getDisplayName();
                if (haveValue) {
                    p.setInvisible(value, true);
                } else {
                    p.setInvisible(!p.isInvisible(), true);
                    value = p.isInvisible();
                }

                p.setGhostMode(value, true);
                GameServer.sendPlayerExtraInfo(p, this.connection, true);
                if (value) {
                    LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " enabled invisibility on " + username);
                    return "User " + username + " is now invisible.";
                } else {
                    LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " disabled invisibility on " + username);
                    return "User " + username + " is no longer invisible.";
                }
            } else {
                return "User " + firstArg + " not found.";
            }
        } else {
            return "Wrong arguments!";
        }
    }
}
