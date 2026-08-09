// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.SpriteDetails;

import java.util.HashMap;
import zombie.UsedFromLua;

@UsedFromLua
public enum IsoObjectType {
    normal(0),
    jukebox(1),
    wall(2),
    stairsTW(3),
    stairsTN(4),
    stairsMW(5),
    stairsMN(6),
    stairsBW(7),
    stairsBN(8),
    UNUSED9(9),
    UNUSED10(10),
    doorW(11),
    doorN(12),
    lightswitch(13),
    radio(14),
    curtainN(15),
    curtainS(16),
    curtainW(17),
    curtainE(18),
    doorFrW(19),
    doorFrN(20),
    tree(21),
    windowFN(22),
    windowFW(23),
    UNUSED24(24),
    WestRoofB(25),
    WestRoofM(26),
    WestRoofT(27),
    isMoveAbleObject(28),
    MAX(29);

    private final int index;
    private static final HashMap<String, IsoObjectType> fromStringMap = new HashMap<>();

    private IsoObjectType(final int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }

    public static IsoObjectType fromIndex(int value) {
        return IsoObjectType.class.getEnumConstants()[value];
    }

    public static IsoObjectType FromString(String str) {
        IsoObjectType e = fromStringMap.get(str);
        return e == null ? MAX : e;
    }

    static {
        for (IsoObjectType e : values()) {
            if (e == MAX) {
                break;
            }

            fromStringMap.put(e.name(), e);
        }
    }
}
