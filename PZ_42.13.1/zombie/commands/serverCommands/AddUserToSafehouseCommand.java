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
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

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
        String invitedName = this.getCommandArg(0);
        String safeHouseName = this.getCommandArg(1);
        LoggerManager.getLogger(this.getExecutorUsername())
            .write(this.getExecutorUsername() + " invited user " + invitedName.trim() + " to safehouse " + safeHouseName.trim());

        try {
            IsoPlayer invitedPlayer = GameServer.getPlayerByUserName(invitedName);
            if (invitedPlayer == null) {
                String message = String.format("Cannot find player \"%s\"!", invitedName);
                DebugLog.Multiplayer.debugln(message);
                return message;
            } else {
                UdpConnection connection = GameServer.getConnectionFromPlayer(invitedPlayer);
                if (connection == null) {
                    String message = String.format("Cannot find connection for player \"%s\"!", invitedName);
                    DebugLog.Multiplayer.debugln(message);
                    return message;
                } else {
                    SafeHouse safeHouse = SafeHouse.getSafeHouse(safeHouseName);
                    if (safeHouse == null) {
                        String message = String.format("Cannot find safehouse \"%s\"!", safeHouseName);
                        DebugLog.Multiplayer.debugln(message);
                        return message;
                    } else {
                        IsoPlayer host = GameServer.getPlayerByUserName(this.getExecutorUsername());
                        if (host == null) {
                            String message = String.format("Cannot find host player \"%s\"!", this.getExecutorUsername());
                            DebugLog.Multiplayer.debugln(message);
                            return message;
                        } else {
                            INetworkPacket.send(connection, PacketTypes.PacketType.SafehouseInvite, safeHouse, host, invitedName);
                            return "Safehouse invite sent to " + invitedName;
                        }
                    }
                }
            }
        } catch (Exception var8) {
            var8.printStackTrace();
            return "exception occurs";
        }
    }
}
