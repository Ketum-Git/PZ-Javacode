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
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;

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
        String kickName = this.getCommandArg(0);
        String safeHouseName = this.getCommandArg(1);

        try {
            SafeHouse safe = SafeHouse.getSafeHouse(safeHouseName);
            if (safe == null) {
                return "safehouse is null";
            } else {
                safe.removePlayer(kickName);
                INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, safe, false);
                if (!ServerOptions.instance.safehouseAllowTrepass.getValue()) {
                    IsoPlayer player = GameServer.getPlayerByUserName(kickName);
                    if (player != null
                        && player.getX() >= safe.getX()
                        && player.getX() < safe.getX2()
                        && player.getY() >= safe.getY()
                        && player.getY() < safe.getY2()) {
                        UdpConnection c = GameServer.getConnectionFromPlayer(player);
                        if (c != null) {
                            GameServer.sendTeleport(player, safe.getX() - 1.0F, safe.getY() - 1.0F, 0.0F);
                            if (player.isAsleep()) {
                                player.setAsleep(false);
                                player.setAsleepTime(0.0F);
                                INetworkPacket.sendToAll(PacketTypes.PacketType.WakeUpPlayer, player);
                            }
                        }
                    }
                }

                LoggerManager.getLogger(this.getExecutorUsername())
                    .write(this.getExecutorUsername() + " kicked user " + kickName.trim() + " from safehouse " + safeHouseName.trim());
                return "Player " + kickName + " kicked from a safehouse " + safeHouseName;
            }
        } catch (Exception var6) {
            var6.printStackTrace();
            return "exception occurs";
        }
    }
}
