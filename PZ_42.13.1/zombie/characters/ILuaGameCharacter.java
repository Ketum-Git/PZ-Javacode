// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import zombie.ai.State;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.Moodles.Moodles;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.characters.traits.CharacterTraits;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Literature;
import zombie.iso.ILuaIsoObject;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.pathfind.Path;
import zombie.pathfind.PathFindBehavior2;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.Recipe;
import zombie.ui.UIFont;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

/**
 * ILuaGameCharacter
 *  Provides the functions expected by LUA when dealing with objects of this type.
 */
public interface ILuaGameCharacter
    extends ILuaIsoObject,
    ILuaVariableSource,
    ILuaGameCharacterAttachedItems,
    ILuaGameCharacterDamage,
    ILuaGameCharacterClothing,
    ILuaGameCharacterHealth {
    String getFullName();

    SurvivorDesc getDescriptor();

    void setDescriptor(SurvivorDesc descriptor);

    boolean isRangedWeaponEmpty();

    void setRangedWeaponEmpty(boolean val);

    BaseVisual getVisual();

    BaseCharacterSoundEmitter getEmitter();

    void resetModel();

    void resetModelNextFrame();

    IsoSpriteInstance getSpriteDef();

    boolean hasItems(String type, int count);

    int getXpForLevel(int level);

    IsoGameCharacter.XP getXp();

    boolean isAsleep();

    void setAsleep(boolean Asleep);

    boolean isResting();

    void setIsResting(boolean arg0);

    int getZombieKills();

    void setForceWakeUpTime(float ForceWakeUpTime);

    ItemContainer getInventory();

    InventoryItem getPrimaryHandItem();

    void setPrimaryHandItem(InventoryItem leftHandItem);

    InventoryItem getSecondaryHandItem();

    void setSecondaryHandItem(InventoryItem rightHandItem);

    boolean hasEquipped(String String);

    boolean hasEquippedTag(ItemTag var1);

    boolean hasWornTag(ItemTag var1);

    boolean isHandItem(InventoryItem item);

    boolean isPrimaryHandItem(InventoryItem item);

    boolean isSecondaryHandItem(InventoryItem item);

    boolean isItemInBothHands(InventoryItem item);

    boolean removeFromHands(InventoryItem item);

    void setSpeakColourInfo(ColorInfo info);

    boolean isSpeaking();

    Moodles getMoodles();

    Stats getStats();

    CharacterTraits getCharacterTraits();

    int getMaxWeight();

    void PlayAnim(String string);

    void PlayAnimWithSpeed(String string, float framesSpeedPerFrame);

    void PlayAnimUnlooped(String string);

    void StartTimedActionAnim(String event);

    void StartTimedActionAnim(String event, String type);

    void StopTimedActionAnim();

    Stack<BaseAction> getCharacterActions();

    void StartAction(BaseAction act);

    void StopAllActionQueue();

    int getPerkLevel(PerkFactory.Perk perks);

    IsoGameCharacter.PerkInfo getPerkInfo(PerkFactory.Perk perk);

    void setPerkLevelDebug(PerkFactory.Perk perks, int level);

    void LoseLevel(PerkFactory.Perk perk);

    void LevelPerk(PerkFactory.Perk perk, boolean removePick);

    void LevelPerk(PerkFactory.Perk perk);

    void ReadLiterature(Literature literature);

    void Callout();

    boolean IsSpeaking();

    void Say(String line);

    void Say(String line, float r, float g, float b, UIFont font, float baseRange, String customTag);

    void setHaloNote(String str);

    void setHaloNote(String str, float dispTime);

    void setHaloNote(String str, int r, int g, int b, float dispTime);

    void initSpritePartsEmpty();

    boolean hasTrait(CharacterTrait var1);

    void changeState(State state);

    boolean isCurrentState(State state);

    State getCurrentState();

    void pathToLocation(int x, int y, int z);

    void pathToLocationF(float x, float y, float z);

    boolean isEnduranceSufficientForAction();

    void smashCarWindow(VehiclePart part);

    void smashWindow(IsoWindow w);

    void openWindow(IsoWindow w);

    void closeWindow(IsoWindow w);

    void climbThroughWindow(IsoWindow w);

    void climbThroughWindow(IsoWindow w, Integer startingFrame);

    void climbThroughWindowFrame(IsoWindowFrame arg0);

    void climbSheetRope();

    void climbDownSheetRope();

    boolean canClimbSheetRope(IsoGridSquare sq);

    boolean canClimbDownSheetRopeInCurrentSquare();

    boolean canClimbDownSheetRope(IsoGridSquare sq);

    void climbThroughWindow(IsoThumpable w);

    void climbThroughWindow(IsoThumpable w, Integer startingFrame);

    void climbOverFence(IsoDirections dir);

    boolean isAboveTopOfStairs();

    double getHoursSurvived();

    boolean isOutside();

    boolean isFemale();

    void setFemale(boolean isFemale);

    boolean isZombie();

    boolean isEquipped(InventoryItem item);

    boolean isEquippedClothing(InventoryItem item);

    boolean isAttachedItem(InventoryItem item);

    void faceThisObject(IsoObject object);

    void facePosition(int x, int y);

    void faceThisObjectAlt(IsoObject object);

    int getAlreadyReadPages(String fullType);

    void setAlreadyReadPages(String fullType, int pages);

    Safety getSafety();

    void setSafety(Safety safety);

    float getMeleeDelay();

    void setMeleeDelay(float delay);

    float getRecoilDelay();

    void setRecoilDelay(float recoilDelay);

    int getMaintenanceMod();

    int getWeaponLevel();

    int getWeaponLevel(HandWeapon arg0);

    float getHammerSoundMod();

    float getWeldingSoundMod();

    boolean isGodMod();

    void setGodMod(boolean b);

    BaseVehicle getVehicle();

    void setVehicle(BaseVehicle v);

    float getInventoryWeight();

    void modifyTraitXPBoost(CharacterTrait var1, boolean var2);

    void modifyTraitXPBoost(CharacterTraitDefinition var1, boolean var2);

    List<String> getKnownRecipes();

    boolean isRecipeKnown(Recipe recipe);

    boolean isRecipeKnown(String name);

    void addKnownMediaLine(String guid);

    void removeKnownMediaLine(String guid);

    void clearKnownMediaLines();

    boolean isKnownMediaLine(String guid);

    long playSound(String file);

    long playSoundLocal(String file);

    void stopOrTriggerSound(long eventInstance);

    void addWorldSoundUnlessInvisible(int radius, int volume, boolean bStressHumans);

    boolean isKnownPoison(InventoryItem item);

    boolean isKnownPoison(Item arg0);

    String getBedType();

    void setBedType(String bedType);

    Path getPath2();

    void setPath2(Path arg0);

    PathFindBehavior2 getPathFindBehavior2();

    IsoObject getBed();

    void setBed(IsoObject bed);

    boolean isReading();

    void setReading(boolean isReading);

    float getTimeSinceLastSmoke();

    void setTimeSinceLastSmoke(float timeSinceLastSmoke);

    boolean isInvisible();

    void setInvisible(boolean b);

    boolean isDriving();

    boolean isInARoom();

    boolean isUnlimitedCarry();

    void setUnlimitedCarry(boolean unlimitedCarry);

    boolean isBuildCheat();

    void setBuildCheat(boolean buildCheat);

    boolean isFarmingCheat();

    void setFarmingCheat(boolean b);

    boolean isFishingCheat();

    void setFishingCheat(boolean arg0);

    boolean isHealthCheat();

    void setHealthCheat(boolean healthCheat);

    boolean isMechanicsCheat();

    void setMechanicsCheat(boolean mechanicsCheat);

    boolean isMovablesCheat();

    void setMovablesCheat(boolean b);

    boolean isAnimalCheat();

    void setAnimalCheat(boolean var1);

    boolean isTimedActionInstantCheat();

    void setTimedActionInstantCheat(boolean b);

    boolean isTimedActionInstant();

    boolean isShowAdminTag();

    void setShowAdminTag(boolean showAdminTag);

    void reportEvent(String name);

    AnimatorDebugMonitor getDebugMonitor();

    void setDebugMonitor(AnimatorDebugMonitor monitor);

    boolean isAiming();

    boolean isTwisting();

    boolean allowsTwist();

    void resetBeardGrowingTime();

    void resetHairGrowingTime();

    float getPerkToUnit(PerkFactory.Perk arg0);

    HashMap<String, Integer> getReadLiterature();

    boolean isLiteratureRead(String arg0);

    void addReadLiterature(String arg0);

    void addReadLiterature(String arg0, int arg1);

    void addReadPrintMedia(String arg0);

    boolean isPrintMediaRead(String arg0);

    HashSet<String> getReadPrintMedia();

    boolean hasReadMap(InventoryItem var1);

    void addReadMap(InventoryItem var1);

    void triggerContextualAction(String arg0);

    void triggerContextualAction(String arg0, Object arg1);

    void triggerContextualAction(String arg0, Object arg1, Object arg2);

    void triggerContextualAction(String arg0, Object arg1, Object arg2, Object arg3);

    void triggerContextualAction(String arg0, Object arg1, Object arg2, Object arg3, Object arg4);
}
