// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum ReadType {
    BOOK("book"),
    NEWSPAPER("newspaper"),
    PHOTO("photo");

    private final String id;

    private ReadType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
