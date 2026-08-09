// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import zombie.core.PerformanceSettings;

public final class FPSTracking {
    private final double[] lastFps = new double[20];
    private int lastFpsCount;
    private long timeAtLastUpdate;
    private final long[] last10 = new long[10];
    private int last10index;

    public void init() {
        for (int n = 0; n < 20; n++) {
            this.lastFps[n] = PerformanceSettings.getLockFPS();
        }

        this.timeAtLastUpdate = System.nanoTime();
    }

    public long frameStep() {
        long timeNow = System.nanoTime();
        long timeDiff = timeNow - this.timeAtLastUpdate;
        if (timeDiff > 0L) {
            float averageFPS = 0.0F;
            double deltaTimeSeconds = timeDiff / 1.0E9;
            double frames = 1.0 / deltaTimeSeconds;
            this.lastFps[this.lastFpsCount] = frames;
            this.lastFpsCount++;
            if (this.lastFpsCount >= 5) {
                this.lastFpsCount = 0;
            }

            for (int n = 0; n < 5; n++) {
                averageFPS = (float)(averageFPS + this.lastFps[n]);
            }

            averageFPS /= 5.0F;
            GameWindow.averageFPS = averageFPS;
            GameTime.instance.fpsMultiplier = (float)(60.0 / frames);
            if (GameTime.instance.fpsMultiplier > 5.0F) {
                GameTime.instance.fpsMultiplier = 5.0F;
            }
        }

        this.timeAtLastUpdate = timeNow;
        this.updateFPS(timeDiff);
        return timeDiff;
    }

    public void updateFPS(long timeDiff) {
        this.last10[this.last10index++] = timeDiff;
        if (this.last10index >= this.last10.length) {
            this.last10index = 0;
        }

        float lowest = 11110.0F;
        float highest = -11110.0F;

        for (long aLast10 : this.last10) {
            if (aLast10 != 0L) {
                if ((float)aLast10 < lowest) {
                    lowest = (float)aLast10;
                }

                if ((float)aLast10 > highest) {
                    highest = (float)aLast10;
                }
            }
        }
    }
}
