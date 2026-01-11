// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.util.Iterator;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerOptions;

@CommandName(name = "showoptions")
@CommandHelp(helpText = "UI_ServerOptionDesc_ShowOptions")
@RequiredCapability(requiredCapability = Capability.SeePublicServerOptions)
public class ShowOptionsCommand extends CommandBase {
    public ShowOptionsCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        Iterator<String> it = ServerOptions.instance.getPublicOptions().iterator();
        String key = null;
        String nl = " <LINE> ";
        if (this.connection == null) {
            nl = "\n";
        }

        String result = "List of Server Options:" + nl;

        while (it.hasNext()) {
            key = it.next();
            if (!key.equals("ServerWelcomeMessage")) {
                result = result + "* " + key + "=" + ServerOptions.instance.getOptionByName(key).asConfigOption().getValueAsString() + nl;
            }
        }

        return result + "* ServerWelcomeMessage=" + ServerOptions.instance.serverWelcomeMessage.getValue();
    }
}
