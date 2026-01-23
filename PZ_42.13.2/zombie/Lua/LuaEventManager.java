// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.Lua;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Platform;
import se.krka.kahlua.vm.Prototype;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.util.Pool;
import zombie.util.PooledObject;

@UsedFromLua
public final class LuaEventManager implements JavaFunction {
    public static final ArrayList<LuaClosure> OnTickCallbacks = new ArrayList<>();
    private static Object[][] a1 = new Object[1][1];
    private static Object[][] a2 = new Object[1][2];
    private static Object[][] a3 = new Object[1][3];
    private static Object[][] a4 = new Object[1][4];
    private static Object[][] a5 = new Object[1][5];
    private static Object[][] a6 = new Object[1][6];
    private static Object[][] a7 = new Object[1][7];
    private static Object[][] a8 = new Object[1][8];
    private static int a1index;
    private static int a2index;
    private static int a3index;
    private static int a4index;
    private static int a5index;
    private static int a6index;
    private static int a7index;
    private static int a8index;
    private static final ArrayList<Event> EventList = new ArrayList<>();
    private static final HashMap<String, Event> EventMap = new HashMap<>();
    private static final ArrayList<LuaEventManager.QueuedEvent> QueuedEvents = new ArrayList<>();

    private static boolean IsMainThread() {
        return LuaManager.thread.debugOwnerThread == Thread.currentThread();
    }

    private static void AddQueuedEvent(LuaEventManager.QueuedEvent qe) {
        synchronized (QueuedEvents) {
            QueuedEvents.add(qe);
        }
    }

    private static void QueueEvent(Event e) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        AddQueuedEvent(qe);
    }

    private static void QueueEvent(Event e, Object p1) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        qe.a.add(p1);
        AddQueuedEvent(qe);
    }

    private static void QueueEvent(Event e, Object p1, Object p2) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        qe.a.add(p1);
        qe.a.add(p2);
        AddQueuedEvent(qe);
    }

    private static void QueueEvent(Event e, Object p1, Object p2, Object p3) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        qe.a.add(p1);
        qe.a.add(p2);
        qe.a.add(p3);
        AddQueuedEvent(qe);
    }

    private static void QueueEvent(Event e, Object p1, Object p2, Object p3, Object p4) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        qe.a.add(p1);
        qe.a.add(p2);
        qe.a.add(p3);
        qe.a.add(p4);
        AddQueuedEvent(qe);
    }

    private static void QueueEvent(Event e, Object p1, Object p2, Object p3, Object p4, Object p5) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        qe.a.add(p1);
        qe.a.add(p2);
        qe.a.add(p3);
        qe.a.add(p4);
        qe.a.add(p5);
        AddQueuedEvent(qe);
    }

    private static void QueueEvent(Event e, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        qe.a.add(p1);
        qe.a.add(p2);
        qe.a.add(p3);
        qe.a.add(p4);
        qe.a.add(p5);
        qe.a.add(p6);
        AddQueuedEvent(qe);
    }

    private static void QueueEvent(Event e, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        qe.a.add(p1);
        qe.a.add(p2);
        qe.a.add(p3);
        qe.a.add(p4);
        qe.a.add(p5);
        qe.a.add(p6);
        qe.a.add(p7);
        AddQueuedEvent(qe);
    }

    private static void QueueEvent(Event e, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        LuaEventManager.QueuedEvent qe = LuaEventManager.QueuedEvent.EventPool.alloc();
        qe.e = e;
        qe.a.add(p1);
        qe.a.add(p2);
        qe.a.add(p3);
        qe.a.add(p4);
        qe.a.add(p5);
        qe.a.add(p6);
        qe.a.add(p7);
        qe.a.add(p8);
        AddQueuedEvent(qe);
    }

    public static void RunQueuedEvents() {
        synchronized (QueuedEvents) {
            RunQueuedEventsInternal();
        }
    }

    private static void RunQueuedEventsInternal() {
        for (int i = 0; i < QueuedEvents.size(); i++) {
            LuaEventManager.QueuedEvent qe = QueuedEvents.get(i);
            switch (qe.a.size()) {
                case 0:
                    qe.e.trigger(LuaManager.env, LuaManager.caller, null);
                    break;
                case 1:
                    RunQueuedEvent(qe, a1index, a1);
                    break;
                case 2:
                    RunQueuedEvent(qe, a2index, a2);
                    break;
                case 3:
                    RunQueuedEvent(qe, a3index, a3);
                    break;
                case 4:
                    RunQueuedEvent(qe, a4index, a4);
                    break;
                case 5:
                    RunQueuedEvent(qe, a5index, a5);
                    break;
                case 6:
                    RunQueuedEvent(qe, a6index, a6);
                    break;
                case 7:
                    RunQueuedEvent(qe, a7index, a7);
                    break;
                case 8:
                    RunQueuedEvent(qe, a8index, a8);
            }

            LuaEventManager.QueuedEvent.EventPool.release(qe);
        }

        QueuedEvents.clear();
    }

    // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
    // Please report this to the Zomboid Decompiler issue tracker at https://github.com/demiurgeQuantified/ZomboidDecompiler/issues with the file name and game version.
    private static void RunQueuedEvent(LuaEventManager.QueuedEvent qe, int index, Object[][] ax) {
        if (index == ax.length) {
            ax = Arrays.copyOf(ax, ax.length * 2);

            for (int n = index; n < ax.length; n++) {
                ax[n] = new Object[3];
            }
        }

        Object[] a = ax[index];

        for (int i = 0; i < qe.a.size(); i++) {
            a[i] = qe.a.get(i);
        }

        index++;
        boolean var8 = false /* VF: Semaphore variable */;

        try {
            var8 = true;
            qe.e.trigger(LuaManager.env, LuaManager.caller, a);
            var8 = false;
        } finally {
            if (var8) {
                index--;

                for (int i = 0; i < qe.a.size(); i++) {
                    a[i] = null;
                }

                qe.e = null;
                qe.a.clear();
            }
        }

        index--;

        for (int i = 0; i < qe.a.size(); i++) {
            a[i] = null;
        }

        qe.e = null;
        qe.a.clear();
    }

    private static Event checkEvent(String event) {
        Event e = EventMap.get(event);
        if (e == null) {
            DebugLog.Lua.println("LuaEventManager: adding unknown event \"" + event + "\"");
            e = AddEvent(event);
        }

        return e.callbacks.isEmpty() ? null : e;
    }

    public static void triggerEvent(String event) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e);
                } else {
                    e.trigger(LuaManager.env, LuaManager.caller, null);
                }
            }
        }
    }

    public static void triggerEvent(String event, Object param1) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e, param1);
                } else {
                    if (a1index == a1.length) {
                        a1 = Arrays.copyOf(a1, a1.length * 2);

                        for (int n = a1index; n < a1.length; n++) {
                            a1[n] = new Object[1];
                        }
                    }

                    Object[] a = a1[a1index];
                    a[0] = param1;
                    a1index++;

                    try {
                        e.trigger(LuaManager.env, LuaManager.caller, a);
                    } finally {
                        a1index--;
                        a[0] = null;
                    }
                }
            }
        }
    }

    public static void triggerEventGarbage(String event, Object param1) {
        triggerEvent(event, param1);
    }

    public static void triggerEventUnique(String event, Object param1) {
        triggerEvent(event, param1);
    }

    public static void triggerEvent(String event, Object param1, Object param2) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e, param1, param2);
                } else {
                    if (a2index == a2.length) {
                        a2 = Arrays.copyOf(a2, a2.length * 2);

                        for (int n = a2index; n < a2.length; n++) {
                            a2[n] = new Object[2];
                        }
                    }

                    Object[] a = a2[a2index];
                    a[0] = param1;
                    a[1] = param2;
                    a2index++;

                    try {
                        e.trigger(LuaManager.env, LuaManager.caller, a);
                    } finally {
                        a2index--;
                        a[0] = null;
                        a[1] = null;
                    }
                }
            }
        }
    }

    public static void triggerEventGarbage(String event, Object param1, Object param2) {
        triggerEvent(event, param1, param2);
    }

    public static void triggerEvent(String event, Object param1, Object param2, Object param3) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e, param1, param2, param3);
                } else {
                    if (a3index == a3.length) {
                        a3 = Arrays.copyOf(a3, a3.length * 2);

                        for (int n = a3index; n < a3.length; n++) {
                            a3[n] = new Object[3];
                        }
                    }

                    Object[] a = a3[a3index];
                    a[0] = param1;
                    a[1] = param2;
                    a[2] = param3;
                    a3index++;

                    try {
                        e.trigger(LuaManager.env, LuaManager.caller, a);
                    } finally {
                        a3index--;
                        a[0] = null;
                        a[1] = null;
                        a[2] = null;
                    }
                }
            }
        }
    }

    public static void triggerEventGarbage(String event, Object param1, Object param2, Object param3) {
        triggerEvent(event, param1, param2, param3);
    }

    public static void triggerEvent(String event, Object param1, Object param2, Object param3, Object param4) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e, param1, param2, param3, param4);
                } else {
                    if (a4index == a4.length) {
                        a4 = Arrays.copyOf(a4, a4.length * 2);

                        for (int n = a4index; n < a4.length; n++) {
                            a4[n] = new Object[4];
                        }
                    }

                    Object[] a = a4[a4index];
                    a[0] = param1;
                    a[1] = param2;
                    a[2] = param3;
                    a[3] = param4;
                    a4index++;

                    try {
                        e.trigger(LuaManager.env, LuaManager.caller, a);
                    } finally {
                        a4index--;
                        a[0] = null;
                        a[1] = null;
                        a[2] = null;
                        a[3] = null;
                    }
                }
            }
        }
    }

    public static void triggerEventGarbage(String event, Object param1, Object param2, Object param3, Object param4) {
        triggerEvent(event, param1, param2, param3, param4);
    }

    public static void triggerEvent(String event, Object param1, Object param2, Object param3, Object param4, Object param5) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e, param1, param2, param3, param4, param5);
                } else {
                    if (a5index == a5.length) {
                        a5 = Arrays.copyOf(a5, a5.length * 2);

                        for (int n = a5index; n < a5.length; n++) {
                            a5[n] = new Object[5];
                        }
                    }

                    Object[] a = a5[a5index];
                    a[0] = param1;
                    a[1] = param2;
                    a[2] = param3;
                    a[3] = param4;
                    a[4] = param5;
                    a5index++;

                    try {
                        e.trigger(LuaManager.env, LuaManager.caller, a);
                    } finally {
                        a5index--;
                        a[0] = null;
                        a[1] = null;
                        a[2] = null;
                        a[3] = null;
                        a[4] = null;
                    }
                }
            }
        }
    }

    public static void triggerEvent(String event, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e, param1, param2, param3, param4, param5, param6);
                } else {
                    if (a6index == a6.length) {
                        a6 = Arrays.copyOf(a6, a6.length * 2);

                        for (int n = a6index; n < a6.length; n++) {
                            a6[n] = new Object[6];
                        }
                    }

                    Object[] a = a6[a6index];
                    a[0] = param1;
                    a[1] = param2;
                    a[2] = param3;
                    a[3] = param4;
                    a[4] = param5;
                    a[5] = param6;
                    a6index++;

                    try {
                        e.trigger(LuaManager.env, LuaManager.caller, a);
                    } finally {
                        a6index--;
                        a[0] = null;
                        a[1] = null;
                        a[2] = null;
                        a[3] = null;
                        a[4] = null;
                        a[5] = null;
                    }
                }
            }
        }
    }

    public static void triggerEvent(String event, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6, Object param7) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e, param1, param2, param3, param4, param5, param6, param7);
                } else {
                    if (a7index == a7.length) {
                        a7 = Arrays.copyOf(a7, a7.length * 2);

                        for (int n = a7index; n < a7.length; n++) {
                            a7[n] = new Object[7];
                        }
                    }

                    Object[] a = a7[a7index];
                    a[0] = param1;
                    a[1] = param2;
                    a[2] = param3;
                    a[3] = param4;
                    a[4] = param5;
                    a[5] = param6;
                    a[6] = param7;
                    a7index++;

                    try {
                        e.trigger(LuaManager.env, LuaManager.caller, a);
                    } finally {
                        a7index--;
                        a[0] = null;
                        a[1] = null;
                        a[2] = null;
                        a[3] = null;
                        a[4] = null;
                        a[5] = null;
                        a[6] = null;
                    }
                }
            }
        }
    }

    public static void triggerEvent(
        String event, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6, Object param7, Object param8
    ) {
        synchronized (EventMap) {
            Event e = checkEvent(event);
            if (e != null) {
                if (!IsMainThread()) {
                    QueueEvent(e, param1, param2, param3, param4, param5, param6, param7, param8);
                } else {
                    if (a8index == a8.length) {
                        a8 = Arrays.copyOf(a8, a8.length * 2);

                        for (int n = a8index; n < a8.length; n++) {
                            a8[n] = new Object[8];
                        }
                    }

                    Object[] a = a8[a8index];
                    a[0] = param1;
                    a[1] = param2;
                    a[2] = param3;
                    a[3] = param4;
                    a[4] = param5;
                    a[5] = param6;
                    a[6] = param7;
                    a[7] = param8;
                    a8index++;

                    try {
                        e.trigger(LuaManager.env, LuaManager.caller, a);
                    } finally {
                        a8index--;
                        a[0] = null;
                        a[1] = null;
                        a[2] = null;
                        a[3] = null;
                        a[4] = null;
                        a[5] = null;
                        a[6] = null;
                        a[7] = null;
                    }
                }
            }
        }
    }

    public static Event AddEvent(String name) {
        Event event = EventMap.get(name);
        if (event != null) {
            return event;
        } else {
            event = new Event(name, EventList.size());
            EventList.add(event);
            EventMap.put(name, event);
            if (LuaManager.env.rawget("Events") instanceof KahluaTable table) {
                event.register(LuaManager.platform, table);
            } else {
                DebugLog.Lua.error("ERROR: 'Events' table not found or not a table");
            }

            return event;
        }
    }

    private static void AddEvents() {
        AddEvent("OnGameBoot");
        AddEvent("OnPreGameStart");
        AddEvent("OnTick");
        AddEvent("OnTickEvenPaused");
        AddEvent("OnRenderUpdate");
        AddEvent("OnFETick");
        AddEvent("OnGameStart");
        AddEvent("OnPreUIDraw");
        AddEvent("OnPostUIDraw");
        AddEvent("OnCharacterCollide");
        AddEvent("OnKeyStartPressed");
        AddEvent("OnKeyPressed");
        AddEvent("OnContextKey");
        AddEvent("OnObjectCollide");
        AddEvent("OnNPCSurvivorUpdate");
        AddEvent("OnPlayerUpdate");
        AddEvent("OnZombieUpdate");
        AddEvent("OnZombieCreate");
        AddEvent("OnTriggerNPCEvent");
        AddEvent("OnMultiTriggerNPCEvent");
        AddEvent("OnLoadMapZones");
        AddEvent("OnLoadedMapZones");
        AddEvent("OnAddBuilding");
        AddEvent("OnCreateLivingCharacter");
        AddEvent("OnChallengeQuery");
        AddEvent("OnClickedAnimalForContext");
        AddEvent("OnFillInventoryObjectContextMenu");
        AddEvent("OnPreFillInventoryObjectContextMenu");
        AddEvent("OnFillWorldObjectContextMenu");
        AddEvent("OnPreFillWorldObjectContextMenu");
        AddEvent("OnRefreshInventoryWindowContainers");
        AddEvent("OnGamepadConnect");
        AddEvent("OnGamepadDisconnect");
        AddEvent("OnJoypadActivate");
        AddEvent("OnJoypadActivateUI");
        AddEvent("OnJoypadBeforeDeactivate");
        AddEvent("OnJoypadDeactivate");
        AddEvent("OnJoypadBeforeReactivate");
        AddEvent("OnJoypadReactivate");
        AddEvent("OnJoypadRenderUI");
        AddEvent("OnMakeItem");
        AddEvent("OnWeaponHitCharacter");
        AddEvent("OnWeaponSwing");
        AddEvent("OnWeaponHitTree");
        AddEvent("OnWeaponHitXp");
        AddEvent("OnWeaponSwingHitPoint");
        AddEvent("OnPlayerAttackFinished");
        AddEvent("OnLoginState");
        AddEvent("OnLoginStateSuccess");
        AddEvent("OnCharacterCreateStats");
        AddEvent("OnLoadSoundBanks");
        AddEvent("OnObjectLeftMouseButtonDown");
        AddEvent("OnObjectLeftMouseButtonUp");
        AddEvent("OnObjectRightMouseButtonDown");
        AddEvent("OnObjectRightMouseButtonUp");
        AddEvent("OnDoTileBuilding");
        AddEvent("OnDoTileBuilding2");
        AddEvent("OnDoTileBuilding3");
        AddEvent("RenderOpaqueObjectsInWorld");
        AddEvent("OnConnectFailed");
        AddEvent("OnConnected");
        AddEvent("OnDisconnect");
        AddEvent("OnConnectionStateChanged");
        AddEvent("OnQRReceived");
        AddEvent("OnGoogleAuthRequest");
        AddEvent("OnScoreboardUpdate");
        AddEvent("OnMouseMove");
        AddEvent("OnMouseDown");
        AddEvent("OnMouseUp");
        AddEvent("OnRightMouseDown");
        AddEvent("OnRightMouseUp");
        AddEvent("OnMouseWheel");
        AddEvent("OnNewSurvivorGroup");
        AddEvent("OnPlayerSetSafehouse");
        AddEvent("OnLoad");
        AddEvent("AddXP");
        AddEvent("LevelPerk");
        AddEvent("OnSave");
        AddEvent("OnMainMenuEnter");
        AddEvent("OnGameStateEnter");
        AddEvent("OnPreMapLoad");
        AddEvent("OnPostFloorSquareDraw");
        AddEvent("OnPostFloorLayerDraw");
        AddEvent("OnPostTilesSquareDraw");
        AddEvent("OnPostTileDraw");
        AddEvent("OnPostWallSquareDraw");
        AddEvent("OnPostCharactersSquareDraw");
        AddEvent("OnCreateUI");
        AddEvent("OnMapLoadCreateIsoObject");
        AddEvent("OnCreateSurvivor");
        AddEvent("OnCreatePlayer");
        AddEvent("OnPlayerDeath");
        AddEvent("OnZombieDead");
        AddEvent("OnCharacterDeath");
        AddEvent("OnCharacterMeet");
        AddEvent("OnSpawnRegionsLoaded");
        AddEvent("OnPostMapLoad");
        AddEvent("OnAIStateExecute");
        AddEvent("OnAIStateEnter");
        AddEvent("OnAIStateExit");
        AddEvent("OnAIStateChange");
        AddEvent("OnPlayerMove");
        AddEvent("OnInitWorld");
        AddEvent("OnNewGame");
        AddEvent("OnIsoThumpableLoad");
        AddEvent("OnIsoThumpableSave");
        AddEvent("ReuseGridsquare");
        AddEvent("LoadGridsquare");
        AddEvent("LoadChunk");
        AddEvent("EveryOneMinute");
        AddEvent("EveryTenMinutes");
        AddEvent("EveryDays");
        AddEvent("EveryHours");
        AddEvent("OnDusk");
        AddEvent("OnDawn");
        AddEvent("OnEquipPrimary");
        AddEvent("OnEquipSecondary");
        AddEvent("OnClothingUpdated");
        AddEvent("OnWeatherPeriodStart");
        AddEvent("OnWeatherPeriodStage");
        AddEvent("OnWeatherPeriodComplete");
        AddEvent("OnWeatherPeriodStop");
        AddEvent("OnRainStart");
        AddEvent("OnRainStop");
        AddEvent("OnAmbientSound");
        AddEvent("OnWorldSound");
        AddEvent("OnResetLua");
        AddEvent("OnModsModified");
        AddEvent("OnSeeNewRoom");
        AddEvent("OnNewFire");
        AddEvent("OnFillContainer");
        AddEvent("OnChangeWeather");
        AddEvent("OnRenderTick");
        AddEvent("OnDestroyIsoThumpable");
        AddEvent("OnPostSave");
        AddEvent("OnResolutionChange");
        AddEvent("OnWaterAmountChange");
        AddEvent("OnClientCommand");
        AddEvent("OnServerCommand");
        AddEvent("OnProcessTransaction");
        AddEvent("OnProcessAction");
        AddEvent("OnContainerUpdate");
        AddEvent("OnObjectAdded");
        AddEvent("OnObjectAboutToBeRemoved");
        AddEvent("onLoadModDataFromServer");
        AddEvent("OnGameTimeLoaded");
        AddEvent("OnCGlobalObjectSystemInit");
        AddEvent("OnSGlobalObjectSystemInit");
        AddEvent("OnWorldMessage");
        AddEvent("OnKeyKeepPressed");
        AddEvent("SendCustomModData");
        AddEvent("ServerPinged");
        AddEvent("OnServerStarted");
        AddEvent("OnLoadedTileDefinitions");
        AddEvent("OnPostRender");
        AddEvent("DoSpecialTooltip");
        AddEvent("OnCoopJoinFailed");
        AddEvent("OnServerWorkshopItems");
        AddEvent("OnVehicleDamageTexture");
        AddEvent("OnCustomUIKey");
        AddEvent("OnCustomUIKeyPressed");
        AddEvent("OnCustomUIKeyReleased");
        AddEvent("OnDeviceText");
        AddEvent("OnRadioInteraction");
        AddEvent("OnLoadRadioScripts");
        AddEvent("OnAcceptInvite");
        AddEvent("OnCoopServerMessage");
        AddEvent("OnReceiveUserlog");
        AddEvent("OnAdminMessage");
        AddEvent("ReceiveFactionInvite");
        AddEvent("AcceptedFactionInvite");
        AddEvent("ReceiveSafehouseInvite");
        AddEvent("AcceptedSafehouseInvite");
        AddEvent("ViewTickets");
        AddEvent("ViewBannedIPs");
        AddEvent("ViewBannedSteamIDs");
        AddEvent("SyncFaction");
        AddEvent("RefreshCheats");
        AddEvent("OnReceiveItemListNet");
        AddEvent("OnMiniScoreboardUpdate");
        AddEvent("OnSafehousesChanged");
        AddEvent("OnWarUpdate");
        AddEvent("RequestTrade");
        AddEvent("AcceptedTrade");
        AddEvent("TradingUIAddItem");
        AddEvent("TradingUIRemoveItem");
        AddEvent("TradingUIUpdateState");
        AddEvent("OnGridBurnt");
        AddEvent("OnPreDistributionMerge");
        AddEvent("OnDistributionMerge");
        AddEvent("OnPostDistributionMerge");
        AddEvent("MngInvReceiveItems");
        AddEvent("OnTileRemoved");
        AddEvent("OnServerStartSaving");
        AddEvent("OnServerFinishSaving");
        AddEvent("OnMechanicActionDone");
        AddEvent("OnClimateTick");
        AddEvent("OnThunderEvent");
        AddEvent("OnEnterVehicle");
        AddEvent("OnSteamGameJoin");
        AddEvent("OnTabAdded");
        AddEvent("OnSetDefaultTab");
        AddEvent("OnTabRemoved");
        AddEvent("OnAddMessage");
        AddEvent("SwitchChatStream");
        AddEvent("OnChatWindowInit");
        AddEvent("OnAlertMessage");
        AddEvent("OnInitSeasons");
        AddEvent("OnClimateTickDebug");
        AddEvent("OnInitModdedWeatherStage");
        AddEvent("OnUpdateModdedWeatherStage");
        AddEvent("OnClimateManagerInit");
        AddEvent("OnPressReloadButton");
        AddEvent("OnPressRackButton");
        AddEvent("OnPressWalkTo");
        AddEvent("OnHitZombie");
        AddEvent("OnBeingHitByZombie");
        AddEvent("OnServerStatisticReceived");
        AddEvent("OnDynamicMovableRecipe");
        AddEvent("OnInitGlobalModData");
        AddEvent("OnReceiveGlobalModData");
        AddEvent("OnInitRecordedMedia");
        AddEvent("onUpdateIcon");
        AddEvent("preAddForageDefs");
        AddEvent("preAddSkillDefs");
        AddEvent("preAddZoneDefs");
        AddEvent("preAddCatDefs");
        AddEvent("preAddItemDefs");
        AddEvent("onAddForageDefs");
        AddEvent("onFillSearchIconContextMenu");
        AddEvent("onItemFall");
        AddEvent("OnTemplateTextInit");
        AddEvent("OnPlayerGetDamage");
        AddEvent("OnWeaponHitThumpable");
        AddEvent("OnFishingActionMPUpdate");
        AddEvent("OnThrowableExplode");
        AddEvent("OnSourceWindowFileReload");
        AddEvent("OnSpawnVehicleStart");
        AddEvent("OnSpawnVehicleEnd");
        AddEvent("OnMovingObjectCrop");
        AddEvent("OnOverrideSearchManager");
        AddEvent("OnSleepingTick");
        AddEvent("OnRolesReceived");
        AddEvent("OnNetworkUsersReceived");
        AddEvent("OnServerCustomizationDataReceived");
        AddEvent("OnDeadBodySpawn");
        AddEvent("OnAnimalTracks");
        AddEvent("OnItemFound");
        AddEvent("SetDragItem");
        AddEvent("OnSteamServerResponded");
        AddEvent("OnSteamServerResponded2");
        AddEvent("OnSteamServerFailedToRespond2");
        AddEvent("OnSteamRulesRefreshComplete");
        AddEvent("OnSteamRefreshInternetServers");
    }

    public static void clear() {
    }

    public static void register(Platform platform, KahluaTable environment) {
        KahluaTable table = platform.newTable();
        environment.rawset("Events", table);
        AddEvents();
    }

    public static void reroute(Prototype prototype, LuaClosure luaClosure) {
        for (int n = 0; n < EventList.size(); n++) {
            Event e = EventList.get(n);

            for (int m = 0; m < e.callbacks.size(); m++) {
                LuaClosure c = e.callbacks.get(m);
                if (c.prototype.filename.equals(prototype.filename) && c.prototype.name.equals(prototype.name)) {
                    e.callbacks.set(m, luaClosure);
                }
            }
        }
    }

    public static void Reset() {
        for (int n = 0; n < EventList.size(); n++) {
            Event e = EventList.get(n);
            e.callbacks.clear();
        }

        EventList.clear();
        EventMap.clear();
    }

    public static void getEvents(ArrayList<Event> eventList, HashMap<String, Event> eventMap) {
        eventList.clear();
        eventList.addAll(EventList);
        eventMap.clear();
        eventMap.putAll(EventMap);
    }

    public static void setEvents(ArrayList<Event> eventList, HashMap<String, Event> eventMap) {
        EventList.clear();
        EventList.addAll(eventList);
        EventMap.clear();
        EventMap.putAll(eventMap);
    }

    public static void ResetCallbacks() {
        for (int n = 0; n < EventList.size(); n++) {
            Event e = EventList.get(n);
            e.callbacks.clear();
        }
    }

    /**
     * Description copied from interface: se.krka.kahlua.vm.JavaFunction
     * @return N, number of return values. The top N objects on the stack are considered the return values.
     */
    @Override
    public int call(LuaCallFrame callFrame, int nArguments) {
        return 0;
    }

    private int OnTick(LuaCallFrame callFrame, int nArguments) {
        return 0;
    }

    public static class QueuedEvent extends PooledObject {
        public static final Pool<LuaEventManager.QueuedEvent> EventPool = new Pool<>(LuaEventManager.QueuedEvent::new);
        public Event e;
        public final ArrayList<Object> a = new ArrayList<>();
    }
}
