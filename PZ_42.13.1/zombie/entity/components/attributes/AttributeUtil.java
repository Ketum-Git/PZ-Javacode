// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.util.enums.IOEnum;
import zombie.inventory.InventoryItem;
import zombie.util.StringUtils;

@UsedFromLua
public class AttributeUtil {
    public static final String enum_prefix = "enum.";
    private static final ArrayDeque<ArrayList<InventoryItem>> itemListPool = new ArrayDeque<>();
    private static final ArrayDeque<ArrayList<Double>> doubleListPool = new ArrayDeque<>();

    public static boolean isEnumString(String s) {
        return StringUtils.startsWithIgnoreCase(s, "enum.");
    }

    private static String getSanitizedEnumString(String s) {
        Objects.requireNonNull(s);
        if (s.toLowerCase().startsWith("enum.")) {
            return s.substring("enum.".length());
        } else {
            throw new IllegalArgumentException("Valid enum string should start with 'enum.'");
        }
    }

    public static <E extends Enum<E> & IOEnum> E enumValueFromScriptString(Class<E> enumClass, String s) {
        try {
            return Enum.valueOf(enumClass, getSanitizedEnumString(s));
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public static <E extends Enum<E> & IOEnum> E tryEnumValueFromScriptString(Class<E> enumClass, String s) {
        try {
            return Enum.valueOf(enumClass, getSanitizedEnumString(s));
        } catch (Exception var3) {
            return null;
        }
    }

    public static ArrayList<InventoryItem> allocItemList() {
        ArrayList<InventoryItem> list = itemListPool.poll();
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public static void releaseItemList(ArrayList<InventoryItem> list) {
        list.clear();

        assert !itemListPool.contains(list);

        itemListPool.offer(list);
    }

    public static ArrayList<InventoryItem> getItemsFromList(String itemString, ArrayList<InventoryItem> sources, ArrayList<InventoryItem> outputlist) {
        for (int i = 0; i < sources.size(); i++) {
            InventoryItem item = sources.get(i);
            boolean hasDot = itemString.contains(".");
            if (hasDot && item.getFullType().equalsIgnoreCase(itemString) || !hasDot && item.getType().equalsIgnoreCase(itemString)) {
                outputlist.add(item);
            }
        }

        return outputlist;
    }

    public static float getAttributeAverage(ArrayList<InventoryItem> items, AttributeType attribute) {
        float result = 0.0F;

        try {
            if (!attribute.isNumeric()) {
                DebugLog.General.warn("Attribute '" + attribute + " is not numeric.");
                return 0.0F;
            } else {
                AttributeType.Numeric numAttribute = (AttributeType.Numeric)attribute;
                float count = items.size();

                for (int i = 0; i < items.size(); i++) {
                    result += items.get(i).getAttributes().getFloatValue(numAttribute);
                }

                return result / count;
            }
        } catch (Exception var6) {
            var6.printStackTrace();
            return result;
        }
    }

    public static float convertAttributeToUnit(InventoryItem item, AttributeType attribute) {
        float unit = 0.0F;

        try {
            AttributeType.Numeric numAttribute = (AttributeType.Numeric)attribute;
            if (!numAttribute.hasBounds()) {
                throw new Exception("Attribute '" + attribute + " has no bounds, cannot convert.");
            }

            float min = numAttribute.getMin().floatValue();
            float max = numAttribute.getMax().floatValue();
            float value = item.getAttributes().getFloatValue(numAttribute);
            unit = (value - min) / (max - min);
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        return unit;
    }

    public static float convertAttribute(InventoryItem item, AttributeType attribute, AttributeType target) {
        float result = 0.0F;

        try {
            float unit = convertAttributeToUnit(item, attribute);
            AttributeType.Numeric numTarget = (AttributeType.Numeric)target;
            if (!numTarget.hasBounds()) {
                throw new Exception("Target attribute '" + target + " has no bounds, cannot convert.");
            }

            float min = numTarget.getMin().floatValue();
            float max = numTarget.getMax().floatValue();
            result = min + (max - min) * unit;
        } catch (Exception var8) {
            var8.printStackTrace();
        }

        return result;
    }

    public static float convertAttributeToRange(InventoryItem item, AttributeType attribute, float rangeMin, float rangeMax) {
        float result = 0.0F;

        try {
            float unit = convertAttributeToUnit(item, attribute);
            result = rangeMin + (rangeMax - rangeMin) * unit;
        } catch (Exception var6) {
            var6.printStackTrace();
        }

        return result;
    }

    public static ArrayList<Double> allocDoubleList() {
        ArrayList<Double> list = doubleListPool.poll();
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public static void releaseDoubleList(ArrayList<Double> list) {
        list.clear();

        assert !doubleListPool.contains(list);

        doubleListPool.offer(list);
    }
}
