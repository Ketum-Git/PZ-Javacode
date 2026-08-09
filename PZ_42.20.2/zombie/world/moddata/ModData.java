// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world.moddata;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;

/**
 * TurboTuTone.
 * 
 *  Exposed class with only allowed functions accessible for modding
 */
@UsedFromLua
public final class ModData {
    private static final ArrayList<String> temp_list = new ArrayList<>();

    public static ArrayList<String> getTableNames() {
        GlobalModData.instance.collectTableNames(temp_list);
        return temp_list;
    }

    public static boolean exists(String tag) {
        return GlobalModData.instance.exists(tag);
    }

    public static KahluaTable getOrCreate(String tag) {
        return GlobalModData.instance.getOrCreate(tag);
    }

    public static KahluaTable get(String tag) {
        return GlobalModData.instance.get(tag);
    }

    public static String create() {
        return GlobalModData.instance.create();
    }

    public static KahluaTable create(String tag) {
        return GlobalModData.instance.create(tag);
    }

    public static KahluaTable remove(String tag) {
        return GlobalModData.instance.remove(tag);
    }

    public static void add(String tag, KahluaTable table) {
        GlobalModData.instance.add(tag, table);
    }

    public static void transmit(String tag) {
        GlobalModData.instance.transmit(tag);
    }

    public static void request(String tag) {
        GlobalModData.instance.request(tag);
    }
}
