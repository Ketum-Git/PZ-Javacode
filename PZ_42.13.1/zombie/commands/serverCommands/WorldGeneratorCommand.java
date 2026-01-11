// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.iso.WorldGenerate;

@CommandName(name = "worldgen")
@CommandArgs(required = "(.+)", optional = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_Worldgen")
@RequiredCapability(requiredCapability = Capability.SaveWorld)
public class WorldGeneratorCommand extends CommandBase {
    public WorldGeneratorCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        if (this.getCommandArgsCount() >= 1) {
            String application = this.getCommandArg(0);
            if (this.getCommandArgsCount() == 2 && "slow".equals(this.getCommandArg(1))) {
                WorldGenerate.instance.getStatus().threadCount = 1;
            } else {
                WorldGenerate.instance.getStatus().threadCount = Runtime.getRuntime().availableProcessors();
            }

            if ("start".equals(application)) {
                WorldGenerate.instance.init(Core.gameSaveWorld);
                return "Preparing for generating map: "
                    + WorldGenerate.instance.getStatus().generatedChunks
                    + "/"
                    + WorldGenerate.instance.getStatus().allChunks
                    + " using "
                    + WorldGenerate.instance.getStatus().threadCount
                    + " threads";
            }

            if ("recheck".equals(application)) {
                WorldGenerate.instance.recheck(Core.gameSaveWorld);
                return "Preparing for generating map: "
                    + WorldGenerate.instance.getStatus().generatedChunks
                    + "/"
                    + WorldGenerate.instance.getStatus().allChunks
                    + " using "
                    + WorldGenerate.instance.getStatus().threadCount
                    + " threads";
            }

            if ("stop".equals(application)) {
                WorldGenerate.instance.stop();
                return "Stopped";
            }

            if ("status".equals(application)) {
                return "Generating map: "
                    + WorldGenerate.instance.getStatus().generatedChunks
                    + "/"
                    + WorldGenerate.instance.getStatus().allChunks
                    + " using "
                    + WorldGenerate.instance.getStatus().threadCount
                    + " threads";
            }
        }

        return this.getHelp();
    }
}
