// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

final class Sync {
    private final int fps = 20;
    private final long period = 50000000L;
    private long excess;
    private long beforeTime = System.nanoTime();
    private long overSleepTime;

    void begin() {
        this.beforeTime = System.nanoTime();
        this.overSleepTime = 0L;
    }

    void startFrame() {
        this.excess = 0L;
    }

    void endFrame() {
        long afterTime = System.nanoTime();
        long timeDiff = afterTime - this.beforeTime;
        long sleepTime = 50000000L - timeDiff - this.overSleepTime;
        if (sleepTime > 0L) {
            try {
                Thread.sleep(sleepTime / 1000000L);
            } catch (InterruptedException var8) {
            }

            this.overSleepTime = System.nanoTime() - afterTime - sleepTime;
        } else {
            this.excess -= sleepTime;
            this.overSleepTime = 0L;
        }

        this.beforeTime = System.nanoTime();
    }
}
