// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public record SoundMapKey(String id) {
    public static final SoundMapKey ACTIVATE = new SoundMapKey("Activate");
    public static final SoundMapKey CONTAINER_CLOSE = new SoundMapKey("ContainerClose");
    public static final SoundMapKey CONTAINER_OPEN = new SoundMapKey("ContainerOpen");
    public static final SoundMapKey CONTAINER_PUT = new SoundMapKey("ContainerPut");
    public static final SoundMapKey CONTAINER_TAKE = new SoundMapKey("ContainerTake");
    public static final SoundMapKey DEACTIVATE = new SoundMapKey("Deactivate");
    public static final SoundMapKey DUMP_CONTENTS = new SoundMapKey("DumpContents");
    public static final SoundMapKey EQUIPPED_AND_ACTIVATED = new SoundMapKey("EquippedAndActivated");
    public static final SoundMapKey SPEAR_STAB = new SoundMapKey("SpearStab");
    public static final SoundMapKey PICK_UP_FURNITURE = new SoundMapKey("PickUpFurniture");
    public static final SoundMapKey PLACE_FURNITURE = new SoundMapKey("PlaceFurniture");

    @Override
    public String toString() {
        return this.id;
    }
}
