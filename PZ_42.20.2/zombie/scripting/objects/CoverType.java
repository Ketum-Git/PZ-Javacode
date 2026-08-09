// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum CoverType {
    HARDCOVER,
    SOFTCOVER,
    BOTH;

    public boolean matches(CoverType coverType) {
        return this == BOTH || coverType == BOTH || this == coverType;
    }
}
