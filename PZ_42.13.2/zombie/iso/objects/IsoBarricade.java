// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.ThumpState;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IHasHealth;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.util.Type;
import zombie.util.list.PZArrayList;

@UsedFromLua
public class IsoBarricade extends IsoObject implements Thumpable, IHasHealth {
    public static final int MAX_PLANKS = 4;
    public static final int PLANK_HEALTH = 1000;
    public static final int METAL_BAR_HEALTH = 3000;
    public static final int METAL_HEALTH = 5000;
    public static final int METAL_HEALTH_DAMAGED = 2500;
    private final int[] plankHealth = new int[4];
    private int metalHealth;
    private int metalBarHealth;

    public IsoBarricade(IsoCell cell) {
        super(cell);
    }

    public IsoBarricade(IsoGridSquare gridSquare, IsoDirections dir) {
        this.square = gridSquare;
        this.dir = dir;
    }

    @Override
    public String getObjectName() {
        return "Barricade";
    }

    public void addPlank(IsoGameCharacter chr) {
        InventoryItem plank = InventoryItemFactory.CreateItem("Base.Plank");
        this.addPlank(chr, plank);
    }

    public void addPlank(IsoGameCharacter chr, InventoryItem plank) {
        if (this.canAddPlank()) {
            int plankHealth = 1000;
            if (plank != null) {
                plankHealth = (int)((float)plank.getCondition() / plank.getConditionMax() * 1000.0F);
            }

            if (chr != null) {
                plankHealth = (int)(plankHealth * chr.getBarricadeStrengthMod());
            }

            for (int i = 0; i < 4; i++) {
                if (this.plankHealth[i] <= 0) {
                    this.plankHealth[i] = plankHealth;
                    break;
                }
            }

            this.updateSprite();
            recalculateLighting();
        }
    }

    public InventoryItem removePlank(IsoGameCharacter chr) {
        if (this.getNumPlanks() <= 0) {
            return null;
        } else {
            InventoryItem item = null;

            for (int i = 3; i >= 0; i--) {
                if (this.plankHealth[i] > 0) {
                    float f = Math.min(this.plankHealth[i] / 1000.0F, 1.0F);
                    item = InventoryItemFactory.CreateItem("Base.Plank");
                    item.setCondition((int)Math.max(item.getConditionMax() * f, 1.0F));
                    this.plankHealth[i] = 0;
                    break;
                }
            }

            if (this.getNumPlanks() <= 0) {
                if (this.square != null) {
                    if (GameServer.server) {
                        this.square.transmitRemoveItemFromSquare(this);
                    } else {
                        this.square.RemoveTileObject(this);
                    }
                }
            } else {
                this.updateSprite();
                recalculateLighting();
            }

            return item;
        }
    }

    public int getNumPlanks() {
        int count = 0;

        for (int i = 0; i < 4; i++) {
            if (this.plankHealth[i] > 0) {
                count++;
            }
        }

        return count;
    }

    public boolean canAddPlank() {
        return !this.isMetal() && this.getNumPlanks() < 4 && !this.isMetalBar();
    }

    public void addMetalBar(IsoGameCharacter chr, InventoryItem metalBar) {
        if (this.getNumPlanks() <= 0) {
            if (this.metalHealth <= 0) {
                if (this.metalBarHealth <= 0) {
                    this.metalBarHealth = 3000;
                    if (metalBar != null) {
                        this.metalBarHealth = (int)((float)metalBar.getCondition() / metalBar.getConditionMax() * 5000.0F);
                    }

                    if (chr != null) {
                        this.metalBarHealth = (int)(this.metalBarHealth * chr.getMetalBarricadeStrengthMod());
                    }

                    this.updateSprite();
                    recalculateLighting();
                }
            }
        }
    }

    public InventoryItem removeMetalBar(IsoGameCharacter chr) {
        if (this.metalBarHealth <= 0) {
            return null;
        } else {
            float f = Math.min(this.metalBarHealth / 3000.0F, 1.0F);
            this.metalBarHealth = 0;
            InventoryItem item = InventoryItemFactory.CreateItem("Base.MetalBar");
            item.setCondition((int)Math.min(item.getConditionMax() * f, 1.0F));
            if (this.square != null) {
                if (GameServer.server) {
                    this.square.transmitRemoveItemFromSquare(this);
                } else {
                    this.square.RemoveTileObject(this);
                }
            }

            return item;
        }
    }

    public void addMetal(IsoGameCharacter chr, InventoryItem metal) {
        if (this.getNumPlanks() <= 0) {
            if (this.metalHealth <= 0) {
                this.metalHealth = 5000;
                if (metal != null) {
                    this.metalHealth = (int)((float)metal.getCondition() / metal.getConditionMax() * 5000.0F);
                }

                if (chr != null) {
                    this.metalHealth = (int)(this.metalHealth * chr.getMetalBarricadeStrengthMod());
                }

                this.updateSprite();
                recalculateLighting();
            }
        }
    }

    public boolean isMetalBar() {
        return this.metalBarHealth > 0;
    }

    public InventoryItem removeMetal(IsoGameCharacter chr) {
        if (this.metalHealth <= 0) {
            return null;
        } else {
            float f = Math.min(this.metalHealth / 5000.0F, 1.0F);
            this.metalHealth = 0;
            InventoryItem item = InventoryItemFactory.CreateItem("Base.SheetMetal");
            item.setCondition((int)Math.max(item.getConditionMax() * f, 1.0F));
            if (this.square != null) {
                if (GameServer.server) {
                    this.square.transmitRemoveItemFromSquare(this);
                } else {
                    this.square.RemoveTileObject(this);
                }
            }

            return item;
        }
    }

    public boolean isMetal() {
        return this.metalHealth > 0;
    }

    public boolean isBlockVision() {
        return this.isMetal() || this.getNumPlanks() > 2;
    }

    private void chooseSprite() {
        IsoSpriteManager spriteManager = IsoSpriteManager.instance;
        if (this.metalHealth > 0) {
            int damageOffset = this.metalHealth <= 2500 ? 2 : 0;
            String tileset = "constructedobjects_01";
            switch (this.dir) {
                case W:
                    this.sprite = spriteManager.getSprite("constructedobjects_01_" + (24 + damageOffset));
                    break;
                case N:
                    this.sprite = spriteManager.getSprite("constructedobjects_01_" + (25 + damageOffset));
                    break;
                case E:
                    this.sprite = spriteManager.getSprite("constructedobjects_01_" + (28 + damageOffset));
                    break;
                case S:
                    this.sprite = spriteManager.getSprite("constructedobjects_01_" + (29 + damageOffset));
                    break;
                default:
                    this.sprite.LoadFramesNoDirPageSimple("media/ui/missing-tile.png");
            }
        } else if (this.metalBarHealth > 0) {
            String tileset = "constructedobjects_01";
            switch (this.dir) {
                case W:
                    this.sprite = spriteManager.getSprite("constructedobjects_01_55");
                    break;
                case N:
                    this.sprite = spriteManager.getSprite("constructedobjects_01_53");
                    break;
                case E:
                    this.sprite = spriteManager.getSprite("constructedobjects_01_52");
                    break;
                case S:
                    this.sprite = spriteManager.getSprite("constructedobjects_01_54");
                    break;
                default:
                    this.sprite.LoadFramesNoDirPageSimple("media/ui/missing-tile.png");
            }
        } else {
            int numPlanks = this.getNumPlanks();
            if (numPlanks <= 0) {
                this.sprite = spriteManager.getSprite("media/ui/missing-tile.png");
            } else {
                String tileset = "carpentry_01";
                switch (this.dir) {
                    case W:
                        this.sprite = spriteManager.getSprite("carpentry_01_" + (8 + (numPlanks - 1) * 2));
                        break;
                    case N:
                        this.sprite = spriteManager.getSprite("carpentry_01_" + (9 + (numPlanks - 1) * 2));
                        break;
                    case E:
                        this.sprite = spriteManager.getSprite("carpentry_01_" + (0 + (numPlanks - 1) * 2));
                        break;
                    case S:
                        this.sprite = spriteManager.getSprite("carpentry_01_" + (1 + (numPlanks - 1) * 2));
                        break;
                    default:
                        this.sprite.LoadFramesNoDirPageSimple("media/ui/missing-tile.png");
                }
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.metalHealth <= 0 && this.getNumPlanks() <= 0 && this.metalBarHealth <= 0;
    }

    @Override
    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        return false;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        if (this.metalHealth <= 0 && this.getNumPlanks() <= 2) {
            return IsoObject.VisionResult.NoEffect;
        } else {
            if (from == this.square) {
                if (this.dir == IsoDirections.N && to.getY() < from.getY()) {
                    return IsoObject.VisionResult.Blocked;
                }

                if (this.dir == IsoDirections.S && to.getY() > from.getY()) {
                    return IsoObject.VisionResult.Blocked;
                }

                if (this.dir == IsoDirections.W && to.getX() < from.getX()) {
                    return IsoObject.VisionResult.Blocked;
                }

                if (this.dir == IsoDirections.E && to.getX() > from.getX()) {
                    return IsoObject.VisionResult.Blocked;
                }
            } else if (to == this.square && from != this.square) {
                return this.TestVision(to, from);
            }

            return IsoObject.VisionResult.NoEffect;
        }
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        if (!this.isDestroyed()) {
            if (thumper instanceof IsoZombie isoZombie) {
                int numPlanks = this.getNumPlanks();
                boolean metalOK = this.metalHealth > 2500;
                int mult = ThumpState.getFastForwardDamageMultiplier();
                this.Damage(isoZombie.strength * mult);
                if (numPlanks != this.getNumPlanks()) {
                    ((IsoGameCharacter)thumper).getEmitter().playSound("BreakBarricadePlank");
                    if (GameServer.server) {
                        GameServer.PlayWorldSoundServer("BreakBarricadePlank", false, thumper.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
                    }
                }

                if (this.isDestroyed()) {
                    if (this.getSquare().getBuilding() != null) {
                        this.getSquare().getBuilding().forceAwake();
                    }

                    this.square.transmitRemoveItemFromSquare(this);
                    if (!GameServer.server) {
                        this.square.RemoveTileObject(this);
                    }
                } else if ((numPlanks != this.getNumPlanks() || metalOK && this.metalHealth < 2500) && GameServer.server) {
                    this.sendObjectChange("state");
                }

                if (!this.isDestroyed()) {
                    this.setRenderEffect(RenderEffectType.Hit_Door, true);
                }

                WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
            }
        }
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        return this.isDestroyed() ? null : this;
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0F, 0.0F);
        } else if (this.dir == IsoDirections.N) {
            return pos.set(this.getX() + 0.5F, this.getY());
        } else if (this.dir == IsoDirections.S) {
            return pos.set(this.getX() + 0.5F, this.getY() + 1.0F);
        } else if (this.dir == IsoDirections.W) {
            return pos.set(this.getX(), this.getY() + 0.5F);
        } else {
            return this.dir == IsoDirections.E ? pos.set(this.getX() + 1.0F, this.getY() + 0.5F) : pos.set(this.getX(), this.getY() + 0.5F);
        }
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        if (!this.isDestroyed()) {
            IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
            if (!GameClient.client) {
                LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
                String sound = !this.isMetal() && !this.isMetalBar() ? "HitBarricadePlank" : "HitBarricadeMetal";
                if (player != null) {
                    player.setMeleeHitSurface(
                        !this.isMetal() && !this.isMetalBar() ? ParameterMeleeHitSurface.Material.Wood : ParameterMeleeHitSurface.Material.Metal
                    );
                }

                SoundManager.instance.PlayWorldSound(sound, false, this.getSquare(), 1.0F, 20.0F, 2.0F, false);
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer(sound, false, this.getSquare(), 1.0F, 20.0F, 2.0F, false);
                }

                if (weapon != null) {
                    this.Damage(weapon.getDoorDamage() * 5.0F);
                } else {
                    this.Damage(100.0F);
                }

                WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                if (this.isDestroyed()) {
                    if (owner != null) {
                        String sound2 = sound.equals("HitBarricadeMetal") ? "BreakBarricadeMetal" : "BreakBarricadePlank";
                        owner.getEmitter().playSound(sound2);
                        if (GameServer.server) {
                            GameServer.PlayWorldSoundServer(sound2, false, owner.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
                        }
                    }

                    this.square.transmitRemoveItemFromSquare(this);
                    if (!GameServer.server) {
                        this.square.RemoveTileObject(this);
                    }
                }

                if (!this.isDestroyed()) {
                    this.setRenderEffect(RenderEffectType.Hit_Door, true);
                }
            }
        }
    }

    @Override
    public void Damage(float amount) {
        if (!"Tutorial".equals(Core.gameMode)) {
            if (this.metalHealth > 0) {
                this.metalHealth = (int)(this.metalHealth - amount);
                if (this.metalHealth <= 0) {
                    this.metalHealth = 0;
                    this.chooseSprite();
                }

                if (GameServer.server) {
                    this.sync();
                }
            } else if (this.metalBarHealth > 0) {
                this.metalBarHealth = (int)(this.metalBarHealth - amount);
                if (this.metalBarHealth <= 0) {
                    this.metalBarHealth = 0;
                    this.chooseSprite();
                }

                if (GameServer.server) {
                    this.sync();
                }
            } else {
                for (int i = 3; i >= 0; i--) {
                    if (this.plankHealth[i] > 0) {
                        this.plankHealth[i] = (int)(this.plankHealth[i] - amount);
                        if (this.plankHealth[i] <= 0) {
                            this.plankHealth[i] = 0;
                            this.chooseSprite();
                        }
                        break;
                    }
                }

                if (GameServer.server) {
                    this.sync();
                }
            }
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        super.syncIsoObjectSend(b);
        b.putInt(this.metalHealth);
        b.putInt(this.metalBarHealth);

        for (int i = 0; i < 4; i++) {
            b.putInt(this.plankHealth[i]);
        }
    }

    @Override
    public void syncIsoObjectReceive(ByteBuffer bb) {
        super.syncIsoObjectReceive(bb);
        this.metalHealth = bb.getInt();
        this.metalBarHealth = bb.getInt();

        for (int i = 0; i < 4; i++) {
            this.plankHealth[i] = bb.getInt();
        }
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source, ByteBuffer bb) {
        if (GameClient.client && bRemote) {
            this.syncIsoObjectReceive(bb);
        }

        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if (source == null || connection.getConnectedGUID() != source.getConnectedGUID()) {
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send(connection);
                }
            }
        }
    }

    @Override
    public float getThumpCondition() {
        if (this.metalHealth > 0) {
            return PZMath.clamp(this.metalHealth, 0, 5000) / 5000.0F;
        } else if (this.metalBarHealth > 0) {
            return PZMath.clamp(this.metalBarHealth, 0, 3000) / 3000.0F;
        } else {
            for (int i = 3; i >= 0; i--) {
                if (this.plankHealth[i] > 0) {
                    return PZMath.clamp(this.plankHealth[i], 0, 1000) / 1000.0F;
                }
            }

            return 0.0F;
        }
    }

    @Override
    public void setHealth(int Health) {
        if (this.metalHealth > 0) {
            this.metalHealth = PZMath.clamp(Health, 0, 5000);
        }

        if (this.metalBarHealth > 0) {
            this.metalBarHealth = PZMath.clamp(Health, 0, 3000);
        }

        for (int i = 3; i >= 0; i--) {
            if (this.plankHealth[i] > 0) {
                this.plankHealth[i] = PZMath.clamp(Health, 0, 1000);
            }
        }
    }

    @Override
    public int getHealth() {
        if (this.metalHealth > 0) {
            return this.metalHealth;
        } else if (this.metalBarHealth > 0) {
            return this.metalBarHealth;
        } else {
            int totalHealth = 0;
            int numPlanks = 0;

            for (int i = 3; i >= 0; i--) {
                if (this.plankHealth[i] > 0) {
                    numPlanks++;
                    totalHealth += this.plankHealth[i];
                }
            }

            return totalHealth > 0 && numPlanks > 0 ? totalHealth / numPlanks : 0;
        }
    }

    @Override
    public int getMaxHealth() {
        if (this.metalHealth > 0) {
            return 5000;
        } else if (this.metalBarHealth > 0) {
            return 3000;
        } else {
            int totalHealth = 0;
            int numPlanks = 0;

            for (int i = 3; i >= 0; i--) {
                if (this.plankHealth[i] > 0) {
                    numPlanks++;
                    totalHealth += 1000;
                }
            }

            return totalHealth > 0 && numPlanks > 0 ? totalHealth / numPlanks : 5000;
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        byte dirIndex = input.get();
        this.dir = IsoDirections.fromIndex(dirIndex);
        int numPlanks = input.get();

        for (int i = 0; i < numPlanks; i++) {
            int plankHealth = input.getShort();
            if (i < 4) {
                this.plankHealth[i] = plankHealth;
            }
        }

        this.metalHealth = input.getShort();
        this.metalBarHealth = input.getShort();
        this.chooseSprite();
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        output.put((byte)1);
        output.put(IsoObject.factoryGetClassID(this.getObjectName()));
        output.put((byte)this.dir.index());
        output.put((byte)4);

        for (int i = 0; i < 4; i++) {
            output.putShort((short)this.plankHealth[i]);
        }

        output.putShort((short)this.metalHealth);
        output.putShort((short)this.metalBarHealth);
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("state".equals(change)) {
            for (int i = 0; i < 4; i++) {
                bb.putShort((short)this.plankHealth[i]);
            }

            bb.putShort((short)this.metalHealth);
            bb.putShort((short)this.metalBarHealth);
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        if ("state".equals(change)) {
            for (int i = 0; i < 4; i++) {
                this.plankHealth[i] = bb.getShort();
            }

            this.metalHealth = bb.getShort();
            this.metalBarHealth = bb.getShort();
            this.updateSprite();
            recalculateLighting();
        }
    }

    public BarricadeAble getBarricadedObject() {
        int index = this.getObjectIndex();
        if (index == -1) {
            return null;
        } else {
            PZArrayList<IsoObject> specials = this.getSquare().getObjects();
            if (this.getDir() != IsoDirections.W && this.getDir() != IsoDirections.N) {
                if (this.getDir() == IsoDirections.E || this.getDir() == IsoDirections.S) {
                    boolean north = this.getDir() == IsoDirections.S;
                    int x = this.getSquare().getX() + (this.getDir() == IsoDirections.E ? 1 : 0);
                    int y = this.getSquare().getY() + (this.getDir() == IsoDirections.S ? 1 : 0);
                    IsoGridSquare sq = this.getCell().getGridSquare((double)x, (double)y, (double)this.getZ());
                    if (sq != null) {
                        specials = sq.getObjects();

                        for (int i = specials.size() - 1; i >= 0; i--) {
                            IsoObject obj = specials.get(i);
                            if (obj instanceof BarricadeAble barricadeAble && north == barricadeAble.getNorth()) {
                                return barricadeAble;
                            }
                        }
                    }
                }
            } else {
                boolean north = this.getDir() == IsoDirections.N;

                for (int ix = index - 1; ix >= 0; ix--) {
                    IsoObject obj = specials.get(ix);
                    if (obj instanceof BarricadeAble barricadeAble && north == barricadeAble.getNorth()) {
                        return barricadeAble;
                    }
                }
            }

            return null;
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        BarricadeAble barricadeAble = this.getBarricadedObject();
        if (barricadeAble != null && this.square.lighting[playerIndex].targetDarkMulti() <= barricadeAble.getSquare().lighting[playerIndex].targetDarkMulti()) {
            col = barricadeAble.getSquare().lighting[playerIndex].lightInfo();
            this.setTargetAlpha(
                playerIndex,
                FBORenderCell.instance
                    .calculateWindowTargetAlpha(playerIndex, (IsoObject)barricadeAble, barricadeAble.getOppositeSquare(), barricadeAble.getNorth())
            );
        }

        super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
    }

    public static IsoBarricade GetBarricadeOnSquare(IsoGridSquare square, IsoDirections dir) {
        if (square == null) {
            return null;
        } else {
            for (int i = 0; i < square.getSpecialObjects().size(); i++) {
                IsoObject obj = square.getSpecialObjects().get(i);
                if (obj instanceof IsoBarricade barricade && barricade.getDir() == dir) {
                    return barricade;
                }
            }

            return null;
        }
    }

    public static IsoBarricade GetBarricadeForCharacter(BarricadeAble obj, IsoGameCharacter chr) {
        if (obj != null && obj.getSquare() != null) {
            if (chr != null) {
                if (obj.getNorth()) {
                    if (chr.getY() < obj.getSquare().getY()) {
                        return GetBarricadeOnSquare(obj.getOppositeSquare(), obj.getNorth() ? IsoDirections.S : IsoDirections.E);
                    }
                } else if (chr.getX() < obj.getSquare().getX()) {
                    return GetBarricadeOnSquare(obj.getOppositeSquare(), obj.getNorth() ? IsoDirections.S : IsoDirections.E);
                }
            }

            return GetBarricadeOnSquare(obj.getSquare(), obj.getNorth() ? IsoDirections.N : IsoDirections.W);
        } else {
            return null;
        }
    }

    public static IsoBarricade GetBarricadeOppositeCharacter(BarricadeAble obj, IsoGameCharacter chr) {
        if (obj != null && obj.getSquare() != null) {
            if (chr != null) {
                if (obj.getNorth()) {
                    if (chr.getY() < obj.getSquare().getY()) {
                        return GetBarricadeOnSquare(obj.getSquare(), obj.getNorth() ? IsoDirections.N : IsoDirections.W);
                    }
                } else if (chr.getX() < obj.getSquare().getX()) {
                    return GetBarricadeOnSquare(obj.getSquare(), obj.getNorth() ? IsoDirections.N : IsoDirections.W);
                }
            }

            return GetBarricadeOnSquare(obj.getOppositeSquare(), obj.getNorth() ? IsoDirections.S : IsoDirections.E);
        } else {
            return null;
        }
    }

    public static IsoBarricade AddBarricadeToObject(BarricadeAble to, boolean addOpposite) {
        IsoGridSquare square = addOpposite ? to.getOppositeSquare() : to.getSquare();
        IsoDirections dir = null;
        if (to.getNorth()) {
            dir = addOpposite ? IsoDirections.S : IsoDirections.N;
        } else {
            dir = addOpposite ? IsoDirections.E : IsoDirections.W;
        }

        if (square != null && dir != null) {
            IsoBarricade barricade = GetBarricadeOnSquare(square, dir);
            if (barricade != null) {
                return barricade;
            } else {
                barricade = new IsoBarricade(square, dir);
                int index = -1;

                for (int i = 0; i < square.getObjects().size(); i++) {
                    IsoObject obj = square.getObjects().get(i);
                    if (obj instanceof IsoCurtain curtain) {
                        if (curtain.getType() == IsoObjectType.curtainW && dir == IsoDirections.W) {
                            index = i;
                        } else if (curtain.getType() == IsoObjectType.curtainN && dir == IsoDirections.N) {
                            index = i;
                        } else if (curtain.getType() == IsoObjectType.curtainE && dir == IsoDirections.E) {
                            index = i;
                        } else if (curtain.getType() == IsoObjectType.curtainS && dir == IsoDirections.S) {
                            index = i;
                        }

                        if (index != -1) {
                            break;
                        }
                    }
                }

                square.AddSpecialObject(barricade, index);

                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                IsoGridSquare.setRecalcLightTime(-1.0F);
                GameTime.instance.lightSourceUpdate = 100.0F;
                return barricade;
            }
        } else {
            return null;
        }
    }

    public static IsoBarricade AddBarricadeToObject(BarricadeAble to, IsoGameCharacter chr) {
        if (to == null || to.getSquare() == null || chr == null) {
            return null;
        } else if (to.getNorth()) {
            boolean addOpposite = chr.getY() < to.getSquare().getY();
            return AddBarricadeToObject(to, addOpposite);
        } else {
            boolean addOpposite = chr.getX() < to.getSquare().getX();
            return AddBarricadeToObject(to, addOpposite);
        }
    }

    public boolean canAttackBypassIsoBarricade(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon) {
        if (handWeapon == null) {
            return false;
        } else if (this.isDestroyed()) {
            return true;
        } else {
            return handWeapon.isAimedFirearm() ? !this.isBlockVision() : handWeapon.canAttackPierceTransparentWall(isoGameCharacter, handWeapon);
        }
    }

    public static void barricadeCurrentCellWithMetalPlate() {
        ArrayList<IsoWindow> isoWindowList = IsoWorld.instance.currentCell.getWindowList();

        for (IsoWindow isoWindow : isoWindowList.toArray(new IsoWindow[0])) {
            isoWindow.addBarricadesDebug(0, true);
            IsoBarricade isoBarricade = isoWindow.getBarricadeOnSameSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(0);
                isoBarricade.metalHealth = 5000;
                isoBarricade.updateSprite();
            }

            isoBarricade = isoWindow.getBarricadeOnOppositeSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(0);
                isoBarricade.metalHealth = 5000;
                isoBarricade.updateSprite();
            }
        }

        recalculateLighting();
    }

    public static void barricadeCurrentCellWithMetalBars() {
        ArrayList<IsoWindow> isoWindowList = IsoWorld.instance.currentCell.getWindowList();

        for (IsoWindow isoWindow : isoWindowList.toArray(new IsoWindow[0])) {
            isoWindow.addBarricadesDebug(0, true);
            IsoBarricade isoBarricade = isoWindow.getBarricadeOnSameSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(0);
                isoBarricade.metalBarHealth = 3000;
                isoBarricade.updateSprite();
            }

            isoBarricade = isoWindow.getBarricadeOnOppositeSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(0);
                isoBarricade.metalBarHealth = 3000;
                isoBarricade.updateSprite();
            }
        }

        recalculateLighting();
    }

    public static void barricadeCurrentCellWithPlanks(int numberOfPlanks) {
        ArrayList<IsoWindow> isoWindowList = IsoWorld.instance.currentCell.getWindowList();

        for (IsoWindow isoWindow : isoWindowList.toArray(new IsoWindow[0])) {
            isoWindow.addBarricadesDebug(numberOfPlanks, false);
            IsoBarricade isoBarricade = isoWindow.getBarricadeOnSameSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(numberOfPlanks);
                isoBarricade.updateSprite();
            }

            isoBarricade = isoWindow.getBarricadeOnOppositeSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(numberOfPlanks);
                isoBarricade.updateSprite();
            }
        }

        recalculateLighting();
    }

    private void setNumberOfPlanks(int numberOfPlanks) {
        this.metalBarHealth = 0;
        this.metalHealth = 0;

        for (int i = 0; i < 4; i++) {
            this.plankHealth[i] = i < numberOfPlanks ? 1000 : 0;
        }
    }

    private static void recalculateLighting() {
        if (!GameServer.server) {
            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                LosUtil.cachecleared[pn] = true;
            }

            IsoGridSquare.setRecalcLightTime(-1.0F);
            GameTime.instance.lightSourceUpdate = 100.0F;
        }
    }

    private void updateSprite() {
        this.chooseSprite();
        if (this.square != null) {
            this.square.RecalcProperties();
        }

        this.invalidateRenderChunkLevel(256L);
    }

    public void addFromCraftRecipe(IsoGameCharacter chr, ArrayList<InventoryItem> items) {
        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            if (item.getFullType().equals("Base.Plank")) {
                this.addPlank(chr, item);
            } else if (item.getFullType().equals("Base.MetalBar") || item.getFullType().equals("Base.IronBar") || item.getFullType().equals("Base.SteelBar")) {
                this.addMetalBar(chr, item);
            } else if (item.getFullType().equals("Base.SheetMetal")) {
                this.addMetal(chr, item);
            }
        }
    }

    public float getLightTransmission() {
        if (this.isMetal()) {
            return 0.0F;
        } else {
            return this.isMetalBar() ? 1.0F : 1.0F - this.getNumPlanks() * 0.15F;
        }
    }
}
