// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.GameTime;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@CommandNames({@CommandName(name = "setTimeSpeed"), @CommandName(name = "sts")})
@CommandArgs(required = "(\\d+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_SetTimeSpeed")
@RequiredCapability(requiredCapability = Capability.ConnectWithDebug)
public class SetTimeSpeedCommand extends CommandBase {
    public SetTimeSpeedCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        float newSpeed = Float.parseFloat(this.getCommandArg(0));
        GameTime.getInstance().setMultiplier(newSpeed);
        INetworkPacket.sendToAll(PacketTypes.PacketType.SetMultiplier, null);
        return "Multiplier was set on the following value: " + newSpeed;
    }
}
