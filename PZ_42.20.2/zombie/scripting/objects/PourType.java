// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum PourType {
    BOWL("bowl"),
    BUCKET("Bucket"),
    KETTLE("Kettle"),
    MUG("Mug"),
    POT("Pot"),
    SAUCE_PAN("saucepan"),
    WATERING_CAN("wateringcan");

    private final String id;

    private PourType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
