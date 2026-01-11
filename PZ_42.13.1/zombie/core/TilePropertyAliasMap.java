// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import zombie.core.properties.IsoPropertyType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;

public final class TilePropertyAliasMap {
    public static final TilePropertyAliasMap instance = new TilePropertyAliasMap();
    public final HashMap<String, Integer> propertyToId = new HashMap<>();
    public final ArrayList<TilePropertyAliasMap.TileProperty> properties = new ArrayList<>();

    public void Generate(HashMap<String, ArrayList<String>> propertyValueMap) {
        this.properties.clear();
        this.propertyToId.clear();

        for (Entry<String, ArrayList<String>> stringArrayListEntry : propertyValueMap.entrySet()) {
            this.register(stringArrayListEntry.getKey(), stringArrayListEntry.getValue());
        }

        this.generateEntityProperties();
    }

    private void generateEntityProperties() {
        ArrayList<String> value = new ArrayList<>();

        for (GameEntityScript script : ScriptManager.instance.getAllGameEntities()) {
            value.add(script.getName());
        }

        this.register("EntityScriptName", value);
    }

    private void register(String propKey, ArrayList<String> propValues) {
        String property = IsoPropertyType.lookup(propKey);
        if (this.properties.size() >= 32767) {
            throw new RuntimeException("too many properties defined");
        } else if (propValues.size() > 32767) {
            throw new RuntimeException("too many property values defined for " + propKey);
        } else {
            this.propertyToId.put(property, this.properties.size());
            TilePropertyAliasMap.TileProperty newProp = new TilePropertyAliasMap.TileProperty();
            this.properties.add(newProp);
            newProp.propertyName = property;
            newProp.possibleValues.addAll(propValues);
            ArrayList<String> possibleValues = newProp.possibleValues;

            for (int i = 0; i < possibleValues.size(); i++) {
                String possibleValue = possibleValues.get(i);
                newProp.idMap.put(possibleValue, i);
            }
        }
    }

    public int getIDFromPropertyName(String name) {
        return this.propertyToId.getOrDefault(name, -1);
    }

    public int getIDFromPropertyValue(int property, String value) {
        TilePropertyAliasMap.TileProperty tileProperty = this.properties.get(property);
        if (tileProperty.possibleValues.isEmpty()) {
            return 0;
        } else {
            return !tileProperty.idMap.containsKey(value) ? 0 : tileProperty.idMap.get(value);
        }
    }

    public String getPropertyValueString(int property, int value) {
        TilePropertyAliasMap.TileProperty tileProperty = this.properties.get(property);
        return tileProperty.possibleValues.isEmpty() ? "" : tileProperty.possibleValues.get(value);
    }

    public static final class TileProperty {
        public String propertyName;
        public final ArrayList<String> possibleValues = new ArrayList<>();
        public final HashMap<String, Integer> idMap = new HashMap<>();
    }
}
