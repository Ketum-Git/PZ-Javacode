// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.iso.objects.IsoDeadBody;

public interface CharacterDiedListener {
    void onDied(IsoGameCharacter var1, IsoDeadBody var2);
}
