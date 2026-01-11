// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoZombie;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.popman.NetworkZombiePacker;
import zombie.util.StringUtils;

@CommandName(name = "removezombies")
@CommandArgs(varArgs = true)
@CommandHelp(helpText = "UI_ServerOptionDesc_RemoveZombies")
@RequiredCapability(requiredCapability = Capability.ManipulateZombie)
public class RemoveZombiesCommand extends CommandBase {
    public RemoveZombiesCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        int radius = -1;
        int x = -1;
        int y = -1;
        int z = -1;
        boolean reanimated = false;
        boolean remove = false;

        for (int i = 0; i < this.getCommandArgsCount() - 1; i += 2) {
            String option = this.getCommandArg(i);
            String value = this.getCommandArg(i + 1);
            switch (option) {
                case "-radius":
                    radius = PZMath.tryParseInt(value, -1);
                    break;
                case "-reanimated":
                    reanimated = StringUtils.tryParseBoolean(value);
                    break;
                case "-x":
                    x = PZMath.tryParseInt(value, -1);
                    break;
                case "-y":
                    y = PZMath.tryParseInt(value, -1);
                    break;
                case "-z":
                    z = PZMath.tryParseInt(value, -1);
                    break;
                case "-remove":
                    remove = StringUtils.tryParseBoolean(value);
                    break;
                default:
                    return this.getHelp();
            }
        }

        if (remove) {
            GameServer.removeZombiesConnection = this.connection;
            return "Zombies removed.";
        } else if (z > -32 && z < 32) {
            for (int y1 = y - radius; y1 <= y + radius; y1++) {
                for (int x1 = x - radius; x1 <= x + radius; x1++) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x1, y1, z);
                    if (square != null && !square.getMovingObjects().isEmpty()) {
                        for (int i = square.getMovingObjects().size() - 1; i >= 0; i--) {
                            if (square.getMovingObjects().get(i) instanceof IsoZombie zombie && (reanimated || !zombie.isReanimatedPlayer())) {
                                NetworkZombiePacker.getInstance().deleteZombie(zombie);
                                zombie.removeFromWorld();
                                zombie.removeFromSquare();
                            }
                        }
                    }
                }
            }

            LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " removed zombies near " + x + "," + y, "IMPORTANT");
            return "Zombies removed.";
        } else {
            return "invalid z";
        }
    }
}
