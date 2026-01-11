// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import se.krka.kahlua.j2se.KahluaTableImpl;

public class AnimalPart {
    public String item;
    public int minNb = -1;
    public int maxNb = -1;
    public int nb = -1;

    public AnimalPart() {
    }

    public AnimalPart(KahluaTableImpl def) {
        this.item = def.rawgetStr("item");
        this.minNb = def.rawgetInt("minNb");
        this.maxNb = def.rawgetInt("maxNb");
        this.nb = def.rawgetInt("nb");
    }

    public String getItem() {
        return this.item;
    }

    public int getMinNb() {
        return this.minNb;
    }

    public int getMaxNb() {
        return this.maxNb;
    }

    public int getNb() {
        return this.nb;
    }
}
