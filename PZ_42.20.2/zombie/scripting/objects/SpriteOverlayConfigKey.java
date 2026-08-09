// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public record SpriteOverlayConfigKey(String id) {
    public static final SpriteOverlayConfigKey DEFAULT = new SpriteOverlayConfigKey("default");
    public static final SpriteOverlayConfigKey BLACK_SAGE = new SpriteOverlayConfigKey("BlackSage");
    public static final SpriteOverlayConfigKey HABANERO = new SpriteOverlayConfigKey("Habanero");
    public static final SpriteOverlayConfigKey HERB = new SpriteOverlayConfigKey("Herb");
    public static final SpriteOverlayConfigKey JALAPENO = new SpriteOverlayConfigKey("Jalapeno");
    public static final SpriteOverlayConfigKey LAVENDER = new SpriteOverlayConfigKey("Lavender");
    public static final SpriteOverlayConfigKey MARIGOLD = new SpriteOverlayConfigKey("Marigold");
    public static final SpriteOverlayConfigKey ROSE = new SpriteOverlayConfigKey("Rose");
    public static final SpriteOverlayConfigKey LEATHER = new SpriteOverlayConfigKey("Leather");
    public static final SpriteOverlayConfigKey ANGUS_LEATHER = new SpriteOverlayConfigKey("AngusLeather");
    public static final SpriteOverlayConfigKey HOLSTEIN_LEATHER = new SpriteOverlayConfigKey("HolsteinLeather");
    public static final SpriteOverlayConfigKey SIMMENTAL_LEATHER = new SpriteOverlayConfigKey("SimmentalLeather");
    public static final SpriteOverlayConfigKey SMALL_LEATHER = new SpriteOverlayConfigKey("SmallLeather");
    public static final SpriteOverlayConfigKey MEDIUM_LEATHER = new SpriteOverlayConfigKey("MediumLeather");
    public static final SpriteOverlayConfigKey LARGE_LEATHER = new SpriteOverlayConfigKey("LargeLeather");
    public static final SpriteOverlayConfigKey DEER_LEATHER = new SpriteOverlayConfigKey("DeerLeather");
    public static final SpriteOverlayConfigKey PIG_LEATHER = new SpriteOverlayConfigKey("PigLeather");
    public static final SpriteOverlayConfigKey PIG_BLACK_LEATHER = new SpriteOverlayConfigKey("PigBlackLeather");
    public static final SpriteOverlayConfigKey SHEEP_LEATHER = new SpriteOverlayConfigKey("SheepLeather");
    public static final SpriteOverlayConfigKey LAMB_LEATHER = new SpriteOverlayConfigKey("LambLeather");
    public static final SpriteOverlayConfigKey TOBACCO = new SpriteOverlayConfigKey("Tobacco");
    public static final SpriteOverlayConfigKey CORN = new SpriteOverlayConfigKey("Corn");
    public static final SpriteOverlayConfigKey SUNFLOWER = new SpriteOverlayConfigKey("Sunflower");
    public static final SpriteOverlayConfigKey HEMP = new SpriteOverlayConfigKey("Hemp");
    public static final SpriteOverlayConfigKey FLAX = new SpriteOverlayConfigKey("Flax");
    public static final SpriteOverlayConfigKey WHEAT = new SpriteOverlayConfigKey("Wheat");

    @Override
    public String toString() {
        return this.id();
    }
}
