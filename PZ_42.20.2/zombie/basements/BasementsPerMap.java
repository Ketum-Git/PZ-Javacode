// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.basements;

import java.util.ArrayList;
import java.util.HashMap;

public final class BasementsPerMap {
    final String mapId;
    final ArrayList<BasementDefinition> basementDefinitions = new ArrayList<>();
    final HashMap<String, BasementDefinition> basementDefinitionByName = new HashMap<>();
    final ArrayList<BasementDefinition> basementAccessDefinitions = new ArrayList<>();
    final HashMap<String, BasementDefinition> basementAccessDefinitionByName = new HashMap<>();
    final ArrayList<BasementSpawnLocation> basementSpawnLocations = new ArrayList<>();

    public BasementsPerMap(String mapId) {
        this.mapId = mapId;
    }
}
