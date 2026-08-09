// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;

@UsedFromLua
public class AnimalGenomeDefinitions {
    public String name;
    public float currentValue;
    public HashMap<String, Float> ratios;
    public float minValue = 0.2F;
    public float maxValue = 0.6F;
    public static HashMap<String, AnimalGenomeDefinitions> fullGenomeDef;
    public static ArrayList<String> geneticDisorder;
    public boolean forcedValues;

    public static void loadGenomeDefinition() {
        fullGenomeDef = new HashMap<>();
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("AnimalGenomeDefinitions");
        if (definitions != null) {
            KahluaTableImpl genes = (KahluaTableImpl)definitions.rawget("genes");
            KahluaTableIterator iterator = genes.iterator();

            while (iterator.advance()) {
                AnimalGenomeDefinitions def = new AnimalGenomeDefinitions();
                String name = iterator.getKey().toString().toLowerCase();
                def.name = name;
                KahluaTableIterator it2 = ((KahluaTableImpl)iterator.getValue()).iterator();

                while (it2.advance()) {
                    String key = (String)it2.getKey();
                    Object value = it2.getValue();
                    String valueStr = value.toString().trim();
                    if ("minValue".equalsIgnoreCase(key)) {
                        def.minValue = Float.parseFloat(valueStr);
                    }

                    if ("maxValue".equalsIgnoreCase(key)) {
                        def.maxValue = Float.parseFloat(valueStr);
                    }

                    if ("forcedValues".equalsIgnoreCase(key)) {
                        def.forcedValues = Boolean.parseBoolean(valueStr);
                    }

                    if ("ratio".equalsIgnoreCase(key)) {
                        def.loadRatio((KahluaTableImpl)value);
                    }
                }

                fullGenomeDef.put(name, def);
            }

            KahluaTableImpl disorder = (KahluaTableImpl)definitions.rawget("geneticDisorder");
            geneticDisorder = new ArrayList<>();
            iterator = disorder.iterator();

            while (iterator.advance()) {
                String name = iterator.getKey().toString().toLowerCase();
                if (!geneticDisorder.contains(name)) {
                    geneticDisorder.add(name);
                }
            }
        }
    }

    private void loadRatio(KahluaTableImpl def) {
        this.ratios = new HashMap<>();
        KahluaTableIterator it = def.iterator();

        while (it.advance()) {
            String name = it.getKey().toString().toLowerCase();
            String valueStr = it.getValue().toString().trim();
            this.ratios.put(name, Float.parseFloat(valueStr));
        }
    }

    public static ArrayList<String> getGeneticDisorderList() {
        return geneticDisorder;
    }
}
