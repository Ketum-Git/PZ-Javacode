// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.stash;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

@UsedFromLua
public final class Stash {
    public String name;
    public String type;
    public String item;
    public String customName;
    public int buildingX;
    public int buildingY;
    public String spawnTable;
    public ArrayList<StashAnnotation> annotations;
    public boolean spawnOnlyOnZed;
    public int minDayToSpawn = -1;
    public int maxDayToSpawn = -1;
    public int minTrapToSpawn = -1;
    public int maxTrapToSpawn = -1;
    public int zombies;
    public ArrayList<StashContainer> containers;
    public int barricades;

    public Stash(String name) {
        this.name = name;
    }

    public void load(KahluaTableImpl stashDesc) {
        this.type = stashDesc.rawgetStr("type");
        this.item = stashDesc.rawgetStr("item");
        StashBuilding stashBuilding = new StashBuilding(this.name, stashDesc.rawgetInt("buildingX"), stashDesc.rawgetInt("buildingY"));
        StashSystem.possibleStashes.add(stashBuilding);
        this.buildingX = stashBuilding.buildingX;
        this.buildingY = stashBuilding.buildingY;
        this.spawnTable = stashDesc.rawgetStr("spawnTable");
        this.customName = Translator.getText(stashDesc.rawgetStr("customName"));
        this.zombies = stashDesc.rawgetInt("zombies");
        this.barricades = stashDesc.rawgetInt("barricades");
        this.spawnOnlyOnZed = stashDesc.rawgetBool("spawnOnlyOnZed");
        String daysToSpawn = stashDesc.rawgetStr("daysToSpawn");
        if (daysToSpawn != null) {
            String[] days = daysToSpawn.split("-");
            if (days.length == 2) {
                this.minDayToSpawn = Integer.parseInt(days[0]);
                this.maxDayToSpawn = Integer.parseInt(days[1]);
            } else {
                this.minDayToSpawn = Integer.parseInt(days[0]);
            }
        }

        String trapsToSpawn = stashDesc.rawgetStr("traps");
        if (trapsToSpawn != null) {
            String[] traps = trapsToSpawn.split("-");
            if (traps.length == 2) {
                this.minTrapToSpawn = Integer.parseInt(traps[0]);
                this.maxTrapToSpawn = Integer.parseInt(traps[1]);
            } else {
                this.minTrapToSpawn = Integer.parseInt(traps[0]);
                this.maxTrapToSpawn = this.minTrapToSpawn;
            }
        }

        KahluaTable luaContainers = (KahluaTable)stashDesc.rawget("containers");
        if (luaContainers != null) {
            this.containers = new ArrayList<>();
            KahluaTableIterator it = luaContainers.iterator();

            while (it.advance()) {
                KahluaTableImpl contDesc = (KahluaTableImpl)it.getValue();
                StashContainer cont = new StashContainer(contDesc.rawgetStr("room"), contDesc.rawgetStr("containerSprite"), contDesc.rawgetStr("containerType"));
                cont.contX = contDesc.rawgetInt("contX");
                cont.contY = contDesc.rawgetInt("contY");
                cont.contZ = contDesc.rawgetInt("contZ");
                cont.containerItem = contDesc.rawgetStr("containerItem");
                if (cont.containerItem != null && ScriptManager.instance.getItem(cont.containerItem) == null) {
                    DebugLog.General.error("Stash containerItem \"%s\" doesn't exist.", cont.containerItem);
                }

                this.containers.add(cont);
            }
        }

        if ("Map".equals(this.type)) {
            KahluaTable luaAnnotations = (KahluaTableImpl)stashDesc.rawget("annotations");
            if (luaAnnotations != null) {
                this.annotations = new ArrayList<>();
                KahluaTableIterator it = luaAnnotations.iterator();

                while (it.advance()) {
                    KahluaTable luaAnnotation = (KahluaTable)it.getValue();
                    StashAnnotation annotation = new StashAnnotation();
                    annotation.fromLua(luaAnnotation);
                    this.annotations.add(annotation);
                }
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public String getItem() {
        return this.item;
    }

    public int getBuildingX() {
        return this.buildingX;
    }

    public int getBuildingY() {
        return this.buildingY;
    }

    public void applyAnnotations(UIWorldMap ui) {
        if (this.annotations != null) {
            if (ui.getSymbolsDirect() != null) {
                for (int i = 0; i < this.annotations.size(); i++) {
                    StashAnnotation annotation = this.annotations.get(i);
                    if (annotation.symbol != null) {
                        float anchorX = Float.isNaN(annotation.anchorX) ? 0.5F : annotation.anchorX;
                        float anchorY = Float.isNaN(annotation.anchorY) ? 0.5F : annotation.anchorY;
                        WorldMapTextureSymbol symbol = ui.getSymbolsDirect()
                            .addTexture(annotation.symbol, annotation.x, annotation.y, anchorX, anchorY, 0.666F, annotation.r, annotation.g, annotation.b, 1.0F);
                        if (!Float.isNaN(annotation.rotation)) {
                            symbol.setRotation(annotation.rotation);
                        }
                    } else if (annotation.text != null) {
                        WorldMapTextSymbol symbol;
                        if (Translator.getTextOrNull(annotation.text) == null) {
                            symbol = ui.getSymbolsDirect()
                                .addTranslatedText(annotation.text, "note", annotation.x, annotation.y, annotation.r, annotation.g, annotation.b, 1.0F);
                        } else {
                            symbol = ui.getSymbolsDirect()
                                .addUntranslatedText(annotation.text, "note", annotation.x, annotation.y, annotation.r, annotation.g, annotation.b, 1.0F);
                        }

                        if (!Float.isNaN(annotation.anchorX) && !Float.isNaN(annotation.anchorY)) {
                            symbol.setAnchor(annotation.anchorX, annotation.anchorY);
                        }

                        if (!Float.isNaN(annotation.rotation)) {
                            symbol.setRotation(annotation.rotation);
                        }
                    }
                }
            }
        }
    }
}
