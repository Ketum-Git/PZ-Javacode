// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;

@UsedFromLua
public final class RDSDevouredByRats extends RandomizedDeadSurvivorBase {
    public RDSDevouredByRats() {
        this.name = "Devoured By Rats";
        this.setChance(1);
        this.setMinimumDays(30);
        this.setUnique(true);
        this.isRat = true;
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        String roomType = "bedroom";
        int roll = Rand.Next(3);
        if (roll == 0) {
            roomType = "kitchen";
        }

        if (roll == 1) {
            roomType = "livingroom";
        }

        RoomDef room = this.getRoomNoKids(def, roomType);
        if (room == null) {
            room = this.getRoom(def, "kitchen");
        }

        if (room == null) {
            room = this.getRoom(def, "livingroom");
        }

        if (room == null) {
            room = this.getRoomNoKids(def, "bedroom");
        }

        if (room != null) {
            int nbrOfSkel = Rand.Next(1, 4);

            for (int i = 0; i < nbrOfSkel; i++) {
                IsoDeadBody body = this.createSkeletonCorpse(room);
                if (body != null) {
                    body.getHumanVisual().setSkinTextureIndex(2);
                    this.addBloodSplat(body.getCurrentSquare(), Rand.Next(7, 12));
                }
            }

            ArrayList<IsoGridSquare> usedSquares = new ArrayList<>();
            int max = room.getIsoRoom().getSquares().size();
            if (max > 21) {
                max = 21;
            }

            int min = max / 2;
            if (min < 1) {
                min = 1;
            }

            if (min > 9) {
                min = 9;
            }

            int nbrOfRats = Rand.Next(min, max);

            for (int ix = 0; ix < nbrOfRats; ix++) {
                IsoGridSquare square = room.getFreeUnoccupiedSquare();
                String breed = "grey";
                if (this.getRoom(def, "laboratory") != null && !Rand.NextBool(3)) {
                    breed = "white";
                }

                if (square != null && square.isFree(true) && !usedSquares.contains(square)) {
                    IsoAnimal animal;
                    if (Rand.NextBool(2)) {
                        animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "rat", breed);
                    } else {
                        animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "ratfemale", breed);
                    }

                    animal.addToWorld();
                    animal.randomizeAge();
                    if (Rand.NextBool(3)) {
                        animal.setStateEventDelayTimer(0.0F);
                    } else {
                        usedSquares.add(square);
                    }
                }
            }

            int nbrOfPoops = Rand.Next(min, max);

            for (int ix = 0; ix < nbrOfPoops; ix++) {
                IsoGridSquare squarex = room.getFreeSquare();
                if (squarex != null && !squarex.isOutside() && squarex.getRoom() != null && squarex.hasRoomDef()) {
                    this.addItemOnGround(squarex, "Base.Dung_Rat");
                }
            }

            def.alarmed = false;
        }
    }
}
