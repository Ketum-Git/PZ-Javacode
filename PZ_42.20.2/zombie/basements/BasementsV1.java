// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.basements;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;

@UsedFromLua
public class BasementsV1 {
    public void addAccessDefinitions(String mapID, KahluaTable table) {
        BasementsPerMap basementsPerMap = Basements.getInstance().getOrCreatePerMap(mapID);
        KahluaTableIterator iterator = table.iterator();

        while (iterator.advance()) {
            String name = (String)iterator.getKey();
            KahluaTable definitionTable = (KahluaTable)iterator.getValue();
            int width = ((Double)definitionTable.rawget("width")).intValue();
            int height = ((Double)definitionTable.rawget("height")).intValue();
            int stairx = ((Double)definitionTable.rawget("stairx")).intValue();
            int stairy = ((Double)definitionTable.rawget("stairy")).intValue();
            boolean north = "N".equals(definitionTable.rawget("stairDir"));
            BasementDefinition basementDefinition = new BasementDefinition();
            basementDefinition.width = width;
            basementDefinition.height = height;
            basementDefinition.stairx = stairx;
            basementDefinition.stairy = stairy;
            basementDefinition.north = north;
            basementDefinition.name = name;
            basementsPerMap.basementAccessDefinitions.add(basementDefinition);
            basementsPerMap.basementAccessDefinitionByName.put(basementDefinition.name, basementDefinition);
        }
    }

    public void addBasementDefinitions(String mapID, KahluaTable table) {
        BasementsPerMap basementsPerMap = Basements.getInstance().getOrCreatePerMap(mapID);
        KahluaTableIterator iterator = table.iterator();

        while (iterator.advance()) {
            String name = (String)iterator.getKey();
            KahluaTable definitionTable = (KahluaTable)iterator.getValue();
            int width = ((Double)definitionTable.rawget("width")).intValue();
            int height = ((Double)definitionTable.rawget("height")).intValue();
            int stairx = ((Double)definitionTable.rawget("stairx")).intValue();
            int stairy = ((Double)definitionTable.rawget("stairy")).intValue();
            boolean north = "N".equals(definitionTable.rawget("stairDir"));
            BasementDefinition basementDefinition = new BasementDefinition();
            basementDefinition.width = width;
            basementDefinition.height = height;
            basementDefinition.stairx = stairx;
            basementDefinition.stairy = stairy;
            basementDefinition.north = north;
            basementDefinition.name = name;
            basementsPerMap.basementDefinitions.add(basementDefinition);
            basementsPerMap.basementDefinitionByName.put(basementDefinition.name, basementDefinition);
        }
    }

    public void addSpawnLocations(String mapID, KahluaTable table) {
        BasementsPerMap basementsPerMap = Basements.getInstance().getOrCreatePerMap(mapID);
        KahluaTableIterator iterator = table.iterator();

        while (iterator.advance()) {
            KahluaTable locationTable = (KahluaTable)iterator.getValue();
            int x = ((Double)locationTable.rawget("x")).intValue();
            int y = ((Double)locationTable.rawget("y")).intValue();
            boolean north = "N".equals(locationTable.rawget("stairDir"));
            BasementSpawnLocation basementSpawnLocation = new BasementSpawnLocation();
            basementSpawnLocation.x = x;
            basementSpawnLocation.y = y;
            if (locationTable.rawget("z") instanceof Double) {
                basementSpawnLocation.z = ((Double)locationTable.rawget("z")).intValue();
            }

            basementSpawnLocation.w = 1;
            basementSpawnLocation.h = 1;
            basementSpawnLocation.stairX = 0;
            basementSpawnLocation.stairY = 0;
            basementSpawnLocation.north = north;
            if (locationTable.rawget("choices") instanceof KahluaTable kahluaTable) {
                basementSpawnLocation.specificBasement = new ArrayList<>();
                KahluaTableIterator iterator2 = kahluaTable.iterator();

                while (iterator2.advance()) {
                    if (iterator2.getValue() instanceof String s) {
                        String basementName = s.trim();
                        if (!basementName.isEmpty()) {
                            basementSpawnLocation.specificBasement.add(basementName);
                        }
                    }
                }
            }

            basementSpawnLocation.access = locationTable.getString("access");
            basementsPerMap.basementSpawnLocations.add(basementSpawnLocation);
        }
    }

    public BasementSpawnLocation registerBasementSpawnLocation(
        String mapID, String name, String type, int x, int y, int z, int width, int height, KahluaTable properties
    ) {
        BasementsPerMap basementsPerMap = Basements.getInstance().getOrCreatePerMap(mapID);
        BasementSpawnLocation bsl = new BasementSpawnLocation();
        bsl.x = x;
        bsl.y = y;
        bsl.z = z;
        bsl.w = width;
        bsl.h = height;
        bsl.north = true;
        bsl.stairX = 0;
        bsl.stairY = 0;
        if (properties != null) {
            if (properties.rawget("StairX") instanceof Double) {
                bsl.stairX = ((KahluaTableImpl)properties).rawgetInt("StairX");
            }

            if (properties.rawget("StairY") instanceof Double) {
                bsl.stairY = ((KahluaTableImpl)properties).rawgetInt("StairY");
            }

            if (properties.rawget("StairDirection") instanceof String) {
                bsl.north = "N".equals(properties.rawget("StairDirection"));
            }

            if (properties.rawget("Access") instanceof String) {
                bsl.access = properties.getString("Access");
            }
        }

        basementsPerMap.basementSpawnLocations.add(bsl);
        return bsl;
    }
}
