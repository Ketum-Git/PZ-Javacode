package zombie.commands.serverCommands;

import zombie.characters.IsoPlayer;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredRight;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

@CommandName(
   name = "additem"
)
@AltCommandArgs({@CommandArgs(
   required = {"(.+)", "([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)"},
   optional = "(\\d+)",
   argName = "add item to player"
), @CommandArgs(
   required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)"},
   optional = "(\\d+)",
   argName = "add item to me"
)})
@CommandHelp(
   helpText = "UI_ServerOptionDesc_AddItem"
)
@RequiredRight(
   requiredRights = 60
)
public class AddItemCommand extends CommandBase {
   public static final String toMe = "add item to me";
   public static final String toPlayer = "add item to player";

   public AddItemCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      int var1 = 1;
      if (this.argsName.equals("add item to me") && this.connection == null) {
         return "Pass username";
      } else {
         if (this.getCommandArgsCount() > 1) {
            int var2 = this.getCommandArgsCount();
            if (this.argsName.equals("add item to me") && var2 == 2 || this.argsName.equals("add item to player") && var2 == 3) {
               var1 = Integer.parseInt(this.getCommandArg(this.getCommandArgsCount() - 1));
            }
         }

         IsoPlayer var3;
         String var9;
         if (this.argsName.equals("add item to player")) {
            var3 = GameServer.getPlayerByUserNameForCommand(this.getCommandArg(0));
            if (var3 == null) {
               return "No such user";
            }

            var9 = var3.getDisplayName();
         } else {
            var3 = GameServer.getPlayerByRealUserName(this.getExecutorUsername());
            if (var3 == null) {
               return "No such user";
            }

            var9 = var3.getDisplayName();
         }

         String var10;
         if (this.argsName.equals("add item to me")) {
            var10 = this.getCommandArg(0);
         } else {
            var10 = this.getCommandArg(1);
         }

         Item var4 = ScriptManager.instance.FindItem(var10);
         if (var4 == null) {
            return "Item " + var10 + " doesn't exist.";
         } else {
            IsoPlayer var5 = GameServer.getPlayerByUserNameForCommand(var9);
            if (var5 != null) {
               var9 = var5.getDisplayName();
               UdpConnection var6 = GameServer.getConnectionByPlayerOnlineID(var5.OnlineID);
               if (var6 != null) {
                  ByteBufferWriter var7 = var6.startPacket();
                  PacketTypes.PacketType.AddItemInInventory.doPacket(var7);
                  var7.putShort(var5.OnlineID);
                  var7.putUTF(var10);
                  var7.putInt(var1);
                  PacketTypes.PacketType.AddItemInInventory.send(var6);
                  LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " added item " + var10 + " in " + var9 + "'s inventory");
                  ByteBufferWriter var8 = var6.startPacket();
                  PacketTypes.PacketType.RequestInventory.doPacket(var8);
                  var8.putShort(var5.OnlineID);
                  PacketTypes.PacketType.RequestInventory.send(var6);
                  return "Item " + var10 + " Added in " + var9 + "'s inventory.";
               }
            }

            return "User " + var9 + " not found.";
         }
      }
   }
}
