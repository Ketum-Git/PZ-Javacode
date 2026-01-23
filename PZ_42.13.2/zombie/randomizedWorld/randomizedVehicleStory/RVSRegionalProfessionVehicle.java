// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;

@UsedFromLua
public final class RVSRegionalProfessionVehicle extends RandomizedVehicleStoryBase {
    public RVSRegionalProfessionVehicle() {
        this.name = "Regional Profession Vehicle - Will not always spawn a vehicle due to unique vehicle control";
        this.minZoneWidth = 2;
        this.minZoneHeight = 5;
        this.setChance(30);
        this.needsRegion = true;
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0F);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        Vector2 v = IsoDirections.N.ToVector();
        float RAND_ANGLE = (float) (Math.PI / 6);
        if (debug) {
            RAND_ANGLE = 0.0F;
        }

        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle1", 0.0F, 0.0F, v.getDirection(), 2.0F, 5.0F);
        spawner.setParameter("zone", zone);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            String var6 = element.id;
            byte var7 = -1;
            switch (var6.hashCode()) {
                case 2014205573:
                    if (var6.equals("vehicle1")) {
                        var7 = 0;
                    }
                default:
                    switch (var7) {
                        case 0:
                            String region = ItemPickerJava.getSquareRegion(square);
                            Object functionObj = LuaManager.getFunctionObject("ProfessionVehicles.OnCreateRegion");
                            if (functionObj != null) {
                                LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, region, square, IsoDirections.fromAngle(element.direction));
                            }
                    }
            }
        }
    }
}
