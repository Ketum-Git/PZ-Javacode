// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

@UsedFromLua
public class SystemDisabler {
    public static boolean doCharacterStats = true;
    public static boolean doZombieCreation = true;
    public static boolean doSurvivorCreation;
    public static boolean doPlayerCreation = true;
    public static boolean doOverridePOVCharacters = true;
    public static boolean doVehiclesEverywhere;
    public static boolean doWorldSyncEnable;
    private static final boolean doHighFriction = false;
    private static final boolean doVehicleLowRider = false;
    public static boolean doEnableDetectOpenGLErrors;
    public static boolean doEnableDetectOpenGLErrorsInTexture;
    public static boolean doVehiclesWithoutTextures;
    public static boolean zombiesDontAttack;
    private static final boolean doPrintDetailedInfo = false;
    private static boolean doMainLoopDealWithNetData = true;
    private static boolean enableAdvancedSoundOptions;
    private static boolean uncappedFPS;

    public static void setDoCharacterStats(boolean bDo) {
        doCharacterStats = bDo;
    }

    public static void setDoZombieCreation(boolean bDo) {
        doZombieCreation = bDo;
    }

    public static void setDoSurvivorCreation(boolean bDo) {
        doSurvivorCreation = bDo;
    }

    public static void setDoPlayerCreation(boolean bDo) {
        doPlayerCreation = bDo;
    }

    public static void setOverridePOVCharacters(boolean bDo) {
        doOverridePOVCharacters = bDo;
    }

    public static void setVehiclesEverywhere(boolean bDo) {
        doVehiclesEverywhere = bDo;
    }

    public static void setWorldSyncEnable(boolean bDo) {
        doWorldSyncEnable = bDo;
    }

    public static boolean getdoHighFriction() {
        return false;
    }

    public static boolean getdoVehicleLowRider() {
        return false;
    }

    public static boolean printDetailedInfo() {
        return false;
    }

    public static boolean getDoMainLoopDealWithNetData() {
        return doMainLoopDealWithNetData;
    }

    public static void setEnableAdvancedSoundOptions(boolean enable) {
        enableAdvancedSoundOptions = enable;
    }

    public static boolean getEnableAdvancedSoundOptions() {
        return enableAdvancedSoundOptions;
    }

    public static void setUncappedFPS(boolean b) {
        uncappedFPS = b;
    }

    public static boolean getUncappedFPS() {
        return uncappedFPS;
    }

    public static void Reset() {
        doCharacterStats = true;
        doZombieCreation = true;
        doSurvivorCreation = false;
        doPlayerCreation = true;
        doOverridePOVCharacters = true;
        doVehiclesEverywhere = false;
        doWorldSyncEnable = false;
        doMainLoopDealWithNetData = true;
        enableAdvancedSoundOptions = false;
    }
}
