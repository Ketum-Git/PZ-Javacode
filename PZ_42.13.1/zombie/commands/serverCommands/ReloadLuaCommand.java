// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;

@CommandName(name = "reloadlua")
@CommandArgs(required = "(\\S+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_ReloadLua")
@RequiredCapability(requiredCapability = Capability.ReloadLuaFiles)
public class ReloadLuaCommand extends CommandBase {
    public ReloadLuaCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String filePath = this.getCommandArg(0);

        for (String s : LuaManager.loaded) {
            if (s.endsWith(filePath)) {
                LuaManager.loaded.remove(s);
                LuaManager.RunLua(s, true);
                return "Lua file reloaded";
            }
        }

        return "Unknown Lua file";
    }
}
