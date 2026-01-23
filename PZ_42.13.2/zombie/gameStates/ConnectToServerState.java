// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.znet.ISteamWorkshopCallback;
import zombie.core.znet.SteamUGCDetails;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.core.znet.SteamWorkshopItem;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.erosion.ErosionConfig;
import zombie.globalObjects.CGlobalObjects;
import zombie.iso.IsoChunkMap;
import zombie.network.ConnectionManager;
import zombie.network.CoopMaster;
import zombie.network.GameClient;
import zombie.network.ServerOptions;
import zombie.savefile.ClientPlayerDB;
import zombie.world.WorldDictionary;

public final class ConnectToServerState extends GameState {
    public static ConnectToServerState instance;
    private final ByteBuffer connectionDetails;
    private ConnectToServerState.State state;
    private final ArrayList<ConnectToServerState.WorkshopItem> workshopItems = new ArrayList<>();
    private final ArrayList<ConnectToServerState.WorkshopItem> confirmItems = new ArrayList<>();
    private ConnectToServerState.ItemQuery query;

    private static void noise(String s) {
        DebugLog.log("ConnectToServerState: " + s);
    }

    public ConnectToServerState(ByteBuffer bb) {
        this.connectionDetails = ByteBuffer.allocate(bb.capacity());
        this.connectionDetails.put(bb);
        this.connectionDetails.rewind();
    }

    @Override
    public void enter() {
        instance = this;
        ConnectionManager.log("connect-state", ConnectToServerState.State.Start.name().toLowerCase(), GameClient.connection);
        this.state = ConnectToServerState.State.Start;
    }

    @Override
    public GameStateMachine.StateAction update() {
        try {
            switch (this.state) {
                case Start:
                    this.Start();
                    break;
                case TestTCP:
                    this.TestTCP();
                    break;
                case WorkshopInit:
                    this.WorkshopInit();
                    break;
                case WorkshopQuery:
                    this.WorkshopQuery();
                    break;
                case WorkshopConfirm:
                    this.WorkshopConfirm();
                    break;
                case ServerWorkshopItemScreen:
                    this.ServerWorkshopItemScreen();
                    break;
                case WorkshopUpdate:
                    this.WorkshopUpdate();
                    break;
                case CheckMods:
                    this.CheckMods();
                    break;
                case Finish:
                    this.Finish();
                    break;
                case Exit:
                    return GameStateMachine.StateAction.Continue;
            }

            return GameStateMachine.StateAction.Remain;
        } catch (Exception var2) {
            DebugLog.General.printStackTrace();
            return GameStateMachine.StateAction.Continue;
        }
    }

    private void receiveStartLocation(ByteBuffer bb) {
        LuaEventManager.triggerEvent("OnConnectionStateChanged", "Connected");
        IsoChunkMap.mpWorldXa = bb.getInt();
        IsoChunkMap.mpWorldYa = bb.getInt();
        IsoChunkMap.mpWorldZa = bb.getInt();
        GameClient.username = GameClient.username.trim();
        Core.getInstance().setGameMode("Multiplayer");
        LuaManager.GlobalObject.createWorld(Core.gameSaveWorld);
        GameClient.instance.connected = true;
    }

    private void receiveServerOptions(ByteBuffer bb) throws IOException {
        int optionsSize = bb.getInt();

        for (int i = 0; i < optionsSize; i++) {
            String option = GameWindow.ReadString(bb);
            String value = GameWindow.ReadString(bb);
            ServerOptions.instance.putOption(option, value);
        }

        Core.getInstance().ResetLua("client", "ConnectedToServer");
        Core.getInstance().setGameMode("Multiplayer");
        GameClient.connection.ip = GameClient.ip;
    }

    private void receiveSandboxOptions(ByteBuffer bb) throws IOException {
        SandboxOptions.instance.load(bb);
        SandboxOptions.instance.applySettings();
        SandboxOptions.instance.toLua();
    }

    private void receiveGameTime(ByteBuffer bb) throws IOException {
        GameTime.getInstance().load(bb);
        GameTime.getInstance().save();
    }

    private void receiveErosionMain(ByteBuffer bb) {
        GameClient.instance.erosionConfig = new ErosionConfig();
        GameClient.instance.erosionConfig.load(bb);
    }

    private void receiveGlobalObjects(ByteBuffer bb) throws IOException {
        CGlobalObjects.loadInitialState(bb);
    }

    private void receiveResetID(ByteBuffer bb) {
        int ResetID = bb.getInt();
        GameClient.instance.setResetID(ResetID);
    }

    private void receiveBerries(ByteBuffer bb) {
        Core.getInstance().setPoisonousBerry(GameWindow.ReadString(bb));
        GameClient.poisonousBerry = Core.getInstance().getPoisonousBerry();
        Core.getInstance().setPoisonousMushroom(GameWindow.ReadString(bb));
        GameClient.poisonousMushroom = Core.getInstance().getPoisonousMushroom();
    }

    private void receiveWorldDictionary(ByteBuffer bb) throws IOException {
        WorldDictionary.loadDataFromServer(bb);
        ClientPlayerDB.setAllow(true);
        LuaEventManager.triggerEvent("OnConnected");
    }

    private void Start() {
        noise("Start");
        ByteBuffer bb = this.connectionDetails;
        GameClient.connection.isCoopHost = bb.get() == 1;
        GameClient.connection.maxPlayers = bb.getInt();
        if (bb.get() == 1) {
            long hostSteamID = bb.getLong();
            String serverName = GameWindow.ReadStringUTF(bb);
            Core.gameSaveWorld = hostSteamID + "_" + serverName + "_player";
        }

        GameClient.instance.id = bb.get();
        ConnectionManager.log(
            "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.TestTCP.name().toLowerCase(), GameClient.connection
        );
        this.state = ConnectToServerState.State.TestTCP;
    }

    private void TestTCP() {
        noise("TestTCP");
        ByteBuffer bb = this.connectionDetails;
        GameClient.connection.role = new Role("");
        GameClient.connection.role.parse(bb);
        if (Core.debug && !GameClient.connection.role.hasCapability(Capability.ConnectWithDebug) && !CoopMaster.instance.isRunning()) {
            LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_DebugNotAllowed"));
            GameClient.connection.forceDisconnect("connect-debug-used");
            ConnectionManager.log(
                "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.Exit;
        } else {
            GameClient.gameMap = GameWindow.ReadStringUTF(bb);
            if (GameClient.gameMap.contains(";")) {
                String[] ss = GameClient.gameMap.split(";");
                Core.gameMap = ss[0].trim();
            } else {
                Core.gameMap = GameClient.gameMap.trim();
            }

            if (SteamUtils.isSteamModeEnabled()) {
                ConnectionManager.log(
                    "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopInit.name().toLowerCase(), GameClient.connection
                );
                this.state = ConnectToServerState.State.WorkshopInit;
            } else {
                ConnectionManager.log(
                    "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.CheckMods.name().toLowerCase(), GameClient.connection
                );
                this.state = ConnectToServerState.State.CheckMods;
            }
        }
    }

    private void WorkshopInit() {
        ByteBuffer bb = this.connectionDetails;
        int count = bb.getShort();

        for (int i = 0; i < count; i++) {
            long itemID = bb.getLong();
            long timeStamp = bb.getLong();
            ConnectToServerState.WorkshopItem item = new ConnectToServerState.WorkshopItem(itemID, timeStamp);
            this.workshopItems.add(item);
        }

        if (this.workshopItems.isEmpty()) {
            ConnectionManager.log(
                "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopUpdate.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.WorkshopUpdate;
        } else {
            long[] itemIDs = new long[this.workshopItems.size()];

            for (int i = 0; i < this.workshopItems.size(); i++) {
                ConnectToServerState.WorkshopItem item = this.workshopItems.get(i);
                itemIDs[i] = item.id;
            }

            this.query = new ConnectToServerState.ItemQuery();
            this.query.handle = SteamWorkshop.instance.CreateQueryUGCDetailsRequest(itemIDs, this.query);
            if (this.query.handle != 0L) {
                ConnectionManager.log(
                    "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopQuery.name().toLowerCase(), GameClient.connection
                );
                this.state = ConnectToServerState.State.WorkshopQuery;
            } else {
                this.query = null;
                LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_CreateQueryUGCDetailsRequest"));
                GameClient.connection.forceDisconnect("connect-workshop-query");
                ConnectionManager.log(
                    "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), GameClient.connection
                );
                this.state = ConnectToServerState.State.Exit;
            }
        }
    }

    private void WorkshopConfirm() {
        this.confirmItems.clear();

        for (int i = 0; i < this.workshopItems.size(); i++) {
            ConnectToServerState.WorkshopItem item = this.workshopItems.get(i);
            long itemState = SteamWorkshop.instance.GetItemState(item.id);
            noise("WorkshopConfirm GetItemState()=" + SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + item.id);
            if (SteamWorkshopItem.ItemState.Installed.and(itemState)
                && SteamWorkshopItem.ItemState.NeedsUpdate.not(itemState)
                && item.details != null
                && item.details.getTimeCreated() != 0L
                && item.details.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(item.id)) {
                noise("Installed status but timeUpdated doesn't match!!!");
                itemState |= SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
            }

            if (itemState != (SteamWorkshopItem.ItemState.Subscribed.getValue() | SteamWorkshopItem.ItemState.Installed.getValue())) {
                this.confirmItems.add(item);
            }
        }

        if (this.confirmItems.isEmpty()) {
            this.query = null;
            ConnectionManager.log(
                "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopUpdate.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.WorkshopUpdate;
        } else if (this.query == null) {
            ConnectionManager.log(
                "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopUpdate.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.WorkshopUpdate;
        } else {
            assert this.query.isCompleted();

            ArrayList<String> itemIDstr = new ArrayList<>();

            for (int i = 0; i < this.workshopItems.size(); i++) {
                ConnectToServerState.WorkshopItem itemx = this.workshopItems.get(i);
                itemIDstr.add(SteamUtils.convertSteamIDToString(itemx.id));
            }

            LuaEventManager.triggerEvent("OnServerWorkshopItems", "Required", itemIDstr);
            ArrayList<SteamUGCDetails> details = this.query.details;
            this.query = null;
            ConnectionManager.log(
                "connect-state-" + this.state.name().toLowerCase(),
                ConnectToServerState.State.ServerWorkshopItemScreen.name().toLowerCase(),
                GameClient.connection
            );
            this.state = ConnectToServerState.State.ServerWorkshopItemScreen;
            LuaEventManager.triggerEvent("OnServerWorkshopItems", "Details", details);
        }
    }

    private void WorkshopQuery() {
        if (!this.query.isCompleted()) {
            if (this.query.isNotCompleted()) {
                this.query = null;
                ConnectionManager.log(
                    "connect-state-" + this.state.name().toLowerCase(),
                    ConnectToServerState.State.ServerWorkshopItemScreen.name().toLowerCase(),
                    GameClient.connection
                );
                this.state = ConnectToServerState.State.ServerWorkshopItemScreen;
                LuaEventManager.triggerEvent("OnServerWorkshopItems", "Error", "ItemQueryNotCompleted");
            }
        } else {
            for (SteamUGCDetails details : this.query.details) {
                for (ConnectToServerState.WorkshopItem workshopItem : this.workshopItems) {
                    if (workshopItem.id == details.getID()) {
                        workshopItem.details = details;
                        break;
                    }
                }
            }

            ConnectionManager.log(
                "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopConfirm.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.WorkshopConfirm;
        }
    }

    private void ServerWorkshopItemScreen() {
    }

    private void WorkshopUpdate() {
        for (int i = 0; i < this.workshopItems.size(); i++) {
            ConnectToServerState.WorkshopItem item = this.workshopItems.get(i);
            item.update();
            if (item.state == ConnectToServerState.WorkshopItemState.Fail) {
                ConnectionManager.log(
                    "connect-state-" + this.state.name().toLowerCase(),
                    ConnectToServerState.State.ServerWorkshopItemScreen.name().toLowerCase(),
                    GameClient.connection
                );
                this.state = ConnectToServerState.State.ServerWorkshopItemScreen;
                LuaEventManager.triggerEvent("OnServerWorkshopItems", "Error", item.id, item.error);
                return;
            }

            if (item.state != ConnectToServerState.WorkshopItemState.Ready) {
                return;
            }
        }

        ZomboidFileSystem.instance.resetModFolders();
        LuaEventManager.triggerEvent("OnServerWorkshopItems", "Success");
        ConnectionManager.log(
            "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.CheckMods.name().toLowerCase(), GameClient.connection
        );
        this.state = ConnectToServerState.State.CheckMods;
    }

    private void CheckMods() {
        ByteBuffer bb = this.connectionDetails;
        ArrayList<String> mods = new ArrayList<>();
        int modCount = bb.getInt();

        for (int i = 0; i < modCount; i++) {
            String id = GameWindow.ReadStringUTF(bb);
            String url = GameWindow.ReadStringUTF(bb);
            String name = GameWindow.ReadStringUTF(bb);
            mods.add(id);
        }

        GameClient.instance.serverMods.clear();
        GameClient.instance.serverMods.addAll(mods);
        mods.clear();
        String missingMod = ZomboidFileSystem.instance.loadModsAux(GameClient.instance.serverMods, mods);
        if (missingMod != null) {
            String error = Translator.getText("UI_OnConnectFailed_ModRequired", missingMod);
            LuaEventManager.triggerEvent("OnConnectFailed", error);
            GameClient.connection.forceDisconnect("connect-mod-required");
            ConnectionManager.log(
                "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.Exit;
        } else {
            ConnectionManager.log(
                "connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Finish.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.Finish;
        }
    }

    private void Finish() {
        ByteBuffer bb = this.connectionDetails;

        try {
            try {
                this.receiveStartLocation(bb);
            } catch (Exception var11) {
                DebugLog.Multiplayer.printException(var11, "receiveStartLocation error", LogSeverity.Error);
                throw var11;
            }

            try {
                this.receiveServerOptions(bb);
            } catch (IOException var10) {
                DebugLog.Multiplayer.printException(var10, "receiveServerOptions error", LogSeverity.Error);
                throw var10;
            }

            try {
                this.receiveSandboxOptions(bb);
            } catch (IOException var9) {
                DebugLog.Multiplayer.printException(var9, "receiveSandboxOptions error", LogSeverity.Error);
                throw var9;
            }

            try {
                this.receiveGameTime(bb);
            } catch (IOException var8) {
                DebugLog.Multiplayer.printException(var8, "receiveGameTime error", LogSeverity.Error);
                throw var8;
            }

            try {
                this.receiveErosionMain(bb);
            } catch (Exception var7) {
                DebugLog.Multiplayer.printException(var7, "receiveErosionMain error", LogSeverity.Error);
                throw var7;
            }

            try {
                this.receiveGlobalObjects(bb);
            } catch (IOException var6) {
                DebugLog.Multiplayer.printException(var6, "receiveGlobalObjects error", LogSeverity.Error);
                throw var6;
            }

            try {
                this.receiveResetID(bb);
            } catch (Exception var5) {
                DebugLog.Multiplayer.printException(var5, "receiveResetID error", LogSeverity.Error);
                throw var5;
            }

            try {
                this.receiveBerries(bb);
            } catch (Exception var4) {
                DebugLog.Multiplayer.printException(var4, "receiveBerries error", LogSeverity.Error);
                throw var4;
            }

            try {
                this.receiveWorldDictionary(bb);
            } catch (IOException var3) {
                DebugLog.Multiplayer.printException(var3, "receiveWorldDictionary error", LogSeverity.Error);
                throw var3;
            }
        } catch (Exception var12) {
            ExceptionLogger.logException(var12);
            LuaEventManager.triggerEvent("OnConnectFailed", "WorldDictionary error");
            GameClient.connection.forceDisconnect("connection-details-error");
        }

        ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), GameClient.connection);
        this.state = ConnectToServerState.State.Exit;
    }

    public void FromLua(String button) {
        if (this.state != ConnectToServerState.State.ServerWorkshopItemScreen) {
            throw new IllegalStateException("state != ServerWorkshopItemScreen");
        } else if ("install".equals(button)) {
            ConnectionManager.log(
                "connect-state-lua-" + this.state.name().toLowerCase(), ConnectToServerState.State.WorkshopUpdate.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.WorkshopUpdate;
        } else if ("disconnect".equals(button)) {
            LuaEventManager.triggerEvent("OnConnectFailed", "ServerWorkshopItemsCancelled");
            if (GameClient.connection != null) {
                GameClient.connection.forceDisconnect("connect-workshop-canceled");
            }

            ConnectionManager.log(
                "connect-state-lua-" + this.state.name().toLowerCase(), ConnectToServerState.State.Exit.name().toLowerCase(), GameClient.connection
            );
            this.state = ConnectToServerState.State.Exit;
        }
    }

    @Override
    public void exit() {
        instance = null;
    }

    private class ItemQuery implements ISteamWorkshopCallback {
        long handle;
        ArrayList<SteamUGCDetails> details;
        boolean completed;
        boolean notCompleted;

        private ItemQuery() {
            Objects.requireNonNull(ConnectToServerState.this);
            super();
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public boolean isNotCompleted() {
            return this.notCompleted;
        }

        @Override
        public void onItemCreated(long itemID, boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotCreated(int result) {
        }

        @Override
        public void onItemUpdated(boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotUpdated(int result) {
        }

        @Override
        public void onItemSubscribed(long itemID) {
        }

        @Override
        public void onItemNotSubscribed(long itemID, int result) {
        }

        @Override
        public void onItemDownloaded(long itemID) {
        }

        @Override
        public void onItemNotDownloaded(long itemID, int result) {
        }

        @Override
        public void onItemQueryCompleted(long handle, int numResults) {
            ConnectToServerState.noise("onItemQueryCompleted handle=" + handle + " numResult=" + numResults);
            if (handle == this.handle) {
                SteamWorkshop.instance.RemoveCallback(this);
                ArrayList<SteamUGCDetails> detailsList = new ArrayList<>();

                for (int i = 0; i < numResults; i++) {
                    SteamUGCDetails details = SteamWorkshop.instance.GetQueryUGCResult(handle, i);
                    if (details != null) {
                        detailsList.add(details);
                    }
                }

                this.details = detailsList;
                SteamWorkshop.instance.ReleaseQueryUGCRequest(handle);
                this.completed = true;
            }
        }

        @Override
        public void onItemQueryNotCompleted(long handle, int result) {
            ConnectToServerState.noise("onItemQueryNotCompleted handle=" + handle + " result=" + result);
            if (handle == this.handle) {
                SteamWorkshop.instance.RemoveCallback(this);
                SteamWorkshop.instance.ReleaseQueryUGCRequest(handle);
                this.notCompleted = true;
            }
        }
    }

    private static enum State {
        Start,
        TestTCP,
        WorkshopInit,
        WorkshopQuery,
        WorkshopConfirm,
        ServerWorkshopItemScreen,
        WorkshopUpdate,
        CheckMods,
        Finish,
        Exit;
    }

    private static final class WorkshopItem implements ISteamWorkshopCallback {
        long id;
        long serverTimeStamp;
        ConnectToServerState.WorkshopItemState state = ConnectToServerState.WorkshopItemState.CheckItemState;
        boolean subscribed;
        long downloadStartTime;
        long downloadQueryTime;
        String error;
        SteamUGCDetails details;

        WorkshopItem(long id, long serverTimeStamp) {
            this.id = id;
            this.serverTimeStamp = serverTimeStamp;
        }

        void update() {
            switch (this.state) {
                case CheckItemState:
                    this.CheckItemState();
                    break;
                case SubscribePending:
                    this.SubscribePending();
                    break;
                case DownloadPending:
                    this.DownloadPending();
                case Ready:
            }
        }

        void setState(ConnectToServerState.WorkshopItemState newState) {
            ConnectToServerState.noise("item state " + this.state + " -> " + newState + " ID=" + this.id);
            this.state = newState;
        }

        void CheckItemState() {
            long itemState = SteamWorkshop.instance.GetItemState(this.id);
            ConnectToServerState.noise("GetItemState()=" + SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + this.id);
            if (!SteamWorkshopItem.ItemState.Subscribed.and(itemState)) {
                if (SteamWorkshop.instance.SubscribeItem(this.id, this)) {
                    this.setState(ConnectToServerState.WorkshopItemState.SubscribePending);
                } else {
                    this.error = "SubscribeItemFalse";
                    this.setState(ConnectToServerState.WorkshopItemState.Fail);
                }
            } else {
                if (SteamWorkshopItem.ItemState.Installed.and(itemState)
                    && SteamWorkshopItem.ItemState.NeedsUpdate.not(itemState)
                    && this.details != null
                    && this.details.getTimeCreated() != 0L
                    && this.details.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(this.id)) {
                    ConnectToServerState.noise("Installed status but timeUpdated doesn't match!!!");
                    itemState |= SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
                }

                if (SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState)) {
                    if (SteamWorkshop.instance.DownloadItem(this.id, true, this)) {
                        this.setState(ConnectToServerState.WorkshopItemState.DownloadPending);
                        this.downloadStartTime = System.currentTimeMillis();
                    } else {
                        this.error = "DownloadItemFalse";
                        this.setState(ConnectToServerState.WorkshopItemState.Fail);
                    }
                } else if (SteamWorkshopItem.ItemState.Installed.and(itemState)) {
                    long timeStamp = SteamWorkshop.instance.GetItemInstallTimeStamp(this.id);
                    if (timeStamp == 0L) {
                        this.error = "GetItemInstallTimeStamp";
                        this.setState(ConnectToServerState.WorkshopItemState.Fail);
                    } else if (timeStamp != this.serverTimeStamp) {
                        this.error = "VersionMismatch";
                        this.setState(ConnectToServerState.WorkshopItemState.Fail);
                    } else {
                        this.setState(ConnectToServerState.WorkshopItemState.Ready);
                    }
                } else {
                    this.error = "UnknownItemState";
                    this.setState(ConnectToServerState.WorkshopItemState.Fail);
                }
            }
        }

        void SubscribePending() {
            if (this.subscribed) {
                long itemState = SteamWorkshop.instance.GetItemState(this.id);
                if (SteamWorkshopItem.ItemState.Subscribed.and(itemState)) {
                    this.setState(ConnectToServerState.WorkshopItemState.CheckItemState);
                }
            }
        }

        void DownloadPending() {
            long time = System.currentTimeMillis();
            if (this.downloadQueryTime + 100L <= time) {
                this.downloadQueryTime = time;
                long itemState = SteamWorkshop.instance.GetItemState(this.id);
                if (SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState)) {
                    long[] progress = new long[2];
                    if (SteamWorkshop.instance.GetItemDownloadInfo(this.id, progress)) {
                        ConnectToServerState.noise("download " + progress[0] + "/" + progress[1] + " ID=" + this.id);
                        LuaEventManager.triggerEvent(
                            "OnServerWorkshopItems", "Progress", SteamUtils.convertSteamIDToString(this.id), progress[0], Math.max(progress[1], 1L)
                        );
                    }
                }
            }
        }

        @Override
        public void onItemCreated(long itemID, boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotCreated(int result) {
        }

        @Override
        public void onItemUpdated(boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotUpdated(int result) {
        }

        @Override
        public void onItemSubscribed(long itemID) {
            ConnectToServerState.noise("onItemSubscribed itemID=" + itemID);
            if (itemID == this.id) {
                SteamWorkshop.instance.RemoveCallback(this);
                this.subscribed = true;
            }
        }

        @Override
        public void onItemNotSubscribed(long itemID, int result) {
            ConnectToServerState.noise("onItemNotSubscribed itemID=" + itemID + " result=" + result);
            if (itemID == this.id) {
                SteamWorkshop.instance.RemoveCallback(this);
                this.error = "ItemNotSubscribed";
                this.setState(ConnectToServerState.WorkshopItemState.Fail);
            }
        }

        @Override
        public void onItemDownloaded(long itemID) {
            ConnectToServerState.noise("onItemDownloaded itemID=" + itemID + " time=" + (System.currentTimeMillis() - this.downloadStartTime) + " ms");
            if (itemID == this.id) {
                SteamWorkshop.instance.RemoveCallback(this);
                this.setState(ConnectToServerState.WorkshopItemState.CheckItemState);
            }
        }

        @Override
        public void onItemNotDownloaded(long itemID, int result) {
            ConnectToServerState.noise("onItemNotDownloaded itemID=" + itemID + " result=" + result);
            if (itemID == this.id) {
                SteamWorkshop.instance.RemoveCallback(this);
                this.error = "ItemNotDownloaded";
                this.setState(ConnectToServerState.WorkshopItemState.Fail);
            }
        }

        @Override
        public void onItemQueryCompleted(long handle, int numResults) {
        }

        @Override
        public void onItemQueryNotCompleted(long handle, int result) {
        }
    }

    private static enum WorkshopItemState {
        CheckItemState,
        SubscribePending,
        DownloadPending,
        Ready,
        Fail;
    }
}
