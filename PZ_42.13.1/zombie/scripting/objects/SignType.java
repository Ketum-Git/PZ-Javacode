// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum SignType {
    SIGN_SKULL(36),
    SIGN_RIGHTARROW(32),
    SIGN_LEFTARROW(33),
    SIGN_DOWNARROW(34),
    SIGN_UPARROW(35);

    private final Integer index;

    private SignType(final int index) {
        this.index = index;
    }

    public Integer getSpriteIndex() {
        return this.index;
    }

    public String getSpriteName() {
        return "constructedobjects_signs_01_" + this.index.toString();
    }

    @Override
    public String toString() {
        return this.index.toString();
    }

    public static SignType typeOf(int index) {
        for (SignType type : values()) {
            if (type.getSpriteIndex() == index) {
                return type;
            }
        }

        throw new IllegalArgumentException("No enum constant SignType with value " + index);
    }
}
