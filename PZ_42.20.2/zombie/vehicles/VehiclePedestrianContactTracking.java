// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.HashSet;
import java.util.Set;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.debug.DebugType;
import zombie.util.Pool;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayUtil;

public class VehiclePedestrianContactTracking {
    private final Set<IsoGameCharacter> hitsThisFrame = new HashSet<>();
    private final Set<IsoGameCharacter> hitsCausingDamage = new HashSet<>();
    private final Set<VehiclePedestrianContact> oldContacts = new HashSet<>();
    private final Set<VehiclePedestrianContact> persistentContacts = new HashSet<>();
    private float timeNow;
    private int numHitsThisFrame;
    private int numHitsCausingDamageThisFrame;
    private final float maxTimeOfContact = 3.5F;

    public void postUpdate(BaseVehicle vehicle, Invokers.Params2.ICallback<BaseVehicle, VehiclePedestrianContactTracking> applyDamageCallback) {
        this.updateTimeNow();
        this.populateHitsCausingDamage(vehicle);
        this.moveHitsThisFrameToPersistentContacts();
        this.clearOldContacts();
        this.applyDamage(vehicle, applyDamageCallback);
    }

    private void updateTimeNow() {
        float timeDelta = GameTime.getInstance().getPhysicsSecondsSinceLastUpdate();
        this.timeNow += timeDelta;
    }

    private void applyDamage(BaseVehicle vehicle, Invokers.Params2.ICallback<BaseVehicle, VehiclePedestrianContactTracking> applyDamageCallback) {
        if (applyDamageCallback != null) {
            applyDamageCallback.accept(vehicle, this);
        }

        this.numHitsCausingDamageThisFrame = this.hitsCausingDamage.size();
        this.hitsCausingDamage.clear();
    }

    private void populateHitsCausingDamage(BaseVehicle vehicle) {
        this.hitsCausingDamage.clear();

        for (IsoGameCharacter hitChar : this.hitsThisFrame) {
            if (!hitChar.causesDamageToVehicleWhenHit(vehicle)) {
                DebugType.VehicleHit.trace("Contact causes no damage: %s", hitChar);
            } else if (this.isPersistentContact(hitChar)) {
                DebugType.VehicleHit.trace("Contact is persistent. Being pushed. Causes no damage: %s", hitChar);
            } else {
                this.hitsCausingDamage.add(hitChar);
            }
        }

        if (!this.hitsCausingDamage.isEmpty()) {
            DebugType.VehicleHit.debugln("HitsCausingDamage: %d", this.hitsCausingDamage.size());
        }
    }

    public boolean isPersistentContact(IsoGameCharacter hitChar) {
        return PZArrayUtil.contains(this.persistentContacts, hitChar, VehiclePedestrianContact::isCharacter);
    }

    private void moveHitsThisFrameToPersistentContacts() {
        for (IsoGameCharacter hitChar : this.hitsThisFrame) {
            VehiclePedestrianContact existingContact = PZArrayUtil.find(this.persistentContacts, hitChar, VehiclePedestrianContact::isCharacter);
            if (existingContact != null) {
                existingContact.hitTime = this.getTimeNow();
            } else {
                VehiclePedestrianContact newContact = VehiclePedestrianContact.alloc();
                newContact.character = hitChar;
                newContact.hitTime = this.getTimeNow();
                this.persistentContacts.add(newContact);
            }
        }

        this.numHitsThisFrame = this.hitsThisFrame.size();
        this.hitsThisFrame.clear();
    }

    private float getTimeNow() {
        return this.timeNow;
    }

    private void clearOldContacts() {
        float timeNow = this.getTimeNow();

        for (VehiclePedestrianContact contact : this.persistentContacts) {
            float timeOfContact = contact.hitTime;
            float contactAge = timeNow - timeOfContact;
            if (contactAge > 3.5F) {
                this.oldContacts.add(contact);
            }
        }

        for (VehiclePedestrianContact contactx : this.oldContacts) {
            this.persistentContacts.remove(contactx);
            Pool.tryRelease(contactx);
        }

        this.oldContacts.clear();
    }

    public void onCharacterHit(IsoGameCharacter hitChar) {
        this.hitsThisFrame.add(hitChar);
    }

    public Set<IsoGameCharacter> getHitsCausingDamage() {
        return this.hitsCausingDamage;
    }

    public void logVariables(AnimationPlayerRecorder animationRecorder) {
        animationRecorder.logVariable("contacts.hitsThisFrame", this.numHitsThisFrame);
        animationRecorder.logVariable("contacts.hitsCausingDamage", this.numHitsCausingDamageThisFrame);
        animationRecorder.logVariable("contacts.persistentContacts", this.persistentContacts.size());
    }
}
