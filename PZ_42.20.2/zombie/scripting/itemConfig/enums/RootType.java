// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.enums;

public enum RootType {
    Attribute(true),
    FluidContainer(true),
    LuaFunc(false);

    private final boolean requiresId;

    private RootType(final boolean requiresId) {
        this.requiresId = requiresId;
    }

    public boolean isRequiresId() {
        return this.requiresId;
    }
}
