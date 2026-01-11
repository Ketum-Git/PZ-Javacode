// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

public class UpdateTimer {
    private long time = System.currentTimeMillis() + 3800L;

    public void reset(long time) {
        this.time = System.currentTimeMillis() + time;
    }

    public boolean check() {
        return this.time != 0L && System.currentTimeMillis() + 200L >= this.time;
    }

    public long getTime() {
        return this.time;
    }
}
