// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.worldMap.network.WorldMapServer;

@CommandName(name = "removemapsymbolsforuser")
@CommandArgs(required = "(.+)", argName = "Remove Map Symbols For User")
@CommandHelp(helpText = "UI_ServerOptionDesc_RemoveMapSymbolsForUser")
@RequiredCapability(requiredCapability = Capability.EditMapSymbols)
public class RemoveMapSymbolsForUserCommand extends CommandBase {
    public static final String userName = "Remove Map Symbols For User";

    public RemoveMapSymbolsForUserCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String userName = this.getCommandArg(0);
        int count = WorldMapServer.instance.removeAllSymbolsForUser(userName);
        return "removed %d symbols".formatted(count);
    }
}
