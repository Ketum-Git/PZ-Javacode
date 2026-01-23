// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;

@UsedFromLua
public enum FluidType {
    Water((byte)1),
    Petrol((byte)2),
    Alcohol((byte)3),
    TaintedWater((byte)4),
    Beer((byte)5),
    Whiskey((byte)6),
    SodaPop((byte)7),
    Coffee((byte)8),
    Tea((byte)9),
    Wine((byte)10),
    Bleach((byte)11),
    Blood((byte)12),
    Honey((byte)14),
    Mead((byte)15),
    Acid((byte)16),
    SpiffoJuice((byte)17),
    SecretFlavoring((byte)18),
    CarbonatedWater((byte)19),
    CowMilk((byte)20),
    SheepMilk((byte)21),
    CleaningLiquid((byte)22),
    AnimalBlood((byte)23),
    AnimalGrease((byte)24),
    Dye((byte)64),
    HairDye((byte)65),
    Paint((byte)66),
    PoisonWeak((byte)70),
    PoisonNormal((byte)71),
    PoisonStrong((byte)72),
    PoisonPotent((byte)73),
    AnimalMilk((byte)74),
    Modded((byte)127),
    None((byte)-1);

    private static final HashSet<String> fluidNames = new HashSet<>();
    private static final HashMap<Byte, FluidType> fluidIdMap = new HashMap<>();
    private static final HashMap<String, FluidType> fluidNameMap = new HashMap<>();
    private final byte id;
    private String lowerCache;

    private FluidType(final byte typeID) {
        this.id = typeID;
    }

    public byte getId() {
        return this.id;
    }

    public String toStringLower() {
        if (this.lowerCache != null) {
            return this.lowerCache;
        } else {
            this.lowerCache = this.toString().toLowerCase();
            return this.lowerCache;
        }
    }

    public static boolean containsNameLowercase(String name) {
        return fluidNames.contains(name.toLowerCase());
    }

    public static FluidType FromId(byte id) {
        return fluidIdMap.get(id);
    }

    public static FluidType FromNameLower(String name) {
        return fluidNameMap.get(name.toLowerCase());
    }

    public static ArrayList<String> getAllFluidName() {
        ArrayList<String> result = new ArrayList<>(fluidNames);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public String getDisplayName() {
        return Translator.getText("Fluid_Name_" + this.toString());
    }

    static {
        for (FluidType type : values()) {
            if (Core.debug && fluidIdMap.containsKey(type.id)) {
                throw new IllegalStateException("ID duplicate in FluidType");
            }

            fluidNames.add(type.toString().toLowerCase());
            fluidNameMap.put(type.toString().toLowerCase(), type);
            fluidIdMap.put(type.id, type);
        }
    }
}
