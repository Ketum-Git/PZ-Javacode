// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public record SoundMapKey(String id) {
    public static final SoundMapKey ACTIVATE = new SoundMapKey("Activate");
    public static final SoundMapKey DEACTIVATE = new SoundMapKey("Deactivate");
    public static final SoundMapKey DUMP_CONTENTS = new SoundMapKey("DumpContents");
    public static final SoundMapKey EQUIPPED_AND_ACTIVATED = new SoundMapKey("EquippedAndActivated");
    public static final SoundMapKey SPEAR_STAB = new SoundMapKey("SpearStab");

    @Override
    public String toString() {
        return this.id;
    }
}
