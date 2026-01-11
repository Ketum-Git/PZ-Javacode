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
import zombie.core.raknet.VoiceManager;
import zombie.network.GameServer;

@CommandName(name = "voiceban")
@AltCommandArgs({@CommandArgs(required = "(.+)", optional = "(-true|-false)"), @CommandArgs(optional = "(-true|-false)")})
@CommandHelp(helpText = "UI_ServerOptionDesc_VoiceBan")
@RequiredCapability(requiredCapability = Capability.BanUnbanUser)
public class VoiceBanCommand extends CommandBase {
    public VoiceBanCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String username = this.getExecutorUsername();
        if (this.getCommandArgsCount() == 2
            || this.getCommandArgsCount() == 1 && !this.getCommandArg(0).equals("-true") && !this.getCommandArg(0).equals("-false")) {
            username = this.getCommandArg(0);
        }

        boolean value = true;
        if (this.getCommandArgsCount() > 0) {
            value = !this.getCommandArg(this.getCommandArgsCount() - 1).equals("-false");
        }

        IsoPlayer p = GameServer.getPlayerByUserNameForCommand(username);
        if (p != null) {
            username = p.getDisplayName();
            VoiceManager.instance.VMServerBan(p.onlineId, value);
            if (value) {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " ban voice " + username);
                return "User " + username + " voice is banned.";
            } else {
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " unban voice " + username);
                return "User " + username + " voice is unbanned.";
            }
        } else {
            return "User " + username + " not found.";
        }
    }
}
