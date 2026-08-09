// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.FileName;

@CommandName(name = "addtosafehouse")
@CommandArgs(required = {"(.+)", "(.+)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_AddToSafehouse")
@RequiredCapability(requiredCapability = Capability.CanSetupSafehouses)
public class AddUserToSafehouseCommand extends CommandBase {
    public AddUserToSafehouseCommand(String username, Role userRole, String command, UdpConnection connection) {
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
            if (SafeHouse.hasSafehouse(username) != null) {
                return "Player " + username + " is already a member of safehouse";
            } else {
                IsoPlayer invitedPlayer = GameServer.getPlayerByUserName(username);
                if (invitedPlayer == null) {
                    return "Cannot find player with username " + username;
                } else if (!GameServer.isPlayerConnected(invitedPlayer)) {
                    return "Cannot find connection for player " + username;
                } else {
                    safeHouse.addInvite(username);
                    INetworkPacket.send(invitedPlayer, PacketTypes.PacketType.SafehouseInvite, safeHouse, this.getExecutorUsername(), username);
                    String executorName = this.getExecutorUsername();
                    LoggerManager.getLogger(FileName.sanitize(executorName))
                        .write(executorName + " invited user " + username.trim() + " to safehouse " + safeHouseTitle.trim());
                    return "Player " + username + " invited to safehouse " + safeHouseTitle;
                }
            }
        }
    }
}
