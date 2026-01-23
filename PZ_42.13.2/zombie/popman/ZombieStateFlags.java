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

    public static ZombieStateFlags fromInt(int in_stateFlags) {
        return new ZombieStateFlags(in_stateFlags);
    }

    public static int intFromZombie(IsoZombie in_fromZombie) {
        int state = 0;
        state = setFlag(state, ZombieStateFlag.Initialized);
        state = setFlag(state, ZombieStateFlag.Crawling, in_fromZombie.isCrawling());
        state = setFlag(state, ZombieStateFlag.CanWalk, in_fromZombie.isCanWalk());
        state = setFlag(state, ZombieStateFlag.FakeDead, in_fromZombie.isFakeDead());
        state = setFlag(state, ZombieStateFlag.CanCrawlUnderVehicle, in_fromZombie.isCanCrawlUnderVehicle());
        return setFlag(state, ZombieStateFlag.ReanimatedForGrappleOnly, in_fromZombie.isReanimatedForGrappleOnly());
    }

    public static ZombieStateFlags fromZombie(IsoZombie in_fromZombie) {
        return fromInt(intFromZombie(in_fromZombie));
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

    public static int setFlag(int in_state, ZombieStateFlag in_flag) {
        return in_state | in_flag.flag;
    }

    public static int clearFlag(int in_state, ZombieStateFlag in_flag) {
        return in_state & ~in_flag.flag;
    }

    public static int setFlag(int in_state, ZombieStateFlag in_flag, boolean in_isTrue) {
        return in_isTrue ? setFlag(in_state, in_flag) : clearFlag(in_state, in_flag);
    }

    public static boolean checkFlag(int in_state, ZombieStateFlag in_flag) {
        return (in_state & in_flag.flag) != 0;
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

    public static String intToString(int in_state) {
        return fromInt(in_state).toString();
    }
}
