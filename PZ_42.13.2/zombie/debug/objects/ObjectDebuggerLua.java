// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.objects;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;

@UsedFromLua
public class ObjectDebuggerLua {
    private static final ConcurrentLinkedDeque<ArrayList<String>> array_list_pool = new ConcurrentLinkedDeque<>();

    public static ArrayList<String> AllocList() {
        ArrayList<String> list = array_list_pool.poll();
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public static void ReleaseList(ArrayList<String> list) {
        list.clear();
        array_list_pool.offer(list);
    }

    public static void Log(Object o) {
        Log(o, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static void Log(Object o, int inheritanceDepth) {
        Log(o, inheritanceDepth, Integer.MAX_VALUE);
    }

    public static void Log(Object o, int inheritanceDepth, int memberDepth) {
        ObjectDebugger.Log(DebugLog.General, o, inheritanceDepth, true, true, memberDepth);
    }

    public static void GetLines(Object o, ArrayList<String> list) {
        GetLines(o, list, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, int inheritanceDepth) {
        GetLines(o, list, inheritanceDepth, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, int inheritanceDepth, int memberDepth) {
        ObjectDebugger.GetLines(o, list, inheritanceDepth, true, true, memberDepth);
    }
}
