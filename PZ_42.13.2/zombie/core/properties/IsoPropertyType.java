// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.properties;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import zombie.UsedFromLua;

@UsedFromLua
public final class IsoPropertyType {
    public static final String IsoType = "IsoType";
    public static final String IsMoveAble = "IsMoveAble";
    public static final String OpenTileOffset = "OpenTileOffset";
    public static final String WindowLocked = "WindowLocked";
    public static final String SmashedTileOffset = "SmashedTileOffset";
    public static final String GlassRemovedOffset = "GlassRemovedOffset";
    public static final String GarageDoor = "GarageDoor";
    public static final String DoubleDoor = "DoubleDoor";
    public static final String LightRadius = "LightRadius";
    public static final String RedLight = "lightR";
    public static final String GreenLight = "lightG";
    public static final String BlueLight = "lightB";
    public static final String ConnectX = "connectX";
    public static final String ConnectY = "connectY";
    public static final String Container = "container";
    public static final String CustomName = "CustomName";
    public static final String GroupName = "GroupName";
    public static final String ContainerCapacity = "ContainerCapacity";
    public static final String ContainerPosition = "ContainerPosition";
    public static final String Facing = "Facing";
    public static final String FuelAmount = "fuelAmount";
    public static final String WaterAmount = "waterAmount";
    public static final String MaximumWaterAmount = "waterMaxAmount";
    public static final String PropaneTank = "propaneTank";
    public static final String DamagedSprite = "DamagedSprite";
    public static final String CanAttachAnimal = "CanAttachAnimal";
    public static final String ContainerCloseSound = "ContainerCloseSound";
    public static final String ContainerOpenSound = "ContainerOpenSound";
    public static final String ContainerPutSound = "ContainerPutSound";
    public static final String ContainerTakeSound = "ContainerTakeSound";
    public static final String IsFridge = "IsFridge";
    public static final String doorTrans = "doorTrans";
    public static final String streetlight = "streetlight";
    public static final String FootstepMaterial = "FootstepMaterial";
    private static final HashMap<String, String> names = new HashMap<>();

    public static String lookup(String name) {
        return names.getOrDefault(name, name);
    }

    static {
        Field[] fields = IsoPropertyType.class.getFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())
                && Modifier.isPublic(field.getModifiers())
                && Modifier.isFinal(field.getModifiers())
                && field.getType().equals(String.class)) {
                names.put(field.getName(), field.getName());
            }
        }
    }
}
