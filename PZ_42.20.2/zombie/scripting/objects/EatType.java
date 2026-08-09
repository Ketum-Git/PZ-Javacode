// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum EatType {
    BLEACH_BOTTLE("BleachBottle"),
    BOTTLE("Bottle"),
    BOURBON("Bourbon"),
    BUCKET("Bucket"),
    CANDRINK("Candrink"),
    CIGARETTES("Cigarettes"),
    EAT_BOX("EatBox"),
    EAT_OFF_STICK("EatOffStick"),
    EAT_SMALL("EatSmall"),
    GLUG_FOOD("GlugFood"),
    BOWL_TWO_HANDS("2handbowl"),
    FORCED_TWO_HANDS("2handforced"),
    PIPE("Pipe"),
    PIZZA("Pizza"),
    PLATE("Plate"),
    POPCAN("Popcan"),
    POT("Pot"),
    SAUCEPAN("Saucepan"),
    SNIFF("Sniff"),
    TEACUP("Teacup");

    private final String id;

    private EatType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
