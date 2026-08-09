// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

public class UnderwearDefinition {
    public static final UnderwearDefinition instance = new UnderwearDefinition();
    public boolean dirty = true;
    private static final ArrayList<UnderwearDefinition.OutfitUnderwearDefinition> m_outfitDefinition = new ArrayList<>();
    private static int baseChance = 50;

    public void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.init();
        }
    }

    private void init() {
        m_outfitDefinition.clear();
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("UnderwearDefinition");
        if (definitions != null) {
            baseChance = definitions.rawgetInt("baseChance");
            KahluaTableIterator iterator = definitions.iterator();

            while (iterator.advance()) {
                ArrayList<UnderwearDefinition.StringChance> allTops = null;
                if (iterator.getValue() instanceof KahluaTableImpl def) {
                    if (def.rawget("top") instanceof KahluaTableImpl tops) {
                        allTops = new ArrayList<>();
                        KahluaTableIterator ittop = tops.iterator();

                        while (ittop.advance()) {
                            if (ittop.getValue() instanceof KahluaTableImpl deftop) {
                                allTops.add(new UnderwearDefinition.StringChance(deftop.rawgetStr("name"), deftop.rawgetFloat("chance")));
                            }
                        }
                    }

                    UnderwearDefinition.OutfitUnderwearDefinition underwearDef = new UnderwearDefinition.OutfitUnderwearDefinition(
                        allTops, def.rawgetStr("bottom"), def.rawgetInt("chanceToSpawn"), def.rawgetStr("gender")
                    );
                    m_outfitDefinition.add(underwearDef);
                }
            }
        }
    }

    public static void addRandomUnderwear(IsoZombie zed) {
        if (!zed.isSkeleton()) {
            instance.checkDirty();
            if (OutfitRNG.Next(100) <= baseChance) {
                ArrayList<UnderwearDefinition.OutfitUnderwearDefinition> validDefs = new ArrayList<>();
                int totalChance = 0;

                for (int i = 0; i < m_outfitDefinition.size(); i++) {
                    UnderwearDefinition.OutfitUnderwearDefinition def = m_outfitDefinition.get(i);
                    if (zed.isFemale() && def.female || !zed.isFemale() && !def.female) {
                        validDefs.add(def);
                        totalChance += def.chanceToSpawn;
                    }
                }

                int choice = OutfitRNG.Next(totalChance);
                UnderwearDefinition.OutfitUnderwearDefinition toDo = null;
                int subtotal = 0;

                for (int ix = 0; ix < validDefs.size(); ix++) {
                    UnderwearDefinition.OutfitUnderwearDefinition testTable = validDefs.get(ix);
                    subtotal += testTable.chanceToSpawn;
                    if (choice < subtotal) {
                        toDo = testTable;
                        break;
                    }
                }

                if (toDo != null) {
                    Item scriptItem = ScriptManager.instance.FindItem(toDo.bottom);
                    ItemVisual bottomVisual = null;
                    if (scriptItem != null) {
                        bottomVisual = zed.getHumanVisual().addClothingItem(zed.getItemVisuals(), scriptItem);
                    }

                    if (toDo.top != null) {
                        String top = null;
                        choice = OutfitRNG.Next(toDo.topTotalChance);
                        subtotal = 0;

                        for (int ixx = 0; ixx < toDo.top.size(); ixx++) {
                            UnderwearDefinition.StringChance testTable = toDo.top.get(ixx);
                            subtotal = (int)(subtotal + testTable.chance);
                            if (choice < subtotal) {
                                top = testTable.str;
                                break;
                            }
                        }

                        if (top != null) {
                            scriptItem = ScriptManager.instance.FindItem(top);
                            if (scriptItem != null) {
                                ItemVisual topVisual = zed.getHumanVisual().addClothingItem(zed.getItemVisuals(), scriptItem);
                                if (OutfitRNG.Next(100) < 60 && topVisual != null && bottomVisual != null) {
                                    topVisual.setTint(bottomVisual.getTint());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static final class OutfitUnderwearDefinition {
        public ArrayList<UnderwearDefinition.StringChance> top;
        public int topTotalChance;
        public String bottom;
        public int chanceToSpawn;
        public boolean female;

        public OutfitUnderwearDefinition(ArrayList<UnderwearDefinition.StringChance> top, String bottom, int chanceToSpawn, String gender) {
            this.top = top;
            if (top != null) {
                for (int i = 0; i < top.size(); i++) {
                    this.topTotalChance = (int)(this.topTotalChance + top.get(i).chance);
                }
            }

            this.bottom = bottom;
            this.chanceToSpawn = chanceToSpawn;
            if ("female".equals(gender)) {
                this.female = true;
            }
        }
    }

    private static final class StringChance {
        String str;
        float chance;

        public StringChance(String str, float chance) {
            this.str = str;
            this.chance = chance;
        }
    }
}
