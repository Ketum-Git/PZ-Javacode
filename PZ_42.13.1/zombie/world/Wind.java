// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world;

import zombie.core.Translator;

public class Wind {
    public static float getWindKnots(float windKph) {
        return windKph * 19.0F / 36.0F;
    }

    public static int getWindsockSegments(float windKph) {
        return Math.max(0, Math.min(5, (int)Math.floor(windKph * 19.0F / 108.0F)));
    }

    public static int getBeaufortNumber(float windKph) {
        if (windKph < 4.0F) {
            return 0;
        } else if (windKph < 9.0F) {
            return 1;
        } else if (windKph < 16.0F) {
            return 2;
        } else if (windKph < 23.0F) {
            return 3;
        } else if (windKph < 31.0F) {
            return 4;
        } else if (windKph < 40.0F) {
            return 5;
        } else if (windKph < 50.0F) {
            return 6;
        } else if (windKph < 60.0F) {
            return 7;
        } else if (windKph < 72.0F) {
            return 8;
        } else if (windKph < 84.0F) {
            return 9;
        } else {
            return windKph < 97.0F ? 10 : 11;
        }
    }

    public static String getName(int beaufortNumber) {
        return String.format(Translator.getText("UI_GameLoad_windName" + beaufortNumber));
    }

    public static String getDescription(int beaufortNumber) {
        return String.format(Translator.getText("UI_GameLoad_windDescription" + beaufortNumber));
    }
}
