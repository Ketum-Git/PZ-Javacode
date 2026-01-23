// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedRanch;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;

public class RanchZoneDefinitions {
    public int chance;
    public String femaleType;
    public String maleType;
    public int minFemaleNb;
    public int maxFemaleNb;
    public int minMaleNb;
    public int maxMaleNb;
    public String forcedBreed;
    public int chanceForBaby;
    public static int totalChance;
    public String globalName;
    public ArrayList<RanchZoneDefinitions> possibleDef = new ArrayList<>();
    public static HashMap<String, ArrayList<RanchZoneDefinitions>> defs;
    public int maleChance;

    public static HashMap<String, ArrayList<RanchZoneDefinitions>> getDefs() {
        if (defs == null) {
            loadDefinitions();
        }

        return defs;
    }

    public static void loadDefinitions() {
        defs = new HashMap<>();
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("RanchZoneDefinitions");
        if (definitions != null) {
            KahluaTableImpl animals = (KahluaTableImpl)definitions.rawget("type");
            KahluaTableIterator iterator = animals.iterator();

            while (iterator.advance()) {
                RanchZoneDefinitions def = new RanchZoneDefinitions();
                KahluaTableIterator it2 = ((KahluaTableImpl)iterator.getValue()).iterator();
                String type = null;

                while (it2.advance()) {
                    String key = it2.getKey().toString();
                    Object value = it2.getValue();
                    String valueStr = value.toString().trim();
                    if ("type".equalsIgnoreCase(key)) {
                        type = valueStr;
                    }

                    if ("chance".equalsIgnoreCase(key)) {
                        def.chance = Float.valueOf(valueStr).intValue();
                        totalChance = totalChance + def.chance;
                    }

                    if ("minFemaleNb".equalsIgnoreCase(key)) {
                        def.minFemaleNb = Float.valueOf(valueStr).intValue();
                    }

                    if ("maleChance".equalsIgnoreCase(key)) {
                        def.maleChance = Float.valueOf(valueStr).intValue();
                    }

                    if ("globalName".equalsIgnoreCase(key)) {
                        def.globalName = valueStr;
                    }

                    if ("maxFemaleNb".equalsIgnoreCase(key)) {
                        def.maxFemaleNb = Float.valueOf(valueStr).intValue();
                    }

                    if ("minMaleNb".equalsIgnoreCase(key)) {
                        def.minMaleNb = Float.valueOf(valueStr).intValue();
                    }

                    if ("maxMaleNb".equalsIgnoreCase(key)) {
                        def.maxMaleNb = Float.valueOf(valueStr).intValue();
                    }

                    if ("chanceForBaby".equalsIgnoreCase(key)) {
                        def.chanceForBaby = Float.valueOf(valueStr).intValue();
                    }

                    if ("forcedBreed".equalsIgnoreCase(key)) {
                        def.forcedBreed = valueStr;
                    }

                    if ("femaleType".equalsIgnoreCase(key)) {
                        def.femaleType = valueStr;
                    }

                    if ("maleType".equalsIgnoreCase(key)) {
                        def.maleType = valueStr;
                    }

                    if ("possibleDef".equalsIgnoreCase(key)) {
                        def.possibleDef = def.loadPossibleDef(type, (KahluaTableImpl)value);
                    }
                }

                ArrayList<RanchZoneDefinitions> existingZone = defs.get(type);
                if (existingZone == null) {
                    existingZone = new ArrayList<>();
                }

                existingZone.add(def);
                defs.put(type, existingZone);
            }
        }
    }

    private ArrayList<RanchZoneDefinitions> loadPossibleDef(String type, KahluaTableImpl table) {
        ArrayList<RanchZoneDefinitions> result = new ArrayList<>();
        KahluaTableIterator it = table.iterator();

        while (it.advance()) {
            String posDef = it.getValue().toString().trim();
            if (defs.get(posDef) == null) {
                DebugLog.Animal.debugln("Couldn't find ranch definition " + posDef + " for definition " + type);
            } else {
                result.addAll(defs.get(posDef));
            }
        }

        return result;
    }
}
