// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;

@CommandNames({@CommandName(name = "reloadalllua"), @CommandName(name = "reloadluaall")})
@CommandHelp(helpText = "UI_ServerOptionDesc_ReloadLua")
@RequiredCapability(requiredCapability = Capability.ReloadLuaFiles)
public class ReloadAllLuaCommand extends CommandBase {
    public ReloadAllLuaCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        int size = LuaManager.loaded.size();

        for (int i = 0; i < size; i++) {
            String luaFile = LuaManager.loaded.get(i);
            System.out.println(luaFile + " Reloaded " + i + "/" + size);
            LuaManager.loaded.remove(luaFile);
            LuaManager.RunLua(luaFile, true);
        }

        return "Lua files reloaded";
    }
}
