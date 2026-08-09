// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects.interfaces;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;

public interface Thumpable {
    boolean isDestroyed();

    default void Thump(IsoMovingObject isoMovingObject) {
        this.Thump(isoMovingObject, 1);
    }

    void Thump(IsoMovingObject var1, int var2);

    void WeaponHit(IsoGameCharacter chr, HandWeapon weapon);

    Thumpable getThumpableFor(IsoGameCharacter chr);

    Thumpable getThumpableFor(IsoGameCharacter var1, HandWeapon var2);

    float getThumpCondition();
}
