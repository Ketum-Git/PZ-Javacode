// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.characters.Talker;
import zombie.chat.ChatManager;
import zombie.chat.ChatMessage;
import zombie.core.properties.PropertyContainer;
import zombie.interfaces.IUpdater;
import zombie.iso.IsoGridSquare;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.WaveSignalDevice;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemType;
import zombie.ui.UIFont;
import zombie.util.StringUtils;

/**
 * Turbo
 */
@UsedFromLua
public final class Radio extends Moveable implements Talker, IUpdater, WaveSignalDevice {
    protected DeviceData deviceData = new DeviceData(this);
    protected GameTime gameTime = GameTime.getInstance();
    private ItemBodyLocation canBeEquipped;
    protected int lastMin;
    protected boolean doPowerTick;
    protected int listenCnt;

    public Radio(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
        this.canBeDroppedOnFloor = true;
        this.itemType = ItemType.RADIO;
    }

    @Override
    public DeviceData getDeviceData() {
        return this.deviceData;
    }

    @Override
    public void setDeviceData(DeviceData data) {
        if (data == null) {
            data = new DeviceData(this);
        }

        if (this.deviceData != null) {
            this.deviceData.cleanSoundsAndEmitter();
        }

        this.deviceData = data;
        this.deviceData.setParent(this);
    }

    public void doReceiveSignal(int distance) {
        if (this.deviceData != null) {
            this.deviceData.doReceiveSignal(distance);
        }
    }

    @Override
    public void AddDeviceText(String line, float r, float g, float b, String guid, String codes, int distance) {
        if (!ZomboidRadio.isStaticSound(line)) {
            this.doReceiveSignal(distance);
        }

        IsoPlayer player = this.getPlayer();
        if (player != null && this.deviceData != null && this.deviceData.getDeviceVolume() > 0.0F && !player.hasTrait(CharacterTrait.DEAF)) {
            player.SayRadio(line, r, g, b, UIFont.Medium, this.deviceData.getDeviceVolumeRange(), this.deviceData.getChannel(), "radio");
            if (codes != null) {
                LuaEventManager.triggerEvent("OnDeviceText", guid, codes, -1, -1, -1, line, this);
            }
        }
    }

    public void AddDeviceText(ChatMessage msg, float r, float g, float b, String guid, String codes, int distance) {
        if (!ZomboidRadio.isStaticSound(msg.getText())) {
            this.doReceiveSignal(distance);
        }

        IsoPlayer player = this.getPlayer();
        if (player != null && this.deviceData != null && this.deviceData.getDeviceVolume() > 0.0F) {
            ChatManager.getInstance().showRadioMessage(msg);
            if (codes != null) {
                LuaEventManager.triggerEvent("OnDeviceText", guid, codes, -1, -1, -1, msg, this);
            }
        }
    }

    @Override
    public boolean HasPlayerInRange() {
        return false;
    }

    @Override
    public boolean ReadFromWorldSprite(String sprite) {
        if (StringUtils.isNullOrWhitespace(sprite)) {
            return false;
        } else {
            IsoSprite spr = IsoSpriteManager.instance.namedMap.get(sprite);
            if (spr != null) {
                PropertyContainer props = spr.getProperties();
                if (props.has("IsMoveAble")) {
                    if (props.has("CustomItem")) {
                        this.customItem = props.get("CustomItem");
                    }

                    this.worldSprite = sprite;
                    return true;
                }
            }

            System.out.println("Warning: Radio worldsprite not valid, sprite = " + (sprite == null ? "null" : sprite));
            return false;
        }
    }

    @Override
    public float getDelta() {
        return this.deviceData != null ? this.deviceData.getPower() : 0.0F;
    }

    @Override
    public void setDelta(float delta) {
        if (this.deviceData != null) {
            this.deviceData.setPower(delta);
        }
    }

    @Override
    public IsoGridSquare getSquare() {
        return this.container != null && this.container.parent != null && this.container.parent instanceof IsoPlayer ? this.container.parent.getSquare() : null;
    }

    @Override
    public float getX() {
        IsoGridSquare square = this.getSquare();
        return square == null ? 0.0F : square.getX();
    }

    @Override
    public float getY() {
        IsoGridSquare square = this.getSquare();
        return square == null ? 0.0F : square.getY();
    }

    @Override
    public float getZ() {
        IsoGridSquare square = this.getSquare();
        return square == null ? 0.0F : square.getZ();
    }

    @Override
    public IsoPlayer getPlayer() {
        return this.container != null && this.container.parent != null && this.container.parent instanceof IsoPlayer isoPlayer ? isoPlayer : null;
    }

    @Override
    public void render() {
    }

    @Override
    public void renderlast() {
    }

    @Override
    public void update() {
        if (this.deviceData != null) {
            if (!GameServer.server && !GameClient.client || GameClient.client) {
                boolean bEquipped = false;

                for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                    IsoPlayer p = IsoPlayer.players[i];
                    if (p != null && p.getEquipedRadio() == this) {
                        bEquipped = true;
                        break;
                    }
                }

                if (bEquipped) {
                    this.deviceData.update(false, true);
                } else {
                    this.deviceData.cleanSoundsAndEmitter();
                }
            }
        }
    }

    @Override
    public boolean IsSpeaking() {
        return false;
    }

    @Override
    public void Say(String line) {
    }

    @Override
    public String getSayLine() {
        return null;
    }

    @Override
    public String getTalkerType() {
        return "radio";
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        if (this.deviceData != null) {
            output.put((byte)1);
            this.deviceData.save(output, net);
        } else {
            output.put((byte)0);
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        if (this.deviceData == null) {
            this.deviceData = new DeviceData(this);
        }

        if (input.get() == 1) {
            this.deviceData.load(input, WorldVersion, false);
        }

        this.deviceData.setParent(this);
    }

    public void setCanBeEquipped(ItemBodyLocation canBeEquipped) {
        this.canBeEquipped = canBeEquipped;
    }

    @Override
    public ItemBodyLocation canBeEquipped() {
        return this.canBeEquipped;
    }

    public String getClothingExtraSubmenu() {
        return this.scriptItem.clothingExtraSubmenu;
    }
}
