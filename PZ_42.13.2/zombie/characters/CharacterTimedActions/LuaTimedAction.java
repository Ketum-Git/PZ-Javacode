// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.CharacterTimedActions;

import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public final class LuaTimedAction extends BaseAction {
    KahluaTable table;
    public static Object[] statObj = new Object[6];

    public LuaTimedAction(KahluaTable table, IsoGameCharacter chr) {
        super(chr);
        this.table = table;
        Object maxTime = table.rawget("maxTime");
        this.maxTime = LuaManager.converterManager.fromLuaToJava(maxTime, Integer.class);
        Object stopOnWalk = table.rawget("stopOnWalk");
        Object stopOnRun = table.rawget("stopOnRun");
        Object stopOnAim = table.rawget("stopOnAim");
        Object onUpdateFunc = table.rawget("onUpdateFunc");
        if (stopOnWalk != null) {
            this.stopOnWalk = LuaManager.converterManager.fromLuaToJava(stopOnWalk, Boolean.class);
        }

        if (stopOnRun != null) {
            this.stopOnRun = LuaManager.converterManager.fromLuaToJava(stopOnRun, Boolean.class);
        }

        if (stopOnAim != null) {
            this.stopOnAim = LuaManager.converterManager.fromLuaToJava(stopOnAim, Boolean.class);
        }
    }

    @Override
    public void update() {
        statObj[0] = this.table.rawget("character");
        statObj[1] = this.table.rawget("param1");
        statObj[2] = this.table.rawget("param2");
        statObj[3] = this.table.rawget("param3");
        statObj[4] = this.table.rawget("param4");
        statObj[5] = this.table.rawget("param5");
        LuaManager.caller.pcallvoid(LuaManager.thread, this.table.rawget("onUpdateFunc"), statObj);
        super.update();
    }

    @Override
    public boolean valid() {
        Object[] o = LuaManager.caller
            .pcall(
                LuaManager.thread,
                this.table.rawget("isValidFunc"),
                this.table.rawget("character"),
                this.table.rawget("param1"),
                this.table.rawget("param2"),
                this.table.rawget("param3"),
                this.table.rawget("param4"),
                this.table.rawget("param5")
            );
        return o.length > 0 && (Boolean)o[0];
    }

    @Override
    public void start() {
        super.start();
        this.currentTime = 0.0F;
        LuaManager.caller
            .pcall(
                LuaManager.thread,
                this.table.rawget("startFunc"),
                this.table.rawget("character"),
                this.table.rawget("param1"),
                this.table.rawget("param2"),
                this.table.rawget("param3"),
                this.table.rawget("param4"),
                this.table.rawget("param5")
            );
    }

    @Override
    public void stop() {
        super.stop();
        LuaManager.caller
            .pcall(
                LuaManager.thread,
                this.table.rawget("onStopFunc"),
                this.table.rawget("character"),
                this.table.rawget("param1"),
                this.table.rawget("param2"),
                this.table.rawget("param3"),
                this.table.rawget("param4"),
                this.table.rawget("param5")
            );
    }

    @Override
    public void perform() {
        super.perform();
        LuaManager.caller
            .pcall(
                LuaManager.thread,
                this.table.rawget("performFunc"),
                this.table.rawget("character"),
                this.table.rawget("param1"),
                this.table.rawget("param2"),
                this.table.rawget("param3"),
                this.table.rawget("param4"),
                this.table.rawget("param5")
            );
    }
}
