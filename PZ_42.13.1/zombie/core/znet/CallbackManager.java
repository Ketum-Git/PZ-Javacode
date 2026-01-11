// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

import zombie.Lua.LuaEventManager;

public class CallbackManager implements IJoinRequestCallback {
    public CallbackManager() {
        SteamUtils.addJoinRequestCallback(this);
    }

    @Override
    public void onJoinRequest(long friendSteamID, String connectionString) {
        LuaEventManager.triggerEvent("OnAcceptInvite", connectionString);
    }
}
