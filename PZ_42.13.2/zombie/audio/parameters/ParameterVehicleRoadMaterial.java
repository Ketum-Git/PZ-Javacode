// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.vehicles.BaseVehicle;

public final class ParameterVehicleRoadMaterial extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleRoadMaterial(BaseVehicle vehicle) {
        super("VehicleRoadMaterial");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        if (!this.vehicle.isEngineRunning()) {
            return Float.isNaN(this.getCurrentValue()) ? 0.0F : this.getCurrentValue();
        } else {
            return this.getMaterial().label;
        }
    }

    private ParameterVehicleRoadMaterial.Material getMaterial() {
        IsoGridSquare square = this.vehicle.getCurrentSquare();
        if (square == null) {
            return ParameterVehicleRoadMaterial.Material.Concrete;
        } else if (IsoWorld.instance.currentCell.gridSquareIsSnow(square.x, square.y, square.z)) {
            return ParameterVehicleRoadMaterial.Material.Snow;
        } else {
            IsoObject floor = this.vehicle.getCurrentSquare().getFloor();
            if (floor != null && floor.getSprite() != null && floor.getSprite().getName() != null) {
                String floorName = floor.getSprite().getName();
                if (floorName.endsWith("blends_natural_01_5")
                    || floorName.endsWith("blends_natural_01_6")
                    || floorName.endsWith("blends_natural_01_7")
                    || floorName.endsWith("blends_natural_01_0")) {
                    return ParameterVehicleRoadMaterial.Material.Sand;
                } else if (floorName.endsWith("blends_natural_01_64")
                    || floorName.endsWith("blends_natural_01_69")
                    || floorName.endsWith("blends_natural_01_70")
                    || floorName.endsWith("blends_natural_01_71")) {
                    return ParameterVehicleRoadMaterial.Material.Dirt;
                } else if (floorName.startsWith("blends_natural_01")) {
                    return ParameterVehicleRoadMaterial.Material.Grass;
                } else if (floorName.endsWith("blends_street_01_48")
                    || floorName.endsWith("blends_street_01_53")
                    || floorName.endsWith("blends_street_01_54")
                    || floorName.endsWith("blends_street_01_55")) {
                    return ParameterVehicleRoadMaterial.Material.Gravel;
                } else if (floorName.startsWith("floors_interior_tilesandwood_01_")) {
                    int index = Integer.parseInt(floorName.replaceFirst("floors_interior_tilesandwood_01_", ""));
                    return index > 40 && index < 48 ? ParameterVehicleRoadMaterial.Material.Wood : ParameterVehicleRoadMaterial.Material.Concrete;
                } else if (floorName.startsWith("carpentry_02_")) {
                    return ParameterVehicleRoadMaterial.Material.Wood;
                } else if (floorName.contains("interior_carpet_")) {
                    return ParameterVehicleRoadMaterial.Material.Carpet;
                } else {
                    float puddles = square.getPuddlesInGround();
                    return puddles > 0.1 ? ParameterVehicleRoadMaterial.Material.Puddle : ParameterVehicleRoadMaterial.Material.Concrete;
                }
            } else {
                return ParameterVehicleRoadMaterial.Material.Concrete;
            }
        }
    }

    static enum Material {
        Concrete(0),
        Grass(1),
        Gravel(2),
        Puddle(3),
        Snow(4),
        Wood(5),
        Carpet(6),
        Dirt(7),
        Sand(8);

        final int label;

        private Material(final int label) {
            this.label = label;
        }
    }
}
