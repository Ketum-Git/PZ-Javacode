// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.characters.IsoGameCharacter;

public interface ILockableDoor {
    boolean isLockedByKey();

    boolean IsOpen();

    int getKeyId();

    void setKeyId(int arg0);

    void setLockedByKey(boolean arg0);

    ICurtain HasCurtains();

    boolean canAddCurtain();

    boolean canClimbOver(IsoGameCharacter arg0);
}
