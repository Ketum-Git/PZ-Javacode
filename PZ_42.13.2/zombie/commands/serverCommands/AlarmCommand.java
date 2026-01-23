// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.AmbientStreamManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandName(name = "alarm")
@CommandHelp(helpText = "UI_ServerOptionDesc_Alarm")
@RequiredCapability(requiredCapability = Capability.MakeEventsAlarmGunshot)
public class AlarmCommand extends CommandBase {
    public AlarmCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        IsoPlayer player = GameServer.getPlayerByUserName(this.getExecutorUsername());
        if (player != null && player.getSquare() != null && player.getSquare().getBuilding() != null) {
            player.getSquare().getBuilding().getDef().alarmed = true;
            AmbientStreamManager.instance.doAlarm(player.getSquare().getRoom().def);
            return "Alarm sounded";
        } else {
            return "Not in a room";
        }
    }
}
