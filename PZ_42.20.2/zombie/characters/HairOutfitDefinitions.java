// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import java.util.Arrays;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;

public final class HairOutfitDefinitions {
    public static final HairOutfitDefinitions instance = new HairOutfitDefinitions();
    public boolean dirty = true;
    public String hairStyle;
    public int minWorldAge;
    public final ArrayList<HairOutfitDefinitions.HaircutDefinition> haircutDefinition = new ArrayList<>();
    public final ArrayList<HairOutfitDefinitions.HaircutOutfitDefinition> outfitDefinition = new ArrayList<>();
    private final ThreadLocal<ArrayList<HairStyle>> tempHairStyles = ThreadLocal.withInitial(ArrayList::new);

    public void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.init();
        }
    }

    private void init() {
        this.haircutDefinition.clear();
        this.outfitDefinition.clear();
        KahluaTableImpl definition = (KahluaTableImpl)LuaManager.env.rawget("HairOutfitDefinitions");
        if (definition != null) {
            if (definition.rawget("haircutDefinition") instanceof KahluaTableImpl hairForWorldAgeDefinition) {
                KahluaTableIterator var7 = hairForWorldAgeDefinition.iterator();

                while (var7.advance()) {
                    if (var7.getValue() instanceof KahluaTableImpl hairForWorldAge) {
                        HairOutfitDefinitions.HaircutDefinition def = new HairOutfitDefinitions.HaircutDefinition(
                            hairForWorldAge.rawgetStr("name"),
                            hairForWorldAge.rawgetInt("minWorldAge"),
                            new ArrayList<>(Arrays.asList(hairForWorldAge.rawgetStr("onlyFor").split(",")))
                        );
                        this.haircutDefinition.add(def);
                    }
                }

                if (definition.rawget("haircutOutfitDefinition") instanceof KahluaTableImpl hairForOutfitDefinition) {
                    var7 = hairForOutfitDefinition.iterator();

                    while (var7.advance()) {
                        if (var7.getValue() instanceof KahluaTableImpl hairForOutfit) {
                            HairOutfitDefinitions.HaircutOutfitDefinition def = new HairOutfitDefinitions.HaircutOutfitDefinition(
                                hairForOutfit.rawgetStr("outfit"),
                                initStringChance(hairForOutfit.rawgetStr("haircut")),
                                initStringChance(hairForOutfit.rawgetStr("femaleHaircut")),
                                initStringChance(hairForOutfit.rawgetStr("maleHaircut")),
                                initStringChance(hairForOutfit.rawgetStr("beard")),
                                initStringChance(hairForOutfit.rawgetStr("haircutColor"))
                            );
                            this.outfitDefinition.add(def);
                        }
                    }
                }
            }
        }
    }

    public boolean isHaircutValid(String outfit, String haircut) {
        instance.checkDirty();
        if (StringUtils.isNullOrEmpty(outfit)) {
            return true;
        } else {
            for (int i = 0; i < instance.haircutDefinition.size(); i++) {
                HairOutfitDefinitions.HaircutDefinition def = instance.haircutDefinition.get(i);
                if (def.hairStyle.equals(haircut)) {
                    if (!def.onlyFor.contains(outfit)) {
                        return false;
                    }

                    if (IsoWorld.instance.getWorldAgeDays() < def.minWorldAge) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public void getValidHairStylesForOutfit(String outfit, ArrayList<HairStyle> hairStyles, ArrayList<HairStyle> result) {
        result.clear();

        for (int i = 0; i < hairStyles.size(); i++) {
            HairStyle hairStyle = hairStyles.get(i);
            if (!hairStyle.isNoChoose() && this.isHaircutValid(outfit, hairStyle.name)) {
                result.add(hairStyle);
            }
        }
    }

    public String getRandomHaircut(String outfit, ArrayList<HairStyle> hairList) {
        ArrayList<HairStyle> validStyles = this.tempHairStyles.get();
        this.getValidHairStylesForOutfit(outfit, hairList, validStyles);
        if (validStyles.isEmpty()) {
            return "";
        } else {
            String haircut = OutfitRNG.pickRandom(validStyles).name;
            boolean done = false;

            for (int i = 0; i < instance.outfitDefinition.size(); i++) {
                HairOutfitDefinitions.HaircutOutfitDefinition outfitDef = instance.outfitDefinition.get(i);
                if (outfitDef.outfit.equals(outfit) && outfitDef.haircutChance != null) {
                    return this.getRandomHaircutFromOutfitDef(outfitDef, haircut, validStyles);
                }
            }

            return haircut;
        }
    }

    public String getRandomHaircutFromOutfitDef(HairOutfitDefinitions.HaircutOutfitDefinition outfitDef, String haircut, ArrayList<HairStyle> validStyles) {
        float choice = OutfitRNG.Next(0.0F, 100.0F);
        float subtotal = 0.0F;

        for (int j = 0; j < outfitDef.haircutChance.size(); j++) {
            HairOutfitDefinitions.StringChance stringChance = outfitDef.haircutChance.get(j);
            subtotal += stringChance.chance;
            if (choice < subtotal) {
                haircut = stringChance.str;
                if ("null".equalsIgnoreCase(stringChance.str)) {
                    haircut = "";
                }

                if ("random".equalsIgnoreCase(stringChance.str)) {
                    haircut = OutfitRNG.pickRandom(validStyles).name;
                }

                return haircut;
            }
        }

        return haircut;
    }

    public String getRandomFemaleHaircut(String outfit, ArrayList<HairStyle> hairList) {
        ArrayList<HairStyle> validStyles = this.tempHairStyles.get();
        this.getValidHairStylesForOutfit(outfit, hairList, validStyles);
        if (validStyles.isEmpty()) {
            return "";
        } else {
            String haircut = OutfitRNG.pickRandom(validStyles).name;
            boolean done = false;

            for (int i = 0; i < instance.outfitDefinition.size() && !done; i++) {
                HairOutfitDefinitions.HaircutOutfitDefinition outfitDef = instance.outfitDefinition.get(i);
                if (outfitDef.outfit.equals(outfit) && outfitDef.femaleHaircutChance != null) {
                    float choice = OutfitRNG.Next(0.0F, 100.0F);
                    float subtotal = 0.0F;

                    for (int j = 0; j < outfitDef.femaleHaircutChance.size(); j++) {
                        HairOutfitDefinitions.StringChance stringChance = outfitDef.femaleHaircutChance.get(j);
                        subtotal += stringChance.chance;
                        if (choice < subtotal) {
                            haircut = stringChance.str;
                            if ("null".equalsIgnoreCase(stringChance.str)) {
                                haircut = "";
                            }

                            if ("random".equalsIgnoreCase(stringChance.str)) {
                                haircut = OutfitRNG.pickRandom(validStyles).name;
                            }

                            done = true;
                            break;
                        }
                    }
                } else if (outfitDef.outfit.equals(outfit) && outfitDef.femaleHaircutChance == null && outfitDef.haircutChance != null) {
                    return this.getRandomHaircutFromOutfitDef(outfitDef, haircut, validStyles);
                }
            }

            return haircut;
        }
    }

    public String getRandomMaleHaircut(String outfit, ArrayList<HairStyle> hairList) {
        ArrayList<HairStyle> validStyles = this.tempHairStyles.get();
        this.getValidHairStylesForOutfit(outfit, hairList, validStyles);
        if (validStyles.isEmpty()) {
            return "";
        } else {
            String haircut = OutfitRNG.pickRandom(validStyles).name;
            boolean done = false;

            for (int i = 0; i < instance.outfitDefinition.size() && !done; i++) {
                HairOutfitDefinitions.HaircutOutfitDefinition outfitDef = instance.outfitDefinition.get(i);
                if (outfitDef.outfit.equals(outfit) && outfitDef.maleHaircutChance != null) {
                    float choice = OutfitRNG.Next(0.0F, 100.0F);
                    float subtotal = 0.0F;

                    for (int j = 0; j < outfitDef.maleHaircutChance.size(); j++) {
                        HairOutfitDefinitions.StringChance stringChance = outfitDef.maleHaircutChance.get(j);
                        subtotal += stringChance.chance;
                        if (choice < subtotal) {
                            haircut = stringChance.str;
                            if ("null".equalsIgnoreCase(stringChance.str)) {
                                haircut = "";
                            }

                            if ("random".equalsIgnoreCase(stringChance.str)) {
                                haircut = OutfitRNG.pickRandom(validStyles).name;
                            }

                            done = true;
                            break;
                        }
                    }
                } else if (outfitDef.outfit.equals(outfit) && outfitDef.maleHaircutChance == null && outfitDef.haircutChance != null) {
                    return this.getRandomHaircutFromOutfitDef(outfitDef, haircut, validStyles);
                }
            }

            return haircut;
        }
    }

    public ImmutableColor getRandomHaircutColor(String outfit) {
        ImmutableColor result = SurvivorDesc.HairCommonColors.get(OutfitRNG.Next(SurvivorDesc.HairCommonColors.size()));
        String strColor = null;
        boolean done = false;

        for (int i = 0; i < instance.outfitDefinition.size() && !done; i++) {
            HairOutfitDefinitions.HaircutOutfitDefinition outfitDef = instance.outfitDefinition.get(i);
            if (outfitDef.outfit.equals(outfit) && outfitDef.haircutColor != null) {
                float choice = OutfitRNG.Next(0.0F, 100.0F);
                float subtotal = 0.0F;

                for (int j = 0; j < outfitDef.haircutColor.size(); j++) {
                    HairOutfitDefinitions.StringChance stringChance = outfitDef.haircutColor.get(j);
                    subtotal += stringChance.chance;
                    if (choice < subtotal) {
                        strColor = stringChance.str;
                        if ("random".equalsIgnoreCase(stringChance.str)) {
                            result = SurvivorDesc.HairCommonColors.get(OutfitRNG.Next(SurvivorDesc.HairCommonColors.size()));
                            strColor = null;
                        }

                        done = true;
                        break;
                    }
                }
            }
        }

        if (!StringUtils.isNullOrEmpty(strColor)) {
            String[] colorTable = strColor.split(",");
            result = new ImmutableColor(Float.parseFloat(colorTable[0]), Float.parseFloat(colorTable[1]), Float.parseFloat(colorTable[2]));
        }

        return result;
    }

    public String getRandomBeard(String outfit, ArrayList<BeardStyle> beardList) {
        String beard = OutfitRNG.pickRandom(beardList).name;
        boolean done = false;

        for (int i = 0; i < instance.outfitDefinition.size() && !done; i++) {
            HairOutfitDefinitions.HaircutOutfitDefinition outfitDef = instance.outfitDefinition.get(i);
            if (outfitDef.outfit.equals(outfit) && outfitDef.beardChance != null) {
                float choice = OutfitRNG.Next(0.0F, 100.0F);
                float subtotal = 0.0F;

                for (int j = 0; j < outfitDef.beardChance.size(); j++) {
                    HairOutfitDefinitions.StringChance stringChance = outfitDef.beardChance.get(j);
                    subtotal += stringChance.chance;
                    if (choice < subtotal) {
                        beard = stringChance.str;
                        if ("null".equalsIgnoreCase(stringChance.str)) {
                            beard = "";
                        }

                        if ("random".equalsIgnoreCase(stringChance.str)) {
                            beard = OutfitRNG.pickRandom(beardList).name;
                        }

                        done = true;
                        break;
                    }
                }
            }
        }

        return beard;
    }

    private static ArrayList<HairOutfitDefinitions.StringChance> initStringChance(String styles) {
        if (StringUtils.isNullOrWhitespace(styles)) {
            return null;
        } else {
            ArrayList<HairOutfitDefinitions.StringChance> result = new ArrayList<>();
            String[] split = styles.split(";");
            int totalChance = 0;

            for (String style : split) {
                String[] splitStyle = style.split(":");
                HairOutfitDefinitions.StringChance stringChance = new HairOutfitDefinitions.StringChance();
                stringChance.str = splitStyle[0];
                stringChance.chance = Float.parseFloat(splitStyle[1]);
                totalChance = (int)(totalChance + stringChance.chance);
                result.add(stringChance);
            }

            if (totalChance < 100) {
                HairOutfitDefinitions.StringChance stringChance = new HairOutfitDefinitions.StringChance();
                stringChance.str = "random";
                stringChance.chance = 100 - totalChance;
                result.add(stringChance);
            }

            return result;
        }
    }

    public static final class HaircutDefinition {
        public String hairStyle;
        public int minWorldAge;
        public ArrayList<String> onlyFor;

        public HaircutDefinition(String hairStyle, int minWorldAge, ArrayList<String> onlyFor) {
            this.hairStyle = hairStyle;
            this.minWorldAge = minWorldAge;
            this.onlyFor = onlyFor;
        }
    }

    public static final class HaircutOutfitDefinition {
        public String outfit;
        public ArrayList<HairOutfitDefinitions.StringChance> haircutChance;
        public ArrayList<HairOutfitDefinitions.StringChance> femaleHaircutChance;
        public ArrayList<HairOutfitDefinitions.StringChance> maleHaircutChance;
        public ArrayList<HairOutfitDefinitions.StringChance> beardChance;
        public ArrayList<HairOutfitDefinitions.StringChance> haircutColor;

        public HaircutOutfitDefinition(
            String outfit,
            ArrayList<HairOutfitDefinitions.StringChance> haircutChance,
            ArrayList<HairOutfitDefinitions.StringChance> femaleHaircutChance,
            ArrayList<HairOutfitDefinitions.StringChance> maleHaircutChance,
            ArrayList<HairOutfitDefinitions.StringChance> beardChance,
            ArrayList<HairOutfitDefinitions.StringChance> haircutColor
        ) {
            this.outfit = outfit;
            this.haircutChance = haircutChance;
            this.femaleHaircutChance = femaleHaircutChance;
            this.maleHaircutChance = maleHaircutChance;
            this.beardChance = beardChance;
            this.haircutColor = haircutColor;
        }
    }

    private static final class StringChance {
        String str;
        float chance;
    }
}
