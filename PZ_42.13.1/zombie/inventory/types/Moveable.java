// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Translator;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.util.StringUtils;

/**
 * Turbo.
 */
@UsedFromLua
public class Moveable extends InventoryItem {
    protected String worldSprite = "";
    private boolean isLight;
    private boolean lightUseBattery;
    private boolean lightHasBattery;
    private String lightBulbItem = "Base.LightBulb";
    private float lightPower;
    private float lightDelta = 2.5E-4F;
    private float lightR = 1.0F;
    private float lightG = 1.0F;
    private float lightB = 1.0F;
    private boolean isMultiGridAnchor;
    private IsoSpriteGrid spriteGrid;
    private String customNameFull = "Moveable Object";
    private String movableFullName = "Moveable Object";
    protected boolean canBeDroppedOnFloor;
    private boolean hasReadWorldSprite;
    protected String customItem;

    public Moveable(String module, String name, String type, String tex) {
        super(module, name, type, tex);
        this.itemType = ItemType.MOVEABLE;
    }

    public Moveable(String module, String name, String type, Item item) {
        super(module, name, type, item);
        this.itemType = ItemType.MOVEABLE;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return this.getName(null);
    }

    @Override
    public String getName(IsoPlayer player) {
        if (this instanceof Radio && ((Radio)this).getDeviceData() != null && ((Radio)this).getDeviceData().getIsTurnedOn()) {
            return Translator.getText("IGUI_ClothingNaming", Translator.getText("Tooltip_activated"), this.name);
        } else if (this.getScriptItem() != null && this.hasTag(ItemTag.USE_DISPLAY_NAME)) {
            return this.getScriptItem().getDisplayName();
        } else if ("Moveable Object".equals(this.movableFullName)) {
            return this.name;
        } else {
            return this.movableFullName.equals(this.name)
                ? Translator.getMoveableDisplayName(this.customNameFull)
                : Translator.getMoveableDisplayName(this.movableFullName) + this.customNameFull.substring(this.movableFullName.length());
        }
    }

    @Override
    public String getDisplayName() {
        return this.getName();
    }

    public boolean CanBeDroppedOnFloor() {
        if (this.worldSprite != null && this.spriteGrid != null) {
            IsoSprite spr = IsoSpriteManager.instance.getSprite(this.worldSprite);
            PropertyContainer props = spr.getProperties();
            return this.canBeDroppedOnFloor || !props.has("ForceSingleItem");
        } else {
            return this.canBeDroppedOnFloor;
        }
    }

    public String getMovableFullName() {
        return this.movableFullName;
    }

    public String getCustomNameFull() {
        return this.customNameFull;
    }

    public boolean isMultiGridAnchor() {
        return this.isMultiGridAnchor;
    }

    public IsoSpriteGrid getSpriteGrid() {
        return this.spriteGrid;
    }

    public String getWorldSprite() {
        return this.worldSprite;
    }

    public boolean ReadFromWorldSprite(String sprite) {
        if (sprite == null) {
            return false;
        } else if (this.hasReadWorldSprite && this.worldSprite != null && this.worldSprite.equalsIgnoreCase(sprite)) {
            return true;
        } else {
            this.customItem = null;

            try {
                IsoSprite spr = IsoSpriteManager.instance.namedMap.get(sprite);
                if (spr != null) {
                    PropertyContainer props = spr.getProperties();
                    if (props.has("IsMoveAble")) {
                        if (props.has("CustomItem")) {
                            this.customItem = props.get("CustomItem");
                            Item scriptItem = ScriptManager.instance.FindItem(this.customItem);
                            if (scriptItem != null) {
                                this.weight = this.actualWeight = scriptItem.actualWeight;
                            }

                            this.worldSprite = sprite;
                            if (spr.getSpriteGrid() != null) {
                                this.spriteGrid = spr.getSpriteGrid();
                                this.isMultiGridAnchor = spr == this.spriteGrid.getAnchorSprite();
                            }

                            this.getCustomIcon(sprite);
                            return true;
                        }

                        this.isLight = props.has("lightR");
                        this.worldSprite = sprite;
                        float pickUpWeight = 1.0F;
                        if (props.has("PickUpWeight")) {
                            pickUpWeight = Float.parseFloat(props.get("PickUpWeight")) / 10.0F;
                        }

                        this.weight = pickUpWeight;
                        this.actualWeight = pickUpWeight;
                        this.setCustomWeight(true);
                        String customName = "Moveable Object";
                        if (props.has("CustomName")) {
                            if (props.has("GroupName")) {
                                customName = props.get("GroupName") + " " + props.get("CustomName");
                            } else {
                                customName = props.get("CustomName");
                            }
                        }

                        this.movableFullName = customName;
                        this.name = customName;
                        this.customNameFull = customName;
                        if (spr.getSpriteGrid() != null) {
                            this.spriteGrid = spr.getSpriteGrid();
                            int id = this.spriteGrid.getSpriteIndex(spr);
                            int max = this.spriteGrid.getSpriteCount();
                            this.isMultiGridAnchor = spr == this.spriteGrid.getAnchorSprite();
                            if (!props.has("ForceSingleItem")) {
                                this.name = this.name + " (" + (id + 1) + "/" + max + ")";
                            } else {
                                this.name = this.name + " (1/1)";
                            }

                            this.customNameFull = this.name;
                            Texture icontexture = null;
                            String tex = "Item_Flatpack";
                            if ("Item_Flatpack" != null) {
                                icontexture = Texture.getSharedTexture("Item_Flatpack");
                                this.setColor(new Color(Rand.Next(0.7F, 1.0F), Rand.Next(0.7F, 1.0F), Rand.Next(0.7F, 1.0F)));
                            }

                            if (icontexture == null) {
                                icontexture = Texture.getSharedTexture("media/inventory/Question_On.png");
                            }

                            this.setTexture(icontexture);
                            this.getModData().rawset("Flatpack", "true");
                        } else if (this.texture == null
                            || this.texture.getName() == null
                            || this.texture.getName().equals("Item_Moveable_object")
                            || this.texture.getName().equals("Question_On")) {
                            Texture icontexturex = null;
                            String texx = sprite;
                            if (sprite != null) {
                                icontexturex = Texture.getSharedTexture(sprite);
                                if (icontexturex != null) {
                                    icontexturex = icontexturex.splitIcon();
                                }
                            }

                            if (icontexturex == null) {
                                if (!props.has("MoveType")) {
                                    texx = "Item_Moveable_object";
                                } else if (props.get("MoveType").equals("WallObject")) {
                                    texx = "Item_Moveable_wallobject";
                                } else if (props.get("MoveType").equals("WindowObject")) {
                                    texx = "Item_Moveable_windowobject";
                                } else if (props.get("MoveType").equals("Window")) {
                                    texx = "Item_Moveable_window";
                                } else if (props.get("MoveType").equals("FloorTile")) {
                                    texx = "Item_Moveable_floortile";
                                } else if (props.get("MoveType").equals("FloorRug")) {
                                    texx = "Item_Moveable_floorrug";
                                } else if (props.get("MoveType").equals("Vegitation")) {
                                    texx = "Item_Moveable_vegitation";
                                }

                                if (texx != null) {
                                    icontexturex = Texture.getSharedTexture(texx);
                                }
                            }

                            if (icontexturex == null) {
                                icontexturex = Texture.getSharedTexture("media/inventory/Question_On.png");
                            }

                            this.setTexture(icontexturex);
                        }

                        this.hasReadWorldSprite = true;
                        return true;
                    }
                }
            } catch (Exception var10) {
                DebugLog.Moveable.debugln("Error in Moveable item: " + var10.getMessage());
            }

            DebugLog.Moveable.warn("Warning: Moveable not valid for " + sprite);
            return false;
        }
    }

    public void getCustomIcon(String sprite) {
        if (this.texture == null
            || this.texture.getName() == null
            || this.texture.getName().equals("Item_Moveable_object")
            || this.texture.getName().equals("Question_On")) {
            IsoSprite spr = IsoSpriteManager.instance.namedMap.get(sprite);
            PropertyContainer props = spr.getProperties();
            Texture icontexture = null;
            String tex = sprite;
            if (sprite != null) {
                icontexture = Texture.getSharedTexture(sprite);
                if (icontexture != null) {
                    icontexture = icontexture.splitIcon();
                }
            }

            if (icontexture == null) {
                if (!props.has("MoveType")) {
                    tex = "Item_Moveable_object";
                } else if (props.get("MoveType").equals("WallObject")) {
                    tex = "Item_Moveable_wallobject";
                } else if (props.get("MoveType").equals("WindowObject")) {
                    tex = "Item_Moveable_windowobject";
                } else if (props.get("MoveType").equals("Window")) {
                    tex = "Item_Moveable_window";
                } else if (props.get("MoveType").equals("FloorTile")) {
                    tex = "Item_Moveable_floortile";
                } else if (props.get("MoveType").equals("FloorRug")) {
                    tex = "Item_Moveable_floorrug";
                } else if (props.get("MoveType").equals("Vegitation")) {
                    tex = "Item_Moveable_vegitation";
                }

                if (tex != null) {
                    icontexture = Texture.getSharedTexture(tex);
                }
            }

            if (icontexture == null) {
                icontexture = Texture.getSharedTexture("media/inventory/Question_On.png");
            }

            this.setTexture(icontexture);
        }
    }

    public boolean isLight() {
        return this.isLight;
    }

    public void setLight(boolean isLight) {
        this.isLight = isLight;
    }

    public boolean isLightUseBattery() {
        return this.lightUseBattery;
    }

    public void setLightUseBattery(boolean lightUseBattery) {
        this.lightUseBattery = lightUseBattery;
    }

    public boolean isLightHasBattery() {
        return this.lightHasBattery;
    }

    public void setLightHasBattery(boolean lightHasBattery) {
        this.lightHasBattery = lightHasBattery;
    }

    public String getLightBulbItem() {
        return this.lightBulbItem;
    }

    public void setLightBulbItem(String lightBulbItem) {
        this.lightBulbItem = lightBulbItem;
    }

    public float getLightPower() {
        return this.lightPower;
    }

    public void setLightPower(float lightPower) {
        this.lightPower = lightPower;
    }

    public float getLightDelta() {
        return this.lightDelta;
    }

    public void setLightDelta(float lightDelta) {
        this.lightDelta = lightDelta;
    }

    public float getLightR() {
        return this.lightR;
    }

    public void setLightR(float lightR) {
        this.lightR = lightR;
    }

    public float getLightG() {
        return this.lightG;
    }

    public void setLightG(float lightG) {
        this.lightG = lightG;
    }

    public float getLightB() {
        return this.lightB;
    }

    public void setLightB(float lightB) {
        this.lightB = lightB;
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        GameWindow.WriteString(output, this.worldSprite);
        output.put((byte)(this.isLight ? 1 : 0));
        if (this.isLight) {
            output.put((byte)(this.lightUseBattery ? 1 : 0));
            output.put((byte)(this.lightHasBattery ? 1 : 0));
            output.put((byte)(this.lightBulbItem != null ? 1 : 0));
            if (this.lightBulbItem != null) {
                GameWindow.WriteString(output, this.lightBulbItem);
            }

            output.putFloat(this.lightPower);
            output.putFloat(this.lightDelta);
            output.putFloat(this.lightR);
            output.putFloat(this.lightG);
            output.putFloat(this.lightB);
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.worldSprite = GameWindow.ReadString(input);
        if (!this.ReadFromWorldSprite(this.worldSprite)
            && this instanceof Radio
            && this.getScriptItem() != null
            && !StringUtils.isNullOrWhitespace(this.getScriptItem().worldObjectSprite)) {
            DebugLog.log("Moveable.load -> Radio item = " + (this.fullType != null ? this.fullType : "unknown"));
        }

        if (this.customItem == null && !StringUtils.isNullOrWhitespace(this.worldSprite) && !this.type.equalsIgnoreCase(this.worldSprite)) {
            this.type = this.worldSprite;
            this.fullType = this.module + "." + this.worldSprite;
        }

        this.isLight = input.get() == 1;
        if (this.isLight) {
            this.lightUseBattery = input.get() == 1;
            this.lightHasBattery = input.get() == 1;
            if (input.get() == 1) {
                this.lightBulbItem = GameWindow.ReadString(input);
            }

            this.lightPower = input.getFloat();
            this.lightDelta = input.getFloat();
            this.lightR = input.getFloat();
            this.lightG = input.getFloat();
            this.lightB = input.getFloat();
        }
    }

    public void setWorldSprite(String WorldSprite) {
        this.worldSprite = WorldSprite;
    }
}
