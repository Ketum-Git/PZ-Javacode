// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.BodyDamage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import zombie.GameTime;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Stats;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.WeaponType;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.Temperature;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.MoodleType;

public class Thermoregulator_tryouts {
    private final BodyDamage bodyDamage;
    private final IsoGameCharacter character;
    private final IsoPlayer player;
    private final Stats stats;
    private final Nutrition nutrition;
    private final ClimateManager climate;
    private static final ItemVisuals itemVisuals = new ItemVisuals();
    private static final ItemVisuals itemVisualsCache = new ItemVisuals();
    private static final ArrayList<BloodBodyPartType> coveredParts = new ArrayList<>();
    private static float simulationMultiplier = 1.0F;
    private float setPoint = 37.0F;
    private float metabolicRate = Metabolics.Default.getMet();
    private float metabolicRateReal = this.metabolicRate;
    private float metabolicTarget = Metabolics.Default.getMet();
    private double fluidsMultiplier = 1.0;
    private double energyMultiplier = 1.0;
    private double fatigueMultiplier = 1.0;
    private float bodyHeatDelta;
    private float coreHeatDelta;
    private boolean thermalChevronUp = true;
    private Thermoregulator_tryouts.ThermalNode core;
    private Thermoregulator_tryouts.ThermalNode[] nodes;
    private float totalHeatRaw;
    private float totalHeat;
    private float primTotal;
    private float secTotal;
    private float externalAirTemperature = 27.0F;
    private float airTemperature;
    private float airAndWindTemp;
    private float rateOfChangeCounter;
    private float coreCelciusCache = 37.0F;
    private float coreRateOfChange;
    private float thermalDamage;
    private float damageCounter;

    public Thermoregulator_tryouts(BodyDamage parent) {
        this.bodyDamage = parent;
        this.character = parent.getParentChar();
        this.stats = this.character.getStats();
        if (this.character instanceof IsoPlayer isoPlayer) {
            this.player = isoPlayer;
            this.nutrition = isoPlayer.getNutrition();
        } else {
            this.player = null;
            this.nutrition = null;
        }

        this.climate = ClimateManager.getInstance();
        this.initNodes();
    }

    public static void setSimulationMultiplier(float multiplier) {
        simulationMultiplier = multiplier;
    }

    public void save(ByteBuffer output) throws IOException {
        output.putFloat(this.setPoint);
        output.putFloat(this.metabolicRate);
        output.putFloat(this.metabolicTarget);
        output.putFloat(this.bodyHeatDelta);
        output.putFloat(this.coreHeatDelta);
        output.putFloat(this.thermalDamage);
        output.putFloat(this.damageCounter);
        output.putInt(this.nodes.length);

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            output.putInt(BodyPartType.ToIndex(node.bodyPartType));
            output.putFloat(node.celcius);
            output.putFloat(node.skinCelcius);
            output.putFloat(node.heatDelta);
            output.putFloat(node.primaryDelta);
            output.putFloat(node.secondaryDelta);
        }
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.setPoint = input.getFloat();
        this.metabolicRate = input.getFloat();
        this.metabolicTarget = input.getFloat();
        this.bodyHeatDelta = input.getFloat();
        this.coreHeatDelta = input.getFloat();
        this.thermalDamage = input.getFloat();
        this.damageCounter = input.getFloat();
        int count = input.getInt();

        for (int i = 0; i < count; i++) {
            int nodeIndex = input.getInt();
            float celcius = input.getFloat();
            float skinCelcius = input.getFloat();
            float heatDelta = input.getFloat();
            float primary = input.getFloat();
            float secondary = input.getFloat();
            Thermoregulator_tryouts.ThermalNode node = this.getNodeForType(BodyPartType.FromIndex(nodeIndex));
            if (node != null) {
                node.celcius = celcius;
                node.skinCelcius = skinCelcius;
                node.heatDelta = heatDelta;
                node.primaryDelta = primary;
                node.secondaryDelta = secondary;
            } else {
                DebugLog.log("Couldnt load node: " + BodyPartType.ToString(BodyPartType.FromIndex(nodeIndex)));
            }
        }
    }

    public void reset() {
        this.setPoint = 37.0F;
        this.metabolicRate = Metabolics.Default.getMet();
        this.metabolicTarget = this.metabolicRate;
        this.core.celcius = 37.0F;
        this.bodyHeatDelta = 0.0F;
        this.coreHeatDelta = 0.0F;
        this.thermalDamage = 0.0F;

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            if (node != this.core) {
                node.celcius = 35.0F;
            }

            node.primaryDelta = 0.0F;
            node.secondaryDelta = 0.0F;
            node.skinCelcius = 33.0F;
            node.heatDelta = 0.0F;
        }
    }

    private void initNodes() {
        List<Thermoregulator_tryouts.ThermalNode> nodesList = new ArrayList<>();

        for (int i = 0; i < this.bodyDamage.getBodyParts().size(); i++) {
            BodyPart bodyPart = this.bodyDamage.getBodyParts().get(i);
            Thermoregulator_tryouts.ThermalNode node = null;
            switch (bodyPart.getType()) {
                case Torso_Upper:
                    node = new Thermoregulator_tryouts.ThermalNode(true, 37.0F, bodyPart, 0.25F);
                    this.core = node;
                    break;
                case Head:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 1.0F);
                    break;
                case Neck:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 0.5F);
                    break;
                case Torso_Lower:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 0.25F);
                    break;
                case Groin:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 0.5F);
                    break;
                case UpperLeg_L:
                case UpperLeg_R:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 0.5F);
                    break;
                case LowerLeg_L:
                case LowerLeg_R:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 0.5F);
                    break;
                case Foot_L:
                case Foot_R:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 0.5F);
                    break;
                case UpperArm_L:
                case UpperArm_R:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 0.25F);
                    break;
                case ForeArm_L:
                case ForeArm_R:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 0.25F);
                    break;
                case Hand_L:
                case Hand_R:
                    node = new Thermoregulator_tryouts.ThermalNode(37.0F, bodyPart, 1.0F);
                    break;
                default:
                    DebugLog.log("Warning: couldnt init thermal node for body part '" + this.bodyDamage.getBodyParts().get(i).getType() + "'.");
            }

            if (node != null) {
                nodesList.add(node);
            }
        }

        this.nodes = new Thermoregulator_tryouts.ThermalNode[nodesList.size()];
        nodesList.toArray(this.nodes);

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            BodyPartType parentType = BodyPartContacts.getParent(node.bodyPartType);
            if (parentType != null) {
                node.upstream = this.getNodeForType(parentType);
            }

            BodyPartType[] children = BodyPartContacts.getChildren(node.bodyPartType);
            if (children != null && children.length > 0) {
                node.downstream = new Thermoregulator_tryouts.ThermalNode[children.length];

                for (int j = 0; j < children.length; j++) {
                    node.downstream[j] = this.getNodeForType(children[j]);
                }
            }
        }

        this.core.celcius = this.setPoint;
    }

    public Thermoregulator_tryouts.ThermalNode getNodeForType(BodyPartType type) {
        for (int i = 0; i < this.nodes.length; i++) {
            if (this.nodes[i].bodyPartType == type) {
                return this.nodes[i];
            }
        }

        return null;
    }

    public Thermoregulator_tryouts.ThermalNode getNodeForBloodType(BloodBodyPartType type) {
        for (int i = 0; i < this.nodes.length; i++) {
            if (this.nodes[i].bloodBpt == type) {
                return this.nodes[i];
            }
        }

        return null;
    }

    public float getBodyHeatDelta() {
        return this.bodyHeatDelta;
    }

    public double getFluidsMultiplier() {
        return this.fluidsMultiplier;
    }

    public double getEnergyMultiplier() {
        return this.energyMultiplier;
    }

    public double getFatigueMultiplier() {
        return this.fatigueMultiplier;
    }

    public float getMovementModifier() {
        float Delta = 1.0F;
        if (this.player != null) {
            int lvl = this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA);
            if (lvl == 2) {
                Delta = 0.66F;
            } else if (lvl == 3) {
                Delta = 0.33F;
            } else if (lvl == 4) {
                Delta = 0.0F;
            }

            lvl = this.player.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA);
            if (lvl == 2) {
                Delta = 0.66F;
            } else if (lvl == 3) {
                Delta = 0.33F;
            } else if (lvl == 4) {
                Delta = 0.0F;
            }
        }

        return Delta;
    }

    public float getCombatModifier() {
        float Delta = 1.0F;
        if (this.player != null) {
            int lvl = this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA);
            if (lvl == 2) {
                Delta = 0.66F;
            } else if (lvl == 3) {
                Delta = 0.33F;
            } else if (lvl == 4) {
                Delta = 0.1F;
            }

            lvl = this.player.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA);
            if (lvl == 2) {
                Delta = 0.66F;
            } else if (lvl == 3) {
                Delta = 0.33F;
            } else if (lvl == 4) {
                Delta = 0.1F;
            }
        }

        return Delta;
    }

    public float getCoreTemperature() {
        return this.core.celcius;
    }

    public float getHeatGeneration() {
        return this.metabolicRateReal;
    }

    public float getMetabolicRate() {
        return this.metabolicRate;
    }

    public float getMetabolicTarget() {
        return this.metabolicTarget;
    }

    public float getMetabolicRateReal() {
        return this.metabolicRateReal;
    }

    public float getSetPoint() {
        return this.setPoint;
    }

    public float getCoreHeatDelta() {
        return this.coreHeatDelta;
    }

    public float getCoreRateOfChange() {
        return this.coreRateOfChange;
    }

    public float getExternalAirTemperature() {
        return this.externalAirTemperature;
    }

    public float getCoreTemperatureUI() {
        float v = PZMath.clamp(this.core.celcius, 20.0F, 42.0F);
        if (v < 37.0F) {
            v = (v - 20.0F) / 17.0F * 0.5F;
        } else {
            v = 0.5F + (v - 37.0F) / 5.0F * 0.5F;
        }

        return v;
    }

    public float getHeatGenerationUI() {
        float v = PZMath.clamp(this.metabolicRateReal, 0.0F, Metabolics.MAX.getMet());
        if (v < Metabolics.Default.getMet()) {
            v = v / Metabolics.Default.getMet() * 0.5F;
        } else {
            v = 0.5F + (v - Metabolics.Default.getMet()) / (Metabolics.MAX.getMet() - Metabolics.Default.getMet()) * 0.5F;
        }

        return v;
    }

    public boolean thermalChevronUp() {
        return this.thermalChevronUp;
    }

    public int thermalChevronCount() {
        if (this.coreRateOfChange > 0.01F) {
            return 3;
        } else if (this.coreRateOfChange > 0.001F) {
            return 2;
        } else {
            return this.coreRateOfChange > 1.0E-4F ? 1 : 0;
        }
    }

    public float getCatchAColdDelta() {
        float delta = 0.0F;
        if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) < 1) {
            return delta;
        } else {
            for (int i = 0; i < this.nodes.length; i++) {
                Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
                float skin = 0.0F;
                if (node.skinCelcius < 33.0F) {
                    skin = (node.skinCelcius - 20.0F) / 13.0F;
                    skin = 1.0F - skin;
                    skin *= skin;
                }

                float add = 0.25F * skin * node.skinSurface;
                if (node.bodyWetness > 0.0F) {
                    add *= 1.0F + node.bodyWetness * 1.0F;
                }

                if (node.clothingWetness > 0.5F) {
                    add *= 1.0F + (node.clothingWetness - 0.5F) * 2.0F;
                }

                if (node.bodyPartType == BodyPartType.Neck) {
                    add *= 8.0F;
                } else if (node.bodyPartType == BodyPartType.Torso_Upper) {
                    add *= 16.0F;
                } else if (node.bodyPartType == BodyPartType.Head) {
                    add *= 4.0F;
                }

                delta += add;
            }

            if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) > 1) {
                delta *= this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA);
            }

            return delta;
        }
    }

    public float getTimedActionTimeModifier() {
        float mod = 1.0F;

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            float skin = 0.0F;
            if (node.skinCelcius < 33.0F) {
                skin = (node.skinCelcius - 20.0F) / 13.0F;
                skin = 1.0F - skin;
                skin *= skin;
            }

            float penalty = 0.25F * skin * node.skinSurface;
            if (node.bodyPartType == BodyPartType.Hand_R || node.bodyPartType == BodyPartType.Hand_L) {
                mod += 0.3F * penalty;
            } else if (node.bodyPartType == BodyPartType.ForeArm_R || node.bodyPartType == BodyPartType.ForeArm_L) {
                mod += 0.15F * penalty;
            } else if (node.bodyPartType == BodyPartType.UpperArm_R || node.bodyPartType == BodyPartType.UpperArm_L) {
                mod += 0.1F * penalty;
            }
        }

        return mod;
    }

    public static float getSkinCelciusMin() {
        return 20.0F;
    }

    public static float getSkinCelciusFavorable() {
        return 33.0F;
    }

    public static float getSkinCelciusMax() {
        return 42.0F;
    }

    public void setMetabolicTarget(Metabolics meta) {
        this.setMetabolicTarget(meta.getMet());
    }

    public void setMetabolicTarget(float target) {
        if (!(target < 0.0F) && !(target < this.metabolicTarget)) {
            this.metabolicTarget = target;
            if (this.metabolicTarget > Metabolics.MAX.getMet()) {
                this.metabolicTarget = Metabolics.MAX.getMet();
            }
        }
    }

    private void updateCoreRateOfChange() {
        this.rateOfChangeCounter = this.rateOfChangeCounter + GameTime.instance.getMultiplier();
        if (this.rateOfChangeCounter > 100.0F) {
            this.rateOfChangeCounter = 0.0F;
            this.coreRateOfChange = this.core.celcius - this.coreCelciusCache;
            this.thermalChevronUp = this.coreRateOfChange >= 0.0F;
            this.coreRateOfChange = PZMath.abs(this.coreRateOfChange);
            this.coreCelciusCache = this.core.celcius;
        }
    }

    public float getSimulationMultiplier() {
        return simulationMultiplier;
    }

    public float getDefaultMultiplier() {
        return this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.Default);
    }

    public float getMetabolicRateIncMultiplier() {
        return this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.MetabolicRateInc);
    }

    public float getMetabolicRateDecMultiplier() {
        return this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.MetabolicRateDec);
    }

    public float getBodyHeatMultiplier() {
        return this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.BodyHeat);
    }

    public float getCoreHeatExpandMultiplier() {
        return this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.CoreHeatExpand);
    }

    public float getCoreHeatContractMultiplier() {
        return this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.CoreHeatContract);
    }

    public float getSkinCelciusMultiplier() {
        return this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.SkinCelcius);
    }

    public float getTemperatureAir() {
        return this.climate.getAirTemperatureForCharacter(this.character, false);
    }

    public float getTemperatureAirAndWind() {
        return this.climate.getAirTemperatureForCharacter(this.character, true);
    }

    public float getDbg_totalHeatRaw() {
        return this.totalHeatRaw;
    }

    public float getDbg_totalHeat() {
        return this.totalHeat;
    }

    public float getCoreCelcius() {
        return this.core != null ? this.core.celcius : 0.0F;
    }

    public float getDbg_primTotal() {
        return this.primTotal;
    }

    public float getDbg_secTotal() {
        return this.secTotal;
    }

    private float getSimulationMultiplier(Thermoregulator_tryouts.Multiplier multiplierType) {
        float multiplier = GameTime.instance.getMultiplier();

        return switch (multiplierType) {
            case MetabolicRateInc -> 0.001F;
            case MetabolicRateDec -> 4.0E-4F;
            case BodyHeat -> 2.5E-4F;
            case CoreHeatExpand -> 5.0E-5F;
            case CoreHeatContract -> 5.0E-4F;
            case SkinCelcius, SkinCelciusExpand -> 0.0025F;
            case SkinCelciusContract -> 0.005F;
            case PrimaryDelta -> 5.0E-4F;
            case SecondaryDelta -> 2.5E-4F;
        } * simulationMultiplier;
    }

    public float getThermalDamage() {
        return this.thermalDamage;
    }

    private void updateThermalDamage(float airTemperature) {
        this.damageCounter = this.damageCounter + GameTime.instance.getRealworldSecondsSinceLastUpdate();
        if (this.damageCounter > 1.0F) {
            this.damageCounter = 0.0F;
            if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) == 4
                && airTemperature < 0.0F
                && this.core.celcius - this.coreCelciusCache <= 0.0F) {
                float frostSpeed = (this.core.celcius - 20.0F) / 5.0F;
                frostSpeed = 1.0F - frostSpeed;
                float seconds = 120.0F;
                seconds += 480.0F * frostSpeed;
                this.thermalDamage = this.thermalDamage + 1.0F / seconds * PZMath.clamp_01(PZMath.abs(airTemperature) / 10.0F);
            } else if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA) == 4
                && airTemperature > 37.0F
                && this.core.celcius - this.coreCelciusCache >= 0.0F) {
                float burnspeed = (this.core.celcius - 41.0F) / 1.0F;
                float seconds = 120.0F;
                seconds += 480.0F * burnspeed;
                this.thermalDamage = this.thermalDamage + 1.0F / seconds * PZMath.clamp_01((airTemperature - 37.0F) / 8.0F);
                this.thermalDamage = Math.min(this.thermalDamage, 0.3F);
            } else {
                this.thermalDamage -= 0.011111111F;
            }

            this.thermalDamage = PZMath.clamp_01(this.thermalDamage);
        }

        this.player.getBodyDamage().setColdDamageStage(this.thermalDamage);
    }

    public void update() {
        this.airTemperature = this.climate.getAirTemperatureForCharacter(this.character, false);
        this.airAndWindTemp = this.climate.getAirTemperatureForCharacter(this.character, true);
        this.externalAirTemperature = this.airTemperature;
        this.setPoint = 37.0F;
        if (this.stats.isAboveMinimum(CharacterStat.SICKNESS)) {
            this.setPoint = this.setPoint + this.stats.get(CharacterStat.SICKNESS) * 2.0F;
        }

        this.updateCoreRateOfChange();
        this.updateMetabolicRate();
        this.updateClothing();
        this.updateTest();
    }

    private void updateMetabolicRate() {
        this.setMetabolicTarget(Metabolics.Default.getMet());
        if (this.player != null) {
            if (this.player.isAttacking()) {
                WeaponType weaponType = WeaponType.getWeaponType(this.player);
                switch (weaponType) {
                    case UNARMED:
                        this.setMetabolicTarget(Metabolics.MediumWork);
                        break;
                    case TWO_HANDED:
                        this.setMetabolicTarget(Metabolics.HeavyWork);
                        break;
                    case ONE_HANDED:
                        this.setMetabolicTarget(Metabolics.MediumWork);
                        break;
                    case HEAVY:
                        this.setMetabolicTarget(Metabolics.Running15kmh);
                        break;
                    case KNIFE:
                        this.setMetabolicTarget(Metabolics.LightWork);
                        break;
                    case SPEAR:
                        this.setMetabolicTarget(Metabolics.MediumWork);
                        break;
                    case HANDGUN:
                        this.setMetabolicTarget(Metabolics.UsingTools);
                        break;
                    case FIREARM:
                        this.setMetabolicTarget(Metabolics.LightWork);
                        break;
                    case THROWING:
                        this.setMetabolicTarget(Metabolics.MediumWork);
                        break;
                    case CHAINSAW:
                        this.setMetabolicTarget(Metabolics.Running15kmh);
                }
            }

            if (this.player.isPlayerMoving()) {
                if (this.player.isSprinting()) {
                    this.setMetabolicTarget(Metabolics.Running15kmh);
                } else if (this.player.isRunning()) {
                    this.setMetabolicTarget(Metabolics.Running10kmh);
                } else if (this.player.isSneaking()) {
                    this.setMetabolicTarget(Metabolics.Walking2kmh);
                } else if (this.player.currentSpeed > 0.0F) {
                    this.setMetabolicTarget(Metabolics.Walking5kmh);
                }
            }
        }

        float excercise = PZMath.clamp_01(1.0F - this.stats.get(CharacterStat.ENDURANCE)) * Metabolics.DefaultExercise.getMet();
        this.setMetabolicTarget(excercise * this.getEnergy());
        float weight = PZMath.clamp_01(this.player.getInventory().getCapacityWeight() / this.player.getMaxWeight());
        float ratio = 1.0F + weight * weight * 0.35F;
        this.setMetabolicTarget(this.metabolicTarget * ratio);
        if (!PZMath.equal(this.metabolicRate, this.metabolicTarget)) {
            float rate = this.metabolicTarget - this.metabolicRate;
            if (this.metabolicTarget > this.metabolicRate) {
                this.metabolicRate = this.metabolicRate + rate * this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.MetabolicRateInc);
            } else {
                this.metabolicRate = this.metabolicRate + rate * this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.MetabolicRateDec);
            }
        }

        float metaMod = 1.0F;
        if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) >= 1) {
            metaMod = this.getMovementModifier();
        }

        this.metabolicRateReal = this.metabolicRate * (0.2F + 0.8F * this.getEnergy() * metaMod);
        this.metabolicTarget = -1.0F;
    }

    private void updateTest() {
        float nodeHeatTotal = 0.0F;

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp -= (this.airTemperature - this.airAndWindTemp) / (1.0F + node.windresist);
            }

            node.heatDelta = externalTemp - node.skinCelcius;
            node.heatDelta = node.heatDelta / (1.0F + node.insulation);
            node.heatDelta /= 3.75F;
            float metabolics = this.metabolicRateReal;
            metabolics *= 1.0F + node.insulation * 0.25F;
            node.heatDelta += metabolics;
            float influence = 1.0F;
            nodeHeatTotal += node.heatDelta * node.skinSurface * 1.0F;
            float skinMin = this.core.celcius - 20.0F;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }

            if (skinMin > skinMax) {
                skinMin = skinMax;
            }

            float skinTarget = node.skinCelcius + node.heatDelta;
            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            float multi = 0.0025F;
            if (node.skinCelcius < 33.0F && skinDelta < 0.0F || node.skinCelcius > 33.0F && skinDelta > 0.0F) {
                multi = 2.5E-4F;
            }

            node.skinCelcius += skinDelta * multi;
        }

        this.coreHeatDelta = nodeHeatTotal;
        float coreMulti = 0.0025F;
        if (this.core.celcius < this.setPoint && this.coreHeatDelta < 0.0F) {
            coreMulti = 5.0E-5F;
        }

        if (this.core.celcius > this.setPoint && this.coreHeatDelta > 0.0F) {
            coreMulti = 2.5E-4F;
        }

        this.core.celcius = this.core.celcius + this.coreHeatDelta * coreMulti;
    }

    private void updateTest_4() {
        float nodeHeatTotal = 0.0F;

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float nodeDistCoreInv = 1.0F - node.getDistToCore();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp -= (this.airTemperature - this.airAndWindTemp) / (1.0F + node.windresist);
            }

            node.heatDelta = externalTemp - node.skinCelcius;
            node.heatDelta = node.heatDelta / (1.0F + node.insulation);
            node.heatDelta /= 12.0F;
            float influence = 1.0F;
            nodeHeatTotal += node.heatDelta * node.skinSurface * 1.0F;
            float skinMin = this.core.celcius - 20.0F;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }

            if (skinMin > skinMax) {
                skinMin = skinMax;
            }

            float coreSkinDelta = this.core.celcius - this.setPoint;
            if (coreSkinDelta > 0.0F) {
                coreSkinDelta *= 2.0F;
                coreSkinDelta *= 1.0F + nodeDistCoreInv * 0.25F;
            } else {
                coreSkinDelta *= 1.0F + node.distToCore * 0.25F;
            }

            float skinTarget = node.skinCelcius + node.heatDelta + coreSkinDelta;
            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            float multi = 0.0025F;
            if (node.skinCelcius < 33.0F && skinDelta < 0.0F || node.skinCelcius > 33.0F && skinDelta > 0.0F) {
                multi = 2.5E-4F;
            }

            node.skinCelcius += skinDelta * multi;
        }

        float metabolicDelta = this.metabolicRateReal / Metabolics.Default.getMet() - 1.0F;
        if (metabolicDelta > 0.0F) {
            metabolicDelta /= Metabolics.Default.getMet() * 2.0F;
        }

        this.coreHeatDelta = nodeHeatTotal + metabolicDelta;
        float coreMulti = 0.0025F;
        if (this.core.celcius < this.setPoint && this.coreHeatDelta < 0.0F) {
            coreMulti = 5.0E-5F;
        }

        if (this.core.celcius > this.setPoint && this.coreHeatDelta > 0.0F) {
            coreMulti = 2.5E-4F;
        }

        this.core.celcius = this.core.celcius + this.coreHeatDelta * coreMulti;
    }

    private void updateTest_3() {
        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp -= (this.airTemperature - this.airAndWindTemp) / (1.0F + node.windresist);
            }

            node.heatDelta = externalTemp - node.skinCelcius;
            node.heatDelta = node.heatDelta / (1.0F + node.insulation);
            float nodeDistCoreMod = 1.0F + (1.0F - node.getDistToCore() * 0.5F);
            float nodeMet = this.metabolicRateReal * nodeDistCoreMod;
            nodeMet *= 1.0F + node.insulation * 0.25F;
            node.heatDelta += nodeMet;
            float skinMin = this.core.celcius - 20.0F;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }

            if (skinMin > skinMax) {
                skinMin = skinMax;
            }

            float skinTarget = node.skinCelcius + node.heatDelta / 12.0F;
            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            node.skinCelcius = node.skinCelcius + skinDelta * this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.SkinCelcius);
        }
    }

    private void updateTest_2() {
        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp = (this.airTemperature - this.airAndWindTemp) / (1.0F + node.windresist);
            }

            float nodeHeatDelta = externalTemp - 27.0F;
            node.heatDelta = nodeHeatDelta;
            node.heatDelta = node.heatDelta / (1.0F + node.insulation);
            float nodeDistCoreMod = 1.0F + (1.0F - node.getDistToCore() * 0.8F);
            float nodeMet = this.metabolicRateReal * nodeDistCoreMod;
            nodeMet *= 1.0F + node.insulation;
            node.heatDelta += nodeMet;
            float primTarget = node.heatDelta / 12.0F;
            primTarget = PZMath.clamp(primTarget, -1.0F, 1.0F);
            float primDelta = primTarget - node.primaryDelta;
            node.primaryDelta = node.primaryDelta + primDelta * this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.PrimaryDelta);
            node.primaryDelta = PZMath.clamp(node.primaryDelta, -1.0F, 1.0F);
        }

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            float skinMin = this.core.celcius - 20.0F;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }

            float skinTarget = this.core.celcius - 4.0F;
            if (node.primaryDelta < 0.0F) {
                skinTarget = 33.0F - 20.0F * PZMath.abs(node.primaryDelta);
            } else {
                skinTarget = 33.0F + 20.0F * node.primaryDelta;
            }

            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            node.skinCelcius = node.skinCelcius + skinDelta * this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.SkinCelcius);
            node.skinCelcius = skinTarget;
        }
    }

    private void updateTest_OLD() {
        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp = (this.airTemperature - this.airAndWindTemp) / (1.0F + node.windresist);
            }

            float nodeHeatDelta = externalTemp - node.skinCelcius;
            node.heatDelta = nodeHeatDelta;
            node.heatDelta = node.heatDelta / (1.0F + node.insulation);
            float nodeDistCoreMod = 1.0F + (1.0F - node.getDistToCore() * 0.8F);
            float nodeMet = this.metabolicRateReal * nodeDistCoreMod;
            nodeMet *= 1.0F + node.insulation;
            node.heatDelta += nodeMet;
            node.primaryDelta = node.heatDelta / 12.0F;
            node.primaryDelta = PZMath.clamp(node.primaryDelta, -1.0F, 1.0F);
        }

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            float skinMin = this.core.celcius - 20.0F;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }

            float skinTarget = this.core.celcius - 4.0F;
            if (node.primaryDelta < 0.0F) {
                skinTarget = 33.0F - 20.0F * PZMath.abs(node.primaryDelta);
            } else {
                skinTarget = 33.0F + 20.0F * node.primaryDelta;
            }

            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            node.skinCelcius = node.skinCelcius + skinDelta * this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.SkinCelcius);
            node.skinCelcius = skinTarget;
        }
    }

    private void updateNodesHeatDelta() {
        float weightDelta = PZMath.clamp_01((float)((this.player.getNutrition().getWeight() / 75.0 - 0.5) * 0.666F));
        weightDelta = (weightDelta - 0.5F) * 2.0F;
        float fitnessDelta = this.stats.get(CharacterStat.FITNESS);
        float primHeatMod = 1.0F;
        if (this.airAndWindTemp > this.setPoint - 2.0F) {
            if (this.airTemperature < this.setPoint + 2.0F) {
                primHeatMod = (this.airTemperature - (this.setPoint - 2.0F)) / 4.0F;
                primHeatMod = 1.0F - primHeatMod;
            } else {
                primHeatMod = 0.0F;
            }
        }

        float humidityMod = 1.0F;
        if (this.climate.getHumidity() > 0.5F) {
            float hum = (this.climate.getHumidity() - 0.5F) * 2.0F;
            humidityMod -= hum;
        }

        float coreNegTempMod = 1.0F;
        if (this.core.celcius < 37.0F) {
            coreNegTempMod = (this.core.celcius - 20.0F) / 17.0F;
            coreNegTempMod *= coreNegTempMod;
        }

        float nodes_totalHeat = 0.0F;

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp = (this.airTemperature - this.airAndWindTemp) / (1.0F + node.windresist);
            }

            float nodeHeatDelta = externalTemp - node.skinCelcius;
            if (nodeHeatDelta <= 0.0F) {
                nodeHeatDelta *= 1.0F + 0.75F * node.bodyWetness;
            } else {
                nodeHeatDelta /= 1.0F + 3.0F * node.bodyWetness;
            }

            nodeHeatDelta *= 0.3F;
            nodeHeatDelta /= 1.0F + node.insulation;
            node.heatDelta = nodeHeatDelta * node.skinSurface;
            if (node.primaryDelta > 0.0F) {
                float fluids = 0.2F + 0.8F * this.getBodyFluids();
                float contribution = Metabolics.Default.getMet() * node.primaryDelta * node.skinSurface / node.insulation;
                contribution *= fluids * (0.1F + 0.9F * primHeatMod);
                contribution *= humidityMod;
                contribution *= 1.0F - 0.2F * weightDelta;
                contribution *= 1.0F + 0.2F * fitnessDelta;
                node.heatDelta -= contribution;
            } else {
                float energy = 0.2F + 0.8F * this.getEnergy();
                float contribution = Metabolics.Default.getMet() * PZMath.abs(node.primaryDelta) * node.skinSurface;
                contribution *= energy;
                contribution *= 1.0F + 0.2F * weightDelta;
                contribution *= 1.0F + 0.2F * fitnessDelta;
                node.heatDelta += contribution;
            }

            if (node.secondaryDelta > 0.0F) {
                float fluids = 0.1F + 0.9F * this.getBodyFluids();
                float var30 = Metabolics.MAX.getMet() * 0.75F * node.secondaryDelta * node.skinSurface / node.insulation;
                var30 *= fluids;
                var30 *= 0.85F + 0.15F * humidityMod;
                var30 *= 1.0F - 0.2F * weightDelta;
                var30 *= 1.0F + 0.2F * fitnessDelta;
                node.heatDelta -= var30;
            } else {
                float energy = 0.1F + 0.9F * this.getEnergy();
                float var35 = Metabolics.Default.getMet() * PZMath.abs(node.secondaryDelta) * node.skinSurface;
                var35 *= energy;
                var35 *= 1.0F + 0.2F * weightDelta;
                var35 *= 1.0F + 0.2F * fitnessDelta;
                node.heatDelta += var35;
            }

            nodes_totalHeat += node.heatDelta;
        }

        this.totalHeatRaw = nodes_totalHeat;
        nodes_totalHeat += this.metabolicRateReal;
        this.totalHeat = nodes_totalHeat;
    }

    private void updateNodesHeatDelta_OLD() {
        float weight = (float)this.player.getNutrition().getWeight();
        float weightMod = PZMath.clamp_01((weight / 75.0F - 0.5F) * 0.666F);
        float weightDelta = (weightMod - 0.5F) * 2.0F;
        float fitnessDelta = this.stats.get(CharacterStat.FITNESS);
        float primHeatMod = 1.0F;
        if (this.airAndWindTemp > this.setPoint - 2.0F) {
            if (this.airTemperature < this.setPoint + 2.0F) {
                primHeatMod = (this.airTemperature - (this.setPoint - 2.0F)) / 4.0F;
                primHeatMod = 1.0F - primHeatMod;
            } else {
                primHeatMod = 0.0F;
            }
        }

        float humidityMod = 1.0F;
        if (this.climate.getHumidity() > 0.5F) {
            float hum = (this.climate.getHumidity() - 0.5F) * 2.0F;
            humidityMod -= hum;
        }

        float coreNegTempMod = 1.0F;
        if (this.core.celcius < 37.0F) {
            coreNegTempMod = (this.core.celcius - 20.0F) / 17.0F;
            coreNegTempMod *= coreNegTempMod;
        }

        float nodes_totalHeat = 0.0F;

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float extTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                float diff = this.airTemperature - this.airAndWindTemp;
                diff /= 1.0F + node.windresist;
                extTemp -= diff;
            }

            float insulation = 1.0F + node.insulation;
            float heatDelta = extTemp - node.skinCelcius;
            if (heatDelta < 0.0F) {
                heatDelta *= 1.0F + 0.25F * node.bodyWetness;
            } else if (heatDelta > 0.0F) {
                heatDelta /= 1.0F + node.bodyWetness * 4.0F;
            }

            heatDelta *= 0.3F;
            heatDelta /= insulation;
            node.heatDelta = heatDelta * node.skinSurface;
            if (node.primaryDelta > 0.0F) {
                float fluids = 0.2F + 0.8F * this.getBodyFluids();
                float contribution = Metabolics.Default.getMet() * 1.0F * node.primaryDelta * node.skinSurface / insulation;
                contribution *= fluids * (0.1F + 0.9F * primHeatMod);
                contribution *= humidityMod;
                contribution *= 1.0F - 0.2F * weightDelta;
                contribution *= 1.0F + 0.2F * fitnessDelta;
                node.heatDelta -= contribution;
            } else {
                float energy = 0.2F + 0.8F * this.getEnergy();
                float contribution = Metabolics.Default.getMet() * 1.0F * PZMath.abs(node.primaryDelta) * node.skinSurface;
                contribution *= energy;
                contribution *= 1.0F + 0.2F * weightDelta;
                contribution *= 1.0F + 0.2F * fitnessDelta;
                contribution *= 0.25F + 0.75F * coreNegTempMod;
                node.heatDelta += contribution;
            }

            if (node.secondaryDelta > 0.0F) {
                float fluids = 0.1F + 0.9F * this.getBodyFluids();
                float var33 = Metabolics.MAX.getMet() * 0.75F * node.secondaryDelta * node.skinSurface / insulation;
                var33 *= fluids;
                var33 *= 0.75F + 0.25F * humidityMod;
                var33 *= 1.0F - 0.2F * weightDelta;
                var33 *= 1.0F + 0.2F * fitnessDelta;
                node.heatDelta -= var33;
            } else {
                float energy = 0.1F + 0.9F * this.getEnergy();
                float var38 = Metabolics.Default.getMet() * 1.0F * PZMath.abs(node.secondaryDelta) * node.skinSurface;
                var38 *= energy;
                var38 *= 1.0F + 0.2F * weightDelta;
                var38 *= 1.0F + 0.2F * fitnessDelta;
                var38 *= 0.25F + 0.75F * coreNegTempMod;
                node.heatDelta += var38;
            }

            nodes_totalHeat += node.heatDelta;
        }

        this.totalHeatRaw = nodes_totalHeat;
        nodes_totalHeat += this.metabolicRateReal;
        this.totalHeat = nodes_totalHeat;
    }

    private void updateHeatDeltas() {
        this.coreHeatDelta = this.totalHeat * this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.BodyHeat);
        if (this.coreHeatDelta < 0.0F) {
            if (this.core.celcius > this.setPoint) {
                this.coreHeatDelta = this.coreHeatDelta * (1.0F + (this.core.celcius - this.setPoint) / 2.0F);
            }
        } else if (this.core.celcius < this.setPoint) {
            this.coreHeatDelta = this.coreHeatDelta * (1.0F + (this.setPoint - this.core.celcius) / 4.0F);
        }

        this.core.celcius = this.core.celcius + this.coreHeatDelta;
        this.core.celcius = PZMath.clamp(this.core.celcius, 20.0F, 42.0F);
        this.stats.set(CharacterStat.TEMPERATURE, this.core.celcius);
        this.bodyHeatDelta = 0.0F;
        if (this.core.celcius > this.setPoint) {
            this.bodyHeatDelta = this.core.celcius - this.setPoint;
        } else if (this.core.celcius < this.setPoint) {
            this.bodyHeatDelta = this.core.celcius - this.setPoint;
        }

        if (this.bodyHeatDelta < 0.0F) {
            float bhd = PZMath.abs(this.bodyHeatDelta);
            if (bhd <= 1.0F) {
                this.bodyHeatDelta *= 0.8F;
            } else {
                bhd = PZMath.clamp(bhd, 1.0F, 11.0F) - 1.0F;
                bhd /= 10.0F;
                this.bodyHeatDelta = -0.8F + -0.2F * bhd;
            }
        }

        this.bodyHeatDelta = PZMath.clamp(this.bodyHeatDelta, -1.0F, 1.0F);
    }

    private void updateNodes() {
        float prim = 0.0F;
        float sec = 0.0F;

        for (int i = 0; i < this.nodes.length; i++) {
            Thermoregulator_tryouts.ThermalNode node = this.nodes[i];
            float insulation = 1.0F + node.insulation;
            float metContribution = this.metabolicRateReal / Metabolics.MAX.getMet();
            metContribution *= metContribution;
            if (this.bodyHeatDelta < 0.0F) {
                float dist = node.distToCore;
                node.primaryDelta = this.bodyHeatDelta * (1.0F + dist);
            } else {
                node.primaryDelta = this.bodyHeatDelta * (1.0F + (1.0F - node.distToCore));
            }

            node.primaryDelta = PZMath.clamp(node.primaryDelta, -1.0F, 1.0F);
            node.secondaryDelta = node.primaryDelta * PZMath.abs(node.primaryDelta) * PZMath.abs(node.primaryDelta);
            prim += node.primaryDelta * node.skinSurface;
            sec += node.secondaryDelta * node.skinSurface;
            if (this.stats.get(CharacterStat.INTOXICATION) > 0.0F) {
                node.primaryDelta = node.primaryDelta + this.stats.get(CharacterStat.INTOXICATION) * 0.02F;
            }

            node.primaryDelta = PZMath.clamp(node.primaryDelta, -1.0F, 1.0F);
            float skinMin = this.core.celcius - 20.0F;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                if (this.airTemperature < 33.0F) {
                    skinMin = this.airTemperature;
                } else {
                    float distCore = 0.4F + 0.6F * (1.0F - node.distToCore);
                    float skinmod = (this.airTemperature - 33.0F) / 6.0F;
                    skinMin = 33.0F;
                    skinMin += 4.0F * skinmod * distCore;
                    skinMin = PZMath.clamp(skinMin, 33.0F, this.airTemperature);
                    if (skinMin > skinMax) {
                        skinMin = skinMax - 0.25F;
                    }
                }
            }

            float skinTarget = this.core.celcius - 4.0F;
            if (node.primaryDelta < 0.0F) {
                float distCore = 0.4F + 0.6F * node.distToCore;
                float target = skinTarget - 12.0F * distCore / insulation;
                skinTarget = PZMath.c_lerp(skinTarget, target, PZMath.abs(node.primaryDelta));
            } else {
                float distCore = 0.4F + 0.6F * (1.0F - node.distToCore);
                float targetAdd = 4.0F * distCore;
                targetAdd *= Math.max(insulation * 0.5F * distCore, 1.0F);
                float var23 = Math.min(skinTarget + targetAdd, skinMax);
                skinTarget = PZMath.c_lerp(skinTarget, var23, node.primaryDelta);
            }

            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            float multipl = 1.0F;
            if (skinDelta < 0.0F && node.skinCelcius > 33.0F) {
                multipl = 3.0F;
            } else if (skinDelta > 0.0F && node.skinCelcius < 33.0F) {
                multipl = 3.0F;
            }

            node.skinCelcius = node.skinCelcius + skinDelta * multipl * this.getSimulationMultiplier(Thermoregulator_tryouts.Multiplier.SkinCelcius);
            if (node != this.core) {
                if (node.skinCelcius >= this.core.celcius) {
                    node.celcius = this.core.celcius;
                } else {
                    node.celcius = PZMath.lerp(node.skinCelcius, this.core.celcius, 0.5F);
                }
            }
        }

        this.primTotal = prim;
        this.secTotal = sec;
    }

    private void updateBodyMultipliers() {
        this.energyMultiplier = 1.0;
        this.fluidsMultiplier = 1.0;
        this.fatigueMultiplier = 1.0;
        float metaEnergy = this.metabolicRateReal / Metabolics.Default.getMet();
        this.energyMultiplier = PZMath.clamp(metaEnergy * metaEnergy, 1.0F, 5.0F);
        float mod = PZMath.abs(this.primTotal);
        mod *= mod;
        if (this.primTotal < 0.0F) {
            this.energyMultiplier += 0.75F * mod;
            this.fatigueMultiplier += 0.5F * mod;
        } else if (this.primTotal > 0.0F) {
            this.fluidsMultiplier += 0.75F * mod;
            this.fatigueMultiplier += 0.5F * mod;
        }

        mod = PZMath.abs(this.secTotal);
        mod *= mod;
        if (this.secTotal < 0.0F) {
            this.energyMultiplier += 8.0F * mod;
            this.fatigueMultiplier += 3.5F * mod;
        } else if (this.secTotal > 0.0F) {
            this.fluidsMultiplier += 6.0F * mod;
            this.fatigueMultiplier += 3.5F * mod;
        }
    }

    private void updateClothing() {
        this.character.getItemVisuals(itemVisuals);
        boolean doUpdate = itemVisuals.size() != itemVisualsCache.size();
        if (!doUpdate) {
            for (int i = 0; i < itemVisuals.size(); i++) {
                if (i >= itemVisualsCache.size() || itemVisuals.get(i) != itemVisualsCache.get(i)) {
                    doUpdate = true;
                    break;
                }
            }
        }

        if (doUpdate) {
            for (int ix = 0; ix < this.nodes.length; ix++) {
                this.nodes[ix].clothing.clear();
            }

            itemVisualsCache.clear();

            for (int ix = 0; ix < itemVisuals.size(); ix++) {
                ItemVisual itemVisual = itemVisuals.get(ix);
                InventoryItem item = itemVisual.getInventoryItem();
                itemVisualsCache.add(itemVisual);
                if (item instanceof Clothing clothing && (clothing.getInsulation() > 0.0F || clothing.getWindresistance() > 0.0F)) {
                    boolean added = false;
                    ArrayList<BloodClothingType> types = item.getBloodClothingType();
                    if (types != null) {
                        coveredParts.clear();
                        BloodClothingType.getCoveredParts(types, coveredParts);

                        for (int j = 0; j < coveredParts.size(); j++) {
                            BloodBodyPartType part = coveredParts.get(j);
                            if (part.index() >= 0 && part.index() < this.nodes.length) {
                                added = true;
                                this.nodes[part.index()].clothing.add(clothing);
                            }
                        }
                    }

                    if (!added && clothing.getBodyLocation() != null) {
                        ItemBodyLocation bodyLocation = clothing.getBodyLocation();
                        if (bodyLocation.equals(ItemBodyLocation.HAT) || bodyLocation.equals(ItemBodyLocation.MASK)) {
                            this.nodes[BodyPartType.ToIndex(BodyPartType.Head)].clothing.add(clothing);
                        }
                    }
                }
            }
        }
    }

    public float getEnergy() {
        float h = 1.0F - this.stats.get(CharacterStat.HUNGER) * this.stats.get(CharacterStat.HUNGER);
        float f = 1.0F - this.stats.get(CharacterStat.FATIGUE) * this.stats.get(CharacterStat.FATIGUE);
        return 0.6F * h + 0.4F * f;
    }

    public float getBodyFluids() {
        return 1.0F - this.stats.get(CharacterStat.THIRST);
    }

    private static enum Multiplier {
        Default,
        MetabolicRateInc,
        MetabolicRateDec,
        BodyHeat,
        CoreHeatExpand,
        CoreHeatContract,
        SkinCelcius,
        SkinCelciusContract,
        SkinCelciusExpand,
        PrimaryDelta,
        SecondaryDelta;
    }

    public class ThermalNode {
        private final float distToCore;
        private final float skinSurface;
        private final BodyPartType bodyPartType;
        private final BloodBodyPartType bloodBpt;
        private final BodyPart bodyPart;
        private final boolean isCore;
        private final float insulationLayerMultiplierUi;
        private Thermoregulator_tryouts.ThermalNode upstream;
        private Thermoregulator_tryouts.ThermalNode[] downstream;
        private float insulation;
        private float windresist;
        private float celcius;
        private float skinCelcius;
        private float heatDelta;
        private float primaryDelta;
        private float secondaryDelta;
        private float clothingWetness;
        private float bodyWetness;
        private final ArrayList<Clothing> clothing;

        public ThermalNode(final float init_temperature, final BodyPart bodyPart, final float insulationMultiplier) {
            this(false, init_temperature, bodyPart, insulationMultiplier);
        }

        public ThermalNode(final boolean isCore, final float init_temperature, final BodyPart bodyPart, final float insulationMultiplier) {
            Objects.requireNonNull(Thermoregulator_tryouts.this);
            super();
            this.celcius = 37.0F;
            this.skinCelcius = 33.0F;
            this.clothing = new ArrayList<>();
            this.isCore = isCore;
            this.celcius = init_temperature;
            this.distToCore = BodyPartType.GetDistToCore(bodyPart.type);
            this.skinSurface = BodyPartType.GetSkinSurface(bodyPart.type);
            this.bodyPartType = bodyPart.type;
            this.bloodBpt = BloodBodyPartType.FromIndex(BodyPartType.ToIndex(bodyPart.type));
            this.bodyPart = bodyPart;
            this.insulationLayerMultiplierUi = insulationMultiplier;
        }

        private void calculateInsulation() {
            int layers = this.clothing.size();
            this.insulation = 0.0F;
            this.windresist = 0.0F;
            this.clothingWetness = 0.0F;
            this.bodyWetness = this.bodyPart != null ? this.bodyPart.getWetness() * 0.01F : 0.0F;
            this.bodyWetness = PZMath.clamp_01(this.bodyWetness);
            if (layers > 0) {
                for (int i = 0; i < layers; i++) {
                    Clothing item = this.clothing.get(i);
                    ItemVisual itemVisual = item.getVisual();
                    float item_wetness = PZMath.clamp(item.getWetness() * 0.01F, 0.0F, 1.0F);
                    this.clothingWetness += item_wetness;
                    boolean hasHole = itemVisual.getHole(this.bloodBpt) > 0.0F;
                    if (!hasHole) {
                        float item_insulation = Temperature.getTrueInsulationValue(item.getInsulation());
                        float item_windResist = Temperature.getTrueWindresistanceValue(item.getWindresistance());
                        float item_condition = PZMath.clamp(item.getCurrentCondition() * 0.01F, 0.0F, 1.0F);
                        item_condition = 0.5F + 0.5F * item_condition;
                        item_insulation *= (1.0F - item_wetness * 0.75F) * item_condition;
                        item_windResist *= (1.0F - item_wetness * 0.45F) * item_condition;
                        this.insulation += item_insulation;
                        this.windresist += item_windResist;
                    }
                }

                this.clothingWetness /= layers;
                this.insulation += layers * 0.05F;
                this.windresist += layers * 0.05F;
            }
        }

        public boolean hasUpstream() {
            return this.upstream != null;
        }

        public boolean hasDownstream() {
            return this.downstream != null && this.downstream.length > 0;
        }

        public float getDistToCore() {
            return this.distToCore;
        }

        public float getSkinSurface() {
            return this.skinSurface;
        }

        public boolean isCore() {
            return this.isCore;
        }

        public float getInsulation() {
            return this.insulation;
        }

        public float getWindresist() {
            return this.windresist;
        }

        public float getCelcius() {
            return this.celcius;
        }

        public float getSkinCelcius() {
            return this.skinCelcius;
        }

        public float getHeatDelta() {
            return this.heatDelta;
        }

        public float getPrimaryDelta() {
            return this.primaryDelta;
        }

        public float getSecondaryDelta() {
            return this.secondaryDelta;
        }

        public float getClothingWetness() {
            return this.clothingWetness;
        }

        public float getBodyWetness() {
            return this.bodyWetness;
        }

        public float getBodyResponse() {
            return PZMath.lerp(this.primaryDelta, this.secondaryDelta, 0.5F);
        }

        public float getSkinCelciusUI() {
            float v = PZMath.clamp(this.getSkinCelcius(), 20.0F, 42.0F);
            if (v < 33.0F) {
                v = (v - 20.0F) / 13.0F * 0.5F;
            } else {
                v = 0.5F + (v - 33.0F) / 9.0F;
            }

            return v;
        }

        public float getHeatDeltaUI() {
            return PZMath.clamp((this.heatDelta * 0.2F + 1.0F) / 2.0F, 0.0F, 1.0F);
        }

        public float getPrimaryDeltaUI() {
            return PZMath.clamp((this.primaryDelta + 1.0F) / 2.0F, 0.0F, 1.0F);
        }

        public float getSecondaryDeltaUI() {
            return PZMath.clamp((this.secondaryDelta + 1.0F) / 2.0F, 0.0F, 1.0F);
        }

        public float getInsulationUI() {
            return PZMath.clamp(this.insulation * this.insulationLayerMultiplierUi, 0.0F, 1.0F);
        }

        public float getWindresistUI() {
            return PZMath.clamp(this.windresist * this.insulationLayerMultiplierUi, 0.0F, 1.0F);
        }

        public float getClothingWetnessUI() {
            return PZMath.clamp(this.clothingWetness, 0.0F, 1.0F);
        }

        public float getBodyWetnessUI() {
            return PZMath.clamp(this.bodyWetness, 0.0F, 1.0F);
        }

        public float getBodyResponseUI() {
            return PZMath.clamp((this.getBodyResponse() + 1.0F) / 2.0F, 0.0F, 1.0F);
        }
    }
}
