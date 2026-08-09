// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.UsedFromLua;

@UsedFromLua
public abstract class Attribute {
    private static final HashMap<String, AttributeType> attributeTypeNameMap = new HashMap<>();
    private static final HashMap<Short, AttributeType> attributeTypeIdMap = new HashMap<>();
    private static final ArrayList<AttributeType> attributeTypes = new ArrayList<>();
    public static final AttributeType.Float TestQuality = registerType(new AttributeType.Float((short)103, "TestQuality", 0.0F));
    public static final AttributeType.Int TestUses = registerType(new AttributeType.Int((short)106, "TestUses", 5));
    public static final AttributeType.Float TestCondition = registerType(new AttributeType.Float((short)104, "TestCondition", 0.0F));
    public static final AttributeType.Bool TestBool = registerType(new AttributeType.Bool((short)105, "TestBool", false));
    public static final AttributeType.String TestString = registerType(new AttributeType.String((short)100, "TestString", "Test string for attribute."));
    public static final AttributeType.String TestString2 = registerType(new AttributeType.String((short)102, "TestString2", ""));
    public static final AttributeType.Enum<TestEnum> TestItemType = registerType(new AttributeType.Enum<>((short)121, "TestItemType", TestEnum.TestValueA));
    public static final AttributeType.EnumSet<TestEnum> TestCategories = registerType(new AttributeType.EnumSet<>((short)123, "TestCategories", TestEnum.class));
    public static final AttributeType.EnumStringSet<TestEnum> TestTags = registerType(new AttributeType.EnumStringSet<>((short)124, "TestTags", TestEnum.class));
    public static final AttributeType.Float Sharpness = registerType(
        new AttributeType.Float((short)0, "Sharpness", 1.0F, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, "")
    );
    public static final AttributeType.Int HeadCondition = registerType(
        new AttributeType.Int((short)1, "HeadCondition", 10, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, "")
    );
    public static final AttributeType.Int HeadConditionMax = registerType(
        new AttributeType.Int((short)2, "HeadConditionMax", 10, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, "")
    );
    public static final AttributeType.Int TimesHeadRepaired = registerType(
        new AttributeType.Int((short)4, "TimesHeadRepaired", 0, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, "")
    );
    public static final AttributeType.Int Quality = registerType(
        new AttributeType.Int((short)3, "Quality", 50, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, "")
    );
    public static final AttributeType.Int OriginX = registerType(
        new AttributeType.Int((short)5, "OriginX", 0, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, "")
    );
    public static final AttributeType.Int OriginY = registerType(
        new AttributeType.Int((short)6, "OriginY", 0, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, "")
    );
    public static final AttributeType.Int OriginZ = registerType(
        new AttributeType.Int((short)7, "OriginZ", 0, false, Attribute.UI.Display.Hidden, Attribute.UI.DisplayAsBar.Never, "")
    );

    private static <E extends AttributeType> E registerType(E type) {
        if (attributeTypeNameMap.containsKey(type.getName().toLowerCase())) {
            throw new RuntimeException("Attribute name registered twice id = '" + type.id() + ", attribute = '" + type.getName() + "'");
        } else if (attributeTypeIdMap.containsKey(type.id())) {
            throw new RuntimeException("Attribute id registered twice id = '" + type.id() + ", attribute = '" + type.getName() + "'");
        } else {
            attributeTypeIdMap.put(type.id(), type);
            attributeTypeNameMap.put(type.getName().toLowerCase(), type);
            attributeTypes.add(type);
            return type;
        }
    }

    public static AttributeType TypeFromName(String name) {
        return attributeTypeNameMap.get(name.toLowerCase());
    }

    public static AttributeType TypeFromId(short value) {
        return attributeTypeIdMap.get(value);
    }

    public static ArrayList<AttributeType> GetAllTypes() {
        return attributeTypes;
    }

    public static void init() {
    }

    static {
        String initVal = "Test string for attribute.";
        TestQuality.setBounds(0.0F, 100.0F);
        TestCondition.setBounds(0.0F, 1.0F);
        TestCategories.getInitialValue().add(TestEnum.TestValueC);
        Sharpness.setBounds(0.0F, 1.0F);
        HeadCondition.setBounds(0, 1000);
        HeadConditionMax.setBounds(0, 1000);
        Quality.setBounds(0, 100);
        TimesHeadRepaired.setBounds(0, 1000);
    }

    public static final class UI {
        public static enum Display {
            Visible,
            Hidden;
        }

        public static enum DisplayAsBar {
            Default,
            ForceIfBounds,
            Never;
        }
    }
}
