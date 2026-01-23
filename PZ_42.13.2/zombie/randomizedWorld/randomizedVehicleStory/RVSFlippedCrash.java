// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

/**
 * Flipped car with bodies & blood near it, can be burnt
 */
@UsedFromLua
public final class RVSFlippedCrash extends RandomizedVehicleStoryBase {
    public RVSFlippedCrash() {
        this.name = "Flipped Crash";
        this.minZoneWidth = 8;
        this.minZoneHeight = 8;
        this.setChance(40);
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0F);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        float RAND_ANGLE = (float) (Math.PI / 6);
        if (debug) {
            RAND_ANGLE = 0.0F;
        }

        Vector2 v = IsoDirections.N.ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle1", 0.0F, 0.0F, v.getDirection(), 2.0F, 5.0F);
        spawner.setParameter("zone", zone);
        spawner.setParameter("burnt", Rand.NextBool(5));
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            boolean burnt = spawner.getParameterBoolean("burnt");
            String var7 = element.id;
            switch (var7) {
                case "vehicle1":
                    BaseVehicle vehicle1 = this.addVehicleFlipped(
                        zone, element.position.x, element.position.y, z + 0.25F, element.direction, burnt ? "normalburnt" : "bad", null, null, null
                    );
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        int rand = Rand.Next(4);
                        String area = null;
                        switch (rand) {
                            case 0:
                                area = "Front";
                                break;
                            case 1:
                                area = "Rear";
                                break;
                            case 2:
                                area = "Left";
                                break;
                            case 3:
                                area = "Right";
                        }

                        vehicle1 = vehicle1.setSmashed(area);
                        vehicle1.setBloodIntensity("Front", Rand.Next(0.3F, 1.0F));
                        vehicle1.setBloodIntensity("Rear", Rand.Next(0.3F, 1.0F));
                        vehicle1.setBloodIntensity("Left", Rand.Next(0.3F, 1.0F));
                        vehicle1.setBloodIntensity("Right", Rand.Next(0.3F, 1.0F));
                        ArrayList<IsoZombie> zedList = this.addZombiesOnVehicle(Rand.Next(2, 4), null, null, vehicle1);
                        if (zedList != null) {
                            for (int i = 0; i < zedList.size(); i++) {
                                IsoZombie zombie = zedList.get(i);
                                this.addBloodSplat(zombie.getSquare(), Rand.Next(10, 20));
                                if (burnt) {
                                    zombie.setSkeleton(true);
                                    zombie.getHumanVisual().setSkinTextureIndex(0);
                                } else {
                                    zombie.DoCorpseInventory();
                                    if (Rand.NextBool(10)) {
                                        zombie.setFakeDead(true);
                                        zombie.crawling = true;
                                        zombie.setCanWalk(false);
                                        zombie.setCrawlerType(1);
                                    }
                                }

                                new IsoDeadBody(zombie, false);
                            }
                        }
                    }
            }
        }
    }
}
