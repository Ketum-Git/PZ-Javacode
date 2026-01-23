// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum SoundMasters {
    AMBIENT("Ambient"),
    MUSIC("Music"),
    VEHICLE_ENGINE("VehicleEngine");

    private final String id;

    private SoundMasters(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
