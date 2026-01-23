// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.UsedFromLua;
import zombie.core.Core;

@UsedFromLua
public class NetworkAIParams {
    public static final int MAX_CONNECTIONS = 100;
    public static final int ZOMBIE_UPDATE_INFO_BUNCH_RATE_MS = 200;
    public static final int CHARACTER_UPDATE_RATE_MS = 200;
    public static final int CHARACTER_EXTRAPOLATION_UPDATE_INTERVAL_MS = 500;
    public static final float ZOMBIE_ANTICIPATORY_UPDATE_MULTIPLIER = 0.6F;
    public static final int ANIMAL_PREDICT_INTERVAL = 1000;
    public static final float ANIMAL_PREDICT_UPDATE_LIMIT = 600.0F;
    public static final int ZOMBIE_OWNERSHIP_INTERVAL = 2000;
    public static final int ZOMBIE_REMOVE_INTERVAL_MS = 4000;
    public static final int ZOMBIE_MAX_UPDATE_INTERVAL_MS = 3800;
    public static final int ZOMBIE_MIN_UPDATE_INTERVAL_MS = 200;
    public static final int CHARACTER_PREDICTION_INTERVAL_MS = 2000;
    public static final int ZOMBIE_TELEPORT_PLAYER = 2;
    public static final int ZOMBIE_TELEPORT_DISTANCE_SQ = 9;
    public static final int VEHICLE_SPEED_CAP = 10;
    public static final int VEHICLE_MOVING_MP_PHYSIC_UPDATE_RATE = 150;
    public static final int VEHICLE_MP_PHYSIC_UPDATE_RATE = 300;
    public static final int VEHICLE_BUFFER_DELAY_MS = 500;
    public static final int VEHICLE_BUFFER_HISTORY_MS = 800;
    public static final float VEHICLE_DELAY_TUNE_PER_SEC = 3.0F;
    public static final float VEHICLE_DELAY_NORMALISE_PER_SEC = 10.0F;
    public static final float VEHICLE_DELAY_TUNE_MULTIPLIXER = 2.0F;
    public static final int VEHICLE_HIGH_PING_COUNT = 290;
    public static final float VEHICLE_DELAY_HIGH_PING_MULTIPLIXER = 3.0F;
    public static final float VEHICLE_DELAY_SLOWING_DOWN_DELAY_MULTIPLIXER = 8.0F;
    public static final float MAX_TOWING_TRAILER_DISTANCE_SQ = 1.0F;
    public static final float MAX_TOWING_CAR_DISTANCE_SQ = 4.0F;
    public static final float MAX_RECONNECT_DISTANCE_SQ = 10.0F;
    public static final float TOWING_DISTANCE = 1.5F;
    private static boolean showConnectionInfo;
    private static boolean showServerInfo;

    public static boolean isShowConnectionInfo() {
        return showConnectionInfo;
    }

    public static void setShowConnectionInfo(boolean enabled) {
        showConnectionInfo = enabled;
    }

    public static boolean isShowServerInfo() {
        return showServerInfo;
    }

    public static void setShowServerInfo(boolean enabled) {
        showServerInfo = enabled;
    }

    public static void Init() {
        if (GameClient.client && Core.debug) {
            showConnectionInfo = false;
            showServerInfo = true;
        }
    }
}
