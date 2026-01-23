// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.util.ArrayList;
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
import zombie.inventory.InventoryItem;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@CommandName(name = "removeitem")
@CommandArgs(required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)", "(\\d+)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_RemoveItem")
@RequiredCapability(requiredCapability = Capability.EditItem)
public class RemoveItemCommand extends CommandBase {
    public RemoveItemCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String itemType = this.getCommandArg(0);
        int count = Integer.parseInt(this.getCommandArg(1));
        IsoPlayer player = GameServer.getPlayerByRealUserName(this.getExecutorUsername());
        if (player != null) {
            UdpConnection c = GameServer.getConnectionByPlayerOnlineID(player.onlineId);
            if (c != null && !player.isDead()) {
                ArrayList<InventoryItem> items = new ArrayList<>();
                if (count == 0) {
                    items.addAll(player.getInventory().RemoveAll(itemType));
                } else {
                    for (int i = 0; i < count; i++) {
                        items.add(player.getInventory().RemoveOneOf(itemType, true));
                    }
                }

                count = items.size();

                for (InventoryItem item : items) {
                    INetworkPacket.send(c, PacketTypes.PacketType.RemoveInventoryItemFromContainer, player.getInventory(), item);
                }

                String message = String.format("%s removed %d items %s from inventory", this.getExecutorUsername(), count, itemType);
                LoggerManager.getLogger("admin").write(message);
                return message;
            }
        }

        return "User " + this.getExecutorUsername() + " not found.";
    }
}
