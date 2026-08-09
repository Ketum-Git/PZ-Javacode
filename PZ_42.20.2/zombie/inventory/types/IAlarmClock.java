// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

public interface IAlarmClock {
    void stopRinging();

    void setAlarmSet(boolean arg0);

    boolean isAlarmSet();

    void setHour(int arg0);

    void setMinute(int arg0);

    void setForceDontRing(int arg0);

    int getHour();

    int getMinute();

    void update();
}
