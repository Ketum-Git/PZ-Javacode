// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum AttachmentType {
    BEDROLL("Bedroll"),
    BIG_BLADE("BigBlade"),
    BIG_WEAPON("BigWeapon"),
    GUITAR("Guitar"),
    GUITAR_ACOUSTIC("GuitarAcoustic"),
    HAMMER("Hammer"),
    HOLSTER("Holster"),
    HOLSTER_SMALL("HolsterSmall"),
    KNIFE("Knife"),
    MEAT_CLEAVER("MeatCleaver"),
    NIGHTSTICK("Nightstick"),
    NOT_KNIFE("NotKnife"),
    PAN("Pan"),
    RACKET("Racket"),
    RIFLE("Rifle"),
    SAUCEPAN("Saucepan"),
    SCREWDRIVER("Screwdriver"),
    SHOVEL("Shovel"),
    SWORD("Sword"),
    WALKIE("Walkie"),
    WEBBING("Webbing"),
    WRENCH("Wrench"),
    BEDROLL_BOTTOM("BedrollBottom"),
    BEDROLL_BOTTOM_ALICE("BedrollBottomALICE"),
    BEDROLL_BOTTOM_BIG("BedrollBottomBig"),
    HOLSTER_ANKLE("HolsterAnkle"),
    HOLSTER_LEFT("HolsterLeft"),
    HOLSTER_RIGHT("HolsterRight"),
    HOLSTER_SHOULDER("HolsterShoulder"),
    SMALL_BELT_LEFT("SmallBeltLeft"),
    SMALL_BELT_RIGHT("SmallBeltRight"),
    WEBBING_LEFT("WebbingLeft"),
    WEBBING_RIGHT("WebbingRight");

    private final String id;

    private AttachmentType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
