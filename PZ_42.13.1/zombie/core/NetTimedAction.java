// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Prototype;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.JSONField;
import zombie.network.PZNetKahluaNull;
import zombie.network.PZNetKahluaTableImpl;

@UsedFromLua
public class NetTimedAction extends Action {
    @JSONField
    public String type = "";
    @JSONField
    public String name = "";
    public KahluaTable action;
    @JSONField
    PZNetKahluaTableImpl actionArgs;
    @JSONField
    boolean isUsingTimeout = true;

    public void set(IsoPlayer player, KahluaTable action) {
        this.action = action;
        this.type = this.action.getMetatable().getString("Type");
        this.name = this.action.getString("name");
        this.actionArgs = new PZNetKahluaTableImpl(new LinkedHashMap<>());
        Prototype itemNew = ((LuaClosure)this.action.getMetatable().rawget("new")).prototype;

        for (int i = 1; i < itemNew.numParams; i++) {
            String paramName = itemNew.locvars[i];
            Object o = this.action.rawget(paramName);
            if (o == null) {
                o = PZNetKahluaNull.instance;
            }

            this.actionArgs.rawset(paramName, o);
        }

        this.isUsingTimeout = LuaManager.caller.protectedCallBoolean(LuaManager.thread, action.rawget("isUsingTimeout"), action);
        this.set(player);
    }

    @Override
    float getDuration() {
        if (this.action == null) {
            return 0.0F;
        } else {
            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, this.action.rawget("getDuration"), this.action);
            if (result.isSuccess()) {
                float duration = ((Double)result.getFirst()).floatValue();
                return duration == -1.0F ? -1.0F : duration * 20.0F;
            } else {
                return 0.0F;
            }
        }
    }

    @Override
    void start() {
        this.setTimeData();
        Object functionObject = this.action.rawget("serverStart");
        if (functionObject != null) {
            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, this.action);
            if (!result.isSuccess()) {
                DebugLog.Action.warn("Get function object \"serverStart\" failed");
            }
        }
    }

    @Override
    void stop() {
        Object functionObject = this.action.rawget("serverStop");
        if (functionObject != null) {
            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, this.action);
            if (!result.isSuccess()) {
                DebugLog.Action.warn("Get function object \"serverStop\" failed");
            }
        }
    }

    @Override
    boolean isValid() {
        return false;
    }

    @Override
    boolean isUsingTimeout() {
        return this.isUsingTimeout;
    }

    @Override
    void update() {
    }

    @Override
    boolean perform() {
        try {
            return LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.action.rawget("complete"), this.action);
        } catch (Exception var2) {
            DebugLog.Action.printException(var2, "Perform filed", LogSeverity.Error);
            return false;
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (this.state == Transaction.TransactionState.Request) {
            this.type = GameWindow.ReadString(b).trim();
            this.name = GameWindow.ReadString(b).trim();
            this.actionArgs = new PZNetKahluaTableImpl(new LinkedHashMap<>());
            this.actionArgs.load(b, connection);
            Object classObject = LuaManager.get(this.type);
            Object functionObject = LuaManager.getFunctionObject(this.type + ".new");
            byte numParams = (byte)(this.actionArgs.size() + 1);
            Object[] arguments = new Object[numParams];
            int i = 1;
            arguments[0] = classObject;
            KahluaTableIterator it = this.actionArgs.iterator();

            while (it.advance()) {
                arguments[i++] = it.getValue();
            }

            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, arguments);
            if (!result.isSuccess() || result.getFirst() == null) {
                this.action = null;
                return;
            }

            this.action = (KahluaTable)result.getFirst();
            this.action.rawset("name", this.name);
            this.action.rawset("netAction", this);
        }
    }

    public void forceComplete() {
        this.endTime = GameTime.getServerTimeMills();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        if (this.state == Transaction.TransactionState.Request) {
            b.putUTF(this.type);
            b.putUTF(this.name);
            this.actionArgs.save(b.bb);
        }
    }

    public void animEvent(String event, String parameter) {
        Object functionObject = this.action.rawget("animEvent");
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this.action, event, parameter);
        }
    }
}
