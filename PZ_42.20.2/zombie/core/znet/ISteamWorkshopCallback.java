// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

public interface ISteamWorkshopCallback {
    void onItemCreated(long itemID, boolean bUserNeedsToAcceptWorkshopLegalAgreement);

    void onItemNotCreated(int result);

    void onItemUpdated(boolean bUserNeedsToAcceptWorkshopLegalAgreement);

    void onItemNotUpdated(int result);

    void onItemSubscribed(long itemID);

    void onItemNotSubscribed(long itemID, int result);

    void onItemDownloaded(long itemID);

    void onItemNotDownloaded(long itemID, int result);

    void onItemQueryCompleted(long handle, int numResults);

    void onItemQueryNotCompleted(long handle, int result);
}
