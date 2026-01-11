// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

@CommandName(name = "help")
@CommandArgs(optional = "(\\w+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_Help")
@RequiredCapability(requiredCapability = Capability.LoginOnServer)
public class HelpCommand extends CommandBase {
    public HelpCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String command = this.getCommandArg(0);
        if (command != null) {
            Class<?> cls = findCommandCls(command);
            return cls != null ? getHelp(cls) : "Unknown command /" + command;
        } else {
            String carriageReturn = " <LINE> ";
            StringBuilder result = new StringBuilder();
            if (this.connection == null) {
                carriageReturn = "\n";
            }

            if (!GameServer.server) {
                for (String line : ServerOptions.getClientCommandList(this.connection != null)) {
                    result.append(line);
                }
            }

            result.append("List of ").append("server").append(" commands : ");
            String help = "";
            Map<String, String> commandsHelp = new TreeMap<>();

            for (Class<?> cls : getSubClasses()) {
                if (!isDisabled(cls)) {
                    help = getHelp(cls);
                    if (help != null) {
                        commandsHelp.put(getCommandName(cls), help);
                    }
                }
            }

            for (Entry<String, String> entry : commandsHelp.entrySet()) {
                result.append(carriageReturn).append("* ").append(entry.getKey()).append(" : ").append(entry.getValue());
            }

            return result.toString();
        }
    }
}
