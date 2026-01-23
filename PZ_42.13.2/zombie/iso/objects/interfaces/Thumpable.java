// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects.interfaces;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;

public interface Thumpable {
    boolean isDestroyed();

    void Thump(IsoMovingObject thumper);

    void WeaponHit(IsoGameCharacter chr, HandWeapon weapon);

    Thumpable getThumpableFor(IsoGameCharacter chr);

    float getThumpCondition();
}
