// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum AnimalFeedType {
    ANIMAL_FEED("AnimalFeed"),
    GRASS("Grass"),
    NUTS("Nuts"),
    SEEDS("Seeds");

    private final String id;

    private AnimalFeedType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
