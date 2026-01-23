// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;

@CommandName(name = "clear")
@RequiredCapability(requiredCapability = Capability.DebugConsole)
public class ClearCommand extends CommandBase {
    public ClearCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String result = "Console cleared";
        if (this.connection == null) {
            for (int clear = 0; clear < 100; clear++) {
                System.out.println();
            }
        } else {
            StringBuilder clearMsg = new StringBuilder();

            for (int i = 0; i < 50; i++) {
                clearMsg.append("<LINE>");
            }

            result = clearMsg.toString() + result;
        }

        return result;
    }
}
