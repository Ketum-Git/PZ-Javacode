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

@CommandNames({@CommandName(name = "teleport"), @CommandName(name = "tp")})
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_Teleport")
@RequiredCapability(requiredCapability = Capability.TeleportToPlayer)
public class TeleportCommand extends CommandBase {
    private String username1;

    public TeleportCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        this.username1 = this.getCommandArg(0);
        return this.TeleportMeToUser();
    }

    private String TeleportMeToUser() {
        if (this.connection == null) {
            return "Need player to teleport to, ex /teleport user1 user2";
        } else {
            IsoPlayer pl = GameServer.getPlayerByUserNameForCommand(this.username1);
            if (pl != null) {
                GameServer.sendTeleport(this.connection.players[0], pl.getX(), pl.getY(), pl.getZ());
                this.username1 = pl.getDisplayName();
                LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " teleport to " + this.username1);
                return "teleported to " + this.username1 + " please wait two seconds to show the map around you.";
            } else {
                return "Can't find player " + this.username1;
            }
        }
    }

    private String CommandArgumentsNotMatch() {
        return this.getHelp();
    }
}
