// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.util.list.PZArrayUtil;

public final class ZombieStateFlags {
    private int flags;

    public ZombieStateFlags() {
    }

    public ZombieStateFlags(int flags) {
        this.flags = flags;
    }

    public ZombieStateFlags(ZombieStateFlag... flags) {
        for (ZombieStateFlag flag : flags) {
            this.setFlag(flag);
        }
    }

    public static ZombieStateFlags fromInt(int stateFlags) {
        return new ZombieStateFlags(stateFlags);
    }

    public static int intFromZombie(IsoZombie fromZombie) {
        int state = 0;
        state = setFlag(state, ZombieStateFlag.Initialized);
        state = setFlag(state, ZombieStateFlag.Crawling, fromZombie.isCrawling());
        state = setFlag(state, ZombieStateFlag.CanWalk, fromZombie.isCanWalk());
        state = setFlag(state, ZombieStateFlag.FakeDead, fromZombie.isFakeDead());
        state = setFlag(state, ZombieStateFlag.CanCrawlUnderVehicle, fromZombie.isCanCrawlUnderVehicle());
        return setFlag(state, ZombieStateFlag.ReanimatedForGrappleOnly, fromZombie.isReanimatedForGrappleOnly());
    }

    public static ZombieStateFlags fromZombie(IsoZombie fromZombie) {
        return fromInt(intFromZombie(fromZombie));
    }

    public void setFlag(ZombieStateFlag flag) {
        this.flags = setFlag(this.flags, flag);
    }

    public void clearFlag(ZombieStateFlag flag) {
        this.flags = clearFlag(this.flags, flag);
    }

    public void setFlag(ZombieStateFlag flag, boolean isTrue) {
        this.flags = setFlag(this.flags, flag, isTrue);
    }

    public boolean checkFlag(ZombieStateFlag flag) {
        return checkFlag(this.flags, flag);
    }

    public static int setFlag(int state, ZombieStateFlag flag) {
        return state | flag.flag;
    }

    public static int clearFlag(int state, ZombieStateFlag flag) {
        return state & ~flag.flag;
    }

    public static int setFlag(int state, ZombieStateFlag flag, boolean isTrue) {
        return isTrue ? setFlag(state, flag) : clearFlag(state, flag);
    }

    public static boolean checkFlag(int state, ZombieStateFlag flag) {
        return (state & flag.flag) != 0;
    }

    public int asInt() {
        return this.flags;
    }

    public boolean isInitialized() {
        return this.checkFlag(ZombieStateFlag.Initialized);
    }

    public boolean isCrawling() {
        return this.checkFlag(ZombieStateFlag.Crawling);
    }

    public boolean isCanWalk() {
        return this.checkFlag(ZombieStateFlag.CanWalk);
    }

    public boolean isFakeDead() {
        return this.checkFlag(ZombieStateFlag.FakeDead);
    }

    public boolean isCanCrawlUnderVehicle() {
        return this.checkFlag(ZombieStateFlag.CanCrawlUnderVehicle);
    }

    public boolean isReanimatedForGrappleOnly() {
        return this.checkFlag(ZombieStateFlag.ReanimatedForGrappleOnly);
    }

    public ZombieStateFlag[] toArray() {
        ArrayList<ZombieStateFlag> array = new ArrayList<>();

        for (ZombieStateFlag flag : ZombieStateFlag.values()) {
            if (this.checkFlag(flag)) {
                array.add(flag);
            }
        }

        return array.toArray(new ZombieStateFlag[0]);
    }

    @Override
    public String toString() {
        String contentsStr = PZArrayUtil.arrayToString(this.toArray(), Enum::toString, "{ ", " }", ", ");
        return this.getClass().getName() + "{ " + contentsStr + "}";
    }

    public static String intToString(int state) {
        return fromInt(state).toString();
    }
}
