// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;

@CommandName(name = "save")
@CommandHelp(helpText = "UI_ServerOptionDesc_Save")
@RequiredCapability(requiredCapability = Capability.SaveWorld)
public class SaveCommand extends CommandBase {
    public SaveCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        ServerMap.instance.QueueSaveAll();
        INetworkPacket.sendToAll(PacketTypes.PacketType.StartPause, null);
        return "World saved";
    }
}
