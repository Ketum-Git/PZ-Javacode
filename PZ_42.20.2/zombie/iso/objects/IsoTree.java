// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.GL20;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.inventory.types.HandWeapon;
import zombie.iso.CellLoader;
import zombie.iso.IHasHealth;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoTreeJumbo;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderTrees;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.WeaponCategory;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class IsoTree extends IsoObject implements IHasHealth {
    private static final IsoGameCharacter.Location[] s_chopTreeLocation = new IsoGameCharacter.Location[4];
    private static final ArrayList<IsoGridSquare> s_chopTreeIndicators = new ArrayList<>();
    private static IsoTree chopTreeHighlighted;
    public float fadeAlpha = 1.0F;
    public static final int SIZE_JUMBO = 5;
    public static final int SIZE_JUMBO_L = 6;
    public static final int SIZE_JUMBO_XL = 7;
    public static final int SIZE_JUMBO_XXL = 8;
    public static final int MAX_SIZE = 8;
    private int logYield = 1;
    private int damage = 500;
    public int size = 4;
    public boolean renderFlag;
    public boolean wasFaded;
    public boolean useTreeShader;
    private float cutawayAlpha;
    private static final int[] LOGS_PER_SIZE = new int[]{1, 1, 2, 3, 4, 5, 6, 8};

    public static IsoTree getNew() {
        synchronized (CellLoader.isoTreeCache) {
            IsoTree o = CellLoader.isoTreeCache.pop();
            if (o == null) {
                return new IsoTree();
            } else {
                o.sx = 0.0F;
                return o;
            }
        }
    }

    public IsoTree() {
    }

    public IsoTree(IsoCell cell) {
        super(cell);
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        IsoSprite originalSprite = this.sprite;
        super.save(output, isDebugSave);
        output.put((byte)this.logYield);
        output.put((byte)(this.damage / 10));
        this.sprite = originalSprite;
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.logYield = input.get();
        this.damage = input.get() * 10;
        if (this.sprite != null && this.sprite.getProperties().get("tree") != null) {
            this.size = Integer.parseInt(this.sprite.getProperties().get("tree"));
            if (this.size < 1) {
                this.size = 1;
            }

            if (this.size > 8) {
                this.size = 8;
            }
        }
    }

    @Override
    protected void checkMoveWithWind() {
        this.checkMoveWithWind(true);
    }

    public IsoTree(IsoGridSquare sq, String gid) {
        super(sq, gid, false);
        this.initTree();
    }

    public IsoTree(IsoGridSquare sq, IsoSprite gid) {
        super(sq.getCell(), sq, gid);
        this.initTree();
    }

    public void initTree() {
        this.setType(IsoObjectType.tree);
        if (this.sprite.getProperties().get("tree") != null) {
            this.size = Integer.parseInt(this.sprite.getProperties().get("tree"));
            if (this.size < 1) {
                this.size = 1;
            }

            if (this.size > 8) {
                this.size = 8;
            }
        } else {
            this.size = 4;
        }

        this.logYield = LOGS_PER_SIZE[this.size - 1];
        this.damage = (this.logYield - 1) * 80;
        this.damage = Math.max(this.damage, 40);
    }

    @Override
    public String getObjectName() {
        return "Tree";
    }

    @Override
    public void Damage(float amount) {
        float dmg = amount * 0.05F;
        this.damage = (int)(this.damage - dmg);
        if (this.damage <= 0) {
            this.toppleTree();
        }
    }

    @Override
    public void HitByVehicle(BaseVehicle vehicle, float amount) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(this.square.x + 0.5F, this.square.y + 0.5F, this.square.z);
        long soundRef = emitter.playSound(SoundKey.VEHICLE_HIT_TREE.getSoundName());
        emitter.setParameterValueByName(soundRef, "VehicleSpeed", vehicle.getCurrentSpeedKmHour());
        WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
        this.Damage(this.damage);
    }

    public void WeaponHitEffects(IsoGameCharacter owner, HandWeapon weapon) {
        if (owner instanceof IsoPlayer isoPlayer) {
            isoPlayer.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Tree);
            if (weapon != null) {
                owner.getEmitter().playSound(weapon.getZombieHitSound());
            }
        } else {
            owner.getEmitter().playSound("ChopTree");
        }

        WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 4.0F, 15.0F);
        this.setRenderEffect(RenderEffectType.Hit_Tree_Shudder, true);
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        if (!GameClient.client && weapon != null) {
            int skill = owner.getWeaponLevel(weapon);
            weapon.checkSyncItemFields(weapon.damageCheck(skill, weapon.hasTag(ItemTag.CHOP_TREE) ? 2.0F : 1.0F, true, true, owner));
        }

        if (!GameServer.server) {
            this.WeaponHitEffects(owner, weapon);
        }

        float dmg = weapon.getTreeDamage();
        if (owner.hasTrait(CharacterTrait.AXEMAN) && weapon.isOfWeaponCategory(WeaponCategory.AXE)) {
            dmg *= 1.5F;
        }

        this.damage = (int)(this.damage - dmg);
        if (this.damage <= 0) {
            this.toppleTree(owner);
        }
    }

    @Override
    public void setHealth(int health) {
        this.damage = Math.max(health, 0);
    }

    @Override
    public int getHealth() {
        return this.damage;
    }

    @Override
    public int getMaxHealth() {
        int maxHealth = (this.logYield - 1) * 80;
        return Math.max(maxHealth, 40);
    }

    public int getSize() {
        return this.size;
    }

    public float getSlowFactor(IsoMovingObject chr) {
        float mod = 1.0F;
        if (chr instanceof IsoGameCharacter isoGameCharacter) {
            if (isoGameCharacter.getDescriptor().isCharacterProfession(CharacterProfession.PARK_RANGER)) {
                mod = 1.5F;
            }

            if (isoGameCharacter.getDescriptor().isCharacterProfession(CharacterProfession.LUMBERJACK)) {
                mod = 1.2F;
            }
        }

        if (this.size == 1 || this.size == 2) {
            return 0.8F * mod;
        } else {
            return this.size != 3 && this.size != 4 ? mod : 0.5F * mod;
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.isHighlighted()) {
            if (this.square != null) {
                chopTreeHighlighted = this;
            }
        } else {
            int playerIndex = IsoCamera.frameState.playerIndex;
            boolean mainTreeCanSee = this.square.lighting[playerIndex].bCanSee();
            int cutaway = mainTreeCanSee ? 1 : 0;
            cutaway += this.countObscuredSeenSquaresOriginal(playerIndex, false);
            boolean transparent = this.sprite.name != null
                && this.sprite.name.contains("XL")
                && (mainTreeCanSee || cutaway >= 4)
                && IsoPlayer.getPlayer(playerIndex).isAnyAimKeyDown();
            this.render(x, y, z, col, bDoAttached, playerIndex, transparent);
            if (!PerformanceSettings.fboRenderChunk) {
                this.checkChopTreeIndicator();
            }

            if (DebugOptions.instance.renderTreeSeen.getValue()) {
                LineDrawer.addRect(x + 0.1F, y + 0.1F, z, 0.8F, 0.8F, 0.0F, this.square.lighting[playerIndex].bCanSee() ? 1.0F : 0.0F, 0.0F);
                this.countObscuredSeenSquaresOriginal(playerIndex, true);
            }
        }
    }

    private int countObscuredSeenSquaresOriginal(int playerIndex, boolean debugRender) {
        int count = 0;

        for (IsoDirections d : IsoDirections.values()) {
            IsoGridSquare square1 = this.square.getAdjacentSquare(d);
            if (square1 != null) {
                if (debugRender) {
                    LineDrawer.addRect(
                        square1.x + 0.1F, square1.y + 0.1F, square1.z, 0.8F, 0.8F, 0.0F, 0.0F, square1.lighting[playerIndex].bCanSee() ? 1.0F : 0.0F
                    );
                } else if (square1.lighting[playerIndex].bCanSee()) {
                    count++;
                }
            }
        }

        return count;
    }

    private int countObscuredSeenSquares(int playerIndex, int stopCount, boolean debugRender) {
        int HGT = 5;
        if (this.size == 5 || this.size == 6) {
            HGT = 8;
        } else if (this.size == 7) {
            HGT = 12;
        } else if (this.size == 8) {
            HGT = 16;
        }

        int count = 0;
        count = this.countObscuredSeenSquares(playerIndex, stopCount, debugRender, count, this.square.x, this.square.y, this.square.z, HGT - 1);
        count = this.countObscuredSeenSquares(playerIndex, stopCount, debugRender, count, this.square.x - 1, this.square.y, this.square.z, HGT - 2);
        return this.countObscuredSeenSquares(playerIndex, stopCount, debugRender, count, this.square.x, this.square.y - 1, this.square.z, HGT - 2);
    }

    private int countObscuredSeenSquares(int playerIndex, int stopCount, boolean debugRender, int count, int x1, int y1, int z1, int num) {
        if (stopCount != -1 && count >= stopCount) {
            return count;
        } else {
            for (int dxy = 1; dxy >= -num; dxy--) {
                IsoGridSquare square1 = this.getCell().getGridSquare(x1 + dxy, y1 + dxy, z1);
                if (square1 != this.square && square1 != null) {
                    if (debugRender) {
                        LineDrawer.addRect(
                            square1.x + 0.1F, square1.y + 0.1F, square1.z, 0.8F, 0.8F, 0.0F, 0.0F, square1.lighting[playerIndex].bCanSee() ? 1.0F : 0.0F
                        );
                    } else if (square1.lighting[playerIndex].bCanSee()) {
                        if (stopCount != -1 && ++count >= stopCount) {
                            return count;
                        }
                    }
                }
            }

            return count;
        }
    }

    private void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, int playerIndex, boolean transparent) {
        boolean bUseStencil = (this.renderFlag || this.fadeAlpha < this.getTargetAlpha(playerIndex))
            && (!PerformanceSettings.fboRenderChunk || FBORenderCell.instance.renderTranslucentOnly);
        if (PerformanceSettings.fboRenderChunk && FBORenderTrees.current != null) {
            FBORenderTrees renderTrees = FBORenderTrees.current;
            float alphaStep = 0.044999998F * GameTime.getInstance().getThirtyFPSMultiplier();
            boolean bPlayerInside = IsoCamera.frameState.camCharacterSquare != null && !IsoCamera.frameState.camCharacterSquare.has(IsoFlagType.exterior);
            float minAlpha = DebugOptions.instance.terrain.renderTiles.forceFullAlpha.getValue() ? 1.0F : (bPlayerInside ? 0.05F : 0.15F);
            if (bUseStencil) {
                if (this.renderFlag && this.fadeAlpha > minAlpha) {
                    this.fadeAlpha -= alphaStep;
                    if (this.fadeAlpha < minAlpha) {
                        this.fadeAlpha = minAlpha;
                    }
                }

                if (!this.renderFlag) {
                    float maxAlpha = this.getTargetAlpha(playerIndex);
                    if (this.fadeAlpha < maxAlpha) {
                        this.fadeAlpha += alphaStep;
                        if (this.fadeAlpha > maxAlpha) {
                            this.fadeAlpha = maxAlpha;
                        }
                    }
                }
            }

            if (transparent) {
                this.cutawayAlpha -= alphaStep;
                if (this.cutawayAlpha < 0.0F) {
                    this.cutawayAlpha = 0.0F;
                }
            } else {
                float maxAlpha = this.getTargetAlpha(playerIndex);
                this.cutawayAlpha += alphaStep;
                if (this.cutawayAlpha > maxAlpha) {
                    this.cutawayAlpha = maxAlpha;
                }
            }

            if (transparent && this.cutawayAlpha != 1.0F && IsoTreeJumbo.Jumbos.get(this.sprite.name) != null) {
                IsoTreeJumbo.TreeDescription treeDescription = IsoTreeJumbo.Jumbos.get(this.sprite.name);
                IsoTreeJumbo.TreeDescription attachedDescription = this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty()
                    ? IsoTreeJumbo.Jumbos.get(this.attachedAnimSprite.get(0).parentSprite.name)
                    : null;
                Texture trunk = IsoSpriteManager.instance.getSprite(treeDescription.trunk()).getTextureForCurrentFrame(this.getDir(), this);
                Texture treetop = IsoSpriteManager.instance.getSprite(treeDescription.treetop()).getTextureForCurrentFrame(this.getDir(), this);
                Texture trunk2 = attachedDescription == null
                    ? null
                    : IsoSpriteManager.instance.getSprite(attachedDescription.trunk()).getTextureForCurrentFrame(this.getDir(), this);
                Texture treetop2 = attachedDescription == null
                    ? null
                    : IsoSpriteManager.instance.getSprite(attachedDescription.treetop()).getTextureForCurrentFrame(this.getDir(), this);
                renderTrees.addTree(
                    trunk, trunk2, x, y, z, col.r, col.g, col.b, col.a, this.getObjectRenderEffectsToApply(), bUseStencil, this.fadeAlpha, false, 1.0F
                );
                renderTrees.addTree(
                    treetop,
                    treetop2,
                    x,
                    y,
                    z,
                    col.r,
                    col.g,
                    col.b,
                    col.a,
                    this.getObjectRenderEffectsToApply(),
                    bUseStencil,
                    this.fadeAlpha,
                    true,
                    this.cutawayAlpha
                );
            } else {
                Texture baseTexture = this.getSprite().getTextureForCurrentFrame(this.getDir(), this);
                Texture foliageTexture = null;
                if (this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty()) {
                    IsoSpriteInstance spriteInstance = this.attachedAnimSprite.get(0);
                    foliageTexture = spriteInstance.parentSprite.getTextureForCurrentFrame(this.getDir(), this);
                }

                renderTrees.addTree(
                    baseTexture,
                    foliageTexture,
                    x,
                    y,
                    z,
                    col.r,
                    col.g,
                    col.b,
                    col.a,
                    this.getObjectRenderEffectsToApply(),
                    bUseStencil,
                    this.fadeAlpha,
                    this.cutawayAlpha != 1.0F,
                    this.cutawayAlpha
                );
            }
        } else if (bUseStencil) {
            IndieGL.enableStencilTest();
            IndieGL.glBlendFunc(770, 771);
            boolean bPlayerInsidex = IsoCamera.frameState.camCharacterSquare != null && !IsoCamera.frameState.camCharacterSquare.has(IsoFlagType.exterior);
            IndieGL.glStencilFunc(517, 128, 128);
            this.renderInner(x, y, z, col, bDoAttached, false);
            float alphaStepx = 0.044999998F * GameTime.getInstance().getThirtyFPSMultiplier();
            float minAlphax = bPlayerInsidex ? 0.05F : 0.25F;
            if (this.renderFlag && this.fadeAlpha > minAlphax) {
                this.fadeAlpha -= alphaStepx;
                if (this.fadeAlpha < minAlphax) {
                    this.fadeAlpha = minAlphax;
                }
            }

            if (!this.renderFlag) {
                float maxAlpha = this.getTargetAlpha(playerIndex);
                if (this.fadeAlpha < maxAlpha) {
                    this.fadeAlpha += alphaStepx;
                    if (this.fadeAlpha > maxAlpha) {
                        this.fadeAlpha = maxAlpha;
                    }
                }
            }

            float a = this.getAlpha(playerIndex);
            float ta = this.getTargetAlpha(playerIndex);
            this.setAlphaAndTarget(playerIndex, this.fadeAlpha);
            IndieGL.glStencilFunc(514, 128, 128);
            this.renderInner(x, y, z, col, true, false);
            this.setAlpha(playerIndex, a);
            this.setTargetAlpha(playerIndex, ta);
            if (IsoTree.TreeShader.instance.StartShader()) {
                IsoTree.TreeShader.instance.setOutlineColor(0.1F, 0.1F, 0.1F, bPlayerInsidex && this.fadeAlpha < 0.5F ? this.fadeAlpha : 1.0F - this.fadeAlpha);
                this.renderInner(x, y, z, col, true, true);
                IndieGL.EndShader();
            }

            IndieGL.glStencilFunc(519, 255, 255);
            IndieGL.glDefaultBlendFunc();
        } else {
            this.renderInner(x, y, z, col, bDoAttached, false);
        }
    }

    private void renderInner(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bShader) {
        float loffsetX = this.offsetX;
        float loffsetY = this.offsetY;
        this.offsetX = FBORenderChunk.FLOOR_WIDTH / 2.0F;
        this.offsetY = FBORenderChunk.PIXELS_PER_LEVEL;
        if (this.sprite != null && this.sprite.name != null) {
            if (this.sprite.name.contains("JUMBOXXL")) {
                this.offsetX = FBORenderChunk.JUMBO_XXL_WIDTH / 2.0F;
                this.offsetY = FBORenderChunk.JUMBO_XXL_HEIGHT - FBORenderChunk.FLOOR_HEIGHT;
            } else if (this.sprite.name.contains("JUMBOXL")) {
                this.offsetX = FBORenderChunk.JUMBO_XL_WIDTH / 2.0F;
                this.offsetY = FBORenderChunk.JUMBO_XL_HEIGHT - FBORenderChunk.FLOOR_HEIGHT;
            } else if (this.sprite.name.contains("JUMBO")) {
                this.offsetX = FBORenderChunk.JUMBO_L_WIDTH / 2.0F;
                this.offsetY = FBORenderChunk.JUMBO_L_HEIGHT - FBORenderChunk.FLOOR_HEIGHT;
            }
        }

        if (this.offsetX != loffsetX || this.offsetY != loffsetY) {
            this.sx = 0.0F;
        }

        if (bShader && this.sprite != null) {
            Texture texture = this.sprite.getTextureForCurrentFrame(this.getForwardIsoDirection(), this);
            if (texture != null) {
                IsoTree.TreeShader.instance.setStepSize(0.25F, texture.getWidth(), texture.getHeight());
            }
        }

        boolean wasRenderFlag = this.renderFlag;
        if (!bShader) {
            this.renderFlag = false;
        }

        this.useTreeShader = bShader;
        super.render(x, y, z, col, false, false, null);
        if (this.attachedAnimSprite != null) {
            int n = this.attachedAnimSprite.size();

            for (int i = 0; i < n; i++) {
                IsoSpriteInstance s = this.attachedAnimSprite.get(i);
                int playerIndex = IsoCamera.frameState.playerIndex;
                float fa = this.getTargetAlpha(playerIndex);
                this.setTargetAlpha(playerIndex, 1.0F);
                s.render(
                    this,
                    x,
                    y,
                    z,
                    this.getForwardIsoDirection(),
                    this.offsetX,
                    this.offsetY,
                    this.isHighlighted(playerIndex) ? this.getHighlightColor(playerIndex) : col
                );
                this.setTargetAlpha(playerIndex, fa);
                s.update();
            }
        }

        this.renderFlag = wasRenderFlag;
    }

    @Override
    protected boolean isUpdateAlphaDuringRender() {
        return false;
    }

    /**
     * 
     * @param sprite the sprite to set
     */
    @Override
    public void setSprite(IsoSprite sprite) {
        super.setSprite(sprite);
        this.initTree();
    }

    @Override
    public boolean isMaskClicked(int x, int y, boolean flip) {
        if (super.isMaskClicked(x, y, flip)) {
            return true;
        } else if (this.attachedAnimSprite == null) {
            return false;
        } else {
            for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                if (this.attachedAnimSprite.get(i).parentSprite.isMaskClicked(this.getForwardIsoDirection(), x, y, flip)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void setChopTreeCursorLocation(int playerIndex, int x, int y, int z) {
        if (s_chopTreeLocation[playerIndex] == null) {
            s_chopTreeLocation[playerIndex] = new IsoGameCharacter.Location(-1, -1, -1);
        }

        IsoGameCharacter.Location location = s_chopTreeLocation[playerIndex];
        location.x = x;
        location.y = y;
        location.z = z;
    }

    public void checkChopTreeIndicator() {
        if (!this.isHighlighted()) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoGameCharacter.Location location = s_chopTreeLocation[playerIndex];
            if (location != null && location.x != -1 && this.square != null) {
                if (this.getCell().getDrag(playerIndex) == null) {
                    location.x = -1;
                } else {
                    if (IsoUtils.DistanceToSquared(this.square.x + 0.5F, this.square.y + 0.5F, location.x + 0.5F, location.y + 0.5F) < 12.25F) {
                        s_chopTreeIndicators.add(this.square);
                    }
                }
            }
        }
    }

    public static void checkChopTreeIndicators(int playerIndex) {
        IsoGameCharacter.Location location = s_chopTreeLocation[playerIndex];
        if (location != null && location.x != -1) {
            if (IsoWorld.instance.currentCell.getDrag(playerIndex) == null) {
                location.x = -1;
            } else {
                int chunkMinX = PZMath.fastfloor((location.x - 4.0F) / 8.0F) - 1;
                int chunkMinY = PZMath.fastfloor((location.y - 4.0F) / 8.0F) - 1;
                int chunkMaxX = (int)Math.ceil((location.x + 4.0F) / 8.0F) + 1;
                int chunkMaxY = (int)Math.ceil((location.y + 4.0F) / 8.0F) + 1;
                IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
                if (!chunkMap.ignore) {
                    for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
                        for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                            IsoChunk chunk = chunkMap.getChunkForGridSquare(cx * 8, cy * 8);
                            if (chunk != null && chunk.loaded && chunk.IsOnScreen(true)) {
                                FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
                                if (renderLevels.isOnScreen(0)) {
                                    ArrayList<IsoGridSquare> trees = renderLevels.treeSquares;

                                    for (int i = 0; i < trees.size(); i++) {
                                        IsoGridSquare square = trees.get(i);
                                        IsoTree tree = square.getTree();
                                        if (tree != null
                                            && !tree.isHighlighted()
                                            && IsoUtils.DistanceToSquared(square.x + 0.5F, square.y + 0.5F, location.x + 0.5F, location.y + 0.5F) < 12.25F) {
                                            s_chopTreeIndicators.add(square);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void renderChopTreeIndicators() {
        if (!s_chopTreeIndicators.isEmpty()) {
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.disableDepthTest();
            }

            PZArrayUtil.forEach(s_chopTreeIndicators, IsoTree::renderChopTreeIndicator);
            s_chopTreeIndicators.clear();
        }

        if (chopTreeHighlighted != null) {
            IsoTree tree = chopTreeHighlighted;
            chopTreeHighlighted = null;
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderObjectHighlight.getInstance().setRenderingGhostTile(true);
            }

            int playerIndex = IsoCamera.frameState.playerIndex;
            tree.renderInner(tree.square.x, tree.square.y, tree.square.z, tree.getHighlightColor(playerIndex), false, false);
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderObjectHighlight.getInstance().setRenderingGhostTile(false);
            }
        }
    }

    private static void renderChopTreeIndicator(IsoGridSquare square) {
        Texture tex = Texture.getSharedTexture("media/ui/chop_tree.png");
        if (tex != null && tex.isReady()) {
            float x = square.x;
            float y = square.y;
            float z = square.z;
            float sx = IsoUtils.XToScreen(x, y, z, 0) + IsoSprite.globalOffsetX;
            float sy = IsoUtils.YToScreen(x, y, z, 0) + IsoSprite.globalOffsetY;
            sx -= 32 * Core.tileScale;
            sy -= 96 * Core.tileScale;
            IndieGL.StartShader(0);
            SpriteRenderer.instance.render(tex, sx, sy, 64 * Core.tileScale, 128 * Core.tileScale, 0.0F, 0.5F, 0.0F, 0.75F, null);
        }
    }

    @Override
    public IsoGridSquare getRenderSquare() {
        if (this.getSquare() == null) {
            return null;
        } else {
            Texture texture = this.getSprite().getTextureForCurrentFrame(this.getDir(), this);
            if (texture != null && texture.getName() != null && texture.getName().contains("JUMBO")) {
                int chunksPerWidth = 8;
                if (PZMath.coordmodulo(this.square.x, 8) == 0 && PZMath.coordmodulo(this.square.y, 8) == 7) {
                    return this.square.getAdjacentSquare(IsoDirections.S);
                }

                if (PZMath.coordmodulo(this.square.x, 8) == 7 && PZMath.coordmodulo(this.square.y, 8) == 0) {
                    return this.square.getAdjacentSquare(IsoDirections.E);
                }
            }

            return this.getSquare();
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.fadeAlpha = 1.0F;
        this.renderFlag = false;
        this.wasFaded = false;
        this.useTreeShader = false;
        this.cutawayAlpha = 1.0F;
    }

    public void dropWood() {
        String name = this.getSprite().getName();
        boolean acorn = name != null
            && (
                name.toLowerCase().contains("oak")
                    || name.equals("vegetation_trees_01_13")
                    || name.equals("vegetation_trees_01_14")
                    || name.equals("vegetation_trees_01_15")
            );
        boolean pinecone = name != null
            && (
                name.toLowerCase().contains("pine")
                    || name.equals("vegetation_trees_01_08")
                    || name.equals("vegetation_trees_01_09")
                    || name.equals("vegetation_trees_01_010")
                    || name.equals("vegetation_trees_01_011")
            );
        boolean holly = name != null
            && name.toLowerCase().contains("holly")
            && ("Autumn".equals(ClimateManager.getInstance().getSeasonName()) || "Winter".equals(ClimateManager.getInstance().getSeasonName()));
        int numPlanks = this.logYield;
        int roll = Math.min(6 - this.logYield, 4);
        if (numPlanks == 1) {
            this.square.AddWorldInventoryItem(ItemKey.Weapon.TREE_BRANCH_2, 0.0F, 0.0F, 0.0F);
        }

        if (numPlanks == 2) {
            this.square.AddWorldInventoryItem(ItemKey.Weapon.SAPLING, 0.0F, 0.0F, 0.0F);
            this.square.AddWorldInventoryItem(ItemKey.Normal.LOG, 0.0F, 0.0F, 0.0F);
        }

        if (numPlanks > 2) {
            for (int i = 0; i < numPlanks - 1; i++) {
                this.square.AddWorldInventoryItem(ItemKey.Normal.LOG, 0.0F, 0.0F, 0.0F);
                if (i > 2 && Rand.NextBool(roll)) {
                    this.square.AddWorldInventoryItem(ItemKey.Weapon.LARGE_BRANCH, 0.0F, 0.0F, 0.0F);
                }

                if (i > 2 && Rand.NextBool(roll)) {
                    this.square.AddWorldInventoryItem(ItemKey.Weapon.SAPLING, 0.0F, 0.0F, 0.0F);
                }

                if (acorn && Rand.NextBool(roll * 2)) {
                    this.square.AddWorldInventoryItem(ItemKey.Food.ACORN, 0.0F, 0.0F, 0.0F, false);
                }

                if (pinecone && Rand.NextBool(roll)) {
                    this.square.AddWorldInventoryItem(ItemKey.Normal.PINECONE, 0.0F, 0.0F, 0.0F);
                }

                if (holly && Rand.NextBool(roll * 2)) {
                    this.square.AddWorldInventoryItem(ItemKey.Food.HOLLY_BERRY, 0.0F, 0.0F, 0.0F, false);
                }
            }
        }

        for (int i = 0; i < numPlanks; i++) {
            if (Rand.NextBool(roll)) {
                this.square.AddWorldInventoryItem(ItemKey.Weapon.TREE_BRANCH_2, 0.0F, 0.0F, 0.0F);
            }

            if (Rand.NextBool(roll)) {
                this.square.AddWorldInventoryItem(ItemKey.Normal.TWIGS, 0.0F, 0.0F, 0.0F);
            }
        }

        this.square.AddWorldInventoryItem(ItemKey.Normal.SPLINTERS, 0.0F, 0.0F, 0.0F);
    }

    public void toppleTree() {
        this.toppleTree(null);
    }

    public void toppleTree(IsoGameCharacter owner) {
        if (!GameClient.client) {
            this.square.transmitRemoveItemFromSquare(this);
            if (GameServer.server) {
                GameServer.PlayWorldSoundServer("FallingTree", this.square, 70.0F, -1);
            } else if (owner != null) {
                owner.getEmitter().playSound("FallingTree");
            }

            if (!GameServer.server) {
                this.square.RecalcAllWithNeighbours(true);
            }

            this.dropWood();
            String stump = IsoTreeJumbo.Jumbos.getOrDefault(this.sprite.name, IsoTreeJumbo.NULL).stump();
            if (stump != null) {
                CellLoader.DoTileObjectCreation(IsoSpriteManager.instance.getSprite(stump), IsoObjectType.MAX, this.square, this.getCell(), -1, -1, -1, stump);
                IsoObject stumpObject = this.square.getObjects().getLast();
                stumpObject.transmitCompleteItemToClients();
            }

            this.reset();
            CellLoader.isoTreeCache.push(this);
            if (!GameServer.server) {
                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                IsoGridSquare.setRecalcLightTime(-1.0F);
                GameTime.instance.lightSourceUpdate = 100.0F;
                LuaEventManager.triggerEvent("OnContainerUpdate");
            }
        }
    }

    public int getLogYield() {
        return this.logYield;
    }

    public static class TreeShader {
        public static final IsoTree.TreeShader instance = new IsoTree.TreeShader();
        private ShaderProgram shaderProgram;
        private int stepSize;
        private int outlineColor;
        private int chunkDepth;
        private int zDepth;

        public void initShader() {
            this.shaderProgram = ShaderProgram.createShaderProgram("tree", false, false, true);
            if (this.shaderProgram.isCompiled()) {
                this.stepSize = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "stepSize");
                this.outlineColor = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "outlineColor");
                this.chunkDepth = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "chunkDepth");
                this.zDepth = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "zDepth");
                ShaderHelper.glUseProgramObjectARB(this.shaderProgram.getShaderID());
                GL20.glUniform2f(this.stepSize, 0.001F, 0.001F);
                ShaderHelper.glUseProgramObjectARB(0);
            }
        }

        public void setOutlineColor(float r, float g, float b, float a) {
            SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.outlineColor, r, g, b, a);
        }

        public void setStepSize(float stepSize, int texWidth, int texHeight) {
            SpriteRenderer.instance.ShaderUpdate2f(this.shaderProgram.getShaderID(), this.stepSize, stepSize / texWidth, stepSize / texHeight);
        }

        public void setDepth(float chunkDepth, float zDepth) {
            SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.chunkDepth, chunkDepth);
            SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.zDepth, zDepth);
        }

        public boolean StartShader() {
            if (this.shaderProgram == null) {
                RenderThread.invokeOnRenderContext(this::initShader);
            }

            if (this.shaderProgram.isCompiled()) {
                IndieGL.StartShader(this.shaderProgram.getShaderID(), 0);
                return true;
            } else {
                return false;
            }
        }
    }
}
