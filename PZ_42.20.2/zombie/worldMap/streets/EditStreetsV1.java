// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import javax.xml.transform.TransformerException;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.util.Pool;
import zombie.worldMap.UIWorldMap;

@UsedFromLua
public final class EditStreetsV1 implements IWorldMapStreetListener {
    private static final Pool<EditStreetV1> s_streetPool = new Pool<>(EditStreetV1::new);
    private final UIWorldMap ui;
    private WorldMapStreets uiStreets;
    private final ArrayList<EditStreetV1> streets = new ArrayList<>();
    private EditStreetV1 editorStreet;

    @Override
    public void onAdd(WorldMapStreet street) {
        if (this.findStreet(street) == null) {
            EditStreetV1 streetV1 = s_streetPool.alloc().init(this, street);
            this.streets.add(this.uiStreets.indexOf(street), streetV1);
        }
    }

    @Override
    public void onBeforeRemove(WorldMapStreet street) {
        EditStreetV1 streetV1 = this.findStreet(street);
        if (streetV1 != null) {
            this.streets.remove(streetV1);
            s_streetPool.release(streetV1);
        }
    }

    @Override
    public void onAfterRemove(WorldMapStreet street) {
    }

    @Override
    public void onBeforeModifyStreet(WorldMapStreet street) {
    }

    @Override
    public void onAfterModifyStreet(WorldMapStreet street) {
    }

    UIWorldMap getUI() {
        return this.ui;
    }

    public EditStreetsV1(UIWorldMap ui) {
        Objects.requireNonNull(ui);
        this.ui = ui;
    }

    public void setStreetData(String relativeFileName) {
        WorldMapStreets streets = this.ui.getWorldMap().getStreetDataByRelativeFileName(relativeFileName);
        if (streets != this.uiStreets) {
            this.clear();
            this.uiStreets = streets;
            if (this.uiStreets != null) {
                this.uiStreets.addListener(this);
                this.reinit();
            }
        }
    }

    WorldMapStreets getStreetData() {
        return this.uiStreets;
    }

    public int getStreetCount() {
        return this.streets.size();
    }

    public EditStreetV1 getStreetByIndex(int index) {
        return this.streets.get(index);
    }

    public boolean canPickStreet(float uiX, float uiY) {
        return this.uiStreets.canPickStreet(this.ui, uiX, uiY);
    }

    public EditStreetV1 pickStreet(float uiX, float uiY) {
        float streetRadius = 20.0F;
        WorldMapStreet street = this.uiStreets.pickStreet(this.ui, uiX, uiY, 20.0F, this.ui.isMapEditor());
        return street == null ? null : this.findStreet(street);
    }

    public EditStreetV1 createEditorStreet() {
        if (this.editorStreet != null) {
            this.editorStreet.release();
        }

        WorldMapStreet street = new WorldMapStreet(this.uiStreets, "Street", new StreetPoints());
        return this.editorStreet = s_streetPool.alloc().init(this, street);
    }

    public void forgetEditorStreet() {
        this.editorStreet = null;
    }

    public void freeEditorStreet(EditStreetV1 streetV1) {
        if (this.editorStreet == streetV1) {
            this.uiStreets.getLookup().removeStreet(this.editorStreet.street);
            this.editorStreet.release();
            this.editorStreet = null;
        }
    }

    private EditStreetV1 findStreet(WorldMapStreet street) {
        for (int i = 0; i < this.getStreetCount(); i++) {
            EditStreetV1 streetV1 = this.getStreetByIndex(i);
            if (streetV1.street == street) {
                return streetV1;
            }
        }

        return null;
    }

    public void addStreet(EditStreetV1 streetV1) {
        Objects.requireNonNull(streetV1);
        if (streetV1 == this.editorStreet) {
            this.editorStreet = null;
        }

        if (!this.streets.contains(streetV1)) {
            this.streets.add(streetV1);
            this.uiStreets.addStreet(streetV1.street);
        }
    }

    public void removeStreet(EditStreetV1 streetV1) {
        if (this.streets.contains(streetV1)) {
            this.uiStreets.removeStreet(streetV1.street);
        }
    }

    public EditStreetV1 splitStreet(EditStreetV1 streetV1, int index) {
        WorldMapStreet newStreet = this.uiStreets.splitStreet(streetV1.street, index);
        return newStreet == null ? null : this.findStreet(newStreet);
    }

    public void setMouseOverStreet(EditStreetV1 streetV1, float worldX, float worldY) {
        WorldMapStreetsV1 streetsAPI = this.getUI().getAPI().getStreetsAPI();
        WorldMapStreetV1 wmStreetV1 = streetsAPI.getStreetV1(streetV1.street);
        streetsAPI.setMouseOverStreet(wmStreetV1, worldX, worldY);
    }

    public void renderStreetLines(float r, float g, float b, float a, int thickness) {
        this.uiStreets.renderStreetLines(this.ui, r, g, b, a, thickness);
    }

    public void save() {
        WorldMapStreetsXML xml = new WorldMapStreetsXML();

        try {
            xml.write(this.uiStreets.getAbsoluteFileName(), this.uiStreets);
        } catch (IOException | TransformerException var3) {
            throw new RuntimeException(var3);
        }
    }

    private void clear() {
        for (int i = 0; i < this.streets.size(); i++) {
            this.streets.get(i).release();
        }

        this.streets.clear();
    }

    private void init() {
        for (int i = 0; i < this.uiStreets.getStreetCount(); i++) {
            WorldMapStreet street = this.uiStreets.getStreetByIndex(i);
            EditStreetV1 streetV1 = s_streetPool.alloc().init(this, street);
            this.streets.add(streetV1);
        }
    }

    private void reinit() {
        this.clear();
        this.init();
    }

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(EditStreetsV1.class);
        exposer.setExposed(EditStreetV1.class);
    }
}
