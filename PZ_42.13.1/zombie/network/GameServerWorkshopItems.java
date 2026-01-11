// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Objects;
import zombie.ZomboidFileSystem;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.znet.ISteamWorkshopCallback;
import zombie.core.znet.SteamUGCDetails;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.core.znet.SteamWorkshopItem;
import zombie.debug.DebugLog;

public class GameServerWorkshopItems {
    private static void noise(String s) {
        DebugLog.log("Workshop: " + s);
    }

    public static boolean Install(ArrayList<Long> itemIDList) {
        if (!GameServer.server) {
            return false;
        } else if (itemIDList.isEmpty()) {
            return true;
        } else {
            ArrayList<GameServerWorkshopItems.WorkshopItem> workshopItems = new ArrayList<>();
            int[] installTries = new int[itemIDList.size()];

            for (long itemID : itemIDList) {
                GameServerWorkshopItems.WorkshopItem item = new GameServerWorkshopItems.WorkshopItem(itemID);
                workshopItems.add(item);
            }

            if (GameServer.coop) {
                CoopSlave.status("UI_ServerStatus_Requesting_Workshop_Item_Details");
            }

            if (!QueryItemDetails(workshopItems)) {
                return false;
            } else {
                int processedCount = 0;
                boolean[] completed = new boolean[workshopItems.size()];
                if (GameServer.coop) {
                    CoopSlave.instance
                        .sendMessage(
                            "status", null, Translator.getText("UI_ServerStatus_Downloaded_Workshop_Items_Progress", processedCount, workshopItems.size())
                        );
                }

                while (true) {
                    SteamUtils.runLoop();
                    boolean busy = false;

                    for (int i = 0; i < workshopItems.size(); i++) {
                        GameServerWorkshopItems.WorkshopItem item = workshopItems.get(i);
                        item.update();
                        if (item.state == GameServerWorkshopItems.WorkshopInstallState.Fail) {
                            CoopSlave.status(SteamWorkshop.instance.GetItemInstallFolder(item.id));
                            if (++installTries[i] >= 3) {
                                return false;
                            }

                            if (GameServer.coop) {
                                CoopSlave.instance
                                    .sendMessage(
                                        "status",
                                        null,
                                        Translator.getText("UI_ServerStatus_Downloaded_Workshop_Reinstall_Progress", processedCount, workshopItems.size())
                                    );
                            }

                            ZomboidFileSystem.deleteDirectory(SteamWorkshop.instance.GetItemInstallFolder(item.id));
                            item.setState(GameServerWorkshopItems.WorkshopInstallState.CheckItemState);
                        }

                        if (item.state != GameServerWorkshopItems.WorkshopInstallState.Ready) {
                            busy = true;
                            break;
                        }

                        if (!completed[i]) {
                            completed[i] = true;
                            if (GameServer.coop) {
                                CoopSlave.instance
                                    .sendMessage(
                                        "status",
                                        null,
                                        Translator.getText("UI_ServerStatus_Downloaded_Workshop_Items_Progress", ++processedCount, workshopItems.size())
                                    );
                            }
                        }
                    }

                    if (!busy) {
                        GameServer.workshopInstallFolders = new String[itemIDList.size()];
                        GameServer.workshopTimeStamps = new long[itemIDList.size()];

                        for (int i = 0; i < itemIDList.size(); i++) {
                            long itemID = itemIDList.get(i);
                            String folder = SteamWorkshop.instance.GetItemInstallFolder(itemID);
                            if (folder == null) {
                                noise("GetItemInstallFolder() failed ID=" + itemID);
                                return false;
                            }

                            noise(itemID + " installed to " + folder);
                            GameServer.workshopInstallFolders[i] = folder;
                            GameServer.workshopTimeStamps[i] = SteamWorkshop.instance.GetItemInstallTimeStamp(itemID);
                        }

                        return true;
                    }

                    try {
                        Thread.sleep(33L);
                    } catch (Exception var9) {
                        var9.printStackTrace();
                    }
                }
            }
        }
    }

    private static boolean QueryItemDetails(ArrayList<GameServerWorkshopItems.WorkshopItem> workshopItems) {
        long[] itemIDs = new long[workshopItems.size()];

        for (int i = 0; i < workshopItems.size(); i++) {
            GameServerWorkshopItems.WorkshopItem item = workshopItems.get(i);
            itemIDs[i] = item.id;
        }

        GameServerWorkshopItems.ItemQuery query = new GameServerWorkshopItems.ItemQuery();
        query.handle = SteamWorkshop.instance.CreateQueryUGCDetailsRequest(itemIDs, query);
        if (query.handle == 0L) {
            return false;
        } else {
            while (true) {
                SteamUtils.runLoop();
                if (query.isCompleted()) {
                    for (SteamUGCDetails details : query.details) {
                        for (GameServerWorkshopItems.WorkshopItem workshopItem : workshopItems) {
                            if (workshopItem.id == details.getID()) {
                                workshopItem.details = details;
                                break;
                            }
                        }
                    }

                    return true;
                }

                if (query.isNotCompleted()) {
                    return false;
                }

                try {
                    Thread.sleep(33L);
                } catch (Exception var7) {
                    var7.printStackTrace();
                }
            }
        }
    }

    private static final class ItemQuery implements ISteamWorkshopCallback {
        long handle;
        ArrayList<SteamUGCDetails> details;
        boolean completed;
        boolean notCompleted;

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
            GameServerWorkshopItems.noise("onItemQueryCompleted handle=" + handle + " numResult=" + numResults);
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
            GameServerWorkshopItems.noise("onItemQueryNotCompleted handle=" + handle + " result=" + result);
            if (handle == this.handle) {
                SteamWorkshop.instance.RemoveCallback(this);
                SteamWorkshop.instance.ReleaseQueryUGCRequest(handle);
                this.notCompleted = true;
            }
        }
    }

    private static enum WorkshopInstallState {
        CheckItemState,
        DownloadPending,
        Ready,
        Fail;
    }

    private static class WorkshopItem implements ISteamWorkshopCallback {
        long id;
        GameServerWorkshopItems.WorkshopInstallState state = GameServerWorkshopItems.WorkshopInstallState.CheckItemState;
        long downloadStartTime;
        long downloadQueryTime;
        String error;
        SteamUGCDetails details;

        WorkshopItem(long id) {
            this.id = id;
        }

        void update() {
            switch (this.state) {
                case CheckItemState:
                    this.CheckItemState();
                    break;
                case DownloadPending:
                    this.DownloadPending();
                case Ready:
            }
        }

        void setState(GameServerWorkshopItems.WorkshopInstallState newState) {
            GameServerWorkshopItems.noise("item state " + this.state + " -> " + newState + " ID=" + this.id);
            this.state = newState;
        }

        void CheckItemState() {
            long itemState = SteamWorkshop.instance.GetItemState(this.id);
            GameServerWorkshopItems.noise("GetItemState()=" + SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + this.id);
            if (SteamWorkshopItem.ItemState.Installed.and(itemState)
                && this.details != null
                && this.details.getTimeCreated() != 0L
                && this.details.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(this.id)) {
                GameServerWorkshopItems.noise("Installed status but timeUpdated doesn't match!!!");
                this.RemoveFolderForReinstall();
                itemState |= SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
            }

            if (itemState != SteamWorkshopItem.ItemState.None.getValue()
                && !SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState)
                && new File(SteamWorkshop.instance.GetItemInstallFolder(this.id)).exists()) {
                if (SteamWorkshopItem.ItemState.Installed.and(itemState)) {
                    this.setState(GameServerWorkshopItems.WorkshopInstallState.Ready);
                } else {
                    this.error = "UnknownItemState";
                    this.setState(GameServerWorkshopItems.WorkshopInstallState.Fail);
                }
            } else if (SteamWorkshop.instance.DownloadItem(this.id, true, this)) {
                this.setState(GameServerWorkshopItems.WorkshopInstallState.DownloadPending);
                this.downloadStartTime = System.currentTimeMillis();
            } else {
                this.error = "DownloadItemFalse";
                this.setState(GameServerWorkshopItems.WorkshopInstallState.Fail);
            }
        }

        void RemoveFolderForReinstall() {
            String folder = SteamWorkshop.instance.GetItemInstallFolder(this.id);
            if (folder == null) {
                GameServerWorkshopItems.noise("not removing install folder because GetItemInstallFolder() failed ID=" + this.id);
            } else {
                Path path = Paths.get(folder);
                if (!Files.exists(path)) {
                    GameServerWorkshopItems.noise("not removing install folder because it does not exist : \"" + folder + "\"");
                } else {
                    try {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            {
                                Objects.requireNonNull(WorkshopItem.this);
                            }

                            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                                Files.delete(path);
                                return FileVisitResult.CONTINUE;
                            }

                            public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException {
                                Files.delete(directory);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (Exception var4) {
                        ExceptionLogger.logException(var4);
                    }
                }
            }
        }

        void DownloadPending() {
            long time = System.currentTimeMillis();
            if (this.downloadQueryTime + 100L <= time) {
                this.downloadQueryTime = time;
                long itemState = SteamWorkshop.instance.GetItemState(this.id);
                GameServerWorkshopItems.noise("DownloadPending GetItemState()=" + SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + this.id);
                if (SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState)) {
                    long[] progress = new long[2];
                    if (SteamWorkshop.instance.GetItemDownloadInfo(this.id, progress)) {
                        GameServerWorkshopItems.noise("download " + progress[0] + "/" + progress[1] + " ID=" + this.id);
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
            GameServerWorkshopItems.noise("onItemSubscribed itemID=" + itemID);
        }

        @Override
        public void onItemNotSubscribed(long itemID, int result) {
            GameServerWorkshopItems.noise("onItemNotSubscribed itemID=" + itemID + " result=" + result);
        }

        @Override
        public void onItemDownloaded(long itemID) {
            GameServerWorkshopItems.noise("onItemDownloaded itemID=" + itemID + " time=" + (System.currentTimeMillis() - this.downloadStartTime) + " ms");
            if (itemID == this.id) {
                SteamWorkshop.instance.RemoveCallback(this);
                this.setState(GameServerWorkshopItems.WorkshopInstallState.CheckItemState);
            }
        }

        @Override
        public void onItemNotDownloaded(long itemID, int result) {
            GameServerWorkshopItems.noise("onItemNotDownloaded itemID=" + itemID + " result=" + result);
            if (itemID == this.id) {
                SteamWorkshop.instance.RemoveCallback(this);
                this.error = "ItemNotDownloaded";
                this.setState(GameServerWorkshopItems.WorkshopInstallState.Fail);
            }
        }

        @Override
        public void onItemQueryCompleted(long handle, int numResults) {
            GameServerWorkshopItems.noise("onItemQueryCompleted handle=" + handle + " numResult=" + numResults);
        }

        @Override
        public void onItemQueryNotCompleted(long handle, int result) {
            GameServerWorkshopItems.noise("onItemQueryNotCompleted handle=" + handle + " result=" + result);
        }
    }
}
