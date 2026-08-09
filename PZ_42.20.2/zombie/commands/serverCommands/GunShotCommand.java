// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.AmbientStreamManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;

@CommandName(name = "gunshot")
@CommandHelp(helpText = "UI_ServerOptionDesc_Gunshot")
@RequiredCapability(requiredCapability = Capability.MakeEventsAlarmGunshot)
public class GunShotCommand extends CommandBase {
    public GunShotCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        AmbientStreamManager.instance.doGunEvent();
        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " did gunshot");
        return "Gunshot fired";
    }
}
