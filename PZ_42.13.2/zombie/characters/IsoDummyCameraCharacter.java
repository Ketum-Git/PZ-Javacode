// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.UsedFromLua;
import zombie.iso.IsoCamera;

@UsedFromLua
public final class IsoDummyCameraCharacter extends IsoGameCharacter {
    public IsoDummyCameraCharacter(float x, float y, float z) {
        super(null, x, y, z);
        IsoCamera.setCameraCharacter(this);
    }

    @Override
    public void update() {
    }
}
