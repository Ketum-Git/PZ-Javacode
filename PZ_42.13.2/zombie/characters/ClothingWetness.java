// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.ZomboidGlobals;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Thermoregulator;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.scripting.objects.ItemTag;

public final class ClothingWetness {
    private static final ItemVisuals itemVisuals = new ItemVisuals();
    private static final ArrayList<BloodBodyPartType> coveredParts = new ArrayList<>();
    private final IsoGameCharacter character;
    private final ClothingWetness.ItemList[] clothingList = new ClothingWetness.ItemList[BloodBodyPartType.MAX.index()];
    private final int[] perspiringParts = new int[BloodBodyPartType.MAX.index()];
    public boolean changed = true;

    public ClothingWetness(IsoGameCharacter character) {
        this.character = character;

        for (int i = 0; i < this.clothingList.length; i++) {
            this.clothingList[i] = new ClothingWetness.ItemList();
        }
    }

    public void calculateExposedItems() {
        for (int i = 0; i < this.clothingList.length; i++) {
            this.clothingList[i].clear();
        }

        this.character.getItemVisuals(itemVisuals);

        for (int i = itemVisuals.size() - 1; i >= 0; i--) {
            ItemVisual itemVisual = itemVisuals.get(i);
            InventoryItem item = itemVisual.getInventoryItem();
            if (item != null) {
                ArrayList<BloodClothingType> types = item.getBloodClothingType();
                if (types != null) {
                    coveredParts.clear();
                    BloodClothingType.getCoveredParts(types, coveredParts);

                    for (int j = 0; j < coveredParts.size(); j++) {
                        BloodBodyPartType part = coveredParts.get(j);
                        this.clothingList[part.index()].add(item);
                    }
                }
            }
        }
    }

    public void updateWetness(float outerWetnessInc, float outerWetnessDec) {
        boolean umbrella = false;
        InventoryItem umbrellaItem = this.character.getPrimaryHandItem();
        if (umbrellaItem != null && umbrellaItem.isProtectFromRainWhileEquipped()) {
            umbrella = true;
        }

        umbrellaItem = this.character.getSecondaryHandItem();
        if (umbrellaItem != null && umbrellaItem.isProtectFromRainWhileEquipped()) {
            umbrella = true;
        }

        if (this.changed) {
            this.changed = false;
            this.calculateExposedItems();
        }

        this.character.getItemVisuals(itemVisuals);

        for (int i = 0; i < itemVisuals.size(); i++) {
            InventoryItem item = itemVisuals.get(i).getInventoryItem();
            if (item instanceof Clothing clothing) {
                if (item.hasTag(ItemTag.BREAK_WHEN_WET) && item.getWetness() >= 100.0F) {
                    item.setCondition(0);
                    this.character.onWornItemsChanged();
                }

                if (item.getBloodClothingType() == null) {
                    clothing.updateWetness(true);
                } else {
                    clothing.flushWetness();
                }
            }
        }

        float baseIncrease = (float)ZomboidGlobals.wetnessIncrease * GameTime.instance.getMultiplier();
        float baseDecrease = (float)ZomboidGlobals.wetnessDecrease * GameTime.instance.getMultiplier();

        label288:
        for (int ix = 0; ix < this.clothingList.length; ix++) {
            BloodBodyPartType part = BloodBodyPartType.FromIndex(ix);
            BodyPartType bodyPartType = BodyPartType.FromIndex(ix);
            if (bodyPartType != BodyPartType.MAX) {
                BodyPart bodyPart = this.character.getBodyDamage().getBodyPart(bodyPartType);
                Thermoregulator.ThermalNode thermalNode = this.character.getBodyDamage().getThermoregulator().getNodeForBloodType(part);
                if (bodyPart != null && thermalNode != null) {
                    float baseDelta = 0.0F;
                    float perspiration = PZMath.clamp(thermalNode.getSecondaryDelta(), 0.0F, 1.0F);
                    perspiration *= perspiration;
                    perspiration *= 0.2F + 0.8F * (1.0F - thermalNode.getDistToCore());
                    if (perspiration > 0.1F) {
                        baseDelta += perspiration;
                    } else {
                        float bodyHeat = (thermalNode.getSkinCelcius() - 20.0F) / 22.0F;
                        bodyHeat *= bodyHeat;
                        bodyHeat -= outerWetnessInc;
                        bodyHeat = Math.max(0.0F, bodyHeat);
                        baseDelta -= bodyHeat;
                        if (outerWetnessInc > 0.0F) {
                            baseDelta = 0.0F;
                        }
                    }

                    this.perspiringParts[ix] = baseDelta > 0.0F ? 1 : 0;
                    if (baseDelta != 0.0F) {
                        if (baseDelta > 0.0F) {
                            baseDelta *= baseIncrease;
                        } else {
                            baseDelta *= baseDecrease;
                        }

                        bodyPart.setWetness(bodyPart.getWetness() + baseDelta);
                        if ((!(baseDelta > 0.0F) || !(bodyPart.getWetness() < 25.0F)) && (!(baseDelta < 0.0F) || !(bodyPart.getWetness() > 50.0F))) {
                            if (baseDelta > 0.0F) {
                                float extTemp = this.character.getBodyDamage().getThermoregulator().getExternalAirTemperature();
                                extTemp += 10.0F;
                                extTemp = PZMath.clamp(extTemp, 0.0F, 20.0F) / 20.0F;
                                baseDelta *= 0.4F + 0.6F * extTemp;
                            }

                            boolean holeBefore = false;
                            boolean soakThrough = false;
                            boolean dryBefore = false;
                            int j = this.clothingList[ix].size() - 1;

                            float increaseMod;
                            InventoryItem item;
                            while (true) {
                                if (j < 0) {
                                    continue label288;
                                }

                                if (baseDelta > 0.0F) {
                                    this.perspiringParts[ix]++;
                                }

                                item = this.clothingList[ix].get(j);
                                if (item instanceof Clothing clothing) {
                                    increaseMod = 1.0F;
                                    ItemVisual itemVisual = clothing.getVisual();
                                    if (itemVisual == null) {
                                        break;
                                    }

                                    if (itemVisual.getHole(part) > 0.0F) {
                                        holeBefore = true;
                                    } else if (baseDelta > 0.0F && clothing.getWetness() >= 100.0F) {
                                        soakThrough = true;
                                    } else {
                                        if (!(baseDelta < 0.0F) || !(clothing.getWetness() <= 0.0F)) {
                                            if (baseDelta > 0.0F && clothing.getWaterResistance() > 0.0F) {
                                                increaseMod = PZMath.max(0.0F, 1.0F - clothing.getWaterResistance());
                                                if (increaseMod <= 0.0F) {
                                                    this.perspiringParts[ix]--;
                                                    continue label288;
                                                }
                                            }
                                            break;
                                        }

                                        dryBefore = true;
                                    }
                                }

                                j--;
                            }

                            coveredParts.clear();
                            BloodClothingType.getCoveredParts(item.getBloodClothingType(), coveredParts);
                            float delta = baseDelta;
                            if (baseDelta > 0.0F) {
                                delta = baseDelta * increaseMod;
                            }

                            if (holeBefore || soakThrough || dryBefore) {
                                delta /= 2.0F;
                            }

                            clothing.setWetness(clothing.getWetness() + delta);
                        }
                    }
                }
            }
        }

        for (int ixx = 0; ixx < this.clothingList.length; ixx++) {
            BloodBodyPartType part = BloodBodyPartType.FromIndex(ixx);
            BodyPartType bodyPartType = BodyPartType.FromIndex(ixx);
            if (bodyPartType != BodyPartType.MAX) {
                BodyPart bodyPart = this.character.getBodyDamage().getBodyPart(bodyPartType);
                Thermoregulator.ThermalNode thermalNode = this.character.getBodyDamage().getThermoregulator().getNodeForBloodType(part);
                if (bodyPart != null && thermalNode != null) {
                    float maxWetness = 100.0F;
                    if (umbrella) {
                        maxWetness = 100.0F * BodyPartType.GetUmbrellaMod(bodyPartType);
                    }

                    float baseDeltax = 0.0F;
                    if (outerWetnessInc > 0.0F) {
                        baseDeltax = outerWetnessInc * baseIncrease;
                    } else {
                        baseDeltax -= outerWetnessDec * baseDecrease;
                    }

                    boolean holeAbove = false;
                    boolean soakThrough = false;
                    boolean dryBefore = false;
                    float layerDivider = 2.0F;

                    for (int j = 0; j < this.clothingList[ixx].size(); j++) {
                        int partCount = 1 + this.clothingList[ixx].size() - j;
                        float increaseModx = 1.0F;
                        InventoryItem itemx = this.clothingList[ixx].get(j);
                        if (itemx instanceof Clothing clothing) {
                            ItemVisual itemVisualx = clothing.getVisual();
                            if (itemVisualx != null) {
                                if (itemVisualx.getHole(part) > 0.0F) {
                                    holeAbove = true;
                                    continue;
                                }

                                if (baseDeltax > 0.0F && clothing.getWetness() >= 100.0F) {
                                    soakThrough = true;
                                    continue;
                                }

                                if (baseDeltax < 0.0F && clothing.getWetness() <= 0.0F) {
                                    dryBefore = true;
                                    continue;
                                }

                                if (baseDeltax > 0.0F && clothing.getWaterResistance() > 0.0F) {
                                    increaseModx = PZMath.max(0.0F, 1.0F - clothing.getWaterResistance());
                                    if (increaseModx <= 0.0F) {
                                        break;
                                    }
                                }
                            }

                            coveredParts.clear();
                            BloodClothingType.getCoveredParts(itemx.getBloodClothingType(), coveredParts);
                            int numParts = coveredParts.size();
                            float deltax = baseDeltax;
                            if (baseDeltax > 0.0F) {
                                deltax = baseDeltax * increaseModx;
                            }

                            deltax /= numParts;
                            if (holeAbove || soakThrough || dryBefore) {
                                deltax /= layerDivider;
                            }

                            if (baseDeltax < 0.0F && partCount > this.perspiringParts[ixx] || baseDeltax > 0.0F && clothing.getWetness() <= maxWetness) {
                                clothing.setWetness(clothing.getWetness() + deltax);
                            }

                            if (baseDeltax > 0.0F) {
                                break;
                            }

                            if (dryBefore) {
                                layerDivider *= 2.0F;
                            }
                        }
                    }

                    if (!this.clothingList[ixx].isEmpty()) {
                        InventoryItem bottomItem = this.clothingList[ixx].get(this.clothingList[ixx].size() - 1);
                        if (bottomItem instanceof Clothing clothing) {
                            if (baseDeltax > 0.0F && this.perspiringParts[ixx] == 0 && clothing.getWetness() >= 50.0F && bodyPart.getWetness() <= maxWetness) {
                                bodyPart.setWetness(bodyPart.getWetness() + baseDeltax / 2.0F);
                            }

                            if (baseDeltax < 0.0F && this.perspiringParts[ixx] == 0 && clothing.getWetness() <= 50.0F) {
                                bodyPart.setWetness(bodyPart.getWetness() + baseDeltax / 2.0F);
                            }
                        }
                    } else if (baseDeltax < 0.0F && this.perspiringParts[ixx] == 0 || bodyPart.getWetness() <= maxWetness) {
                        bodyPart.setWetness(bodyPart.getWetness() + baseDeltax);
                    }
                }
            }
        }
    }

    private static final class ItemList extends ArrayList<InventoryItem> {
    }
}
