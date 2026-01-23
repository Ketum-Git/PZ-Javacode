// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.DisabledCommand;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@DisabledCommand
@CommandNames({@CommandName(name = "connections"), @CommandName(name = "list")})
@CommandHelp(helpText = "UI_ServerOptionDesc_Connections")
@RequiredCapability(requiredCapability = Capability.SeePlayersConnected)
public class ConnectionsCommand extends CommandBase {
    public ConnectionsCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String result = "";
        String nl = " <LINE> ";
        if (this.connection == null) {
            nl = "\n";
        }

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);

            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                if (c.usernames[playerIndex] != null) {
                    result = result
                        + "connection="
                        + (n + 1)
                        + "/"
                        + GameServer.udpEngine.connections.size()
                        + " "
                        + c.idStr
                        + " player="
                        + (playerIndex + 1)
                        + "/4 id="
                        + c.playerIds[playerIndex]
                        + " username=\""
                        + c.usernames[playerIndex]
                        + "\" fullyConnected="
                        + c.isFullyConnected()
                        + nl;
                }
            }
        }

        return result + "Players listed";
    }
}
