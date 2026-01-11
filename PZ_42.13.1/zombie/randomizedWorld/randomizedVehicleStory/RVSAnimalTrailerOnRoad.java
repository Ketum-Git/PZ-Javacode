// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.inventory.types.Food;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public final class RVSAnimalTrailerOnRoad extends RandomizedVehicleStoryBase {
    public RVSAnimalTrailerOnRoad() {
        this.name = "Livestock Trailer On Road";
        this.minZoneWidth = 5;
        this.minZoneHeight = 12;
        this.setChance(45);
    }

    private static ArrayList<RVSAnimalTrailerOnRoad.AnimalSpawn> getAnimalType() {
        ArrayList<RVSAnimalTrailerOnRoad.AnimalSpawn> result = new ArrayList<>();
        result.add(new RVSAnimalTrailerOnRoad.AnimalSpawn("bull", "cow", 1, 2));
        result.add(new RVSAnimalTrailerOnRoad.AnimalSpawn("ram", "ewe", 2, 4));
        result.add(new RVSAnimalTrailerOnRoad.AnimalSpawn("boar", "sow", 2, 4));
        result.add(new RVSAnimalTrailerOnRoad.AnimalSpawn("cockerel", "hen", 3, 10));
        result.add(new RVSAnimalTrailerOnRoad.AnimalSpawn("gobblers", "turkeyhen", 3, 6));
        result.add(new RVSAnimalTrailerOnRoad.AnimalSpawn("rabdoe", "rabbuck", 3, 10));
        return result;
    }

    private static ArrayList<RVSAnimalTrailerOnRoad.FoodSpawn> getFoodType() {
        ArrayList<RVSAnimalTrailerOnRoad.FoodSpawn> result = new ArrayList<>();
        result.add(new RVSAnimalTrailerOnRoad.FoodSpawn("AnimalFeedBag", 2, 5));
        result.add(new RVSAnimalTrailerOnRoad.FoodSpawn("HayTuft", 15, 50));
        result.add(new RVSAnimalTrailerOnRoad.FoodSpawn("GrassTuft", 15, 50));
        return result;
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
        float vehicle1X = 0.0F;
        float vehicle1Y = -1.5F;
        spawner.addElement("vehicle1", 0.0F, -1.5F, v.getDirection(), 2.0F, 5.0F);
        int trailerLength = 4;
        boolean east = Rand.NextBool(2);
        v = east ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        float vehicle2X = 0.0F;
        float vehicle2Y = -5.0F;
        spawner.addElement("vehicle2", 0.0F, -5.0F, v.getDirection(), 2.0F, 5.0F);
        spawner.addElement("animals", 0.0F, 8.5F, v.getDirection(), 2.0F, 4.0F);
        spawner.setParameter("zone", zone);
        spawner.setParameter("east", east);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            boolean east = spawner.getParameterBoolean("east");
            String var7 = element.id;
            switch (var7) {
                case "vehicle1":
                    BaseVehicle vehicle1 = this.addVehicle(
                        zone, element.position.x, element.position.y, z, element.direction, null, "Base.PickUpVan", null, "Rancher", true
                    );
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        vehicle1 = vehicle1.setSmashed("Front");
                        BaseVehicle trailer = null;
                        if (Rand.NextBool(2)) {
                            trailer = this.addTrailer(vehicle1, zone, square.getChunk(), null, null, "Base.Trailer_Livestock");
                        } else {
                            trailer = this.addTrailer(vehicle1, zone, square.getChunk(), null, null, "Base.Trailer_Horsebox");
                        }

                        spawner.setParameter("vehicle1", vehicle1);
                        spawner.setParameter("trailer", trailer);
                        if (Rand.Next(100) < 80) {
                            this.addZombiesOnVehicle(1, "Farmer", null, vehicle1);
                        }
                    }
                    break;
                case "vehicle2":
                    BaseVehicle vehicle2 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null);
                    if (vehicle2 != null) {
                        vehicle2.setAlarmed(false);
                        String location = east ? "Right" : "Left";
                        vehicle2 = vehicle2.setSmashed(location);
                        vehicle2.setBloodIntensity(location, 1.0F);
                        spawner.setParameter("vehicle2", vehicle2);
                    }
                    break;
                case "animals":
                    this.spawnAnimals(spawner, square);
            }
        }
    }

    private void spawnAnimals(VehicleStorySpawner spawner, IsoGridSquare square) {
        ArrayList<RVSAnimalTrailerOnRoad.AnimalSpawn> animals = getAnimalType();
        RVSAnimalTrailerOnRoad.AnimalSpawn toSpawn = animals.get(Rand.Next(0, animals.size()));
        int nb = Rand.Next(toSpawn.minNbOfAnimals, toSpawn.maxNbOfAnimals);
        String breed = "";
        ArrayList<IsoAnimal> animalsSpawned = new ArrayList<>();
        IsoAnimal male = null;
        ArrayList<IsoAnimal> female = new ArrayList<>();
        IsoGridSquare sq = this.getRandomSquare(square.getX(), square.getY(), square.getZ());
        if (sq != null) {
            IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), sq.getX(), sq.getY(), sq.getZ(), toSpawn.male, "");
            this.randomizeAnimal(animal, null);
            male = animal;
            animalsSpawned.add(animal);
            breed = animal.getBreed().name;
            nb--;
        }

        for (int i = 0; i < nb; i++) {
            sq = this.getRandomSquare(square.getX(), square.getY(), square.getZ());
            if (sq != null) {
                IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), sq.getX(), sq.getY(), sq.getZ(), toSpawn.female, breed);
                this.randomizeAnimal(animal, male);
                animalsSpawned.add(animal);
                breed = animal.getBreed().name;
                female.add(animal);
                if (animal.getData().getDaysSurvived() >= animal.getMinAgeForBaby() && Rand.NextBool(6)) {
                    IsoAnimal baby = animal.addBaby();
                    this.randomizeAnimal(baby, null);
                    animalsSpawned.add(baby);
                    nb--;
                }
            }
        }

        BaseVehicle trailer = spawner.getParameter("trailer", BaseVehicle.class);
        if (trailer != null) {
            boolean openedDoor = false;
            if (Rand.NextBool(3)) {
                openedDoor = true;
                VehiclePart door = trailer.getPartById("TrunkDoorOpened");
                if (door != null && door.getDoor() != null) {
                    door.getDoor().setOpen(true);
                }
            }

            for (int ix = 0; ix < animalsSpawned.size(); ix++) {
                IsoAnimal animal = animalsSpawned.get(ix);
                if (openedDoor && (Rand.Next(100) <= 30 || !trailer.canAddAnimalInTrailer(animal))) {
                    if (Rand.NextBool(10)) {
                        animal.setHealth(0.0F);
                    }
                } else {
                    trailer.addAnimalInTrailer(animal);
                }
            }

            this.spawnFood(trailer);
            this.spawnEggs(trailer, male, female);
        }
    }

    private void spawnEggs(BaseVehicle trailer, IsoAnimal male, ArrayList<IsoAnimal> femaleList) {
        if (femaleList != null && !femaleList.isEmpty()) {
            IsoAnimal animal = femaleList.get(0);
            if (animal != null && animal.adef != null && !StringUtils.isNullOrEmpty(animal.adef.eggType)) {
                VehiclePart eggCont = trailer.getPartById("TrailerAnimalEggs");
                if (eggCont != null && eggCont.getItemContainer() != null) {
                    int nbOfEggs = Rand.Next(3, 10);

                    for (int i = 0; i < nbOfEggs; i++) {
                        IsoAnimal female = femaleList.get(Rand.Next(0, femaleList.size()));
                        Food egg = female.createEgg();
                        egg.setFertilizedTime(Rand.Next(2, 20));
                        eggCont.getItemContainer().addItem(egg);
                    }
                }
            }
        }
    }

    private void spawnFood(BaseVehicle trailer) {
        VehiclePart foodCont = trailer.getPartById("TrailerAnimalFood");
        if (foodCont != null && foodCont.getItemContainer() != null) {
            ArrayList<RVSAnimalTrailerOnRoad.FoodSpawn> foodList = getFoodType();
            RVSAnimalTrailerOnRoad.FoodSpawn food = foodList.get(Rand.Next(0, foodList.size()));
            foodCont.getItemContainer().AddItems(food.type, Rand.Next(food.min, food.max));
        }
    }

    private IsoGridSquare getRandomSquare(int x, int y, int z) {
        IsoGridSquare sq = null;

        for (int i = 0; i < 20; i++) {
            sq = IsoWorld.instance.getCell().getGridSquare(Rand.Next(x - 5, x + 5), Rand.Next(y - 5, y + 5), z);
            if (sq != null && sq.isFree(false)) {
                return sq;
            }
        }

        return sq;
    }

    private void randomizeAnimal(IsoAnimal animal, IsoAnimal male) {
        animal.addToWorld();
        animal.randomizeAge();
        animal.setWild(false);
        animal.setHealth(Rand.Next(0.5F, 1.0F));
        animal.setDebugStress(Rand.Next(30.0F, 70.0F));
        if (Rand.NextBool(5) && animal.isFemale() && male != null && animal.getData().getDaysSurvived() >= animal.getMinAgeForBaby()) {
            animal.fertilize(male, true);
            animal.getData().setPregnancyTime(Rand.Next(20, 60));
            animal.getData().setFertilizedTime(Rand.Next(20, 60));
        }
    }

    private static final class AnimalSpawn {
        String male;
        String female;
        int maxNbOfAnimals;
        int minNbOfAnimals;

        public AnimalSpawn(String male, String female, int minNb, int maxNb) {
            this.male = male;
            this.female = female;
            this.minNbOfAnimals = minNb;
            this.maxNbOfAnimals = maxNb;
        }
    }

    private static final class FoodSpawn {
        String type;
        int max;
        int min;

        public FoodSpawn(String type, int minNb, int maxNb) {
            this.type = type;
            this.min = minNb;
            this.max = maxNb;
        }
    }
}
