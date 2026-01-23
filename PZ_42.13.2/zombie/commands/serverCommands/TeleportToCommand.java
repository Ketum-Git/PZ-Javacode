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
import zombie.commands.CommandNames;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandNames({@CommandName(name = "teleportto"), @CommandName(name = "tpto")})
@AltCommandArgs(
    {
            @CommandArgs(required = {"(.+)", "(-?\\d+.*\\d*),(-?\\d+.*\\d*),(-?\\d+.*\\d*)"}, argName = "Teleport user"),
            @CommandArgs(required = "(-?\\d+.*\\d*),(-?\\d+.*\\d*),(-?\\d+.*\\d*)", argName = "teleport me")
    }
)
@CommandHelp(helpText = "UI_ServerOptionDesc_TeleportTo")
@RequiredCapability(requiredCapability = Capability.TeleportToCoordinates)
public class TeleportToCommand extends CommandBase {
    public static final String teleportMe = "teleport me";
    public static final String teleportUser = "Teleport user";
    private String username;
    private Float[] coords;

    public TeleportToCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String var1 = this.argsName;
        switch (var1) {
            case "teleport me":
                this.coords = new Float[3];

                for (int i = 0; i < 3; i++) {
                    this.coords[i] = Float.parseFloat(this.getCommandArg(i));
                }

                return this.TeleportMeToCoords();
            case "Teleport user":
                this.username = this.getCommandArg(0);
                this.coords = new Float[3];

                for (int i = 0; i < 3; i++) {
                    this.coords[i] = Float.parseFloat(this.getCommandArg(i + 1));
                }

                return this.TeleportUserToCoords();
            default:
                return this.CommandArgumentsNotMatch();
        }
    }

    private String TeleportMeToCoords() {
        float x = this.coords[0];
        float y = this.coords[1];
        float z = this.coords[2];
        if (this.connection != null) {
            if (!this.connection.role.hasCapability(Capability.TeleportToCoordinates)) {
                return "An Observer can only teleport himself";
            } else {
                GameServer.sendTeleport(this.connection.players[0], x, y, z);
                LoggerManager.getLogger("admin")
                    .write(this.getExecutorUsername() + " teleported to " + PZMath.fastfloor(x) + "," + PZMath.fastfloor(y) + "," + PZMath.fastfloor(z));
                return "teleported to "
                    + PZMath.fastfloor(x)
                    + ","
                    + PZMath.fastfloor(y)
                    + ","
                    + PZMath.fastfloor(z)
                    + " please wait two seconds to show the map around you.";
            }
        } else {
            return "Error";
        }
    }

    private String TeleportUserToCoords() {
        float x = this.coords[0];
        float y = this.coords[1];
        float z = this.coords[2];
        if (this.connection != null
            && !this.connection.role.hasCapability(Capability.TeleportPlayerToAnotherPlayer)
            && !this.username.equals(this.getExecutorUsername())) {
            return "An Observer can only teleport himself";
        } else {
            IsoPlayer player1 = GameServer.getPlayerByUserNameForCommand(this.username);
            if (player1 == null) {
                return "Can't find player " + this.username;
            } else {
                GameServer.sendTeleport(player1, x, y, z);
                LoggerManager.getLogger("admin")
                    .write(this.getExecutorUsername() + " teleported to " + PZMath.fastfloor(x) + "," + PZMath.fastfloor(y) + "," + PZMath.fastfloor(z));
                return this.username
                    + " teleported to "
                    + PZMath.fastfloor(x)
                    + ","
                    + PZMath.fastfloor(y)
                    + ","
                    + PZMath.fastfloor(z)
                    + " please wait two seconds to show the map around you.";
            }
        }
    }

    private String CommandArgumentsNotMatch() {
        return this.getHelp();
    }
}
