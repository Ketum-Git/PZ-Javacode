// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.math.PZMath;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMap;
import zombie.worldMap.WorldMapRenderer;

@UsedFromLua
public class WorldMapStreetsV1 {
    private final UIWorldMap ui;
    private final WorldMap worldMap;
    private final HashMap<WorldMapStreet, WorldMapStreetV1> streetV1map = new HashMap<>();
    protected final EditStreetsV1 editStreetsV1;
    private WorldMapStreet mouseOverStreet;
    private float mouseOverStreetWorldX;
    private float mouseOverStreetWorldY;

    public WorldMapStreetsV1(UIWorldMap ui) {
        this.ui = ui;
        this.worldMap = ui.getWorldMap();
        this.editStreetsV1 = new EditStreetsV1(this.ui);
    }

    public void addStreetData(String relativeFileName) {
        this.worldMap.addStreetData(relativeFileName);
    }

    public int getStreetDataCount() {
        return this.worldMap.getStreetDataCount();
    }

    public WorldMapStreets getStreetDataByIndex(int index) {
        return this.worldMap.getStreetDataByIndex(index);
    }

    public WorldMapStreets getStreetDataByRelativeFileName(String relativeFileName) {
        return this.worldMap.getStreetDataByRelativeFileName(relativeFileName);
    }

    public void clearStreetData() {
        this.worldMap.clearStreetData();
    }

    WorldMapStreetV1 getStreetV1(WorldMapStreet street) {
        if (street == null) {
            return null;
        } else {
            WorldMapStreetV1 streetV1 = this.streetV1map.get(street);
            if (streetV1 == null) {
                streetV1 = new WorldMapStreetV1().init(this, street);
                this.streetV1map.put(street, streetV1);
            }

            return streetV1;
        }
    }

    public void setMouseOverStreet(WorldMapStreetV1 streetV1, float worldX, float worldY) {
        this.mouseOverStreet = streetV1 == null ? null : streetV1.street;
        this.mouseOverStreetWorldX = worldX;
        this.mouseOverStreetWorldY = worldY;
    }

    public boolean canPickStreet(float uiX, float uiY) {
        int worldX = PZMath.fastfloor(this.ui.getAPI().uiToWorldX(uiX, uiY));
        int worldY = PZMath.fastfloor(this.ui.getAPI().uiToWorldY(uiX, uiY));
        WorldMapRenderer renderer = this.ui.getAPI().getRenderer();
        return renderer.getVisited() == null || !renderer.getBoolean("HideUnvisited") || renderer.getVisited().isKnown(worldX, worldY);
    }

    public WorldMapStreetV1 pickStreet(float uiX, float uiY) {
        float streetRadius = 20.0F;
        WorldMapStreets streets = this.worldMap.combinedStreets;
        WorldMapStreet street = streets.pickStreet(this.ui, uiX, uiY, 20.0F, this.ui.isMapEditor());
        return this.getStreetV1(street);
    }

    public WorldMapStreet getMouseOverStreet() {
        return this.mouseOverStreet;
    }

    public float getMouseOverStreetWorldX() {
        return this.mouseOverStreetWorldX;
    }

    public float getMouseOverStreetWorldY() {
        return this.mouseOverStreetWorldY;
    }

    public EditStreetsV1 getEditorAPI() {
        return this.editStreetsV1;
    }

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(WorldMapStreetsV1.class);
        exposer.setExposed(WorldMapStreetV1.class);
        EditStreetsV1.setExposed(exposer);
    }
}
