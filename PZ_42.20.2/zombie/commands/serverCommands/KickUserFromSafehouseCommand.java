// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.scripting.FileName;

@CommandName(name = "kickfromsafehouse")
@CommandArgs(required = {"(.+)", "(.+)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_KickFromSafehouse")
@RequiredCapability(requiredCapability = Capability.CanSetupSafehouses)
public class KickUserFromSafehouseCommand extends CommandBase {
    public KickUserFromSafehouseCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String safeHouseTitle = this.getCommandArg(0);
        SafeHouse safeHouse = SafeHouse.getSafeHouse(safeHouseTitle);
        if (safeHouse == null) {
            return "Safehouse " + safeHouseTitle + " not found";
        } else {
            String username = this.getCommandArg(1);
            if (!safeHouse.getPlayers().contains(username)) {
                return "Player " + username + " not a member of safehouse " + safeHouseTitle;
            } else {
                SafeHouse.kickUserFromSafehouse(safeHouse, username);
                String executorName = this.getExecutorUsername();
                LoggerManager.getLogger(FileName.sanitize(executorName))
                    .write(executorName + " kicked user " + username.trim() + " from safehouse " + safeHouseTitle.trim());
                return "Player " + username + " kicked from a safehouse " + safeHouseTitle;
            }
        }
    }
}
