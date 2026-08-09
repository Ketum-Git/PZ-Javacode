// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;

@CommandName(name = "chopper")
@CommandArgs(optional = "([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)")
@CommandHelp(helpText = "UI_ServerOptionDesc_Chopper")
@RequiredCapability(requiredCapability = Capability.MakeEventsAlarmGunshot)
public class ChopperCommand extends CommandBase {
    public ChopperCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String msg;
        if (this.getCommandArgsCount() == 1) {
            if ("stop".equals(this.getCommandArg(0))) {
                IsoWorld.instance.helicopter.deactivate();
                msg = "Chopper deactivated";
            } else if ("start".equals(this.getCommandArg(0))) {
                IsoWorld.instance.helicopter.pickRandomTarget();
                msg = "Chopper activated";
            } else {
                msg = this.getHelp();
            }
        } else {
            IsoWorld.instance.helicopter.pickRandomTarget();
            msg = "Chopper launched";
        }

        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " did chopper");
        return msg;
    }
}
