// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.WorldSoundManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.interfaces.IClothingWasherDryerLogic;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class ClothingDryerLogic implements IClothingWasherDryerLogic {
    private final IsoObject object;
    private boolean activated;
    private long soundInstance = -1L;
    private float lastUpdate = -1.0F;
    private boolean cycleFinished;
    private float startTime;
    private final float cycleLengthMinutes = 90.0F;
    private boolean alreadyExecuted;

    public ClothingDryerLogic(IsoObject object) {
        this.object = object;
    }

    public IsoObject getObject() {
        return this.object;
    }

    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        this.activated = input.get() == 1;
    }

    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        output.put((byte)(this.isActivated() ? 1 : 0));
    }

    @Override
    public void update() {
        if (this.getObject().getObjectIndex() != -1) {
            if (this.getContainer() != null) {
                if (!this.getContainer().isPowered()) {
                    this.setActivated(false);
                }

                this.cycleFinished();
                this.updateSound();
                if (GameClient.client) {
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

                        for (int i = 0; i < this.getContainer().getItems().size(); i++) {
                            InventoryItem item = this.getContainer().getItems().get(i);
                            if (item instanceof Clothing clothing) {
                                float wetness = clothing.getWetness();
                                if (wetness > 0.0F) {
                                    wetness -= elapsedMinutes;
                                    clothing.setWetness(wetness);
                                    if (GameServer.server) {
                                    }
                                }
                            }

                            if (item.isWet() && item.getItemWhenDry() != null) {
                                item.setWetCooldown(item.getWetCooldown() - elapsedMinutes * 250);
                                if (item.getWetCooldown() <= 0.0F) {
                                    InventoryItem dryItem = InventoryItemFactory.CreateItem(item.getItemWhenDry());
                                    this.getContainer().addItem(dryItem);
                                    this.getContainer().Remove(item);
                                    i--;
                                    item.setWet(false);
                                    IsoWorld.instance.currentCell.addToProcessItemsRemove(item);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("dryer.state".equals(change)) {
            bb.put((byte)(this.isActivated() ? 1 : 0));
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        if ("dryer.state".equals(change)) {
            this.setActivated(bb.get() == 1);
        }
    }

    @Override
    public ItemContainer getContainer() {
        return this.getObject().getContainerByType("clothingdryer");
    }

    private void updateSound() {
        if (this.isActivated()) {
            if (!GameServer.server) {
                if (this.getObject().emitter != null && this.getObject().emitter.isPlaying("ClothingDryerFinished")) {
                    this.getObject().emitter.stopOrTriggerSoundByName("ClothingDryerFinished");
                }

                if (this.soundInstance == -1L) {
                    this.getObject().emitter = IsoWorld.instance
                        .getFreeEmitter(this.getObject().getXi() + 0.5F, this.getObject().getYi() + 0.5F, this.getObject().getZi());
                    IsoWorld.instance.setEmitterOwner(this.getObject().emitter, this.getObject());
                    this.soundInstance = this.getObject().emitter.playSoundLoopedImpl("ClothingDryerRunning");
                }
            }

            if (!GameClient.client) {
                WorldSoundManager.instance
                    .addSoundRepeating(this, this.getObject().square.x, this.getObject().square.y, this.getObject().square.z, 10, 10, false);
            }
        } else if (this.soundInstance != -1L) {
            this.getObject().emitter.stopOrTriggerSound(this.soundInstance);
            this.soundInstance = -1L;
            if (this.cycleFinished) {
                this.cycleFinished = false;
                this.getObject().emitter.playSoundImpl("ClothingDryerFinished", this.getObject());
            }
        }
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
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        return this.isActivated() ? false : this.getContainer() == container;
    }

    @Override
    public boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item) {
        return !this.getContainer().isEmpty() && this.isActivated() ? false : this.getContainer() == container;
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
