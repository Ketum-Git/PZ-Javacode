// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public class MagazineSubject {
    public static final MagazineSubject ART = registerBase("Art");
    public static final MagazineSubject BUSINESS = registerBase("Business");
    public static final MagazineSubject CARS = registerBase("Cars");
    public static final MagazineSubject CHILDS = registerBase("Childs");
    public static final MagazineSubject CINEMA = registerBase("Cinema");
    public static final MagazineSubject CRIME = registerBase("Crime");
    public static final MagazineSubject FASHION = registerBase("Fashion");
    public static final MagazineSubject FIREARM = registerBase("Firearm");
    public static final MagazineSubject GAMING = registerBase("Gaming");
    public static final MagazineSubject GOLF = registerBase("Golf");
    public static final MagazineSubject HEALTH = registerBase("Health");
    public static final MagazineSubject HOBBY = registerBase("Hobby");
    public static final MagazineSubject HORROR = registerBase("Horror");
    public static final MagazineSubject HUMOR = registerBase("Humor");
    public static final MagazineSubject MILITARY = registerBase("Military");
    public static final MagazineSubject MUSIC = registerBase("Music");
    public static final MagazineSubject OUTDOORS = registerBase("Outdoors");
    public static final MagazineSubject POLICE = registerBase("Police");
    public static final MagazineSubject POPULAR = registerBase("Popular");
    public static final MagazineSubject RICH = registerBase("Rich");
    public static final MagazineSubject SCIENCE = registerBase("Science");
    public static final MagazineSubject SPORTS = registerBase("Sports");
    public static final MagazineSubject TECH = registerBase("Tech");
    public static final MagazineSubject TEENS = registerBase("Teens");
    private final String key;

    private MagazineSubject(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public static MagazineSubject get(ResourceLocation id) {
        return Registries.MAGAZINE_SUBJECT.get(id);
    }

    @Override
    public String toString() {
        return Registries.MAGAZINE_SUBJECT.getLocation(this).toString();
    }

    public static MagazineSubject register(String id) {
        return register(false, id);
    }

    private static MagazineSubject registerBase(String id) {
        return register(true, id);
    }

    private static MagazineSubject register(boolean allowDefaultNamespace, String id) {
        return Registries.MAGAZINE_SUBJECT.register(RegistryReset.createLocation(id, allowDefaultNamespace), new MagazineSubject(id));
    }
}
