// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

public class PoolIDGenerator {
    private static int currentID = 1;
    private static final Object currentIDLock = new Object();

    public static int getNewID() {
        synchronized (currentIDLock) {
            return currentID++;
        }
    }
}
