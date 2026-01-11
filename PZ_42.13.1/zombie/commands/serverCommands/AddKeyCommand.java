// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.AltCommandArgs;
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

@CommandName(name = "addkey")
@AltCommandArgs(
    {
            @CommandArgs(required = {"(.+)", "(\\d+)"}, optional = "(.+)", argName = "add item to player"),
            @CommandArgs(required = "(\\d+)", optional = "(.+)", argName = "add item to me")
    }
)
@CommandHelp(helpText = "UI_ServerOptionDesc_AddKey")
@RequiredCapability(requiredCapability = Capability.AddItem)
public class AddKeyCommand extends CommandBase {
    public static final String toMe = "add item to me";
    public static final String toPlayer = "add item to player";

    public AddKeyCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String name = "";
        if (this.argsName.equals("add item to me") && this.connection == null) {
            return "Pass username";
        } else {
            if (this.getCommandArgsCount() > 1) {
                int arsc = this.getCommandArgsCount();
                if (this.argsName.equals("add item to me") && arsc == 2 || this.argsName.equals("add item to player") && arsc == 3) {
                    name = this.getCommandArg(this.getCommandArgsCount() - 1);
                }
            }

            String username;
            if (this.argsName.equals("add item to player")) {
                IsoPlayer p = GameServer.getPlayerByUserNameForCommand(this.getCommandArg(0));
                if (p == null) {
                    return "No such user";
                }

                username = p.getDisplayName();
            } else {
                IsoPlayer p = GameServer.getPlayerByRealUserName(this.getExecutorUsername());
                if (p == null) {
                    return "No such user";
                }

                username = p.getDisplayName();
            }

            int keyID;
            if (this.argsName.equals("add item to me")) {
                keyID = Integer.parseInt(this.getCommandArg(0));
            } else {
                keyID = Integer.parseInt(this.getCommandArg(1));
            }

            IsoPlayer player = GameServer.getPlayerByUserNameForCommand(username);
            if (player != null) {
                username = player.getDisplayName();
                UdpConnection c = GameServer.getConnectionByPlayerOnlineID(player.onlineId);
                if (c != null && !player.isDead()) {
                    InventoryItem item = player.getInventory().AddItem("Base.Key1");
                    item.setKeyId(keyID);
                    if (!name.isBlank()) {
                        item.setName(name);
                    }

                    INetworkPacket.send(c, PacketTypes.PacketType.AddInventoryItemToContainer, player.getInventory(), item);
                    LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " added item " + item + " in " + username + "'s inventory");
                    return "Key " + item + " Added in " + username + "'s inventory.";
                }
            }

            return "User " + username + " not found.";
        }
    }
}
