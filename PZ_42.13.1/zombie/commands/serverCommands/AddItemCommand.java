// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.util.ArrayList;
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
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

@CommandName(name = "additem")
@AltCommandArgs(
    {
            @CommandArgs(required = {"(.+)", "([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)"}, optional = "(\\d+)", argName = "add item to player"),
            @CommandArgs(required = "([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)", optional = "(\\d+)", argName = "add item to me")
    }
)
@CommandHelp(helpText = "UI_ServerOptionDesc_AddItem")
@RequiredCapability(requiredCapability = Capability.AddItem)
public class AddItemCommand extends CommandBase {
    public static final String toMe = "add item to me";
    public static final String toPlayer = "add item to player";

    public AddItemCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        int total = 1;
        if (this.argsName.equals("add item to me") && this.connection == null) {
            return "Pass username";
        } else {
            if (this.getCommandArgsCount() > 1) {
                int arsc = this.getCommandArgsCount();
                if (this.argsName.equals("add item to me") && arsc == 2 || this.argsName.equals("add item to player") && arsc == 3) {
                    total = Integer.parseInt(this.getCommandArg(this.getCommandArgsCount() - 1));
                }
            }

            if (total > 100) {
                System.out.println("Cannot spawn over 100 items at a time");
                total = 100;
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

            String item;
            if (this.argsName.equals("add item to me")) {
                item = this.getCommandArg(0);
            } else {
                item = this.getCommandArg(1);
            }

            Item scriptItem = ScriptManager.instance.FindItem(item);
            if (scriptItem == null) {
                return "Item " + item + " doesn't exist.";
            } else {
                IsoPlayer player = GameServer.getPlayerByUserNameForCommand(username);
                if (player != null) {
                    username = player.getDisplayName();
                    UdpConnection c = GameServer.getConnectionByPlayerOnlineID(player.onlineId);
                    if (c != null && !player.isDead()) {
                        ArrayList<InventoryItem> items = player.getInventory().AddItems(item, total);
                        INetworkPacket.send(c, PacketTypes.PacketType.AddItemInInventory, player, items);
                        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " added item " + item + " in " + username + "'s inventory");
                        return "Item " + item + " Added in " + username + "'s inventory.";
                    }
                }

                return "User " + username + " not found.";
            }
        }
    }
}
