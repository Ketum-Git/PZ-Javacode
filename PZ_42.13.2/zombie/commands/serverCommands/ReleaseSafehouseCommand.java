// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;

@CommandName(name = "releasesafehouse")
@CommandHelp(helpText = "UI_ServerOptionDesc_SafeHouse")
@RequiredCapability(requiredCapability = Capability.CanSetupSafehouses)
public class ReleaseSafehouseCommand extends CommandBase {
    public ReleaseSafehouseCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        if (this.isCommandComeFromServerConsole()) {
            return getCommandName(this.getClass()) + " can be executed only from the game";
        } else {
            String username = this.getExecutorUsername();
            SafeHouse safeHouse = SafeHouse.hasSafehouse(username);
            if (safeHouse != null) {
                if (!safeHouse.isOwner(username)) {
                    return "Only owner can release safehouse";
                } else {
                    SafeHouse.removeSafeHouse(safeHouse);
                    return "Your safehouse was released";
                }
            } else {
                return "You have no safehouse";
            }
        }
    }
}
