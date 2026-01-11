// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;

@UsedFromLua
public class AnimalPartsDefinitions {
    public static String getLeather(String animalType) {
        KahluaTableImpl animalDef = getAnimalDef(animalType);
        return animalDef == null ? null : animalDef.rawgetStr("leather");
    }

    public static ArrayList<AnimalPart> getAllPartsDef(String animalType) {
        KahluaTableImpl animalDef = getAnimalDef(animalType);
        return animalDef == null ? new ArrayList<>() : getDef(animalDef, "parts");
    }

    public static ArrayList<AnimalPart> getAllBonesDef(String animalType) {
        KahluaTableImpl animalDef = getAnimalDef(animalType);
        return animalDef == null ? new ArrayList<>() : getDef(animalDef, "bones");
    }

    public static ArrayList<AnimalPart> getDef(KahluaTableImpl def, String type) {
        ArrayList<AnimalPart> result = new ArrayList<>();
        KahluaTableImpl parts = (KahluaTableImpl)def.rawget(type);
        if (parts == null) {
            return result;
        } else {
            KahluaTableIterator allParts = parts.iterator();

            while (allParts.advance()) {
                KahluaTableImpl partDef = (KahluaTableImpl)allParts.getValue();
                AnimalPart part = new AnimalPart(partDef);
                result.add(part);
            }

            return result;
        }
    }

    public static KahluaTableImpl getAnimalDef(String animalType) {
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("AnimalPartsDefinitions");
        if (definitions == null) {
            return null;
        } else {
            KahluaTableImpl animals = (KahluaTableImpl)definitions.rawget("animals");
            if (animals == null) {
                return null;
            } else {
                KahluaTableIterator iterator = animals.iterator();

                while (iterator.advance()) {
                    String type = iterator.getKey().toString();
                    if (animalType.equals(type)) {
                        return (KahluaTableImpl)iterator.getValue();
                    }
                }

                return null;
            }
        }
    }
}
