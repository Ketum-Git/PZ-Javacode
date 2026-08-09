// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.StorySounds;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Color;

/**
 * Turbo
 */
@UsedFromLua
public final class EventSound {
    protected String name;
    protected Color color = new Color(1.0F, 1.0F, 1.0F);
    protected ArrayList<DataPoint> dataPoints = new ArrayList<>();
    protected ArrayList<StorySound> storySounds = new ArrayList<>();

    public EventSound() {
        this("Unnamed");
    }

    public EventSound(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public ArrayList<DataPoint> getDataPoints() {
        return this.dataPoints;
    }

    public void setDataPoints(ArrayList<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public ArrayList<StorySound> getStorySounds() {
        return this.storySounds;
    }

    public void setStorySounds(ArrayList<StorySound> storySounds) {
        this.storySounds = storySounds;
    }
}
