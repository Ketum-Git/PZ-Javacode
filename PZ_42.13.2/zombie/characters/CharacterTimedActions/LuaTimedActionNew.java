// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.CharacterTimedActions;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.ai.astar.IPathfinder;
import zombie.ai.astar.Mover;
import zombie.ai.astar.Path;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.MoveDeltaModifiers;
import zombie.chat.ChatManager;
import zombie.core.ActionManager;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;

@UsedFromLua
public final class LuaTimedActionNew extends BaseAction implements IPathfinder {
    KahluaTable table;
    boolean useCustomRemoteTimedActionSync;
    byte transactionId = -1;
    boolean started;
    private static final ArrayList<Object> keys = new ArrayList<>();

    public LuaTimedActionNew(KahluaTable table, IsoGameCharacter chr) {
        super(chr);
        this.table = table;
        Object maxTime = table.rawget("maxTime");
        this.maxTime = LuaManager.converterManager.fromLuaToJava(maxTime, Integer.class);
        Object stopOnWalk = table.rawget("stopOnWalk");
        Object stopOnRun = table.rawget("stopOnRun");
        Object stopOnAim = table.rawget("stopOnAim");
        Object caloriesModifier = table.rawget("caloriesModifier");
        Object useProgressBar = table.rawget("useProgressBar");
        Object forceProgressBar = table.rawget("forceProgressBar");
        Object loopedAction = table.rawget("loopedAction");
        if (stopOnWalk != null) {
            this.stopOnWalk = LuaManager.converterManager.fromLuaToJava(stopOnWalk, Boolean.class);
        }

        if (stopOnRun != null) {
            this.stopOnRun = LuaManager.converterManager.fromLuaToJava(stopOnRun, Boolean.class);
        }

        if (stopOnAim != null) {
            this.stopOnAim = LuaManager.converterManager.fromLuaToJava(stopOnAim, Boolean.class);
        }

        if (caloriesModifier != null) {
            this.caloriesModifier = LuaManager.converterManager.fromLuaToJava(caloriesModifier, Float.class);
        }

        if (useProgressBar != null) {
            this.useProgressBar = LuaManager.converterManager.fromLuaToJava(useProgressBar, Boolean.class);
        }

        if (forceProgressBar != null) {
            this.forceProgressBar = LuaManager.converterManager.fromLuaToJava(forceProgressBar, Boolean.class);
        }

        if (loopedAction != null) {
            this.loopAction = LuaManager.converterManager.fromLuaToJava(loopedAction, Boolean.class);
        }

        if (table.getMetatable().rawget("complete") == null) {
            this.useCustomRemoteTimedActionSync = true;
        }
    }

    @Override
    public void waitToStart() {
        Boolean bWait = LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.table.rawget("waitToStart"), this.table);
        if (bWait == Boolean.FALSE) {
            super.waitToStart();
        }
    }

    @Override
    public void update() {
        super.update();
        LuaManager.caller.pcallvoid(LuaManager.thread, this.table.rawget("update"), this.table);
        if (GameClient.client && !this.useCustomRemoteTimedActionSync) {
            if (ActionManager.isDone(this.transactionId)) {
                this.forceComplete();
            } else if (ActionManager.isRejected(this.transactionId)) {
                this.forceStop();
            }

            if (this.getTime() == -1) {
                float duration = ActionManager.getDuration(this.transactionId) / 20.0F;
                if (duration > 0.0F && !ActionManager.isLooped(this.transactionId)) {
                    this.table.rawset("maxTime", (double)duration);
                    this.setTime((int)duration);
                }
            }
        }
    }

    @Override
    public boolean valid() {
        Object[] o = LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("isValid"), this.table);
        return o.length > 1 && o[1] instanceof Boolean && (Boolean)o[1];
    }

    @Override
    public void start() {
        DebugLog.Action.trace("%s: start", this.table.rawget("Type"));
        super.start();
        this.currentTime = 0.0F;
        LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("start"), this.table);
        if (GameClient.client && !this.useCustomRemoteTimedActionSync) {
            this.setWaitForFinished(true);
            this.transactionId = ActionManager.getInstance().createNetTimedAction((IsoPlayer)this.chr, this.table);
            this.started = true;
        }

        if (GameClient.client && DebugLog.isLogEnabled(DebugType.Action, LogSeverity.Trace)) {
            ChatManager.getInstance().showServerChatMessage(" -> " + this.table.rawget("Type"));
        }
    }

    @Override
    public void stop() {
        DebugLog.Action.trace("%s: stop", this.table.rawget("Type"));
        super.stop();
        LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("stop"), this.table);
        if (GameClient.client && !this.useCustomRemoteTimedActionSync) {
            ActionManager.remove(this.transactionId, true);
            this.started = false;
        }
    }

    @Override
    public void perform() {
        DebugLog.Action.trace("%s: perform", this.table.rawget("Type"));
        super.perform();
        LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("perform"), this.table);
        if (GameClient.client && !this.useCustomRemoteTimedActionSync) {
            ActionManager.remove(this.transactionId, false);
        }
    }

    @Override
    public void complete() {
        DebugLog.Action.trace("%s: complete", this.table.rawget("Type"));
        super.complete();
        if (!GameClient.client) {
            LuaManager.caller.pcall(LuaManager.thread, this.table.rawget("complete"), this.table);
        }
    }

    @Override
    public void Failed(Mover mover) {
        this.table.rawset("path", null);
        LuaManager.caller.pcallvoid(LuaManager.thread, this.table.rawget("failedPathfind"), this.table);
    }

    @Override
    public void Succeeded(Path path, Mover mover) {
        this.table.rawset("path", path);
        LuaManager.caller.pcallvoid(LuaManager.thread, this.table.rawget("succeededPathfind"), this.table);
    }

    public void Pathfind(IsoGameCharacter chr, int x, int y, int z) {
    }

    @Override
    public String getName() {
        return "timedActionPathfind";
    }

    public void setCurrentTime(float time) {
        this.currentTime = PZMath.clamp(time, 0.0F, (float)this.maxTime);
    }

    public int getTime() {
        return this.maxTime;
    }

    public void setCustomRemoteTimedActionSync(boolean customRemoteTimedActionSync) {
        this.useCustomRemoteTimedActionSync = customRemoteTimedActionSync;
    }

    public void setTime(int maxTime) {
        this.maxTime = maxTime;
    }

    @Override
    public void OnAnimEvent(AnimEvent event) {
        Object functionObj = this.table.rawget("animEvent");
        if (functionObj != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, this.table, event.eventName, event.parameterValue);
        }
    }

    @Override
    public void getDeltaModifiers(MoveDeltaModifiers modifiers) {
        Object functionObj = this.table.rawget("getDeltaModifiers");
        if (functionObj != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, this.table, modifiers);
        }
    }

    public String getMetaType() {
        return this.table != null && this.table.getMetatable() != null ? this.table.getMetatable().getString("Type") : "";
    }

    public void replaceObjectInTable(Object oldObj, Object newObj) {
        if (this.table != null) {
            synchronized (keys) {
                KahluaTableIterator it = this.table.iterator();

                while (it.advance()) {
                    if (it.getValue() == oldObj) {
                        keys.add(it.getKey());
                    }
                }

                if (!keys.isEmpty()) {
                    for (Object key : keys) {
                        this.table.rawset(key, newObj);
                    }
                }

                keys.clear();
            }
        }
    }

    public KahluaTable getTable() {
        return this.table;
    }
}
