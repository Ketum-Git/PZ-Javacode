// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

public final class IsoLuaCharacter extends IsoGameCharacter {
    public IsoLuaCharacter(float x, float y, float z) {
        super(null, x, y, z);
        this.descriptor = SurvivorFactory.CreateSurvivor();
        this.descriptor.setInstance(this);
        SurvivorDesc desc = this.descriptor;
        this.InitSpriteParts(desc);
    }

    @Override
    public void update() {
    }
}
