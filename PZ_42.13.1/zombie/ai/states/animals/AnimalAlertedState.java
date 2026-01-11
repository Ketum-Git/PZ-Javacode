// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;

public final class AnimalAlertedState extends State {
    private static final AnimalAlertedState _instance = new AnimalAlertedState();
    float alertedFor;
    float spottedDist;

    public static AnimalAlertedState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        this.alertedFor = 0.0F;
        if (animal.alertedChr != null && animal.alertedChr.getCurrentSquare() != null) {
            this.spottedDist = animal.alertedChr.getCurrentSquare().DistToProper((int)animal.getX(), (int)animal.getY());
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        if (animal.alertedChr != null && animal.alertedChr.getCurrentSquare() != null) {
            this.setTurnAlertedValues(animal, animal.alertedChr);
            if (this.alertedFor > 1000.0F) {
                animal.setIsAlerted(false);
                animal.alertedChr = null;
                animal.setDefaultState();
            }

            this.alertedFor = this.alertedFor + GameTime.getInstance().getMultiplier();
            if (animal.alertedChr != null && animal.alertedChr.getCurrentSquare() != null) {
                float dist = animal.alertedChr.getCurrentSquare().DistToProper((int)animal.getX(), (int)animal.getY());
                if (animal.alertedChr != null && animal.alertedChr.getCurrentSquare() != null && (dist < this.spottedDist - 2.0F || dist <= 4.0F)) {
                    animal.spottedChr = animal.alertedChr;
                    animal.getBehavior().lastAlerted = 10000.0F;
                    animal.setIsAlerted(false);
                    animal.alertedChr = null;
                }
            }
        } else {
            animal.setDefaultState();
        }
    }

    public void setTurnAlertedValues(IsoAnimal animal, IsoMovingObject chr) {
        IsoAnimal.tempVector2.x = animal.getX() - chr.getX();
        IsoAnimal.tempVector2.y = animal.getY() - chr.getY();
        float radDirOfSound = IsoAnimal.tempVector2.getDirectionNeg();
        if (radDirOfSound < 0.0F) {
            radDirOfSound = Math.abs(radDirOfSound);
        } else {
            radDirOfSound = (float)((Math.PI * 2) - radDirOfSound);
        }

        double degreeOfSound = Math.toDegrees(radDirOfSound);
        IsoAnimal.tempVector2.x = IsoDirections.reverse(animal.getDir()).ToVector().x;
        IsoAnimal.tempVector2.y = IsoDirections.reverse(animal.getDir()).ToVector().y;
        IsoAnimal.tempVector2.normalize();
        float radDirOfZombie = IsoAnimal.tempVector2.getDirectionNeg();
        if (radDirOfZombie < 0.0F) {
            radDirOfZombie = Math.abs(radDirOfZombie);
        } else {
            radDirOfZombie = (float) (Math.PI * 2) - radDirOfZombie;
        }

        double degreeOfZombie = Math.toDegrees(radDirOfZombie);
        if ((int)degreeOfZombie == 360) {
            degreeOfZombie = 0.0;
        }

        if ((int)degreeOfSound == 360) {
            degreeOfSound = 0.0;
        }

        int sumUpDegree = 0;
        float degX = 0.0F;
        if (degreeOfSound > degreeOfZombie) {
            sumUpDegree = (int)(degreeOfSound - degreeOfZombie);
            if (sumUpDegree > 180) {
                sumUpDegree = 180 - (sumUpDegree - 180);
                degX = sumUpDegree / 180.0F - sumUpDegree / 180.0F * 2.0F;
            } else {
                degX = sumUpDegree / 180.0F;
            }
        } else {
            sumUpDegree = (int)(degreeOfZombie - degreeOfSound);
            degX = sumUpDegree / 180.0F - sumUpDegree / 180.0F * 2.0F;
        }

        if (GameClient.client) {
            animal.setVariable("AlertX", IsoAnimal.tempVector2.set(chr.getX() - animal.getX(), chr.getY() - animal.getY()).getDirection());
            animal.setVariable("AlertY", 0.0F);
        } else {
            animal.setVariable("AlertX", degX);
            animal.setVariable("AlertY", 0.0F);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }
}
