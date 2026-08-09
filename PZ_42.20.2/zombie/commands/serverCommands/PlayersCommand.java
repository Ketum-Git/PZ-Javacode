// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandName(name = "players")
@CommandHelp(helpText = "UI_ServerOptionDesc_Players")
@RequiredCapability(requiredCapability = Capability.SeePlayersConnected)
public class PlayersCommand extends CommandBase {
    public PlayersCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        ArrayList<String> usernames = new ArrayList<>();

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);

            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                if (c.usernames[playerIndex] != null) {
                    usernames.add(c.usernames[playerIndex]);
                }
            }
        }

        StringBuilder result = new StringBuilder("Players connected (" + usernames.size() + "): ");
        String returnCarriage = " <LINE> ";
        if (this.connection == null) {
            returnCarriage = "\n";
        }

        result.append(returnCarriage);

        for (String username : usernames) {
            result.append("-").append(username).append(returnCarriage);
        }

        return result.toString();
    }
}
