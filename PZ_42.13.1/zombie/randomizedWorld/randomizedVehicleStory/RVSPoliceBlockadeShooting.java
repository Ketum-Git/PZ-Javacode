// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

/**
 * Police barricading a road, 2 police cars, some zombies police with guns/rifle, dead corpses around
 */
@UsedFromLua
public final class RVSPoliceBlockadeShooting extends RandomizedVehicleStoryBase {
    public RVSPoliceBlockadeShooting() {
        this.name = "Police Blockade Shooting";
        this.minZoneWidth = 8;
        this.minZoneHeight = 8;
        this.setChance(10);
        this.setMaximumDays(30);
    }

    @Override
    public boolean isValid(Zone zone, IsoChunk chunk, boolean force) {
        boolean isValid = super.isValid(zone, chunk, force);
        return !isValid ? false : zone.isRectangle();
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0F);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        float RAND_ANGLE = (float) (Math.PI / 18);
        if (debug) {
            RAND_ANGLE = 0.0F;
        }

        float xOffset = 1.5F;
        float yOffset = 1.0F;
        if (this.zoneWidth >= 10) {
            xOffset = 2.5F;
            yOffset = 0.0F;
        }

        boolean barricadeNorth = Rand.NextBool(2);
        if (debug) {
            barricadeNorth = true;
        }

        IsoDirections firstCarDir = Rand.NextBool(2) ? IsoDirections.W : IsoDirections.E;
        Vector2 v = firstCarDir.ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle1", -xOffset, yOffset, v.getDirection(), 2.0F, 5.0F);
        v = firstCarDir.Rot180().ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle2", xOffset, -yOffset, v.getDirection(), 2.0F, 5.0F);
        spawner.addElement(
            "barricade", 0.0F, barricadeNorth ? -yOffset - 2.5F : yOffset + 2.5F, IsoDirections.N.ToVector().getDirection(), this.zoneWidth, 1.0F
        );
        int nbrOfCorpse = Rand.Next(7, 15);

        for (int i = 0; i < nbrOfCorpse; i++) {
            spawner.addElement(
                "corpse",
                Rand.Next(-this.zoneWidth / 2.0F + 1.0F, this.zoneWidth / 2.0F - 1.0F),
                barricadeNorth ? Rand.Next(-7, -4) - yOffset : Rand.Next(5, 8) + yOffset,
                IsoDirections.getRandom().ToVector().getDirection(),
                1.0F,
                2.0F
            );
        }

        String scriptName = "Base.CarLightsPolice";
        if (Rand.NextBool(3)) {
            scriptName = "Base.PickUpVanLightsPolice";
        }

        spawner.setParameter("zone", zone);
        spawner.setParameter("script", scriptName);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            String scriptName = spawner.getParameterString("script");
            String var7 = element.id;
            switch (var7) {
                case "barricade":
                    if (this.horizontalZone) {
                        int y1 = PZMath.fastfloor(element.position.y - element.width / 2.0F);
                        int y2 = PZMath.fastfloor(element.position.y + element.width / 2.0F) - 1;
                        int x = PZMath.fastfloor(element.position.x);

                        for (int y = y1; y <= y2; y++) {
                            IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, zone.z);
                            if (sq != null) {
                                if (y != y1 && y != y2) {
                                    sq.AddTileObject(IsoObject.getNew(sq, "construction_01_9", null, false));
                                } else {
                                    sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                                }
                            }
                        }
                    } else {
                        int x1 = PZMath.fastfloor(element.position.x - element.width / 2.0F);
                        int x2 = PZMath.fastfloor(element.position.x + element.width / 2.0F) - 1;
                        int yx = PZMath.fastfloor(element.position.y);

                        for (int x = x1; x <= x2; x++) {
                            IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, yx, zone.z);
                            if (sq != null) {
                                if (x != x1 && x != x2) {
                                    sq.AddTileObject(IsoObject.getNew(sq, "construction_01_8", null, false));
                                } else {
                                    sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                                }
                            }
                        }
                    }
                    break;
                case "corpse":
                    BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
                    if (vehicle1 != null) {
                        createRandomDeadBody(element.position.x, element.position.y, zone.z, element.direction, false, 10, 10, null);
                        IsoDirections dir = this.horizontalZone
                            ? (element.position.x < vehicle1.getX() ? IsoDirections.W : IsoDirections.E)
                            : (element.position.y < vehicle1.getY() ? IsoDirections.N : IsoDirections.S);
                        float direction = dir.ToVector().getDirection();
                        this.addTrailOfBlood(element.position.x, element.position.y, element.z, direction, 5);
                    }
                    break;
                case "vehicle1":
                case "vehicle2":
                    BaseVehicle vehicle = this.addVehicle(
                        zone, element.position.x, element.position.y, z, element.direction, null, scriptName, null, null, true
                    );
                    if (vehicle != null) {
                        vehicle.setAlarmed(false);
                        spawner.setParameter(element.id, vehicle);
                        if (Rand.NextBool(3)) {
                            vehicle.setHeadlightsOn(true);
                            vehicle.setLightbarLightsMode(2);
                            VehiclePart battery = vehicle.getBattery();
                            if (battery != null) {
                                battery.setLastUpdated(0.0F);
                            }
                        }

                        String outfit = "PoliceRiot";
                        if (vehicle.getZombieType() != null && vehicle.hasZombieType("Police_SWAT")) {
                            outfit = vehicle.getRandomZombieType();
                        }

                        Integer fChance = 0;
                        this.addZombiesOnVehicle(Rand.Next(2, 4), outfit, fChance, vehicle);
                    }
            }
        }
    }
}
