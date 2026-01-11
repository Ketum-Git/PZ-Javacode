// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoDirections;

public final class TableNetworkUtils {
    private static final byte SBYT_NO_SAVE = -1;
    private static final byte SBYT_STRING = 0;
    private static final byte SBYT_DOUBLE = 1;
    private static final byte SBYT_TABLE = 2;
    private static final byte SBYT_BOOLEAN = 3;
    private static final byte SBYT_ITEM = 4;
    private static final byte SBYT_DIRECTION = 5;

    public static void save(KahluaTable tbl, ByteBuffer output) throws IOException {
        KahluaTableIterator it = tbl.iterator();
        int count = 0;

        while (it.advance()) {
            if (canSave(it.getKey(), it.getValue())) {
                count++;
            }
        }

        it = tbl.iterator();
        output.putInt(count);

        while (it.advance()) {
            byte keyByte = getKeyByte(it.getKey());
            byte valueByte = getValueByte(it.getValue());
            if (keyByte != -1 && valueByte != -1) {
                save(output, keyByte, it.getKey());
                save(output, valueByte, it.getValue());
            }
        }
    }

    public static void saveSome(KahluaTable tbl, ByteBuffer output, HashSet<? extends Object> keys) throws IOException {
        KahluaTableIterator it = tbl.iterator();
        int count = 0;

        while (it.advance()) {
            if (keys.contains(it.getKey()) && canSave(it.getKey(), it.getValue())) {
                count++;
            }
        }

        it = tbl.iterator();
        output.putInt(count);

        while (it.advance()) {
            if (keys.contains(it.getKey())) {
                byte keyByte = getKeyByte(it.getKey());
                byte valueByte = getValueByte(it.getValue());
                if (keyByte != -1 && valueByte != -1) {
                    save(output, keyByte, it.getKey());
                    save(output, valueByte, it.getValue());
                }
            }
        }
    }

    private static void save(ByteBuffer output, byte sbyt, Object o) throws IOException, RuntimeException {
        output.put(sbyt);
        if (sbyt == 0) {
            GameWindow.WriteString(output, (String)o);
        } else if (sbyt == 1) {
            output.putDouble((Double)o);
        } else if (sbyt == 3) {
            output.put((byte)((Boolean)o ? 1 : 0));
        } else if (sbyt == 2) {
            save((KahluaTable)o, output);
        } else if (sbyt == 4) {
            ((InventoryItem)o).saveWithSize(output, false);
        } else {
            if (sbyt != 5) {
                throw new RuntimeException("invalid lua table type " + sbyt);
            }

            output.put((byte)((IsoDirections)o).index());
        }
    }

    public static void load(KahluaTable tbl, ByteBuffer input) throws IOException {
        int count = input.getInt();
        tbl.wipe();

        for (int n = 0; n < count; n++) {
            byte keyByte = input.get();
            Object key = load(input, keyByte);
            byte valueByte = input.get();
            Object value = load(input, valueByte);
            tbl.rawset(key, value);
        }
    }

    public static Object load(ByteBuffer input, byte sbyt) throws IOException, RuntimeException {
        if (sbyt == 0) {
            return GameWindow.ReadString(input);
        } else if (sbyt == 1) {
            return input.getDouble();
        } else if (sbyt == 3) {
            return input.get() == 1;
        } else if (sbyt == 2) {
            KahluaTableImpl v = (KahluaTableImpl)LuaManager.platform.newTable();
            load(v, input);
            return v;
        } else if (sbyt == 4) {
            InventoryItem item = null;

            try {
                item = InventoryItem.loadItem(input, 240);
            } catch (Exception var4) {
                var4.printStackTrace();
            }

            return item;
        } else if (sbyt == 5) {
            return IsoDirections.fromIndex(input.get());
        } else {
            throw new RuntimeException("invalid lua table type " + sbyt);
        }
    }

    private static byte getKeyByte(Object o) {
        if (o instanceof String) {
            return 0;
        } else {
            return (byte)(o instanceof Double ? 1 : -1);
        }
    }

    private static byte getValueByte(Object o) {
        if (o instanceof String) {
            return 0;
        } else if (o instanceof Double) {
            return 1;
        } else if (o instanceof Boolean) {
            return 3;
        } else if (o instanceof KahluaTableImpl) {
            return 2;
        } else if (o instanceof InventoryItem) {
            return 4;
        } else {
            return (byte)(o instanceof IsoDirections ? 5 : -1);
        }
    }

    public static boolean canSave(Object key, Object value) {
        return getKeyByte(key) != -1 && getValueByte(value) != -1;
    }
}
