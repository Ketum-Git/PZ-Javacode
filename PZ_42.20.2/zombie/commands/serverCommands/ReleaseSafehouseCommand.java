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
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@CommandName(name = "releasesafehouse")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_ReleaseSafeHouse")
@RequiredCapability(requiredCapability = Capability.CanSetupSafehouses)
public class ReleaseSafehouseCommand extends CommandBase {
    public ReleaseSafehouseCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String safeHouseTitle = this.getCommandArg(0);
        SafeHouse safeHouse = SafeHouse.getSafeHouse(safeHouseTitle);
        if (safeHouse == null) {
            return "Safehouse " + safeHouseTitle + " not found";
        } else {
            SafeHouse.removeSafeHouse(safeHouse);
            INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseRelease, safeHouse);
            LoggerManager.getLogger(this.getExecutorUsername()).write(this.getExecutorUsername() + " released safehouse " + safeHouseTitle.trim());
            return "Safehouse " + safeHouseTitle + " released";
        }
    }
}
