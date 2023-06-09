package zombie.commands.serverCommands;

import zombie.characters.IsoZombie;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredRight;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameServer;
import zombie.popman.NetworkZombiePacker;
import zombie.util.StringUtils;
import zombie.util.Type;

@CommandName(
   name = "removezombies"
)
@CommandArgs(
   varArgs = true
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_RemoveZombies"
)
@RequiredRight(
   requiredRights = 56
)
public class RemoveZombiesCommand extends CommandBase {
   public RemoveZombiesCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      int var1 = -1;
      int var2 = -1;
      int var3 = -1;
      int var4 = -1;
      boolean var5 = false;
      boolean var6 = false;
      boolean var7 = false;

      int var8;
      for(var8 = 0; var8 < this.getCommandArgsCount() - 1; var8 += 2) {
         String var9 = this.getCommandArg(var8);
         String var10 = this.getCommandArg(var8 + 1);
         switch (var9) {
            case "-radius":
               var1 = PZMath.tryParseInt(var10, -1);
               break;
            case "-reanimated":
               var5 = StringUtils.tryParseBoolean(var10);
               break;
            case "-x":
               var2 = PZMath.tryParseInt(var10, -1);
               break;
            case "-y":
               var3 = PZMath.tryParseInt(var10, -1);
               break;
            case "-z":
               var4 = PZMath.tryParseInt(var10, -1);
               break;
            case "-remove":
               var6 = StringUtils.tryParseBoolean(var10);
               break;
            case "-clear":
               var7 = StringUtils.tryParseBoolean(var10);
               break;
            default:
               return this.getHelp();
         }
      }

      if (var6) {
         GameServer.removeZombiesConnection = this.connection;
         return "Zombies removed.";
      } else if (var4 >= 0 && var4 < 8) {
         for(var8 = var3 - var1; var8 <= var3 + var1; ++var8) {
            for(int var13 = var2 - var1; var13 <= var2 + var1; ++var13) {
               IsoGridSquare var14 = IsoWorld.instance.CurrentCell.getGridSquare(var13, var8, var4);
               if (var14 != null) {
                  int var11;
                  if (var7) {
                     if (!var14.getStaticMovingObjects().isEmpty()) {
                        for(var11 = var14.getStaticMovingObjects().size() - 1; var11 >= 0; --var11) {
                           IsoDeadBody var16 = (IsoDeadBody)Type.tryCastTo((IsoMovingObject)var14.getStaticMovingObjects().get(var11), IsoDeadBody.class);
                           if (var16 != null) {
                              GameServer.sendRemoveCorpseFromMap(var16);
                              var16.removeFromWorld();
                              var16.removeFromSquare();
                           }
                        }
                     }
                  } else if (!var14.getMovingObjects().isEmpty()) {
                     for(var11 = var14.getMovingObjects().size() - 1; var11 >= 0; --var11) {
                        IsoZombie var15 = (IsoZombie)Type.tryCastTo((IsoMovingObject)var14.getMovingObjects().get(var11), IsoZombie.class);
                        if (var15 != null && (var5 || !var15.isReanimatedPlayer())) {
                           NetworkZombiePacker.getInstance().deleteZombie(var15);
                           var15.removeFromWorld();
                           var15.removeFromSquare();
                        }
                     }
                  }
               }
            }
         }

         LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " removed zombies near " + var2 + "," + var3, "IMPORTANT");
         return "Zombies removed.";
      } else {
         return "invalid z";
      }
   }
}
