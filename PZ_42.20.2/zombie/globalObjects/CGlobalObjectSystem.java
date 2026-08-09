// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;

@UsedFromLua
public final class CGlobalObjectSystem extends GlobalObjectSystem {
    public CGlobalObjectSystem(String name) {
        super(name);
    }

    @Override
    protected GlobalObject makeObject(int x, int y, int z) {
        return new CGlobalObject(this, x, y, z);
    }

    public void sendCommand(String command, IsoPlayer player, KahluaTable args) {
        CGlobalObjectNetwork.sendClientCommand(player, this.name, command, args);
    }

    public void receiveServerCommand(String command, KahluaTable args) {
        Object function = this.modData.rawget("OnServerCommand");
        if (function == null) {
            throw new IllegalStateException("OnServerCommand method undefined for system '" + this.name + "'");
        } else {
            LuaManager.caller.pcallvoid(LuaManager.thread, function, this.modData, command, args);
        }
    }

    public void receiveNewLuaObjectAt(int x, int y, int z, KahluaTable args) {
        Object function = this.modData.rawget("newLuaObjectAt");
        if (function == null) {
            throw new IllegalStateException("newLuaObjectAt method undefined for system '" + this.name + "'");
        } else {
            LuaManager.caller
                .pcall(LuaManager.thread, function, this.modData, BoxedStaticValues.toDouble(x), BoxedStaticValues.toDouble(y), BoxedStaticValues.toDouble(z));
            GlobalObject globalObject = this.getObjectAt(x, y, z);
            if (globalObject != null) {
                KahluaTableIterator it = args.iterator();

                while (it.advance()) {
                    globalObject.getModData().rawset(it.getKey(), it.getValue());
                }
            }
        }
    }

    public void receiveRemoveLuaObjectAt(int x, int y, int z) {
        Object function = this.modData.rawget("removeLuaObjectAt");
        if (function == null) {
            throw new IllegalStateException("removeLuaObjectAt method undefined for system '" + this.name + "'");
        } else {
            LuaManager.caller
                .pcall(LuaManager.thread, function, this.modData, BoxedStaticValues.toDouble(x), BoxedStaticValues.toDouble(y), BoxedStaticValues.toDouble(z));
        }
    }

    public void receiveUpdateLuaObjectAt(int x, int y, int z, KahluaTable args) {
        GlobalObject globalObject = this.getObjectAt(x, y, z);
        if (globalObject != null) {
            KahluaTableIterator it = args.iterator();

            while (it.advance()) {
                globalObject.getModData().rawset(it.getKey(), it.getValue());
            }

            Object function = this.modData.rawget("OnLuaObjectUpdated");
            if (function == null) {
                throw new IllegalStateException("OnLuaObjectUpdated method undefined for system '" + this.name + "'");
            } else {
                LuaManager.caller.pcall(LuaManager.thread, function, this.modData, globalObject.getModData());
            }
        }
    }

    @Override
    public void Reset() {
        super.Reset();
        this.modData.wipe();
    }
}
