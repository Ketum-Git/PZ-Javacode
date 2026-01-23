// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;

@UsedFromLua
public class AnimalAllele {
    public String name;
    public float currentValue;
    public float trueRatioValue;
    public boolean dominant = true;
    public boolean used;
    public String geneticDisorder;

    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        GameWindow.WriteString(output, this.name);
        output.putFloat(this.currentValue);
        output.putFloat(this.trueRatioValue);
        output.put((byte)(this.dominant ? 1 : 0));
        GameWindow.WriteString(output, this.geneticDisorder);
    }

    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        this.name = GameWindow.ReadString(input);
        this.currentValue = input.getFloat();
        this.trueRatioValue = input.getFloat();
        this.dominant = input.get() == 1;
        this.geneticDisorder = GameWindow.ReadString(input);
    }

    public String getName() {
        return this.name;
    }

    public float getCurrentValue() {
        return this.currentValue;
    }

    public void setCurrentValue(float newValue) {
        this.currentValue = newValue;
    }

    public float getTrueRatioValue() {
        return this.trueRatioValue;
    }

    public void setTrueRatioValue(float newValue) {
        this.trueRatioValue = newValue;
    }

    public boolean isDominant() {
        return this.dominant;
    }

    public void setDominant(boolean dom) {
        this.dominant = dom;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isUsed() {
        return this.used;
    }

    public String getGeneticDisorder() {
        return this.geneticDisorder;
    }

    public void setGeneticDisorder(String gd) {
        this.geneticDisorder = gd;
    }
}
