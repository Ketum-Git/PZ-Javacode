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

@CommandNames({@CommandName(name = "teleportplayer"), @CommandName(name = "tpp")})
@CommandArgs(required = {"(.+)", "(.+)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_TeleportPlayer")
@RequiredCapability(requiredCapability = Capability.TeleportPlayerToAnotherPlayer)
public class TeleportPlayerCommand extends CommandBase {
    private String username1;
    private String username2;

    public TeleportPlayerCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        this.username1 = this.getCommandArg(0);
        this.username2 = this.getCommandArg(1);
        return this.TeleportUser1ToUser2();
    }

    private String TeleportUser1ToUser2() {
        if (!this.getRole().hasCapability(Capability.TeleportPlayerToAnotherPlayer) && !this.username1.equals(this.getExecutorUsername())) {
            return "An Observer can only teleport himself";
        } else {
            IsoPlayer player1 = GameServer.getPlayerByUserNameForCommand(this.username1);
            IsoPlayer player2 = GameServer.getPlayerByUserNameForCommand(this.username2);
            if (player1 == null) {
                return "Can't find player " + this.username1;
            } else if (player2 == null) {
                return "Can't find player " + this.username2;
            } else {
                this.username1 = player1.getDisplayName();
                this.username2 = player2.getDisplayName();
                UdpConnection connection1 = GameServer.getConnectionFromPlayer(player1);
                if (connection1 == null) {
                    return "No connection for player " + this.username1;
                } else {
                    GameServer.sendTeleport(player1, player2.getX(), player2.getY(), player2.getZ());
                    LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " teleported " + this.username1 + " to " + this.username2);
                    return "teleported " + this.username1 + " to " + this.username2;
                }
            }
        }
    }

    private String CommandArgumentsNotMatch() {
        return this.getHelp();
    }
}
