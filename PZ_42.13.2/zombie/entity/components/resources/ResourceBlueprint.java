// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.util.enums.EnumBitStore;
import zombie.util.StringUtils;

@UsedFromLua
public class ResourceBlueprint {
    private static final ConcurrentLinkedDeque<ResourceBlueprint> pool = new ConcurrentLinkedDeque<>();
    public static final String serialElementSeparator = "@";
    public static final String serialSubSeparator = ":";
    private static final String str_null = "null";
    private static final String stackAnyItemIdentifier = "StackAny";
    private static final int initialSerialLength = 64;
    private String id;
    private ResourceType type;
    private ResourceIO io = ResourceIO.Any;
    private float capacity = 1.0F;
    private ResourceChannel channel = ResourceChannel.NO_CHANNEL;
    private final EnumBitStore<ResourceFlag> resourceFlags = EnumBitStore.noneOf(ResourceFlag.class);
    private String filter;
    private boolean stackAnyItem;
    private static final ThreadLocal<StringBuilder> threadLocalSb = ThreadLocal.withInitial(StringBuilder::new);

    private static ResourceBlueprint alloc_empty() {
        ResourceBlueprint o = pool.poll();
        if (o == null) {
            o = new ResourceBlueprint();
        }

        return o;
    }

    public static ResourceBlueprint alloc(
        String id, ResourceType type, ResourceIO io, float capacity, String filter, ResourceChannel channel, EnumBitStore<ResourceFlag> flags
    ) {
        ResourceBlueprint bp = alloc_empty();
        bp.id = Objects.requireNonNull(id);
        bp.type = Objects.requireNonNull(type);
        bp.io = Objects.requireNonNull(io);
        bp.capacity = capacity;
        bp.channel = Objects.requireNonNull(channel);
        bp.resourceFlags.addAll(flags);
        bp.filter = filter;
        return bp;
    }

    public static void release(ResourceBlueprint bp) {
        bp.reset();

        assert !Core.debug || pool.contains(bp) : "Object already in pool.";

        pool.offer(bp);
    }

    private ResourceBlueprint() {
    }

    public String getId() {
        return this.id;
    }

    public ResourceType getType() {
        return this.type;
    }

    public ResourceIO getIO() {
        return this.io;
    }

    public float getCapacity() {
        return this.capacity;
    }

    public boolean isStackAnyItem() {
        return this.stackAnyItem;
    }

    public ResourceChannel getChannel() {
        return this.channel;
    }

    public boolean hasFlag(ResourceFlag flag) {
        return this.resourceFlags.contains(flag);
    }

    public int getFlagBits() {
        return this.resourceFlags.getBits();
    }

    public String getFilter() {
        return this.filter;
    }

    private void reset() {
        this.id = null;
        this.type = ResourceType.Any;
        this.io = ResourceIO.Any;
        this.capacity = 1.0F;
        this.stackAnyItem = false;
        this.channel = ResourceChannel.NO_CHANNEL;
        this.resourceFlags.clear();
        this.filter = null;
    }

    private static String checkCharacters(String s) {
        if (!Core.debug || !s.contains("@") && !s.contains(":")) {
            return s;
        } else {
            throw new IllegalArgumentException("String contains illegal characters.");
        }
    }

    public static String Serialize(ResourceBlueprint bp) {
        return Serialize(bp.id, bp.type, bp.io, bp.capacity, bp.stackAnyItem, bp.filter, bp.channel, bp.resourceFlags);
    }

    public static String Serialize(
        String id,
        ResourceType type,
        ResourceIO io,
        float capacity,
        boolean stackAnyItem,
        String filter,
        ResourceChannel channel,
        EnumBitStore<ResourceFlag> flags
    ) {
        if (StringUtils.isNullOrWhitespace(id)) {
            throw new IllegalArgumentException("Id cannot be null or whitespace");
        } else if (type == ResourceType.Energy && StringUtils.isNullOrWhitespace(filter)) {
            throw new IllegalArgumentException("Energy requires filter set.");
        } else {
            StringBuilder sb = threadLocalSb.get();
            sb.setLength(0);
            sb.append(checkCharacters(id));
            sb.append("@");
            sb.append(type.toString());
            sb.append("@");
            sb.append(io.toString());
            sb.append("@");
            if (type == ResourceType.Item) {
                sb.append((int)capacity);
            } else {
                sb.append(capacity);
            }

            if (stackAnyItem) {
                sb.append("@");
                sb.append("StackAny");
            }

            if (channel != null && channel != ResourceChannel.NO_CHANNEL || flags != null && !flags.isEmpty() || !StringUtils.isNullOrWhitespace(filter)) {
                sb.append("@");
                if (!StringUtils.isNullOrWhitespace(filter)) {
                    sb.append(checkCharacters(filter));
                } else {
                    sb.append("null");
                }

                sb.append("@");
                if (channel != null && channel != ResourceChannel.NO_CHANNEL) {
                    sb.append(channel);
                } else {
                    sb.append("null");
                }

                sb.append("@");
                if (flags != null && !flags.isEmpty()) {
                    sb.append(flags.getBits());
                } else {
                    sb.append("null");
                }
            }

            String s = sb.toString();
            if (Core.debug && s.length() > 64) {
                DebugLog.log("Created serial surpassed initial serial length: " + s);
            }

            return s;
        }
    }

    public static ResourceBlueprint DeserializeFromScript(String serial) {
        ResourceBlueprint bp = alloc_empty();
        return Deserialize(bp, serial, true);
    }

    public static ResourceBlueprint Deserialize(String serial) {
        ResourceBlueprint bp = alloc_empty();
        return Deserialize(bp, serial);
    }

    public static ResourceBlueprint Deserialize(ResourceBlueprint bp, String serial) {
        return Deserialize(bp, serial, false);
    }

    public static ResourceBlueprint Deserialize(ResourceBlueprint bp, String serial, boolean flagsAsString) {
        String[] elements = serial.split("@");
        if (elements.length != 4 && elements.length != 5 && elements.length != 7) {
            throw new IllegalArgumentException("Serial string has invalid number of elements.");
        } else {
            bp.reset();
            bp.id = elements[0];
            bp.type = ResourceType.valueOf(elements[1]);
            bp.io = ResourceIO.valueOf(elements[2]);
            if (bp.type == ResourceType.Item) {
                bp.capacity = Integer.parseInt(elements[3]);
            } else {
                bp.capacity = Float.parseFloat(elements[3]);
            }

            if (elements.length == 5 && elements[4].equalsIgnoreCase("StackAny")) {
                bp.stackAnyItem = true;
            }

            if (elements.length == 7) {
                String filterStr = elements[4];
                if (!"null".equalsIgnoreCase(filterStr)) {
                    bp.filter = filterStr;
                }

                String channelStr = elements[5];
                if (!"null".equalsIgnoreCase(channelStr)) {
                    bp.channel = ResourceChannel.valueOf(channelStr);
                }

                String flagsStr = elements[6];
                if (!"null".equalsIgnoreCase(flagsStr)) {
                    if (flagsAsString) {
                        String[] flags = flagsStr.split(":");

                        for (int i = 0; i < flags.length; i++) {
                            ResourceFlag flag = ResourceFlag.valueOf(flags[i]);
                            bp.resourceFlags.add(flag);
                        }
                    } else {
                        int bits = Integer.parseInt(flagsStr);
                        bp.resourceFlags.setBits(bits);
                    }
                }
            }

            return bp;
        }
    }
}
