// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import java.util.List;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.energy.Energy;
import zombie.interfaces.IUpdater;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemUser;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBarbecue;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.util.StringUtils;

@UsedFromLua
public final class DrainableComboItem extends InventoryItem implements Drainable, IUpdater {
    private boolean useWhileEquiped = true;
    private boolean useWhileUnequiped;
    private int ticksPerEquipUse = 30;
    private float useDelta = 0.03125F;
    private float ticks;
    private String replaceOnDeplete;
    private String replaceOnDepleteFullType;
    public List<String> replaceOnCooked;
    private String onCooked;
    private boolean canConsolidate = true;
    private float weightEmpty;
    private static final float MIN_HEAT = 0.2F;
    private static final float MAX_HEAT = 3.0F;
    private String onEat;
    private int lastUpdateMinutes = -1;
    private float heat = 1.0F;
    private int lastCookMinute;

    public DrainableComboItem(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
    }

    public DrainableComboItem(String module, String name, String itemType, Item item) {
        super(module, name, itemType, item);
    }

    @Override
    public boolean IsDrainable() {
        return true;
    }

    @Override
    public boolean CanStack(InventoryItem item) {
        return false;
    }

    @Override
    public int getMaxUses() {
        return (int)Math.floor(1.0F / this.useDelta);
    }

    @Override
    public void setCurrentUses(int newuses) {
        this.uses = newuses;
        this.updateWeight();
    }

    @Deprecated
    public void setUsedDelta(float delta) {
        this.setCurrentUsesFloat(delta);
    }

    @Override
    public void setCurrentUsesFloat(float newUses) {
        newUses = PZMath.clamp(newUses, 0.0F, 1.0F);
        this.uses = Math.round(newUses / this.useDelta);
        this.updateWeight();
    }

    @Override
    public float getCurrentUsesFloat() {
        return this.uses * this.useDelta;
    }

    @Override
    public void render() {
    }

    @Override
    public void renderlast() {
    }

    @Override
    public boolean shouldUpdateInWorld() {
        return !GameServer.server && this.heat != 1.0F;
    }

    @Override
    public void update() {
        ItemContainer outermostContainer = this.getOutermostContainer();
        if (outermostContainer != null) {
            float temp = outermostContainer.getTemprature();
            if (this.heat > temp) {
                this.heat = this.heat - 0.001F * GameTime.instance.getMultiplier();
                if (this.heat < Math.max(0.2F, temp)) {
                    this.heat = Math.max(0.2F, temp);
                }
            }

            if (this.heat < temp) {
                this.heat = this.heat + temp / 1000.0F * GameTime.instance.getMultiplier();
                if (this.heat > Math.min(3.0F, temp)) {
                    this.heat = Math.min(3.0F, temp);
                }
            }

            if (this.isCookable && this.heat > 1.6F) {
                int CurrentCookMinute = GameTime.getInstance().getMinutes();
                if (CurrentCookMinute != this.lastCookMinute) {
                    this.lastCookMinute = CurrentCookMinute;
                    float dt = this.heat / 1.5F;
                    if (outermostContainer.getTemprature() <= 1.6F) {
                        dt *= 0.05F;
                    }

                    float timeToCook = this.cookingTime;
                    if (timeToCook < 1.0F) {
                        timeToCook = 10.0F;
                    }

                    timeToCook += dt;
                    if (!this.isCooked() && timeToCook > this.minutesToCook) {
                        this.setCooked(true);
                        if (this.getReplaceOnCooked() != null) {
                            for (int i = 0; i < this.getReplaceOnCooked().size(); i++) {
                                InventoryItem newItem = this.container.AddItem(this.getReplaceOnCooked().get(i));
                                if (newItem != null) {
                                    if (newItem instanceof DrainableComboItem) {
                                        newItem.setCurrentUses(this.getCurrentUses());
                                    }

                                    newItem.copyConditionStatesFrom(this);
                                }
                            }

                            this.container.Remove(this);
                            IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
                            return;
                        }

                        if (this.getOnCooked() != null) {
                            LuaManager.caller.protectedCall(LuaManager.thread, LuaManager.env.rawget(this.getOnCooked()), this);
                            return;
                        }
                    }

                    if (this.cookingTime > this.minutesToBurn) {
                        this.burnt = true;
                        this.setCooked(false);
                    }
                }
            }
        }

        if (this.container == null && this.heat != 1.0F) {
            float tempx = 1.0F;
            if (this.heat > 1.0F) {
                this.heat = this.heat - 0.001F * GameTime.instance.getMultiplier();
                if (this.heat < 1.0F) {
                    this.heat = 1.0F;
                }
            }

            if (this.heat < 1.0F) {
                this.heat = this.heat + 0.001F * GameTime.instance.getMultiplier();
                if (this.heat > 1.0F) {
                    this.heat = 1.0F;
                }
            }
        }

        if (this.useWhileEquiped && this.uses > 0) {
            IsoPlayer p = null;
            if (this.container != null && this.container.parent instanceof IsoPlayer isoPlayer) {
                p = isoPlayer;
            }

            if (p == null || (!this.canBeActivated() || !this.isActivated()) && this.canBeActivated() || !p.isHandItem(this) && !p.isAttachedItem(this)) {
                if (p != null && this.canBeActivated() && this.isActivated() && !p.isHandItem(this) && !p.isAttachedItem(this)) {
                    this.setActivated(false);
                    this.playDeactivateSound();
                } else if (p == null && this.canBeActivated() && this.isActivated()) {
                    this.setActivated(false);
                    this.playDeactivateSound();
                }
            } else {
                int minutes = GameTime.instance.getMinutes() / 10 * 10;
                if (minutes != this.lastUpdateMinutes) {
                    if (this.lastUpdateMinutes > -1) {
                        this.Use();
                    }

                    this.lastUpdateMinutes = minutes;
                }
            }
        }

        if (this.useWhileUnequiped && this.uses > 0 && (this.canBeActivated() && this.isActivated() || !this.canBeActivated())) {
            this.ticks = this.ticks + GameTime.instance.getMultiplier();

            while (this.ticks >= this.ticksPerEquipUse) {
                this.ticks = this.ticks - this.ticksPerEquipUse;
                if (this.uses > 0) {
                    this.Use();
                }
            }
        }

        if (this.getCurrentUses() <= 0 && this.getReplaceOnDeplete() == null && !this.isKeepOnDeplete() && this.container != null) {
            if (this.container.parent instanceof IsoGameCharacter chr) {
                chr.removeFromHands(this);
            }

            this.container.items.remove(this);
            this.container.setDirty(true);
            this.container.setDrawDirty(true);
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(this.container, this);
            }

            this.container = null;
        }

        if (this.getCurrentUses() <= 0 && this.getReplaceOnDeplete() != null) {
            String s = this.getReplaceOnDepleteFullType();
            if (this.container != null) {
                InventoryItem item = this.container.AddItem(s);
                if (this.container.parent instanceof IsoGameCharacter chr) {
                    if (chr.getPrimaryHandItem() == this) {
                        chr.setPrimaryHandItem(item);
                    }

                    if (chr.getSecondaryHandItem() == this) {
                        chr.setSecondaryHandItem(item);
                    }
                }

                item.copyConditionStatesFrom(this);
                ItemContainer lastContainer = this.container;
                this.container.Remove(this);
                if (GameServer.server) {
                    GameServer.sendReplaceItemInContainer(lastContainer, this, item);
                }
            }
        }
    }

    @Override
    public void Use() {
        this.Use(false, false, false);
    }

    @Override
    public void Use(boolean bCrafting, boolean bInContainer, boolean bNeedSync) {
        if (this.getWorldItem() != null) {
            ItemUser.UseItem(this);
            if (GameServer.server && bNeedSync) {
                this.syncItemFields();
            }
        } else {
            this.uses--;
            if (this.uses <= 0) {
                if (this.getReplaceOnDeplete() != null) {
                    String s = this.getReplaceOnDepleteFullType();
                    if (this.container != null) {
                        InventoryItem item = this.container.AddItem(s);
                        if (item != null) {
                            if (this.container.parent instanceof IsoGameCharacter chr) {
                                if (chr.getPrimaryHandItem() == this) {
                                    chr.setPrimaryHandItem(item);
                                }

                                if (chr.getSecondaryHandItem() == this) {
                                    chr.setSecondaryHandItem(item);
                                }
                            }

                            item.copyConditionStatesFrom(this);
                            ItemContainer lastContainer = this.container;
                            this.container.Remove(this);
                            if (GameServer.server) {
                                GameServer.sendReplaceItemInContainer(lastContainer, this, item);
                            }
                        }
                    }
                } else {
                    if (this.isKeepOnDeplete()) {
                        if (bNeedSync) {
                            this.syncItemFields();
                        }

                        return;
                    }

                    if (this.container != null && this.isDisappearOnUse()) {
                        if (this.container.parent instanceof IsoGameCharacter chr) {
                            chr.removeFromHands(this);
                        }

                        this.container.items.remove(this);
                        this.container.setDirty(true);
                        this.container.setDrawDirty(true);
                        if (GameServer.server && bNeedSync) {
                            GameServer.sendRemoveItemFromContainer(this.container, this);
                        }

                        this.container = null;
                    }
                }
            }

            this.updateWeight();
            if (bNeedSync) {
                this.syncItemFields();
            }
        }
    }

    @Override
    public void syncItemFields() {
        ItemContainer outer = this.getOutermostContainer();
        if (outer != null && outer.getParent() instanceof IsoPlayer) {
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.ItemStats, this.getContainer(), this);
            } else if (GameServer.server) {
                INetworkPacket.send((IsoPlayer)outer.getParent(), PacketTypes.PacketType.ItemStats, this.getContainer(), this);
            }
        }
    }

    public void updateWeight() {
        if (this.getReplaceOnDeplete() != null) {
            if (this.getCurrentUsesFloat() >= 1.0F) {
                this.setCustomWeight(true);
                this.setActualWeight(this.getScriptItem().getActualWeight());
                this.setWeight(this.getActualWeight());
                return;
            }

            Item emptyItem = ScriptManager.instance.getItem(this.replaceOnDepleteFullType);
            if (emptyItem != null) {
                this.setCustomWeight(true);
                this.setActualWeight(
                    (this.getScriptItem().getActualWeight() - emptyItem.getActualWeight()) * this.getCurrentUsesFloat() + emptyItem.getActualWeight()
                );
                this.setWeight(this.getActualWeight());
            }
        }

        if (this.getWeightEmpty() != 0.0F) {
            this.setCustomWeight(true);
            this.setActualWeight((this.getScriptItem().getActualWeight() - this.weightEmpty) * this.getCurrentUsesFloat() + this.weightEmpty);
        }
    }

    /**
     * @return the EmptyWeight
     */
    public float getWeightEmpty() {
        return this.weightEmpty;
    }

    /**
     * 
     * @param weight the EmptyWeight to set
     */
    public void setWeightEmpty(float weight) {
        this.weightEmpty = weight;
    }

    /**
     * @return the bUseWhileEquiped
     */
    public boolean isUseWhileEquiped() {
        return this.useWhileEquiped;
    }

    /**
     * 
     * @param bUseWhileEquiped the bUseWhileEquiped to set
     */
    public void setUseWhileEquiped(boolean bUseWhileEquiped) {
        this.useWhileEquiped = bUseWhileEquiped;
    }

    /**
     * @return the bUseWhileUnequiped
     */
    public boolean isUseWhileUnequiped() {
        return this.useWhileUnequiped;
    }

    /**
     * 
     * @param bUseWhileUnequiped the bUseWhileUnequiped to set
     */
    public void setUseWhileUnequiped(boolean bUseWhileUnequiped) {
        this.useWhileUnequiped = bUseWhileUnequiped;
    }

    /**
     * @return the ticksPerEquipUse
     */
    public int getTicksPerEquipUse() {
        return this.ticksPerEquipUse;
    }

    /**
     * 
     * @param ticksPerEquipUse the ticksPerEquipUse to set
     */
    public void setTicksPerEquipUse(int ticksPerEquipUse) {
        this.ticksPerEquipUse = ticksPerEquipUse;
    }

    /**
     * @return the useDelta
     */
    @Override
    public float getUseDelta() {
        return this.useDelta;
    }

    /**
     * 
     * @param useDelta the useDelta to set
     */
    @Override
    public void setUseDelta(float useDelta) {
        this.useDelta = useDelta;
    }

    /**
     * @return the ticks
     */
    public float getTicks() {
        return this.ticks;
    }

    /**
     * 
     * @param ticks the ticks to set
     */
    public void setTicks(float ticks) {
        this.ticks = ticks;
    }

    public void setReplaceOnDeplete(String ReplaceOnDeplete) {
        this.replaceOnDeplete = ReplaceOnDeplete;
        this.replaceOnDepleteFullType = this.getReplaceOnDepleteFullType();
    }

    /**
     * @return the ReplaceOnDeplete
     */
    public String getReplaceOnDeplete() {
        return this.replaceOnDeplete;
    }

    public String getReplaceOnDepleteFullType() {
        return StringUtils.moduleDotType(this.getModule(), this.replaceOnDeplete);
    }

    public void setHeat(float heat) {
        this.heat = PZMath.clamp(heat, 0.0F, 3.0F);
    }

    public float getHeat() {
        return this.heat;
    }

    @Override
    public float getInvHeat() {
        return (1.0F - this.heat) / 3.0F;
    }

    @Override
    public boolean finishupdate() {
        if (this.container != null) {
            if (this.heat != this.container.getTemprature() || this.container.isTemperatureChanging()) {
                return false;
            }

            if (this.container.type.equals("campfire") || this.container.parent instanceof IsoBarbecue) {
                return false;
            }
        }

        return true;
    }

    public boolean canConsolidate() {
        return this.canConsolidate;
    }

    public void setCanConsolidate(boolean canConsolidate) {
        this.canConsolidate = canConsolidate;
    }

    /**
     * @return the ReplaceOnCooked
     */
    public List<String> getReplaceOnCooked() {
        return this.replaceOnCooked;
    }

    /**
     * 
     * @param replaceOnCooked the ReplaceOnCooked to set
     */
    public void setReplaceOnCooked(List<String> replaceOnCooked) {
        this.replaceOnCooked = replaceOnCooked;
    }

    /**
     * @return the OnCooked
     */
    public String getOnCooked() {
        return this.onCooked;
    }

    /**
     * 
     * @param onCooked the onCooked to set
     */
    public void setOnCooked(String onCooked) {
        this.onCooked = onCooked;
    }

    public String getOnEat() {
        return this.onEat;
    }

    public void setOnEat(String onEat) {
        this.onEat = onEat;
    }

    public boolean isEnergy() {
        return this.getEnergy() != null;
    }

    public Energy getEnergy() {
        return null;
    }

    public boolean isFullUses() {
        return this.uses >= this.getMaxUses();
    }

    public boolean isEmptyUses() {
        return this.getCurrentUsesFloat() <= 0.0F;
    }

    public void randomizeUses() {
        if (this.getMaxUses() != 1) {
            int amount = Rand.Next(this.getMaxUses()) + 1;
            if (this.hasTag(ItemTag.LESS_FULL)) {
                amount = Math.min(amount, Rand.Next(this.getMaxUses()) + 1);
            }

            if (amount <= this.getMaxUses()) {
                if (amount > 0) {
                    this.setCurrentUses(amount);
                }
            }
        }
    }
}
