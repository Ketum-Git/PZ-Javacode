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
import zombie.network.chat.ChatServer;

@CommandName(name = "servermsg")
@CommandArgs(required = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_ServerMsg")
@RequiredCapability(requiredCapability = Capability.DisplayServerMessage)
public class ServerMessageCommand extends CommandBase {
    public ServerMessageCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String message = this.getCommandArg(0);
        if (this.connection == null) {
            ChatServer.getInstance().sendServerAlertMessageToServerChat(message);
        } else {
            String author = this.getExecutorUsername();
            ChatServer.getInstance().sendServerAlertMessageToServerChat(author, message);
        }

        return "Message sent.";
    }
}
