// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;

@CommandName(name = "checkModsNeedUpdate")
@CommandHelp(helpText = "UI_ServerOptionDesc_CheckModsNeedUpdate")
@RequiredCapability(requiredCapability = Capability.ManipulateMods)
public class CheckModsNeedUpdate extends CommandBase {
    public CheckModsNeedUpdate(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        LuaManager.GlobalObject.checkModsNeedUpdate(this.connection);
        return "Checking started. The answer will be written in the log file and in the chat";
    }
}
