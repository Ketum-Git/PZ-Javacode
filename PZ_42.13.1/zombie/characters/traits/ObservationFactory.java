// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.traits;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.interfaces.IListBoxItem;

@UsedFromLua
public final class ObservationFactory {
    public static HashMap<String, ObservationFactory.Observation> observationMap = new HashMap<>();

    public static void init() {
    }

    public static void setMutualExclusive(String a, String b) {
        observationMap.get(a).mutuallyExclusive.add(b);
        observationMap.get(b).mutuallyExclusive.add(a);
    }

    public static void addObservation(String type, String name, String desc) {
        observationMap.put(type, new ObservationFactory.Observation(type, name, desc));
    }

    public static ObservationFactory.Observation getObservation(String name) {
        return observationMap.containsKey(name) ? observationMap.get(name) : null;
    }

    @UsedFromLua
    public static class Observation implements IListBoxItem {
        private String traitId;
        private String name;
        private String description;
        public ArrayList<String> mutuallyExclusive = new ArrayList<>(0);

        public Observation(String tr, String name, String desc) {
            this.setTraitID(tr);
            this.setName(name);
            this.setDescription(desc);
        }

        @Override
        public String getLabel() {
            return this.getName();
        }

        @Override
        public String getLeftLabel() {
            return this.getName();
        }

        @Override
        public String getRightLabel() {
            return null;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTraitID() {
            return this.traitId;
        }

        public void setTraitID(String traitId) {
            this.traitId = traitId;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
