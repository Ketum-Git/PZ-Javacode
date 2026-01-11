// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.characters.animals.IsoAnimal;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.popman.animal.AnimalInstanceManager;

@CommandName(name = "list")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_List")
@RequiredCapability(requiredCapability = Capability.LoginOnServer)
public class ListCommand extends CommandBase {
    public ListCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    public static String List(String subsystem) {
        StringBuilder result = new StringBuilder();
        if ("animals".equalsIgnoreCase(subsystem)) {
            if (GameServer.server) {
                result.append("Server animals list:\n");
            } else if (GameClient.client) {
                result.append("Client animals list:\n");
            }

            for (IsoAnimal animal : AnimalInstanceManager.getInstance().getAnimals()) {
                if (animal != null) {
                    result.append("* ")
                        .append(animal.getOnlineID())
                        .append(" ")
                        .append(animal.getAnimalType())
                        .append(" ")
                        .append(animal.getBreed().getName())
                        .append(" ")
                        .append(animal.getHutch() == null ? "world" : "hutch")
                        .append("\n");
                }
            }
        } else {
            result.append("Subsystem error: ").append(subsystem);
        }

        return result.toString();
    }

    @Override
    protected String Command() {
        return List(this.getCommandArg(0));
    }
}
