// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityFactory;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.input.Mouse;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemSoundManager;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IItemProvider;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerGUI;
import zombie.network.packets.INetworkPacket;
import zombie.ui.ObjectTooltip;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@UsedFromLua
public class IsoWorldInventoryObject extends IsoObject implements IItemProvider {
    public InventoryItem item;
    public float xoff;
    public float yoff;
    public float zoff;
    public boolean removeProcess;
    public double dropTime = -1.0;
    public boolean ignoreRemoveSandbox;
    private boolean extendedPlacement;

    public IsoWorldInventoryObject(InventoryItem item, IsoGridSquare sq, float xoff, float yoff, float zoff) {
        this.outlineOnMouseover = true;
        if (item != null) {
            item.worldXRotation = 0.0F;
            item.worldYRotation = 0.0F;
            if (item.worldZRotation < 0.0F) {
                item.worldZRotation = Rand.Next(0, 360);
            }

            item.setContainer(null);
        }

        this.xoff = xoff;
        this.yoff = yoff;
        this.zoff = zoff;
        if (this.xoff == 0.0F) {
            this.xoff = Rand.Next(1000) / 1000.0F;
        }

        if (this.yoff == 0.0F) {
            this.yoff = Rand.Next(1000) / 1000.0F;
        }

        this.item = item;
        this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        if (item != null) {
            this.updateSprite();
        }

        this.square = sq;
        this.offsetY = 0.0F;
        this.offsetX = 0.0F;
        this.dropTime = GameTime.getInstance().getWorldAgeHours();
        if (item != null && item.hasComponent(ComponentType.FluidContainer)) {
            GameEntityFactory.TransferComponent(item, this, ComponentType.FluidContainer);
        }
    }

    public IsoWorldInventoryObject(IsoCell cell) {
        super(cell);
        this.offsetY = 0.0F;
        this.offsetX = 0.0F;
    }

    public void swapItem(InventoryItem newItem) {
        if (newItem != null) {
            if (this.getItem() != null) {
                if (this.hasComponent(ComponentType.FluidContainer)) {
                    GameEntityFactory.TransferComponent(this, this.item, ComponentType.FluidContainer);
                }

                IsoWorld.instance.currentCell.addToProcessItemsRemove(this.getItem());
                ItemSoundManager.removeItem(this.getItem());
                this.getItem().setWorldItem(null);
                newItem.setID(this.getItem().getID());
                newItem.worldScale = this.getItem().worldScale;
                newItem.worldZRotation = this.getItem().worldZRotation;
                newItem.worldYRotation = this.getItem().worldYRotation;
                newItem.worldXRotation = this.getItem().worldXRotation;
            }

            this.item = newItem;
            if (newItem.getWorldItem() != null) {
                throw new IllegalArgumentException("newItem.getWorldItem() != null");
            } else {
                this.getItem().setWorldItem(this);
                this.setKeyId(this.getItem().getKeyId());
                this.setName(this.getItem().getName());
                if (this.item != null && this.item.hasComponent(ComponentType.FluidContainer)) {
                    GameEntityFactory.TransferComponent(this.item, this, ComponentType.FluidContainer);
                }

                if (this.getItem().shouldUpdateInWorld()) {
                    IsoWorld.instance.currentCell.addToProcessWorldItems(this);
                }

                IsoWorld.instance.currentCell.addToProcessItems(newItem);
                this.updateSprite();
                LuaEventManager.triggerEvent("OnContainerUpdate");
                if (GameServer.server) {
                    this.sendObjectChange("swapItem");
                }
            }
        }
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("swapItem".equals(change)) {
            if (this.getItem() == null) {
                return;
            }

            try {
                this.getItem().saveWithSize(bb, false);
            } catch (Exception var5) {
                ExceptionLogger.logException(var5);
            }
        } else {
            super.saveChange(change, tbl, bb);
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        if ("swapItem".equals(change)) {
            try {
                InventoryItem newItem = InventoryItem.loadItem(bb, 240);
                if (newItem != null) {
                    this.swapItem(newItem);
                }
            } catch (Exception var4) {
                ExceptionLogger.logException(var4);
            }
        } else {
            super.loadChange(change, bb);
        }
    }

    private boolean isWaterSource() {
        if (this.item == null) {
            return false;
        } else {
            if (this.item.isBroken()) {
            }

            return this.hasComponent(ComponentType.FluidContainer) ? this.getFluidContainer().getRainCatcher() > 0.0F : false;
        }
    }

    public boolean isPureWater(boolean includeTainted) {
        FluidContainer fc = this.getFluidContainer();
        if (fc != null && fc.getAmount() > 0.0F) {
            float totalWater = fc.getSpecificFluidAmount(Fluid.Water);
            if (includeTainted) {
                totalWater += fc.getSpecificFluidAmount(Fluid.TaintedWater);
            }

            return totalWater == fc.getAmount();
        } else {
            return false;
        }
    }

    @Override
    public boolean hasWater() {
        FluidContainer fc = this.getFluidContainer();
        return fc != null && fc.getAmount() > 0.0F && fc.isAllCategory(FluidCategory.Water);
    }

    @Override
    public float getFluidAmount() {
        return this.getFluidContainer() != null ? this.getFluidContainer().getAmount() : 0.0F;
    }

    @Override
    public void emptyFluid() {
        if (this.getFluidContainer() != null && (GameServer.server || !GameServer.server && !GameClient.client)) {
            float old = this.getFluidAmount();
            FluidContainer fc = this.getFluidContainer();
            if (fc != null) {
                fc.Empty();
                this.sync();
                LuaEventManager.triggerEvent("OnWaterAmountChange", this, old);
            }
        }
    }

    @Override
    public float useFluid(float amount) {
        if (this.getFluidContainer() == null) {
            return 0.0F;
        } else {
            float avail = this.getFluidAmount();
            float used = PZMath.clamp(amount, 0.0F, avail);
            if ((GameServer.server || !GameServer.server && !GameClient.client) && used > 0.0F) {
                FluidContainer fc = this.getFluidContainer();
                if (fc != null) {
                    fc.removeFluid(used);
                    this.sync();
                    LuaEventManager.triggerEvent("OnWaterAmountChange", this, avail);
                }
            }

            return used;
        }
    }

    @Override
    public void addFluid(FluidType fluidType, float amount) {
        if (this.getFluidContainer() != null && (GameServer.server || !GameServer.server && !GameClient.client)) {
            float old = this.getFluidAmount();
            float freeCapacity = this.getFluidCapacity() - this.getFluidAmount();
            amount = PZMath.clamp(amount, 0.0F, freeCapacity);
            if (amount > 0.0F) {
                FluidContainer fc = this.getFluidContainer();
                if (fc != null) {
                    fc.addFluid(fluidType, amount);
                    this.sync();
                    LuaEventManager.triggerEvent("OnWaterAmountChange", this, old);
                }
            }
        }
    }

    @Override
    public boolean canTransferFluidFrom(FluidContainer other) {
        FluidContainer fc = this.getFluidContainer();
        return fc != null ? FluidContainer.CanTransfer(other, fc) : false;
    }

    @Override
    public boolean canTransferFluidTo(FluidContainer other) {
        FluidContainer fc = this.getFluidContainer();
        return fc != null ? FluidContainer.CanTransfer(fc, other) : false;
    }

    @Override
    public float transferFluidTo(FluidContainer target, float amount) {
        if (this.getFluidContainer() != null) {
            if (target == null) {
                return 0.0F;
            } else {
                float sourceAvail = this.getFluidAmount();
                float targetCapacity = target.getFreeCapacity();
                float transferMax = Math.min(targetCapacity, sourceAvail);
                float used = PZMath.clamp(amount, 0.0F, transferMax);
                if ((GameServer.server || !GameServer.server && !GameClient.client) && used > 0.0F) {
                    FluidContainer fc = this.getFluidContainer();
                    if (fc != null) {
                        fc.transferTo(target, used);
                        this.sync();
                        LuaEventManager.triggerEvent("OnWaterAmountChange", this, sourceAvail);
                        return used;
                    }
                }

                return used;
            }
        } else {
            return 0.0F;
        }
    }

    @Override
    public float transferFluidFrom(FluidContainer source, float amount) {
        if (this.getFluidContainer() != null) {
            if (source == null) {
                return 0.0F;
            } else {
                float sourceAvail = source.getAmount();
                float targetCapacity = this.getFluidCapacity() - this.getFluidAmount();
                float transferMax = Math.min(targetCapacity, sourceAvail);
                float used = PZMath.clamp(amount, 0.0F, transferMax);
                if (GameServer.server || !GameServer.server && !GameClient.client) {
                    float old = this.getFluidAmount();
                    if (used > 0.0F) {
                        FluidContainer fc = this.getFluidContainer();
                        if (fc != null) {
                            fc.transferFrom(source, used);
                            this.sync();
                            LuaEventManager.triggerEvent("OnWaterAmountChange", this, old);
                        }
                    }
                }

                return used;
            }
        } else {
            return 0.0F;
        }
    }

    @Override
    public float getFluidCapacity() {
        return this.getFluidContainer() != null ? this.getFluidContainer().getCapacity() : 0.0F;
    }

    @Override
    public boolean isFluidInputLocked() {
        FluidContainer fc = this.getFluidContainer();
        return fc != null ? fc.isInputLocked() : true;
    }

    @Override
    public boolean isTaintedWater() {
        return this.getFluidContainer() != null ? this.getFluidContainer().isTainted() : false;
    }

    @Override
    public String getFluidUiName() {
        return this.getFluidCapacity() > 0.0F
            ? this.getFluidContainer().getUiName()
            : Translator.getText("Fluid_HoldingOneType", this.getFluidContainer().getTranslatedContainerName(), Translator.getText("Fluid_Empty"));
    }

    public String getCustomMenuOption() {
        return this.getItem() != null ? this.getItem().getCustomMenuOption() : null;
    }

    @Override
    public void update() {
        IsoCell cell = IsoWorld.instance.getCell();
        if (!this.removeProcess && this.item != null && this.item.shouldUpdateInWorld()) {
            cell.addToProcessItems(this.item);
        }
    }

    public void updateSprite() {
        this.sprite.setTintMod(new ColorInfo(this.item.col.r, this.item.col.g, this.item.col.b, this.item.col.a));
        if (!GameServer.server || ServerGUI.isCreated()) {
            String str = this.item.getTex().getName();
            if (this.item.isUseWorldItem()) {
                str = this.item.getWorldTexture();
            }

            try {
                Texture tex = Texture.getSharedTexture(str);
                if (tex == null) {
                    str = this.item.getTex().getName();
                }
            } catch (Exception var4) {
                str = "media/inventory/world/WItem_Sack.png";
            }

            Texture tex = this.sprite.LoadSingleTexture(str);
            if (this.item.getScriptItem() == null) {
                this.sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * Core.tileScale, 16 * Core.tileScale);
            } else {
                float var10001 = Core.tileScale;
                float scale = this.item.getScriptItem().scaleWorldIcon * (var10001 / 2.0F);
                this.sprite.def.setScale(scale, scale);
            }
        }
    }

    public boolean finishupdate() {
        return this.removeProcess || this.item == null || !this.item.shouldUpdateInWorld();
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        this.xoff = input.getFloat();
        this.yoff = input.getFloat();
        this.zoff = input.getFloat();
        float offsetX = input.getFloat();
        float offsetY = input.getFloat();
        this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        this.item = InventoryItem.loadItem(input, WorldVersion);
        if (this.item == null) {
            input.getDouble();
            BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
            bits.release();
        } else {
            this.item.setWorldItem(this);
            this.sprite.getTintMod().r = this.item.getR();
            this.sprite.getTintMod().g = this.item.getG();
            this.sprite.getTintMod().b = this.item.getB();
            this.dropTime = input.getDouble();
            BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
            this.ignoreRemoveSandbox = bits.hasFlags(1);
            if (bits.hasFlags(2)) {
                this.loadEntity(input, WorldVersion);
                FluidContainer fluidComponent = this.getFluidContainer();
                if (fluidComponent != null) {
                    fluidComponent.setNonSavedFieldsFromItemScript(this.item);
                }
            }

            this.setExtendedPlacement(bits.hasFlags(4));
            bits.release();
            if (!GameServer.server || ServerGUI.isCreated()) {
                String str = this.item.getTex() == null ? "media/inventory/world/WItem_Sack.png" : this.item.getTex().getName();
                if (this.item.isUseWorldItem()) {
                    str = this.item.getWorldTexture();
                }

                try {
                    Texture tex = Texture.getSharedTexture(str);
                    if (tex == null) {
                        str = this.item.getTex().getName();
                    }
                } catch (Exception var10) {
                    str = "media/inventory/world/WItem_Sack.png";
                }

                Texture tex = this.sprite.LoadSingleTexture(str);
                if (tex != null) {
                    if (this.item.getScriptItem() == null) {
                        this.sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * Core.tileScale, 16 * Core.tileScale);
                    } else {
                        float var10001 = Core.tileScale;
                        float scale = this.item.getScriptItem().scaleWorldIcon * (var10001 / 2.0F);
                        this.sprite.def.setScale(scale, scale);
                    }

                    if (this.item != null && this.item.hasComponent(ComponentType.FluidContainer) && this.hasComponent(ComponentType.FluidContainer)) {
                        GameEntityFactory.RemoveComponent(this.item, this.item.getComponent(ComponentType.FluidContainer));
                    }
                }
            }
        }
    }

    @Override
    public boolean Serialize() {
        return true;
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        output.put((byte)(this.Serialize() ? 1 : 0));
        if (this.Serialize()) {
            output.put(IsoObject.factoryGetClassID(this.getObjectName()));
            output.putFloat(this.xoff);
            output.putFloat(this.yoff);
            output.putFloat(this.zoff);
            output.putFloat(this.offsetX);
            output.putFloat(this.offsetY);
            this.item.saveWithSize(output, false);
            output.putDouble(this.dropTime);
            BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
            if (this.ignoreRemoveSandbox) {
                bits.addFlags(1);
            }

            if (this.requiresEntitySave()) {
                bits.addFlags(2);
                this.saveEntity(output);
            }

            if (this.isExtendedPlacement()) {
                bits.addFlags(4);
            }

            bits.write();
            bits.release();
        }
    }

    @Override
    public void softReset() {
        this.square.removeWorldObject(this);
    }

    @Override
    public String getObjectName() {
        return "WorldInventoryItem";
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI) {
        this.item.DoTooltip(tooltipUI);
    }

    @Override
    public boolean HasTooltip() {
        return false;
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return false;
    }

    private void debugDrawLocation(float x, float y, float z) {
        if (!PerformanceSettings.fboRenderChunk) {
            if (Core.debug && DebugOptions.instance.model.render.axis.getValue()) {
                x += this.xoff;
                y += this.yoff;
                z += this.zoff;
                LineDrawer.DrawIsoLine(x - 0.25F, y, z, x + 0.25F, y, z, 1.0F, 1.0F, 1.0F, 0.5F, 1);
                LineDrawer.DrawIsoLine(x, y - 0.25F, z, x, y + 0.25F, z, 1.0F, 1.0F, 1.0F, 0.5F, 1);
            }
        }
    }

    private void debugHitTest() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        float zoom = Core.getInstance().getZoom(playerIndex);
        float mx = Mouse.getXA();
        float my = Mouse.getYA();
        mx -= IsoCamera.getScreenLeft(playerIndex);
        my -= IsoCamera.getScreenTop(playerIndex);
        mx *= zoom;
        my *= zoom;
        float sx = this.getScreenPosX(playerIndex) * zoom;
        float sy = this.getScreenPosY(playerIndex) * zoom;
        float dist = IsoUtils.DistanceTo2D(sx, sy, mx, my);
        int radius = 48;
        if (dist < 48.0F) {
            LineDrawer.drawCircle(sx, sy, 48.0F, 16, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (Core.debug) {
        }

        if (this.getItem().getScriptItem().isWorldRender()) {
            ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(
                this.getItem(), this.getSquare(), this.getRenderSquare(), this.getX() + this.xoff, this.getY() + this.yoff, this.getZ() + this.zoff, 0.0F
            );
            if (status == ItemModelRenderer.RenderStatus.Loading || status == ItemModelRenderer.RenderStatus.Ready) {
                this.debugDrawLocation(x, y, z);
            } else if (!this.sprite.hasNoTextures()) {
                Texture tex = this.sprite.getTextureForCurrentFrame(this.dir);
                if (tex != null) {
                    float dx = tex.getWidthOrig() * this.sprite.def.getScaleX() / 2.0F;
                    float dy = tex.getHeightOrig() * this.sprite.def.getScaleY() * 3.0F / 4.0F;
                    int playerIndex = IsoCamera.frameState.playerIndex;
                    float oldAlpha = this.getAlpha(playerIndex);
                    float oldTargetAlpha = this.getTargetAlpha(playerIndex);
                    float alpha = PZMath.min(getSurfaceAlpha(this.square, this.zoff), oldAlpha);
                    this.setAlphaAndTarget(playerIndex, alpha);
                    float r = col.r;
                    float g = col.g;
                    float b = col.b;
                    float a = col.a;
                    if (this.isHighlighted(playerIndex)) {
                        ColorInfo highlightColor = Core.getInstance().getWorldItemHighlightColor();
                        col.r = r * (1.0F - a) + highlightColor.r * a;
                        col.g = g * (1.0F - a) + highlightColor.g * a;
                        col.b = b * (1.0F - a) + highlightColor.b * a;
                    }

                    this.sprite.render(this, x + this.xoff, y + this.yoff, z + this.zoff, this.dir, this.offsetX + dx, this.offsetY + dy, col, true);
                    col.set(r, g, b, a);
                    this.setAlpha(playerIndex, oldAlpha);
                    this.setTargetAlpha(playerIndex, oldTargetAlpha);
                    this.debugDrawLocation(x, y, z);
                }
            }
        }
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
        if (this.sprite != null) {
            if (!this.sprite.hasNoTextures()) {
                Texture tex = this.sprite.getTextureForCurrentFrame(this.dir);
                if (tex != null) {
                    float dx = tex.getWidthOrig() / 2;
                    float dy = tex.getHeightOrig();
                    this.sprite.renderObjectPicker(this.sprite.def, this, this.dir);
                }
            }
        }
    }

    @Override
    public InventoryItem getItem() {
        return this.item;
    }

    @Override
    public void addToWorld() {
        if (this.item != null && this.item.shouldUpdateInWorld() && !IsoWorld.instance.currentCell.getProcessWorldItems().contains(this)) {
            IsoWorld.instance.currentCell.getProcessWorldItems().add(this);
        }

        if (this.item instanceof InventoryContainer inventoryContainer) {
            ItemContainer container = inventoryContainer.getInventory();
            if (container != null) {
                container.addItemsToProcessItems();
            }
        }

        super.addToWorld();
    }

    @Override
    public void removeFromWorld() {
        this.removeProcess = true;
        IsoWorld.instance.getCell().getProcessWorldItems().remove(this);
        if (this.item != null) {
            IsoWorld.instance.currentCell.addToProcessItemsRemove(this.item);
            ItemSoundManager.removeItem(this.item);
            this.item.atlasTexture = null;
        }

        if (this.item instanceof InventoryContainer inventoryContainer) {
            ItemContainer container = inventoryContainer.getInventory();
            if (container != null) {
                container.removeItemsFromProcessItems();
            }
        }

        super.removeFromWorld();
    }

    @Override
    public void removeFromSquare() {
        if (this.hasComponent(ComponentType.FluidContainer)) {
            GameEntityFactory.TransferComponent(this, this.item, ComponentType.FluidContainer);
        }

        if (this.square != null) {
            this.square.getWorldObjects().remove(this);
        }

        super.removeFromSquare();
    }

    public float getScreenPosX(int playerIndex) {
        float screenX = IsoUtils.XToScreen(this.getX() + this.xoff, this.getY() + this.yoff, this.getZ() + this.zoff, 0);
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        return (screenX - camera.getOffX()) / Core.getInstance().getZoom(playerIndex);
    }

    public float getScreenPosY(int playerIndex) {
        Texture tex = this.sprite == null ? null : this.sprite.getTextureForCurrentFrame(this.dir);
        float dy = tex == null ? 0.0F : tex.getHeightOrig() * this.sprite.def.getScaleY() * 1.0F / 4.0F;
        float screenY = IsoUtils.YToScreen(this.getX() + this.xoff, this.getY() + this.yoff, this.getZ() + this.zoff, 0);
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        return (screenY - camera.getOffY() - dy) / Core.getInstance().getZoom(playerIndex);
    }

    public void setIgnoreRemoveSandbox(boolean b) {
        this.ignoreRemoveSandbox = b;
    }

    public boolean isIgnoreRemoveSandbox() {
        return this.ignoreRemoveSandbox;
    }

    public void setExtendedPlacement(boolean b) {
        this.extendedPlacement = b;
    }

    public boolean isExtendedPlacement() {
        return this.extendedPlacement;
    }

    public float getWorldPosX() {
        return this.getX() + this.xoff;
    }

    public float getWorldPosY() {
        return this.getY() + this.yoff;
    }

    public float getWorldPosZ() {
        return this.getZ() + this.zoff;
    }

    public static float getSurfaceAlpha(IsoGridSquare square, float zoff) {
        return getSurfaceAlpha(square, zoff, false);
    }

    public static float getSurfaceAlpha(IsoGridSquare square, float zoff, boolean bTargetAlpha) {
        if (square == null) {
            return 1.0F;
        } else {
            int playerIndex = IsoCamera.frameState.playerIndex;
            float alpha = 1.0F;
            if (zoff > 0.01F) {
                boolean bHasSurface = false;

                for (int i = 0; i < square.getObjects().size(); i++) {
                    IsoObject obj = square.getObjects().get(i);
                    if (obj.getSurfaceOffsetNoTable() > 0.0F && !obj.getProperties().isSurfaceOffset()) {
                        if (!bHasSurface) {
                            bHasSurface = true;
                            alpha = 0.0F;
                        }

                        if (bTargetAlpha) {
                            alpha = PZMath.max(alpha, obj.getRenderInfo(playerIndex).targetAlpha);
                        } else {
                            alpha = PZMath.max(alpha, obj.getAlpha(playerIndex));
                        }
                    }
                }
            }

            return alpha;
        }
    }

    public void setOffset(float x, float y, float z) {
        if (x != this.xoff || y != this.yoff || z != this.zoff) {
            this.xoff = x;
            this.yoff = y;
            this.zoff = z;
            this.syncIsoObject(false, (byte)1, null, null);
            this.invalidateRenderChunkLevel(256L);
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        byte i = (byte)this.square.getObjects().indexOf(this);
        b.putByte(i);
        b.putByte((byte)1);
        b.putByte((byte)1);
        b.putFloat(this.xoff);
        b.putFloat(this.yoff);
        b.putFloat(this.zoff);
        this.syncFluidContainerSend(b);
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source, ByteBuffer bb) {
        if (this.square == null) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
        } else if (this.getObjectIndex() == -1) {
            System.out
                .println(
                    "ERROR: "
                        + this.getClass().getSimpleName()
                        + " not found on square "
                        + this.square.getX()
                        + ","
                        + this.square.getY()
                        + ","
                        + this.square.getZ()
                );
        } else {
            if (GameClient.client && !bRemote) {
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                this.syncIsoObjectSend(b);
                PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
            } else if (GameServer.server && !bRemote) {
                for (UdpConnection connection : GameServer.udpEngine.connections) {
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send(connection);
                }
            } else if (bRemote) {
                this.xoff = bb.getFloat();
                this.yoff = bb.getFloat();
                this.zoff = bb.getFloat();
                this.syncFluidContainerReceive(bb);
                if (GameServer.server) {
                    for (UdpConnection connection : GameServer.udpEngine.connections) {
                        if (source != null && connection.getConnectedGUID() != source.getConnectedGUID()) {
                            ByteBufferWriter b = connection.startPacket();
                            PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                            this.syncIsoObjectSend(b);
                            PacketTypes.PacketType.SyncIsoObject.send(connection);
                        }
                    }
                }

                this.invalidateRenderChunkLevel(256L);
                this.square.RecalcProperties();
            }

            this.flagForHotSave();
        }
    }

    @Override
    public IsoGridSquare getRenderSquare() {
        if (this.getSquare() == null) {
            return null;
        } else {
            int CPW = 8;
            if (PZMath.coordmodulo(this.square.x, 8) == 0 && PZMath.coordmodulo(this.square.y, 8) == 7) {
                return this.square.getAdjacentSquare(IsoDirections.S);
            } else {
                return PZMath.coordmodulo(this.square.x, 8) == 7 && PZMath.coordmodulo(this.square.y, 8) == 0
                    ? this.square.getAdjacentSquare(IsoDirections.E)
                    : this.getSquare();
            }
        }
    }

    @Override
    public void setHighlighted(int playerIndex, boolean bHighlight, boolean bRenderOnce) {
        super.setHighlighted(playerIndex, bHighlight, bRenderOnce);
        ColorInfo highlightColor = Core.getInstance().getWorldItemHighlightColor();
        this.setHighlightColor(playerIndex, highlightColor.r, highlightColor.g, highlightColor.b, 1.0F);
        this.setOutlineHighlight(playerIndex, bHighlight);
        this.setOutlineHighlightCol(playerIndex, highlightColor);
    }

    public float getOffX() {
        return this.xoff;
    }

    public float getOffY() {
        return this.yoff;
    }

    public float getOffZ() {
        return this.zoff;
    }

    public void setOffX(float newoff) {
        this.xoff = newoff;
    }

    public void setOffY(float newoff) {
        this.yoff = newoff;
    }

    public void setOffZ(float newoff) {
        this.zoff = newoff;
    }

    public void syncExtendedPlacement() {
        if (this.getWorldObjectIndex() != -1) {
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.SyncExtendedPlacement, this);
            } else if (GameServer.server) {
                INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncExtendedPlacement, this.getX(), this.getY(), this);
            }
        }
    }
}
