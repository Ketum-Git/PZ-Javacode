// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.inventory.types.HandWeapon;
import zombie.vehicles.BaseVehicle;

/**
 * ILuaGameCharacterDamage
 *   Provides the functions expected by LUA when dealing with objects of this type.
 */
public interface ILuaGameCharacterDamage {
    BodyDamage getBodyDamage();

    BodyDamage getBodyDamageRemote();

    float getHealth();

    void setHealth(float Health);

    float Hit(BaseVehicle vehicle, float speed, boolean isHitFromBehind, float hitDirX, float hitDirY);

    float Hit(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta);

    float Hit(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta, boolean bRemote);

    boolean isOnFire();

    void StopBurning();

    int getLastHitCount();

    void setLastHitCount(int hitCount);

    boolean addHole(BloodBodyPartType part);

    void addBlood(BloodBodyPartType part, boolean scratched, boolean bitten, boolean allLayers);

    boolean isBumped();

    String getBumpType();

    boolean isOnDeathDone();

    void setOnDeathDone(boolean done);

    boolean isOnKillDone();

    void setOnKillDone(boolean done);

    boolean isDeathDragDown();

    void setDeathDragDown(boolean dragDown);

    boolean isPlayingDeathSound();

    void setPlayingDeathSound(boolean playing);
}
