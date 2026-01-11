// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.characters;

public class AttackRateChecker {
    public long timestamp;
    public short count;

    public void set(long timestamp, short count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public void reset() {
        this.set(System.currentTimeMillis(), (short)0);
    }

    public boolean check(int maxProjectiles, int maxTimeMs) {
        this.count++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.timestamp > maxTimeMs) {
            this.set(currentTime, (short)1);
        }

        return this.count > maxProjectiles;
    }
}
