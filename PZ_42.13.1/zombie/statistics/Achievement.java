// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.statistics;

public class Achievement {
    private final String name;
    private final String description;
    private final float threshold;
    private boolean isUnlocked;

    public Achievement(String name, String description, float threshold) {
        this.name = name;
        this.description = description;
        this.threshold = threshold;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public float getThreshold() {
        return this.threshold;
    }

    public void onAchieved() {
        this.isUnlocked = true;
    }

    public boolean isUnlocked() {
        return this.isUnlocked;
    }

    public void reset() {
        this.isUnlocked = false;
    }
}
