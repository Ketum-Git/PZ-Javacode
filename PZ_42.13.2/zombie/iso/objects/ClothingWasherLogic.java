// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import fmod.fmod.FMODManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.WorldSoundManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.interfaces.IClothingWasherDryerLogic;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.ItemBodyLocation;

public final class ClothingWasherLogic implements IClothingWasherDryerLogic {
    private final IsoObject object;
    private boolean activated;
    private long soundInstance = -1L;
    private float lastUpdate = -1.0F;
    private boolean cycleFinished;
    private float startTime;
    private final float cycleLengthMinutes = 90.0F;
    private boolean alreadyExecuted;

    public ClothingWasherLogic(IsoObject object) {
        this.object = object;
    }

    public IsoObject getObject() {
        return this.object;
    }

    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        this.activated = input.get() == 1;
        this.lastUpdate = input.getFloat();
    }

    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        output.put((byte)(this.isActivated() ? 1 : 0));
        output.putFloat(this.lastUpdate);
    }

    @Override
    public void update() {
        if (this.getObject().getObjectIndex() != -1) {
            if (!this.getContainer().isPowered()) {
                this.setActivated(false);
            }

            this.updateSound();
            this.cycleFinished();
            if (GameClient.client) {
            }

            if (this.getObject().getFluidAmount() <= 0.0F) {
                this.setActivated(false);
            }

            if (!this.isActivated()) {
                this.lastUpdate = -1.0F;
            } else {
                float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
                if (this.lastUpdate < 0.0F) {
                    this.lastUpdate = worldAgeHours;
                } else if (this.lastUpdate > worldAgeHours) {
                    this.lastUpdate = worldAgeHours;
                }

                float elapsedHours = worldAgeHours - this.lastUpdate;
                int elapsedMinutes = (int)(elapsedHours * 60.0F);
                if (elapsedMinutes >= 1) {
                    this.lastUpdate = worldAgeHours;
                    this.getObject().useFluid(1 * elapsedMinutes);

                    for (int i = 0; i < this.getContainer().getItems().size(); i++) {
                        InventoryItem item = this.getContainer().getItems().get(i);
                        if (item instanceof Clothing clothing) {
                            float bloodLevel = clothing.getBloodlevel();
                            if (bloodLevel > 0.0F) {
                                this.removeBlood(clothing, elapsedMinutes * 2);
                            }

                            float dirtyness = clothing.getDirtyness();
                            if (dirtyness > 0.0F) {
                                this.removeDirt(clothing, elapsedMinutes * 2);
                            }

                            clothing.setWetness(100.0F);
                        } else {
                            if (item.getModData().rawget("ItemAfterCleaning") != null) {
                                String resultItem = (String)item.getModData().rawget("ItemAfterCleaning");
                                Double cleaningProgress = (Double)item.getModData().rawget("CleaningProgress");
                                if (cleaningProgress == null) {
                                    item.getModData().rawset("CleaningProgress", elapsedMinutes * 3.0);
                                } else if (cleaningProgress >= 100.0) {
                                    this.getContainer().getItems().remove(item);
                                    this.getContainer().addItem(InventoryItemFactory.CreateItem(resultItem));
                                } else {
                                    item.getModData().rawset("CleaningProgress", cleaningProgress + elapsedMinutes * 2);
                                }
                            }

                            if (item instanceof InventoryContainer invContainer && invContainer.getBloodLevel() > 0.0F) {
                                invContainer.setBloodLevel(invContainer.getBloodLevel() - elapsedMinutes * 2 / 100.0F);
                            }
                        }
                    }
                }
            }
        }
    }

    private void removeBlood(Clothing item, float amount) {
        ItemVisual itemVisual = item.getVisual();
        if (itemVisual != null) {
            for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
                float blood = itemVisual.getBlood(part);
                if (blood > 0.0F) {
                    itemVisual.setBlood(part, blood - amount / 100.0F);
                }
            }

            BloodClothingType.calcTotalBloodLevel(item);
        }
    }

    private void removeDirt(Clothing item, float amount) {
        ItemVisual itemVisual = item.getVisual();
        if (itemVisual != null) {
            for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
                float dirt = itemVisual.getDirt(part);
                if (dirt > 0.0F) {
                    itemVisual.setDirt(part, dirt - amount / 100.0F);
                }
            }

            BloodClothingType.calcTotalDirtLevel(item);
        }
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("washer.state".equals(change)) {
            bb.put((byte)(this.isActivated() ? 1 : 0));
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        if ("washer.state".equals(change)) {
            this.setActivated(bb.get() == 1);
        }
    }

    @Override
    public ItemContainer getContainer() {
        return this.getObject().getContainerByType("clothingwasher");
    }

    private void updateSound() {
        if (this.isActivated()) {
            if (!GameServer.server) {
                if (this.getObject().emitter != null && this.getObject().emitter.isPlaying("ClothingWasherFinished")) {
                    this.getObject().emitter.stopOrTriggerSoundByName("ClothingWasherFinished");
                }

                if (this.soundInstance == -1L) {
                    this.getObject().emitter = IsoWorld.instance
                        .getFreeEmitter(this.getObject().getXi() + 0.5F, this.getObject().getYi() + 0.5F, this.getObject().getZi());
                    IsoWorld.instance.setEmitterOwner(this.getObject().emitter, this.getObject());
                    this.soundInstance = this.getObject().emitter.playSoundLoopedImpl("ClothingWasherRunning");
                    ItemContainer container = this.getContainer();
                    boolean bHasNoisyItems = this.hasNoisyItems(container);
                    this.getObject()
                        .emitter
                        .setParameterValue(
                            this.soundInstance, FMODManager.instance.getParameterDescription("ClothingWasherLoaded"), bHasNoisyItems ? 1.0F : 0.0F
                        );
                }
            }

            if (!GameClient.client) {
                int radius = this.hasNoisyItems(this.getContainer()) ? 20 : 10;
                WorldSoundManager.instance
                    .addSoundRepeating(this, this.getObject().square.x, this.getObject().square.y, this.getObject().square.z, radius, 10, false);
            }
        } else if (this.soundInstance != -1L) {
            this.getObject().emitter.stopOrTriggerSound(this.soundInstance);
            this.soundInstance = -1L;
            if (this.cycleFinished) {
                this.cycleFinished = false;
                this.getObject().emitter.playSoundImpl("ClothingWasherFinished", this.getObject());
            }
        }
    }

    private boolean hasNoisyItems(ItemContainer container) {
        if (container == null) {
            return false;
        } else {
            ArrayList<InventoryItem> items = container.getItems();
            if (items != null && !items.isEmpty()) {
                float nonClothingWeight = 0.0F;

                for (int i = 0; i < items.size(); i++) {
                    InventoryItem item = items.get(i);
                    if (!(item instanceof Clothing)) {
                        nonClothingWeight += item.getActualWeight();
                    } else if (item.getBodyLocation() == ItemBodyLocation.SHOES) {
                        return true;
                    }
                }

                return nonClothingWeight >= 5.0F;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        return container != this.getContainer() ? false : !this.isActivated();
    }

    @Override
    public boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item) {
        return container != this.getContainer() ? false : container.isEmpty() || !this.isActivated();
    }

    private boolean cycleFinished() {
        if (this.isActivated()) {
            if (!this.alreadyExecuted) {
                this.startTime = (float)GameTime.getInstance().getWorldAgeHours();
                this.alreadyExecuted = true;
            }

            float elapsedHours = (float)GameTime.getInstance().getWorldAgeHours() - this.startTime;
            int elapsedMinutes = (int)(elapsedHours * 60.0F);
            if (elapsedMinutes < 90.0F) {
                return false;
            }

            this.cycleFinished = true;
            this.setActivated(false);
        }

        return true;
    }

    @Override
    public boolean isActivated() {
        return this.activated;
    }

    @Override
    public void setActivated(boolean activated) {
        boolean bUpdateGenerator = activated != this.activated;
        this.activated = activated;
        this.alreadyExecuted = false;
        if (bUpdateGenerator) {
            Thread thread = Thread.currentThread();
            if (thread == GameWindow.gameThread || thread == GameServer.mainThread) {
                IsoGenerator.updateGenerator(this.getObject().getSquare());
            }
        }
    }

    @Override
    public void switchModeOn() {
    }

    @Override
    public void switchModeOff() {
        this.setActivated(false);
        this.updateSound();
        this.cycleFinished = false;
    }
}
