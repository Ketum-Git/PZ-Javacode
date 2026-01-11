// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream.Filter;
import java.util.ArrayList;
import java.util.Objects;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.debug.DebugLog;
import zombie.network.GameServer;

public class SteamWorkshop implements ISteamWorkshopCallback {
    public static final SteamWorkshop instance = new SteamWorkshop();
    private final ArrayList<SteamWorkshopItem> stagedItems = new ArrayList<>();
    private final ArrayList<ISteamWorkshopCallback> callbacks = new ArrayList<>();

    public static void init() {
        if (SteamUtils.isSteamModeEnabled()) {
            instance.n_Init();
        }

        if (!GameServer.server) {
            instance.initWorkshopFolder();
        }
    }

    public static void shutdown() {
        if (SteamUtils.isSteamModeEnabled()) {
            instance.n_Shutdown();
        }
    }

    private void copyFile(File src, File dst) {
        try (
            FileInputStream inStream = new FileInputStream(src);
            FileOutputStream outStream = new FileOutputStream(dst);
        ) {
            outStream.getChannel().transferFrom(inStream.getChannel(), 0L, src.length());
        } catch (IOException var11) {
            var11.printStackTrace();
        }
    }

    private void copyFileOrFolder(File src, File dst) {
        if (src.isDirectory()) {
            if (!dst.mkdirs()) {
                return;
            }

            String[] internalNames = src.list();

            for (int i = 0; i < internalNames.length; i++) {
                this.copyFileOrFolder(new File(src, internalNames[i]), new File(dst, internalNames[i]));
            }
        } else {
            this.copyFile(src, dst);
        }
    }

    private void initWorkshopFolder() {
        File file = new File(this.getWorkshopFolder());
        if (file.exists() || file.mkdirs()) {
            File src = new File("Workshop" + File.separator + "ModTemplate");
            File dst = new File(this.getWorkshopFolder() + File.separator + "ModTemplate");
            if (src.exists() && !dst.exists()) {
                this.copyFileOrFolder(src, dst);
            }
        }
    }

    public ArrayList<SteamWorkshopItem> loadStagedItems() {
        this.stagedItems.clear();

        for (String folder : this.getStageFolders()) {
            SteamWorkshopItem item = new SteamWorkshopItem(folder);
            item.readWorkshopTxt();
            this.stagedItems.add(item);
        }

        return this.stagedItems;
    }

    public String getWorkshopFolder() {
        return ZomboidFileSystem.instance.getCacheDir() + File.separator + "Workshop";
    }

    public ArrayList<String> getStageFolders() {
        ArrayList<String> folders = new ArrayList<>();
        Path dir = FileSystems.getDefault().getPath(this.getWorkshopFolder());

        try {
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException var11) {
            var11.printStackTrace();
            return folders;
        }

        Filter<Path> filter = new Filter<Path>() {
            {
                Objects.requireNonNull(SteamWorkshop.this);
            }

            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry);
            }
        };

        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir, filter)) {
            for (Path path : dstrm) {
                String filePath = path.toAbsolutePath().toString();
                folders.add(filePath);
            }
        } catch (Exception var10) {
            var10.printStackTrace();
        }

        return folders;
    }

    public boolean CreateWorkshopItem(SteamWorkshopItem item) {
        if (item.getID() != null) {
            throw new RuntimeException("can't recreate an existing item");
        } else {
            return this.n_CreateItem();
        }
    }

    public boolean SubmitWorkshopItem(SteamWorkshopItem item) {
        if (item.getID() != null && SteamUtils.isValidSteamID(item.getID())) {
            long itemID = SteamUtils.convertStringToSteamID(item.getID());
            if (!this.n_StartItemUpdate(itemID)) {
                return false;
            } else if (!this.n_SetItemTitle(item.getTitle())) {
                return false;
            } else if (!this.n_SetItemDescription(item.getSubmitDescription())) {
                return false;
            } else {
                int visibility = item.getVisibilityInteger();
                if ("Mod Template".equals(item.getTitle())) {
                    visibility = 2;
                }

                if (!this.n_SetItemVisibility(visibility)) {
                    return false;
                } else {
                    if (!this.n_SetItemTags(item.getSubmitTags())) {
                    }

                    if (!this.n_SetItemContent(item.getContentFolder())) {
                        return false;
                    } else {
                        return !this.n_SetItemPreview(item.getPreviewImage()) ? false : this.n_SubmitItemUpdate(item.getChangeNote());
                    }
                }
            }
        } else {
            throw new RuntimeException("workshop ID is required");
        }
    }

    public boolean GetItemUpdateProgress(long[] progress) {
        return this.n_GetItemUpdateProgress(progress);
    }

    public String[] GetInstalledItemFolders() {
        return GameServer.server ? GameServer.workshopInstallFolders : this.n_GetInstalledItemFolders();
    }

    public long GetItemState(long itemID) {
        return this.n_GetItemState(itemID);
    }

    public String GetItemInstallFolder(long itemID) {
        return this.n_GetItemInstallFolder(itemID);
    }

    public long GetItemInstallTimeStamp(long itemID) {
        return this.n_GetItemInstallTimeStamp(itemID);
    }

    public boolean SubscribeItem(long itemID, ISteamWorkshopCallback callback) {
        if (!this.callbacks.contains(callback)) {
            this.callbacks.add(callback);
        }

        return this.n_SubscribeItem(itemID);
    }

    public boolean DownloadItem(long itemID, boolean bHighPriority, ISteamWorkshopCallback callback) {
        if (!this.callbacks.contains(callback)) {
            this.callbacks.add(callback);
        }

        return this.n_DownloadItem(itemID, bHighPriority);
    }

    public boolean GetItemDownloadInfo(long itemID, long[] progress) {
        return this.n_GetItemDownloadInfo(itemID, progress);
    }

    public long CreateQueryUGCDetailsRequest(long[] itemIDs, ISteamWorkshopCallback callback) {
        if (!this.callbacks.contains(callback)) {
            this.callbacks.add(callback);
        }

        return this.n_CreateQueryUGCDetailsRequest(itemIDs);
    }

    public SteamUGCDetails GetQueryUGCResult(long handle, int index) {
        return this.n_GetQueryUGCResult(handle, index);
    }

    public long[] GetQueryUGCChildren(long handle, int index) {
        return this.n_GetQueryUGCChildren(handle, index);
    }

    public boolean ReleaseQueryUGCRequest(long handle) {
        return this.n_ReleaseQueryUGCRequest(handle);
    }

    public void RemoveCallback(ISteamWorkshopCallback callback) {
        this.callbacks.remove(callback);
    }

    public String getIDFromItemInstallFolder(String dir) {
        if (dir != null && dir.replace("\\", "/").contains("/workshop/content/108600/")) {
            File file = new File(dir);
            String id = file.getName();
            if (SteamUtils.isValidSteamID(id)) {
                return id;
            }

            DebugLog.log("ERROR: " + id + " isn't a valid workshop item ID");
        }

        return null;
    }

    private native void n_Init();

    private native void n_Shutdown();

    private native boolean n_CreateItem();

    private native boolean n_StartItemUpdate(long var1);

    private native boolean n_SetItemTitle(String var1);

    private native boolean n_SetItemDescription(String var1);

    private native boolean n_SetItemVisibility(int var1);

    private native boolean n_SetItemTags(String[] var1);

    private native boolean n_SetItemContent(String var1);

    private native boolean n_SetItemPreview(String var1);

    private native boolean n_SubmitItemUpdate(String var1);

    private native boolean n_GetItemUpdateProgress(long[] var1);

    private native String[] n_GetInstalledItemFolders();

    private native long n_GetItemState(long var1);

    private native boolean n_SubscribeItem(long var1);

    private native boolean n_DownloadItem(long var1, boolean var3);

    private native String n_GetItemInstallFolder(long var1);

    private native long n_GetItemInstallTimeStamp(long var1);

    private native boolean n_GetItemDownloadInfo(long var1, long[] var3);

    private native long n_CreateQueryUGCDetailsRequest(long[] var1);

    private native SteamUGCDetails n_GetQueryUGCResult(long var1, int var3);

    private native long[] n_GetQueryUGCChildren(long var1, int var3);

    private native boolean n_ReleaseQueryUGCRequest(long var1);

    @Override
    public void onItemCreated(long itemID, boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        LuaEventManager.triggerEvent("OnSteamWorkshopItemCreated", SteamUtils.convertSteamIDToString(itemID), bUserNeedsToAcceptWorkshopLegalAgreement);
    }

    @Override
    public void onItemNotCreated(int result) {
        LuaEventManager.triggerEvent("OnSteamWorkshopItemNotCreated", result);
    }

    @Override
    public void onItemUpdated(boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        LuaEventManager.triggerEvent("OnSteamWorkshopItemUpdated", bUserNeedsToAcceptWorkshopLegalAgreement);
    }

    @Override
    public void onItemNotUpdated(int result) {
        LuaEventManager.triggerEvent("OnSteamWorkshopItemNotUpdated", result);
    }

    @Override
    public void onItemSubscribed(long itemID) {
        for (int i = 0; i < this.callbacks.size(); i++) {
            this.callbacks.get(i).onItemSubscribed(itemID);
        }
    }

    @Override
    public void onItemNotSubscribed(long itemID, int result) {
        for (int i = 0; i < this.callbacks.size(); i++) {
            this.callbacks.get(i).onItemNotSubscribed(itemID, result);
        }
    }

    @Override
    public void onItemDownloaded(long itemID) {
        for (int i = 0; i < this.callbacks.size(); i++) {
            this.callbacks.get(i).onItemDownloaded(itemID);
        }
    }

    @Override
    public void onItemNotDownloaded(long itemID, int result) {
        for (int i = 0; i < this.callbacks.size(); i++) {
            this.callbacks.get(i).onItemNotDownloaded(itemID, result);
        }
    }

    @Override
    public void onItemQueryCompleted(long handle, int numResults) {
        for (int i = 0; i < this.callbacks.size(); i++) {
            this.callbacks.get(i).onItemQueryCompleted(handle, numResults);
        }
    }

    @Override
    public void onItemQueryNotCompleted(long handle, int result) {
        for (int i = 0; i < this.callbacks.size(); i++) {
            this.callbacks.get(i).onItemQueryNotCompleted(handle, result);
        }
    }
}
