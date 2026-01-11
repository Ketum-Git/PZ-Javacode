// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandName(name = "remove")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_Remove")
@RequiredCapability(requiredCapability = Capability.AnimalCheats)
public class RemoveCommand extends CommandBase {
    public RemoveCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    public static String Remove(UdpConnection connection, String subsystem) {
        String result;
        if ("animals".equalsIgnoreCase(subsystem)) {
            GameServer.removeAnimalsConnection = connection;
            result = "Animals removed";
        } else if ("zombies".equalsIgnoreCase(subsystem)) {
            GameServer.removeZombiesConnection = connection;
            result = "Zombies removed";
        } else if ("corpses".equalsIgnoreCase(subsystem)) {
            GameServer.removeCorpsesConnection = connection;
            result = "Corpses removed";
        } else if ("vehicles".equalsIgnoreCase(subsystem)) {
            GameServer.removeVehiclesConnection = connection;
            result = "Vehicles removed";
        } else {
            result = "Subsystem error: " + subsystem;
        }

        return result;
    }

    @Override
    protected String Command() {
        return Remove(this.connection, this.getCommandArg(0));
    }
}
