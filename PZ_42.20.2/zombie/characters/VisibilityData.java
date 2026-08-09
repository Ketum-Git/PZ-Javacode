// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

public class VisibilityData {
    private final float fatigue;
    private final float noiseDistance;
    private final float cone;
    private final float baseAmbient;

    public VisibilityData(float fatigue, float noiseDistance, float cone, float baseAmbient) {
        this.fatigue = fatigue;
        this.noiseDistance = noiseDistance;
        this.cone = cone;
        this.baseAmbient = baseAmbient;
    }

    public float getFatigue() {
        return this.fatigue;
    }

    public float getNoiseDistance() {
        return this.noiseDistance;
    }

    public float getCone() {
        return this.cone;
    }

    public float getBaseAmbient() {
        return this.baseAmbient;
    }
}
