// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum ModelAttachmentId {
    WORLD("world"),
    HEAD("head"),
    HEAD_HAT("head_hat"),
    BOWTIE("bowtie"),
    BIP01_PROP2("Bip01_Prop2"),
    BIP01_PROP1("Bip01_Prop1"),
    KNIFE_SHOULDER("knife_shoulder"),
    MUZZLE("muzzle");

    private final String id;

    private ModelAttachmentId(final String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
}
