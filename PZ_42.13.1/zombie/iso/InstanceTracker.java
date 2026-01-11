// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import com.google.common.base.Utf8;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.util.StringUtils;

public abstract class InstanceTracker {
    public static final String DEFAULT = "";
    public static final String ITEMS = "Item Spawns";
    public static final String CONTAINERS = "Container Rolls";
    public static final String STATS = "Stats";
    private static final HashMap<String, TObjectIntHashMap<String>> InstanceGroups = new HashMap<>();

    public static int get(String group, String key) {
        return InstanceGroups.containsKey(group) ? InstanceGroups.get(group).get(key) : 0;
    }

    public static int get(String key) {
        return get("", key);
    }

    public static void set(String group, String key, int value) {
        if (group != null && key != null && !key.isBlank()) {
            InstanceGroups.putIfAbsent(group, new TObjectIntHashMap<>());
            InstanceGroups.get(group).put(key, value);
        }
    }

    public static void set(String key, int value) {
        set("", key, value);
    }

    public static void adj(String group, String key, int value) {
        InstanceGroups.putIfAbsent(group, new TObjectIntHashMap<>());
        InstanceGroups.get(group).put(key, PZMath.clamp(InstanceGroups.get(group).get(key) + value, Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    public static void adj(String key, int value) {
        adj("", key, value);
    }

    public static void inc(String group, String key) {
        adj(group, key, 1);
    }

    public static void inc(String key) {
        adj("", key, 1);
    }

    public static void dec(String group, String key) {
        adj(group, key, -1);
    }

    public static void dec(String key) {
        adj("", key, -1);
    }

    public static List<String> sort(String group, InstanceTracker.Sort sort) {
        return switch (sort) {
            case NONE -> InstanceGroups.get(group).keySet().stream().toList();
            case KEY -> InstanceGroups.get(group).keySet().stream().sorted().toList();
            case COUNT -> InstanceGroups.get(group).keySet().stream().sorted(Comparator.comparingInt(InstanceGroups.get(group)::get).reversed()).toList();
        };
    }

    public static String exportGroup(String group, InstanceTracker.Format format, InstanceTracker.Sort sort) {
        if (!InstanceGroups.containsKey(group)) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            String newLine = System.lineSeparator();
            switch (format) {
                case TEXT:
                    sb.append("[%s] ; %d entries%s".formatted(group, InstanceGroups.get(group).size(), newLine));
                    sort(group, sort).forEach(key -> sb.append("%s = %d%s".formatted(key, get(group, key), newLine)));
                    break;
                case CSV:
                    sort(group, sort).forEach(key -> sb.append("%s,%s,%d%s".formatted(group, key, get(group, key), newLine)));
            }

            sb.append(newLine);
            return sb.toString();
        }
    }

    public static void exportFile(List<String> groups, String filename, InstanceTracker.Format format, InstanceTracker.Sort sort) {
        if (!filename.isBlank() && !StringUtils.containsDoubleDot(filename) && !new File(filename).isAbsolute()) {
            String path = ZomboidFileSystem.instance.getCacheDir() + File.separator + filename;
            StringBuilder sb = new StringBuilder();
            if (groups == null || groups.isEmpty()) {
                groups = InstanceGroups.keySet().stream().toList();
            }

            if (format == InstanceTracker.Format.CSV) {
                sb.append("group,key,count").append(System.lineSeparator());
            }

            groups.forEach(group -> sb.append(exportGroup(group, format, sort)));
            File file = new File(path);

            try (
                FileWriter fr = new FileWriter(file);
                BufferedWriter br = new BufferedWriter(fr);
            ) {
                br.write(sb.toString());
            } catch (Exception var15) {
                ExceptionLogger.logException(var15);
            }
        }
    }

    public static void save() {
        if (!GameClient.client && !Core.getInstance().isNoSave()) {
            try {
                int[] size = new int[]{8};
                InstanceGroups.forEach((group, logs) -> {
                    size[0] += 6 + Utf8.encodedLength(group);
                    logs.forEachKey(key -> {
                        size[0] += 6 + Utf8.encodedLength(key);
                        return true;
                    });
                });
                ByteBuffer bb = ByteBuffer.allocate(size[0]);

                try {
                    bb.putInt(240);
                    bb.putInt(InstanceGroups.size());
                    InstanceGroups.forEach((group, logs) -> {
                        GameWindow.WriteString(bb, group);
                        bb.putInt(logs.size());
                        logs.forEachEntry((key, count) -> {
                            GameWindow.WriteString(bb, key);
                            bb.putInt(count);
                            return true;
                        });
                    });
                } catch (BufferOverflowException var4) {
                    DebugLog.General.debugln("InstanceTracker Overflow");
                    ExceptionLogger.logException(var4);
                    return;
                }

                bb.flip();
                File path = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("iTrack.bin"));
                FileOutputStream output = new FileOutputStream(path);
                output.getChannel().truncate(0L);
                output.write(bb.array(), 0, bb.limit());
                output.flush();
                output.close();
            } catch (Exception var5) {
                ExceptionLogger.logException(var5);
            }

            exportFile(List.of("Item Spawns", "Container Rolls"), "ItemTracker.log", InstanceTracker.Format.TEXT, InstanceTracker.Sort.KEY);
        }
    }

    public static void load() {
        InstanceGroups.clear();
        if (!Core.getInstance().isNoSave()) {
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("iTrack.bin");
            File path = new File(fileName);
            if (path.exists()) {
                try (FileInputStream inStream = new FileInputStream(path)) {
                    ByteBuffer bb = ByteBuffer.allocate((int)path.length());
                    bb.clear();
                    int len = inStream.read(bb.array());
                    bb.limit(len);
                    int worldVersion = bb.getInt();
                    int instanceGroupsSize = bb.getInt();

                    for (int i = 0; i < instanceGroupsSize; i++) {
                        String group = GameWindow.ReadString(bb);
                        int size = bb.getInt();
                        InstanceGroups.put(group, new TObjectIntHashMap<>());

                        for (int j = 0; j < size; j++) {
                            String key = GameWindow.ReadString(bb);
                            int count = bb.getInt();
                            set(group, key, count);
                        }
                    }
                } catch (Exception var15) {
                    ExceptionLogger.logException(var15);
                }
            }
        }
    }

    public static enum Format {
        TEXT,
        CSV;
    }

    public static enum Sort {
        NONE,
        KEY,
        COUNT;
    }
}
