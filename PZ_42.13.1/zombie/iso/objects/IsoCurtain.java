// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.iso.ICurtain;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.LosUtil;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.util.Type;
import zombie.util.list.PZArrayList;

@UsedFromLua
public class IsoCurtain extends IsoObject implements ICurtain {
    public boolean barricaded;
    public Integer barricadeMaxStrength = 0;
    public Integer barricadeStrength = 0;
    public Integer health = 1000;
    public boolean locked;
    public Integer maxHealth = 1000;
    public Integer pushedMaxStrength = 0;
    public Integer pushedStrength = 0;
    private IsoSprite closedSprite;
    public boolean north;
    public boolean open;
    private IsoSprite openSprite;
    private final boolean destroyed = false;

    public void removeSheet(IsoGameCharacter chr) {
        this.square.transmitRemoveItemFromSquare(this);
        InventoryItem item = chr.getInventory().AddItem("Base.Sheet");
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(chr.getInventory(), item);
        }

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            LosUtil.cachecleared[pn] = true;
        }

        GameTime.instance.lightSourceUpdate = 100.0F;
        IsoGridSquare.setRecalcLightTime(-1.0F);
    }

    public IsoCurtain(IsoCell cell, IsoGridSquare gridSquare, IsoSprite gid, boolean north, boolean spriteclosed) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        if (spriteclosed) {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 4);
            this.closedSprite = gid;
        } else {
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -4);
            this.openSprite = gid;
        }

        this.open = true;
        this.sprite = this.openSprite;
        this.square = gridSquare;
        this.north = north;
        this.DirtySlice();
    }

    public IsoCurtain(IsoCell cell, IsoGridSquare gridSquare, String gid, boolean north) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -4);
        this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 0);
        this.open = true;
        this.sprite = this.openSprite;
        this.square = gridSquare;
        this.north = north;
        this.DirtySlice();
    }

    public IsoCurtain(IsoCell cell) {
        super(cell);
    }

    @Override
    public String getObjectName() {
        return "Curtain";
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0F, 0.0F);
        } else if (this.getType() == IsoObjectType.curtainS) {
            return pos.set(this.getX() + 0.5F, this.getY() + 1.0F);
        } else if (this.getType() == IsoObjectType.curtainE) {
            return pos.set(this.getX() + 1.0F, this.getY() + 0.5F);
        } else {
            return this.north ? pos.set(this.getX() + 0.5F, this.getY()) : pos.set(this.getX(), this.getY() + 0.5F);
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.open = input.get() == 1;
        this.north = input.get() == 1;
        this.health = input.getInt();
        this.barricadeStrength = input.getInt();
        if (this.open) {
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
            this.openSprite = this.sprite;
        } else {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
            this.closedSprite = this.sprite;
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.put((byte)(this.open ? 1 : 0));
        output.put((byte)(this.north ? 1 : 0));
        output.putInt(this.health);
        output.putInt(this.barricadeStrength);
        if (this.open) {
            output.putInt(this.closedSprite.id);
        } else {
            output.putInt(this.openSprite.id);
        }
    }

    public boolean getNorth() {
        return this.north;
    }

    public boolean IsOpen() {
        return this.open;
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return false;
    }

    public boolean canInteractWith(IsoGameCharacter chr) {
        if (chr != null && chr.getCurrentSquare() != null) {
            IsoGridSquare chrSq = chr.getCurrentSquare();
            return (this.isAdjacentToSquare(chrSq) || chrSq == this.getOppositeSquare()) && !this.getSquare().isBlockedTo(chrSq);
        } else {
            return false;
        }
    }

    public IsoGridSquare getOppositeSquare() {
        if (this.getType() == IsoObjectType.curtainN) {
            return this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ());
        } else if (this.getType() == IsoObjectType.curtainS) {
            return this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() + 1.0F), (double)this.getZ());
        } else if (this.getType() == IsoObjectType.curtainW) {
            return this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ());
        } else {
            return this.getType() == IsoObjectType.curtainE
                ? this.getCell().getGridSquare((double)(this.getX() + 1.0F), (double)this.getY(), (double)this.getZ())
                : null;
        }
    }

    public boolean isAdjacentToSquare(IsoGridSquare square1, IsoGridSquare square2) {
        if (square1 != null && square2 != null) {
            return this.getType() != IsoObjectType.curtainN && this.getType() != IsoObjectType.curtainS
                ? square1.x == square2.x && Math.abs(square1.y - square2.y) <= 1
                : square1.y == square2.y && Math.abs(square1.x - square2.x) <= 1;
        } else {
            return false;
        }
    }

    public boolean isAdjacentToSquare(IsoGridSquare square2) {
        return this.isAdjacentToSquare(this.getSquare(), square2);
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        if (to.getZ() != from.getZ()) {
            return IsoObject.VisionResult.NoEffect;
        } else {
            if (from == this.square && (this.getType() == IsoObjectType.curtainW || this.getType() == IsoObjectType.curtainN)
                || from != this.square && (this.getType() == IsoObjectType.curtainE || this.getType() == IsoObjectType.curtainS)) {
                if (this.north && to.getY() < from.getY() && !this.open) {
                    return IsoObject.VisionResult.Blocked;
                }

                if (!this.north && to.getX() < from.getX() && !this.open) {
                    return IsoObject.VisionResult.Blocked;
                }
            } else {
                if (this.north && to.getY() > from.getY() && !this.open) {
                    return IsoObject.VisionResult.Blocked;
                }

                if (!this.north && to.getX() > from.getX() && !this.open) {
                    return IsoObject.VisionResult.Blocked;
                }
            }

            return IsoObject.VisionResult.NoEffect;
        }
    }

    public void ToggleDoor(IsoGameCharacter chr) {
        if (!this.barricaded) {
            this.DirtySlice();
            if (!this.locked || chr == null || chr.getCurrentSquare().getRoom() != null || this.open) {
                this.open = !this.open;
                this.sprite = this.closedSprite;
                if (this.open) {
                    this.sprite = this.openSprite;
                    if (chr != null) {
                        chr.playSound(this.getSoundPrefix() + "Open");
                    }
                } else if (chr != null) {
                    chr.playSound(this.getSoundPrefix() + "Close");
                }

                this.square.RecalcAllWithNeighbours(true);
                this.syncIsoObject(false, (byte)(this.open ? 1 : 0), null);
                this.invalidateVispolyChunkLevel();
                this.invalidateRenderChunkLevel(256L);
            }
        }
    }

    public void ToggleDoorSilent() {
        if (!this.barricaded) {
            this.DirtySlice();

            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                LosUtil.cachecleared[pn] = true;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
            IsoGridSquare.setRecalcLightTime(-1.0F);
            this.open = !this.open;
            this.sprite = this.closedSprite;
            if (this.open) {
                this.sprite = this.openSprite;
            }

            this.syncIsoObject(false, (byte)(this.open ? 1 : 0), null);
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (!PerformanceSettings.fboRenderChunk) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoObject attachedTo = this.getObjectAttachedTo();
            if (attachedTo != null && this.getSquare().getTargetDarkMulti(playerIndex) <= attachedTo.getSquare().getTargetDarkMulti(playerIndex)) {
                col = attachedTo.getSquare().lighting[playerIndex].lightInfo();
                this.setTargetAlpha(playerIndex, attachedTo.getTargetAlpha(playerIndex));
            }
        }
        IsoDirections dir = switch (this.getType()) {
            case curtainN -> IsoDirections.N;
            case curtainS -> IsoDirections.S;
            case curtainW -> IsoDirections.W;
            case curtainE -> IsoDirections.E;
            default -> IsoDirections.Max;
        };
        Vector3f curtainOffset = null;
        float closestDistSq = Float.MAX_VALUE;

        for (int i = 0; i < this.getSquare().getObjects().size(); i++) {
            IsoObject object = this.getSquare().getObjects().get(i);
            if (!(object instanceof IsoWorldInventoryObject)) {
                IsoSprite sprite = object.getSprite();
                if (sprite != null
                    && sprite.getType() != IsoObjectType.curtainN
                    && sprite.getType() != IsoObjectType.curtainS
                    && sprite.getType() != IsoObjectType.curtainW
                    && sprite.getType() != IsoObjectType.curtainE) {
                    Vector3f curtainOffset1 = sprite.getCurtainOffset();
                    if (curtainOffset1 != null) {
                        float distSq = IsoUtils.DistanceToSquared(curtainOffset1.x(), curtainOffset1.z(), 0.0F, 0.0F);
                        if (curtainOffset == null) {
                            curtainOffset = curtainOffset1;
                            closestDistSq = distSq;
                        } else if (distSq < closestDistSq) {
                            curtainOffset = curtainOffset1;
                            closestDistSq = distSq;
                        }
                    }
                }
            }
        }

        if (PerformanceSettings.fboRenderChunk && curtainOffset != null && this.getSpriteModel() != null) {
            float ox = curtainOffset.x();
            float oy = curtainOffset.z();
            float oz = curtainOffset.y() / 2.44949F;
            if (IsoBarricade.GetBarricadeOnSquare(this.getSquare(), dir) != null) {
                ox = PZMath.max(ox, dir.dx() * 0.4F);
                oy = PZMath.max(oy, dir.dy() * 0.37F);
            }

            SpriteModel spriteModel1 = this.getSpriteModel();
            float x1 = spriteModel1.getTranslate().x();
            float y1 = spriteModel1.getTranslate().y();
            float z1 = spriteModel1.getTranslate().z();

            try {
                spriteModel1.getTranslate().set(0.0F);
                super.render(x + ox, y + oy, z + oz, col, bDoAttached, bWallLightingPass, shader);
            } finally {
                spriteModel1.getTranslate().set(x1, y1, z1);
            }

            this.sx = 0.0F;
        } else if (IsoBarricade.GetBarricadeOnSquare(this.getSquare(), dir) != null) {
            float offsetX1 = this.offsetX;
            float offsetY1 = this.offsetY;
            this.offsetX = this.offsetX + IsoUtils.XToScreen(dir.dx() * 0.03F, dir.dy() * 0.03F, 0.0F, 0);
            this.offsetY = this.offsetY + IsoUtils.YToScreen(dir.dx() * 0.03F, dir.dy() * 0.03F, 0.0F, 0);
            super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
            this.offsetX = offsetX1;
            this.offsetY = offsetY1;
            this.sx = 0.0F;
        } else {
            super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
            this.sx = 0.0F;
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
        b.putByte((byte)(this.open ? 1 : 0));
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source, ByteBuffer bb) {
        this.syncIsoObject(bRemote, val, source);
    }

    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source) {
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
            } else {
                if (bRemote) {
                    if (val == 1) {
                        this.open = true;
                        this.sprite = this.openSprite;
                    } else {
                        this.open = false;
                        this.sprite = this.closedSprite;
                    }
                }

                if (GameServer.server) {
                    for (UdpConnection connection : GameServer.udpEngine.connections) {
                        ByteBufferWriter b = connection.startPacket();
                        PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                        this.syncIsoObjectSend(b);
                        PacketTypes.PacketType.SyncIsoObject.send(connection);
                    }
                }
            }

            this.square.RecalcProperties();
            this.square.RecalcAllWithNeighbours(true);

            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                LosUtil.cachecleared[pn] = true;
            }

            IsoGridSquare.setRecalcLightTime(-1.0F);
            GameTime.instance.lightSourceUpdate = 100.0F;
            Core.dirtyGlobalLightsCount++;
            LuaEventManager.triggerEvent("OnContainerUpdate");
            if (this.square != null) {
                this.square.RecalcProperties();
            }

            this.invalidateVispolyChunkLevel();
            this.invalidateRenderChunkLevel(256L);
            this.flagForHotSave();
        }
    }

    public IsoObject getObjectAttachedTo() {
        int index = this.getObjectIndex();
        if (index == -1) {
            return null;
        } else {
            PZArrayList<IsoObject> objects = this.getSquare().getObjects();
            if (this.getType() != IsoObjectType.curtainW && this.getType() != IsoObjectType.curtainN) {
                if (this.getType() == IsoObjectType.curtainE || this.getType() == IsoObjectType.curtainS) {
                    IsoGridSquare sq = this.getOppositeSquare();
                    if (sq != null) {
                        boolean north = this.getType() == IsoObjectType.curtainS;
                        objects = sq.getObjects();

                        for (int i = objects.size() - 1; i >= 0; i--) {
                            BarricadeAble obj = Type.tryCastTo(objects.get(i), BarricadeAble.class);
                            if (obj != null && north == obj.getNorth()) {
                                return objects.get(i);
                            }
                        }
                    }
                }
            } else {
                boolean north = this.getType() == IsoObjectType.curtainN;

                for (int ix = index - 1; ix >= 0; ix--) {
                    BarricadeAble obj = Type.tryCastTo(objects.get(ix), BarricadeAble.class);
                    if (obj != null && north == obj.getNorth()) {
                        return objects.get(ix);
                    }
                }
            }

            return null;
        }
    }

    public String getSoundPrefix() {
        if (this.closedSprite == null) {
            return "CurtainShort";
        } else {
            PropertyContainer props = this.closedSprite.getProperties();
            return props.has("CurtainSound") ? "Curtain" + props.get("CurtainSound") : "CurtainShort";
        }
    }

    public static boolean isSheet(IsoObject curtain) {
        if (curtain instanceof IsoDoor isoDoor) {
            curtain = isoDoor.HasCurtains();
        }

        if (curtain instanceof IsoThumpable isoThumpable) {
            curtain = isoThumpable.HasCurtains();
        }

        if (curtain instanceof IsoWindow isoWindow) {
            curtain = isoWindow.HasCurtains();
        }

        if (curtain != null && curtain.getSprite() != null) {
            IsoSprite sprite = curtain.getSprite();
            return sprite.getProperties().has("CurtainSound") ? "Sheet".equals(sprite.getProperties().get("CurtainSound")) : false;
        } else {
            return false;
        }
    }

    @Override
    public boolean isCurtainOpen() {
        return this.IsOpen();
    }
}
