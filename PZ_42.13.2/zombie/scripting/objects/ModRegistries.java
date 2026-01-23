// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.io.File;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.gameStates.ChooseGameInfo;

public final class ModRegistries {
    public static void init() {
        for (String modId : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modId);
            if (mod != null) {
                File file = new File(mod.getVersionDir() + "/media/registries.lua");
                if (file.exists() && !file.isDirectory()) {
                    LuaManager.RunLua(file.getAbsolutePath());
                } else {
                    file = new File(mod.getCommonDir() + "/media/registries.lua");
                    if (file.exists() && !file.isDirectory()) {
                        LuaManager.RunLua(file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
