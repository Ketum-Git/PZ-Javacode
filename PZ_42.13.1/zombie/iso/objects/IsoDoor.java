// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.ThumpState;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.IsoObjectModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Key;
import zombie.iso.BuildingDef;
import zombie.iso.ICurtain;
import zombie.iso.IHasHealth;
import zombie.iso.ILockableDoor;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.fields.NetObject;
import zombie.pathfind.PolygonalMap2;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class IsoDoor extends IsoObject implements BarricadeAble, Thumpable, IHasHealth, ILockableDoor, ICurtain {
    public int health = 500;
    public boolean lockedByKey;
    private boolean haveKey;
    public boolean locked;
    public int maxHealth = 500;
    public int pushedMaxStrength;
    public int pushedStrength;
    public IsoDoor.DoorType type = IsoDoor.DoorType.WeakWooden;
    private IsoSprite closedSprite;
    public boolean north;
    int gid = -1;
    public boolean open;
    private IsoSprite openSprite;
    private boolean destroyed;
    private boolean hasCurtain;
    private boolean curtainInside;
    private boolean curtainOpen;
    private static final ColorInfo curtainColor = new ColorInfo();
    private short lastPlayerOnlineId = -1;
    private boolean wasTryingToggleLockedDoor;
    private boolean wasTryingToggleBarricadedDoor;
    private static final ColorInfo stCol = new ColorInfo();
    KahluaTable table;
    public static final Vector2 tempo = new Vector2();
    private IsoSpriteInstance curtainN;
    private IsoSpriteInstance curtainS;
    private IsoSpriteInstance curtainW;
    private IsoSpriteInstance curtainE;
    private IsoSpriteInstance curtainNopen;
    private IsoSpriteInstance curtainSopen;
    private IsoSpriteInstance curtainWopen;
    private IsoSpriteInstance curtainEopen;
    private static final int[] DoubleDoorNorthSpriteOffset = new int[]{5, 3, 4, 4};
    private static final int[] DoubleDoorWestSpriteOffset = new int[]{4, 4, 5, 3};
    private static final int[] DoubleDoorNorthClosedXOffset = new int[]{0, 1, 2, 3};
    private static final int[] DoubleDoorNorthOpenXOffset = new int[]{0, 0, 3, 3};
    private static final int[] DoubleDoorNorthClosedYOffset = new int[]{0, 0, 0, 0};
    private static final int[] DoubleDoorNorthOpenYOffset = new int[]{0, 1, 1, 0};
    private static final int[] DoubleDoorWestClosedXOffset = new int[]{0, 0, 0, 0};
    private static final int[] DoubleDoorWestOpenXOffset = new int[]{0, 1, 1, 0};
    private static final int[] DoubleDoorWestClosedYOffset = new int[]{0, -1, -2, -3};
    private static final int[] DoubleDoorWestOpenYOffset = new int[]{0, 0, -3, -3};

    public IsoDoor(IsoCell cell) {
        super(cell);
    }

    @Override
    public String getObjectName() {
        return "Door";
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (!this.hasCurtain) {
            super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
        } else {
            this.initCurtainSprites();
            IsoDirections edge = this.getSpriteEdge(false);
            this.prerender(x, y, z, info, bDoAttached, bWallLightingPass, edge);
            super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
            this.postrender(x, y, z, info, bDoAttached, bWallLightingPass, edge);
        }
    }

    @Override
    public void renderWallTile(
        IsoDirections dir,
        float x,
        float y,
        float z,
        ColorInfo col,
        boolean bDoAttached,
        boolean bWallLightingPass,
        Shader shader,
        Consumer<TextureDraw> texdModifier
    ) {
        this.sx = 0.0F;
        int ddIndex = getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            IsoObject master = null;
            if (ddIndex == 2) {
                master = getDoubleDoorObject(this, 1);
            } else if (ddIndex == 3) {
                master = getDoubleDoorObject(this, 4);
            }

            if (master != null && master.getSpriteModel() != null) {
                this.updateRenderInfoForObjectPicker(x, y, z, col);
                this.sx = 0.0F;
                return;
            }
        }

        if (!this.hasCurtain) {
            super.renderWallTile(dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, texdModifier);
        } else {
            this.initCurtainSprites();
            this.initCurtainColor();
            if (PerformanceSettings.fboRenderChunk && this.getSpriteModel() != null) {
                this.renderCurtainModel(
                    this.isCurtainOpen() ? this.curtainNopen : this.curtainN, this.getX() + 0.5F, this.getY() + 0.5F, this.getZ(), curtainColor
                );
                super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
            } else {
                IsoDirections edge = this.getSpriteEdge(false);
                this.prerender(x, y, z, curtainColor, bDoAttached, bWallLightingPass, edge);
                super.renderWallTile(dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, texdModifier);
                this.postrender(x, y, z, curtainColor, bDoAttached, bWallLightingPass, edge);
            }
        }
    }

    private ColorInfo initCurtainColor() {
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            return curtainColor.set(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            IsoGridSquare curtainSq = this.getSheetSquare();
            if (curtainSq == null) {
                return curtainColor.set(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                ColorInfo lightInfo = curtainSq.getLightInfo(IsoCamera.frameState.playerIndex);
                if (lightInfo == null) {
                    return curtainColor.set(1.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    curtainColor.set(lightInfo);
                    IsoDirections edge = this.getSpriteEdge(false);

                    float interpX = switch (edge) {
                        case N -> 0.5F;
                        case S -> 0.5F;
                        case W -> 0.0F;
                        case E -> 1.0F;
                        default -> 0.0F;
                    };

                    float interpY = switch (edge) {
                        case N -> 0.0F;
                        case S -> 1.0F;
                        case W -> 0.5F;
                        case E -> 1.5F;
                        default -> 0.0F;
                    };
                    curtainSq.interpolateLight(curtainColor, interpX, interpY);
                    curtainColor.a = FBORenderCell.instance
                        .calculateWindowTargetAlpha(IsoCamera.frameState.playerIndex, this, this.getOppositeSquare(), this.getNorth());
                    return curtainColor;
                }
            }
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        IsoObjectAnimations.getInstance().addDancingDoor(this);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        IsoObjectAnimations.getInstance().removeDancingDoor(this);
    }

    public void checkKeyHighlight(int playerIndex) {
        Key key = Key.highlightDoor[playerIndex].key;
        if (key != null) {
            boolean seen = this.square.isSeen(playerIndex);
            if (!seen) {
                IsoGridSquare oppositeSq = this.getOppositeSquare();
                seen = oppositeSq != null && oppositeSq.isSeen(playerIndex);
            }

            if (seen) {
                this.checkKeyId();
                if (this.getKeyId() == key.getKeyId()) {
                    this.setHighlighted(playerIndex, true, false);
                }
            }
        }
    }

    private void prerender(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, IsoDirections edge) {
        if (Core.tileScale == 1) {
            switch (edge) {
                case N:
                    this.prerender1xN(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case S:
                    this.prerender1xS(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case W:
                    this.prerender1xW(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case E:
                    this.prerender1xE(x, y, z, col, bDoAttached, bWallLightingPass, null);
            }
        } else {
            switch (edge) {
                case N:
                    this.prerender2xN(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case S:
                    this.prerender2xS(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case W:
                    this.prerender2xW(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case E:
                    this.prerender2xE(x, y, z, col, bDoAttached, bWallLightingPass, null);
            }
        }
    }

    private void postrender(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, IsoDirections edge) {
        if (Core.tileScale == 1) {
            switch (edge) {
                case N:
                    this.postrender1xN(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case S:
                    this.postrender1xS(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case W:
                    this.postrender1xW(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case E:
                    this.postrender1xE(x, y, z, col, bDoAttached, bWallLightingPass, null);
            }
        } else {
            switch (edge) {
                case N:
                    this.postrender2xN(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case S:
                    this.postrender2xS(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case W:
                    this.postrender2xW(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                case E:
                    this.postrender2xE(x, y, z, col, bDoAttached, bWallLightingPass, null);
            }
        }
    }

    private void prerender1xN(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (!this.north && this.open) {
                (this.curtainOpen ? this.curtainSopen : this.curtainS)
                    .render(null, x, y - 1.0F, z, this.dir, this.offsetX + 3.0F, this.offsetY + (this.curtainOpen ? -14 : -14), info, true);
            }
        } else if (this.north && !this.open) {
            (this.curtainOpen ? this.curtainSopen : this.curtainS)
                .render(null, x, y - 1.0F, z, this.dir, this.offsetX - 1.0F - 1.0F, this.offsetY + -15.0F, info, true);
        }
    }

    private void postrender1xN(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && !this.open) {
                (this.curtainOpen ? this.curtainNopen : this.curtainN)
                    .render(null, x, y, z, this.dir, this.offsetX - 10.0F - 1.0F, this.offsetY + -10.0F, info, true);
            }
        } else if (!this.north && this.open) {
            (this.curtainOpen ? this.curtainNopen : this.curtainN)
                .render(null, x, y, z, this.dir, this.offsetX - 4.0F, this.offsetY + (this.curtainOpen ? -10 : -10), info, true);
        }
    }

    private void prerender1xS(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert !this.north && this.open;

        if (!this.curtainInside) {
            (this.curtainOpen ? this.curtainSopen : this.curtainS)
                .render(
                    null, x, y, z, this.dir, this.offsetX + (this.curtainOpen ? -14 : -14) / 2, this.offsetY + (this.curtainOpen ? -16 : -16) / 2, info, true
                );
        }
    }

    private void postrender1xS(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert !this.north && this.open;

        if (this.curtainInside) {
            (this.curtainOpen ? this.curtainNopen : this.curtainN)
                .render(
                    null,
                    x,
                    y + 1.0F,
                    z,
                    this.dir,
                    this.offsetX + (this.curtainOpen ? -28 : -28) / 2,
                    this.offsetY + (this.curtainOpen ? -8 : -8) / 2,
                    info,
                    true
                );
        }
    }

    private void prerender1xW(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && this.open) {
                (this.curtainOpen ? this.curtainEopen : this.curtainE)
                    .render(
                        null,
                        x - 1.0F,
                        y,
                        z,
                        this.dir,
                        this.offsetX + (this.curtainOpen ? -16 : -18),
                        this.offsetY + (this.curtainOpen ? -14 : -15),
                        info,
                        true
                    );
            }

            if (!this.north && this.open) {
                (this.curtainOpen ? this.curtainSopen : this.curtainS)
                    .render(null, x, y - 1.0F, z, this.dir, this.offsetX + 3.0F, this.offsetY + (this.curtainOpen ? -14 : -14), info, true);
            }
        } else {
            if (this.north && !this.open) {
                (this.curtainOpen ? this.curtainSopen : this.curtainS)
                    .render(null, x, y - 1.0F, z, this.dir, this.offsetX - 1.0F - 1.0F, this.offsetY + -15.0F, info, true);
            }

            if (!this.north && !this.open) {
                (this.curtainOpen ? this.curtainEopen : this.curtainE)
                    .render(
                        null,
                        x - 1.0F,
                        y,
                        z,
                        this.dir,
                        this.offsetX + (this.curtainOpen ? -12 : -14),
                        this.offsetY + (this.curtainOpen ? -14 : -15),
                        info,
                        true
                    );
            }
        }
    }

    private void postrender1xW(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && !this.open) {
                (this.curtainOpen ? this.curtainNopen : this.curtainN)
                    .render(null, x, y, z, this.dir, this.offsetX - 10.0F - 1.0F, this.offsetY + -10.0F, info, true);
            }

            if (!this.north && !this.open) {
                (this.curtainOpen ? this.curtainWopen : this.curtainW)
                    .render(null, x, y, z, this.dir, this.offsetX - 2.0F - 1.0F, this.offsetY + -10.0F, info, true);
            }
        } else {
            if (this.north && this.open) {
                (this.curtainOpen ? this.curtainWopen : this.curtainW).render(null, x, y, z, this.dir, this.offsetX - 9.0F, this.offsetY + -10.0F, info, true);
            }

            if (!this.north && this.open) {
                (this.curtainOpen ? this.curtainNopen : this.curtainN)
                    .render(null, x, y, z, this.dir, this.offsetX - 4.0F, this.offsetY + (this.curtainOpen ? -10 : -10), info, true);
            }
        }
    }

    private void prerender1xE(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert this.north && this.open;

        if (!this.curtainInside) {
            (this.curtainOpen ? this.curtainEopen : this.curtainE)
                .render(
                    null, x, y, z, this.dir, this.offsetX + (this.curtainOpen ? -13 : -18) / 2, this.offsetY + (this.curtainOpen ? -15 : -18) / 2, info, true
                );
        }
    }

    private void postrender1xE(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert this.north && this.open;

        if (this.curtainInside) {
            (this.curtainOpen ? this.curtainWopen : this.curtainW)
                .render(null, x + 1.0F, y, z, this.dir, this.offsetX + (this.curtainOpen ? 0 : 0), this.offsetY + (this.curtainOpen ? 0 : 0), info, true);
        }
    }

    private void prerender2xN(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (!this.north && this.open) {
                (this.curtainOpen ? this.curtainSopen : this.curtainS)
                    .render(null, x, y - 1.0F, z, this.dir, this.offsetX + 7.0F, this.offsetY + (this.curtainOpen ? -28 : -28), info, false);
            }
        } else if (this.north && !this.open) {
            (this.curtainOpen ? this.curtainSopen : this.curtainS)
                .render(null, x, y - 1.0F, z, this.dir, this.offsetX - 3.0F, this.offsetY + (this.curtainOpen ? -30 : -30), info, false);
        }
    }

    private void postrender2xN(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && !this.open) {
                (this.curtainOpen ? this.curtainNopen : this.curtainN)
                    .render(null, x, y, z, this.dir, this.offsetX - 20.0F, this.offsetY + (this.curtainOpen ? -20 : -20), info, false);
            }
        } else if (!this.north && this.open) {
            (this.curtainOpen ? this.curtainNopen : this.curtainN)
                .render(null, x, y, z, this.dir, this.offsetX - 8.0F, this.offsetY + (this.curtainOpen ? -20 : -20), info, false);
        }
    }

    private void prerender2xS(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert !this.north && this.open;

        if (!this.curtainInside) {
            (this.curtainOpen ? this.curtainSopen : this.curtainS)
                .render(null, x, y, z, this.dir, this.offsetX + (this.curtainOpen ? -14 : -14), this.offsetY + (this.curtainOpen ? -16 : -16), info, false);
        }
    }

    private void postrender2xS(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert !this.north && this.open;

        if (this.curtainInside) {
            (this.curtainOpen ? this.curtainNopen : this.curtainN)
                .render(null, x, y + 1.0F, z, this.dir, this.offsetX + (this.curtainOpen ? -28 : -28), this.offsetY + (this.curtainOpen ? -8 : -8), info, false);
        }
    }

    private void prerender2xW(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && this.open) {
                (this.curtainOpen ? this.curtainEopen : this.curtainE)
                    .render(
                        null,
                        x - 1.0F,
                        y,
                        z,
                        this.dir,
                        this.offsetX + (this.curtainOpen ? -32 : -37),
                        this.offsetY + (this.curtainOpen ? -28 : -31),
                        info,
                        false
                    );
            }
        } else if (!this.north && !this.open) {
            (this.curtainOpen ? this.curtainEopen : this.curtainE)
                .render(
                    null, x - 1.0F, y, z, this.dir, this.offsetX + (this.curtainOpen ? -22 : -26), this.offsetY + (this.curtainOpen ? -28 : -31), info, false
                );
        }
    }

    private void postrender2xW(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (!this.north && !this.open) {
                (this.curtainOpen ? this.curtainWopen : this.curtainW)
                    .render(null, x, y, z, this.dir, this.offsetX - 5.0F, this.offsetY + (this.curtainOpen ? -20 : -20), info, false);
            }
        } else if (this.north && this.open) {
            (this.curtainOpen ? this.curtainWopen : this.curtainW)
                .render(null, x, y, z, this.dir, this.offsetX - 19.0F, this.offsetY + (this.curtainOpen ? -20 : -20), info, false);
        }
    }

    private void prerender2xE(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert this.north && this.open;

        if (!this.curtainInside) {
            (this.curtainOpen ? this.curtainEopen : this.curtainE)
                .render(null, x, y, z, this.dir, this.offsetX + (this.curtainOpen ? -13 : -18), this.offsetY + (this.curtainOpen ? -15 : -18), info, false);
        }
    }

    private void postrender2xE(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert this.north && this.open;

        if (this.curtainInside) {
            (this.curtainOpen ? this.curtainWopen : this.curtainW)
                .render(null, x + 1.0F, y, z, this.dir, this.offsetX + (this.curtainOpen ? 0 : 0), this.offsetY + (this.curtainOpen ? 0 : 0), info, false);
        }
    }

    private void renderCurtainSpriteOrModel(
        IsoSpriteInstance spriteInstance, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo col, boolean bDoRenderPrep
    ) {
        if (!this.renderCurtainModel(spriteInstance, this.getX() + 0.5F, this.getY() + 0.5F, z, col)) {
            spriteInstance.render(null, x, y, z, dir, offsetX, offsetY, col, bDoRenderPrep);
        }
    }

    private boolean renderCurtainModel(IsoSpriteInstance spriteInstance, float x, float y, float z, ColorInfo col) {
        if (!PerformanceSettings.fboRenderChunk) {
            return false;
        } else {
            SpriteModel spriteModel = spriteInstance.getParentSprite().spriteModel;
            if (spriteModel == null) {
                return false;
            } else {
                float offset = this.sprite.getProperties().isSurfaceOffset() ? this.sprite.getProperties().getSurface() : 0.0F;
                int playerIndex = IsoCamera.frameState.playerIndex;
                ColorInfo colOrig = col;
                float a = col.a;
                float targetAlpha = this.getTargetAlpha(IsoCamera.frameState.playerIndex);
                float alpha = this.getAlpha(IsoCamera.frameState.playerIndex);
                if (alpha < targetAlpha) {
                    alpha += IsoSprite.alphaStep;
                    if (alpha > targetAlpha) {
                        alpha = targetAlpha;
                    }
                } else if (alpha > targetAlpha) {
                    alpha -= IsoSprite.alphaStep;
                    if (alpha < targetAlpha) {
                        alpha = targetAlpha;
                    }
                }

                if (alpha < 0.0F) {
                    alpha = 0.0F;
                }

                if (alpha > 1.0F) {
                    alpha = 1.0F;
                }

                this.setAlpha(IsoCamera.frameState.playerIndex, alpha);
                col.a = col.a * this.getAlpha(IsoCamera.frameState.playerIndex);
                if (DebugOptions.instance.fboRenderChunk.forceAlphaAndTargetOne.getValue()) {
                    col.a = 1.0F;
                }

                boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
                if (bHighlighted) {
                    stCol.set(this.getHighlightColor(playerIndex));
                    if (this.isBlink()) {
                        stCol.a = stCol.a * Core.blinkAlpha;
                    }

                    stCol.r = col.r * (1.0F - stCol.a) + this.getHighlightColor(playerIndex).r * stCol.a;
                    stCol.g = col.g * (1.0F - stCol.a) + this.getHighlightColor(playerIndex).g * stCol.a;
                    stCol.b = col.b * (1.0F - stCol.a) + this.getHighlightColor(playerIndex).b * stCol.a;
                    stCol.a = col.a;
                    col = stCol;
                }

                if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
                    alpha = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this.getSquare());
                    col = stCol.set(col.r * alpha, col.g * alpha, col.b * alpha, col.a);
                }

                IsoDirections edge = this.getSpriteEdge(true);
                if (IsoBarricade.GetBarricadeOnSquare(this.getSquare(), edge) != null) {
                    x += edge.dx() * -0.07F;
                    y += edge.dy() * -0.07F;
                }

                AnimationPlayer animationPlayer = IsoObjectAnimations.getInstance().getAnimationPlayer(this);
                float oldScale = spriteModel.scale;
                spriteModel.scale = 1.5F;
                String attachmentName = this.curtainInside ? "curtain1" : "curtain2";
                Matrix4f xfrm = IsoObjectAnimations.getInstance().getAttachmentTransform(this, attachmentName, BaseVehicle.allocMatrix4f());
                ObjectRenderEffects renderEffects = this.getObjectRenderEffectsToApply();
                IsoObjectModelDrawer.RenderStatus renderStatus;
                if (renderEffects == null) {
                    renderStatus = IsoObjectModelDrawer.renderMain(
                        spriteModel, x, y, z, col, this.getRenderYOffset() + offset, this.getSpriteModel(), xfrm, false, true
                    );
                } else {
                    renderStatus = IsoObjectModelDrawer.renderMain(
                        spriteModel,
                        x + (float)renderEffects.x1 * 1.5F,
                        y + (float)renderEffects.y1 * 1.5F,
                        z,
                        col,
                        this.getRenderYOffset() + offset,
                        this.getSpriteModel(),
                        xfrm,
                        false,
                        true
                    );
                }

                BaseVehicle.releaseMatrix4f(xfrm);
                spriteModel.scale = oldScale;
                colOrig.a = a;
                if (renderStatus == IsoObjectModelDrawer.RenderStatus.Loading
                    && PerformanceSettings.fboRenderChunk
                    && FBORenderChunkManager.instance.isCaching()) {
                    FBORenderCell.instance.handleDelayedLoading(this);
                    return true;
                } else {
                    return renderStatus == IsoObjectModelDrawer.RenderStatus.Ready;
                }
            }
        }
    }

    public IsoDirections getSpriteEdge(boolean ignoreOpen) {
        if (this.open && !ignoreOpen) {
            PropertyContainer properties = this.getProperties();
            if (properties != null && properties.has("GarageDoor")) {
                return this.north ? IsoDirections.N : IsoDirections.W;
            } else if (properties != null && properties.has(IsoFlagType.attachedE)) {
                return IsoDirections.E;
            } else if (properties != null && properties.has(IsoFlagType.attachedS)) {
                return IsoDirections.S;
            } else {
                return this.north ? IsoDirections.W : IsoDirections.N;
            }
        } else {
            return this.north ? IsoDirections.N : IsoDirections.W;
        }
    }

    public IsoDoor(IsoCell cell, IsoGridSquare gridSquare, IsoSprite gid, boolean north) {
        this.open = gid.getProperties().has(IsoFlagType.open);
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        int openSpriteOffset = 2;
        if (gid.getProperties().has("DoubleDoor")) {
            openSpriteOffset = 4;
        }

        if (gid.getProperties().has("GarageDoor")) {
            openSpriteOffset = 8;
        }

        this.closedSprite = this.open ? IsoSprite.getSprite(IsoSpriteManager.instance, gid, -openSpriteOffset) : gid;
        this.openSprite = this.open ? gid : IsoSprite.getSprite(IsoSpriteManager.instance, gid, openSpriteOffset);
        this.sprite = this.open ? this.openSprite : this.closedSprite;
        this.square = gridSquare;
        this.north = north;
        switch (this.type) {
            case WeakWooden:
                this.maxHealth = this.health = 500;
                break;
            case StrongWooden:
                this.maxHealth = this.health = 800;
        }

        if (this.getProperties().has("forceLocked")) {
            this.maxHealth = this.health = 2000;
        }

        if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
            this.maxHealth = this.health = 100;
        }

        int randLock = 69;
        if (SandboxOptions.instance.lockedHouses.getValue() == 1) {
            randLock = -1;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 2) {
            randLock = 5;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 3) {
            randLock = 10;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 4) {
            randLock = 50;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 5) {
            randLock = 60;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 6) {
            randLock = 70;
        }

        if (randLock > -1) {
            this.locked = Rand.Next(100) < randLock;
            if (this.locked && Rand.Next(3) == 0) {
                this.lockedByKey = true;
            }
        }

        if (this.getProperties().has("forceLocked")) {
            this.locked = true;
            this.lockedByKey = true;
        }

        if (this.open) {
            this.locked = false;
            this.lockedByKey = false;
        }
    }

    public IsoDoor(IsoCell cell, IsoGridSquare gridSquare, String gid, boolean north) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        IsoSprite existing = IsoSpriteManager.instance.namedMap.get(gid);
        if (existing != null && existing.getProperties().has(IsoFlagType.open)) {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 0);
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -2);
            this.open = true;
        } else {
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 0);
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 2);
            this.open = false;
        }

        IsoSprite validSprite = this.open ? this.openSprite : this.closedSprite;
        String GarageDoor = validSprite.getProperties().get("GarageDoor");
        if (GarageDoor != null) {
            int index = Integer.parseInt(GarageDoor);
            if (index <= 3) {
                this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 8);
            } else {
                this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -8);
            }
        }

        this.sprite = this.open ? this.openSprite : this.closedSprite;
        this.square = gridSquare;
        this.north = north;
        switch (this.type) {
            case WeakWooden:
                this.maxHealth = this.health = 500;
                break;
            case StrongWooden:
                this.maxHealth = this.health = 800;
        }

        if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
            this.maxHealth = this.health = 100;
        }
    }

    public IsoDoor(IsoCell cell, IsoGridSquare gridSquare, String gid, boolean north, KahluaTable table) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 0);
        this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 2);
        this.table = table;
        this.sprite = this.closedSprite;
        String GarageDoor = this.sprite.getProperties().get("GarageDoor");
        if (GarageDoor != null) {
            int index = Integer.parseInt(GarageDoor);
            if (index <= 3) {
                this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 8);
            } else {
                this.openSprite = this.sprite;
                this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -8);
            }
        }

        this.square = gridSquare;
        this.north = north;
        switch (this.type) {
            case WeakWooden:
                this.maxHealth = this.health = 500;
                break;
            case StrongWooden:
                this.maxHealth = this.health = 800;
        }

        if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
            this.maxHealth = this.health = 100;
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.open = input.get() == 1;
        this.locked = input.get() == 1;
        this.north = input.get() == 1;
        this.health = input.getInt();
        this.maxHealth = input.getInt();
        this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = 2500;
        this.keyId = input.getInt();
        this.lockedByKey = input.get() == 1;
        byte b = input.get();
        if ((b & 1) != 0) {
            this.hasCurtain = true;
            this.curtainOpen = (b & 2) != 0;
            this.curtainInside = (b & 4) != 0;
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.put((byte)(this.open ? 1 : 0));
        output.put((byte)(this.locked ? 1 : 0));
        output.put((byte)(this.north ? 1 : 0));
        output.putInt(this.health);
        output.putInt(this.maxHealth);
        output.putInt(this.closedSprite.id);
        output.putInt(this.openSprite.id);
        output.putInt(this.getKeyId());
        output.put((byte)(this.isLockedByKey() ? 1 : 0));
        byte b = 0;
        if (this.hasCurtain) {
            b = (byte)(b | 1);
            if (this.curtainOpen) {
                b = (byte)(b | 2);
            }

            if (this.curtainInside) {
                b = (byte)(b | 4);
            }
        }

        output.put(b);
    }

    @Override
    public void saveState(ByteBuffer bb) throws IOException {
        bb.put((byte)(this.open ? 1 : 0));
        bb.put((byte)(this.locked ? 1 : 0));
        bb.put((byte)(this.lockedByKey ? 1 : 0));
    }

    @Override
    public void loadState(ByteBuffer bb) throws IOException {
        boolean open = bb.get() == 1;
        boolean Locked = bb.get() == 1;
        boolean lockedByKey = bb.get() == 1;
        if (open != this.open) {
            this.open = open;
            this.sprite = open ? this.openSprite : this.closedSprite;
        }

        if (Locked != this.locked) {
            this.locked = Locked;
        }

        if (lockedByKey != this.lockedByKey) {
            this.lockedByKey = lockedByKey;
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    @Override
    public boolean IsOpen() {
        return this.open;
    }

    public boolean IsStrengthenedByPushedItems() {
        return false;
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return false;
    }

    @Override
    public boolean TestPathfindCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        boolean north = this.north;
        if (!this.isBarricaded()) {
            return false;
        } else if (obj instanceof IsoSurvivor isoSurvivor && isoSurvivor.getInventory().contains("Hammer")) {
            return false;
        } else {
            if (this.open) {
                north = !north;
            }

            if (from == this.square) {
                if (north && to.getY() < from.getY()) {
                    return true;
                }

                if (!north && to.getX() < from.getX()) {
                    return true;
                }
            } else {
                if (north && to.getY() > from.getY()) {
                    return true;
                }

                if (!north && to.getX() > from.getX()) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        boolean north = this.north;
        if (this.open) {
            return false;
        } else {
            if (from == this.square) {
                if (north && to.getY() < from.getY()) {
                    if (obj != null) {
                        obj.collideWith(this);
                    }

                    return true;
                }

                if (!north && to.getX() < from.getX()) {
                    if (obj != null) {
                        obj.collideWith(this);
                    }

                    return true;
                }
            } else {
                if (north && to.getY() > from.getY()) {
                    if (obj != null) {
                        obj.collideWith(this);
                    }

                    return true;
                }

                if (!north && to.getX() > from.getX()) {
                    if (obj != null) {
                        obj.collideWith(this);
                    }

                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        boolean doorTrans = this.sprite != null && this.sprite.getProperties().has("doorTrans");
        if (this.sprite != null && this.sprite.getProperties().has("GarageDoor") && this.open) {
            doorTrans = true;
        }

        if (this.open) {
            doorTrans = true;
        } else if (this.hasCurtain && !this.curtainOpen) {
            doorTrans = false;
        }

        boolean north = this.north;
        if (this.open) {
            north = !north;
        }

        if (to.getZ() != from.getZ()) {
            return IsoObject.VisionResult.NoEffect;
        } else {
            if (from == this.square) {
                if (north && to.getY() < from.getY()) {
                    if (doorTrans) {
                        return IsoObject.VisionResult.Unblocked;
                    }

                    return IsoObject.VisionResult.Blocked;
                }

                if (!north && to.getX() < from.getX()) {
                    if (doorTrans) {
                        return IsoObject.VisionResult.Unblocked;
                    }

                    return IsoObject.VisionResult.Blocked;
                }
            } else {
                if (north && to.getY() > from.getY()) {
                    if (doorTrans) {
                        return IsoObject.VisionResult.Unblocked;
                    }

                    return IsoObject.VisionResult.Blocked;
                }

                if (!north && to.getX() > from.getX()) {
                    if (doorTrans) {
                        return IsoObject.VisionResult.Unblocked;
                    }

                    return IsoObject.VisionResult.Blocked;
                }
            }

            return IsoObject.VisionResult.NoEffect;
        }
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        if (!this.isDestroyed()) {
            if (thumper instanceof IsoGameCharacter isoGameCharacter) {
                Thumpable thumpable = this.getThumpableFor(isoGameCharacter);
                if (thumpable == null) {
                    return;
                }

                if (thumpable != this) {
                    thumpable.Thump(thumper);
                    return;
                }
            }

            if (thumper instanceof IsoZombie isoZombie) {
                if (isoZombie.cognition == 1
                    && !this.open
                    && (!this.locked || thumper.getCurrentSquare() != null && !thumper.getCurrentSquare().has(IsoFlagType.exterior))) {
                    this.ToggleDoor((IsoGameCharacter)thumper);
                    if (this.open) {
                        return;
                    }
                }

                if (GameClient.client) {
                    if (isoZombie.isLocal()) {
                        GameClient.sendZombieHitThumpable((IsoGameCharacter)thumper, this);
                    }

                    return;
                }

                int tot = thumper.getSurroundingThumpers();
                int mult = ThumpState.getFastForwardDamageMultiplier();
                int max = isoZombie.strength;
                if (tot >= 2) {
                    this.DirtySlice();
                    this.Damage(isoZombie.strength * mult);
                    if (SandboxOptions.instance.lore.strength.getValue() == 1) {
                        this.Damage(tot * 2 * mult);
                    }
                }

                if (Core.gameMode.equals("LastStand")) {
                    this.Damage(1 * mult);
                }

                WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0F, 15.0F);
                this.setRenderEffect(RenderEffectType.Hit_Door, true);
            }

            if (this.health <= 0) {
                if (this.getSquare().getBuilding() != null) {
                    this.getSquare().getBuilding().forceAwake();
                }

                this.playDoorSound(((IsoGameCharacter)thumper).getEmitter(), "Break");
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer((IsoGameCharacter)thumper, "BreakDoor", false, thumper.getCurrentSquare(), 0.2F, 20.0F, 1.1F, true);
                }

                WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0F, 15.0F);
                thumper.setThumpTarget(null);
                if (destroyDoubleDoor(this)) {
                    return;
                }

                if (destroyGarageDoor(this)) {
                    return;
                }

                this.destroy();
            }
        }
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        IsoBarricade barricade = this.getBarricadeForCharacter(chr);
        if (barricade != null) {
            return barricade;
        } else {
            barricade = this.getBarricadeOppositeCharacter(chr);
            if (barricade != null) {
                return barricade;
            } else {
                return !this.isDestroyed() && !this.IsOpen() ? this : null;
            }
        }
    }

    @Override
    public float getThumpCondition() {
        return this.getMaxHealth() <= 0 ? 0.0F : (float)PZMath.clamp(this.getHealth(), 0, this.getMaxHealth()) / this.getMaxHealth();
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (GameClient.client) {
            this.setRenderEffect(RenderEffectType.Hit_Door, true);
        } else {
            Thumpable thumpable = this.getThumpableFor(owner);
            if (thumpable != null) {
                if (thumpable instanceof IsoBarricade) {
                    thumpable.WeaponHit(owner, weapon);
                } else {
                    LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
                    if (!this.open) {
                        if (!this.isDestroyed()) {
                            int perk = owner.getPerkLevel(PerkFactory.Perks.Strength);
                            float mod = 1.0F;
                            if (perk == 0) {
                                mod = 0.5F;
                            } else if (perk == 1) {
                                mod = 0.63F;
                            } else if (perk == 2) {
                                mod = 0.76F;
                            } else if (perk == 3) {
                                mod = 0.89F;
                            } else if (perk == 4) {
                                mod = 1.02F;
                            }

                            if (perk == 6) {
                                mod = 1.15F;
                            } else if (perk == 7) {
                                mod = 1.27F;
                            } else if (perk == 8) {
                                mod = 1.3F;
                            } else if (perk == 9) {
                                mod = 1.45F;
                            } else if (perk == 10) {
                                mod = 1.7F;
                            }

                            this.Damage((int)(weapon.getDoorDamage() * 2.0F * mod));
                            this.setRenderEffect(RenderEffectType.Hit_Door, true);
                            if (Rand.Next(10) == 0) {
                                this.Damage((int)(weapon.getDoorDamage() * 6.0F * mod));
                            }

                            float delta = GameTime.getInstance().getThirtyFPSMultiplier();
                            switch (owner.getPerkLevel(PerkFactory.Perks.Fitness)) {
                                case 0:
                                    owner.exert(0.01F * delta);
                                    break;
                                case 1:
                                    owner.exert(0.007F * delta);
                                    break;
                                case 2:
                                    owner.exert(0.0065F * delta);
                                    break;
                                case 3:
                                    owner.exert(0.006F * delta);
                                    break;
                                case 4:
                                    owner.exert(0.005F * delta);
                                    break;
                                case 5:
                                    owner.exert(0.004F * delta);
                                    break;
                                case 6:
                                    owner.exert(0.0035F * delta);
                                    break;
                                case 7:
                                    owner.exert(0.003F * delta);
                                    break;
                                case 8:
                                    owner.exert(0.0025F * delta);
                                    break;
                                case 9:
                                    owner.exert(0.002F * delta);
                            }

                            this.DirtySlice();
                            if (weapon.getDoorHitSound() != null) {
                                if (player != null) {
                                    player.setMeleeHitSurface(this.getSoundPrefix());
                                }

                                owner.getEmitter().playSound(weapon.getDoorHitSound(), this);
                                if (GameServer.server) {
                                    GameServer.PlayWorldSoundServer(owner, weapon.getDoorHitSound(), false, this.getSquare(), 1.0F, 20.0F, 2.0F, false);
                                }
                            }

                            WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                            if (!this.IsStrengthenedByPushedItems() && this.health <= 0
                                || this.IsStrengthenedByPushedItems() && this.health <= -this.pushedMaxStrength) {
                                this.playDoorSound(owner.getEmitter(), "Break");
                                if (GameServer.server) {
                                    GameServer.PlayWorldSoundServer(owner, "BreakDoor", false, this.getSquare(), 0.2F, 20.0F, 1.1F, true);
                                }

                                WorldSoundManager.instance
                                    .addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0F, 15.0F);
                                if (destroyDoubleDoor(this)) {
                                    return;
                                }

                                if (destroyGarageDoor(this)) {
                                    return;
                                }

                                this.destroy();
                                LuaEventManager.triggerEvent("OnContainerUpdate");
                            }
                        }
                    }
                }
            }
        }
    }

    public void destroy() {
        if (this.sprite != null && this.sprite.getProperties().has("GarageDoor")) {
            this.destroyed = true;
            this.square.transmitRemoveItemFromSquare(this);
        } else {
            PropertyContainer props = this.getProperties();
            if (props != null) {
                String Material = props.get("Material");
                String Material2 = props.get("Material2");
                String Material3 = props.get("Material3");
                if (StringUtils.isNullOrEmpty(Material) && StringUtils.isNullOrEmpty(Material2) && StringUtils.isNullOrEmpty(Material3)) {
                    int NumPlanks = Rand.Next(2) + 1;

                    for (int i = 0; i < NumPlanks; i++) {
                        this.square.AddWorldInventoryItem("Base.Plank", 0.0F, 0.0F, 0.0F);
                    }
                } else {
                    this.addItemsFromProperties();
                }

                InventoryItem item = InventoryItemFactory.CreateItem("Base.Doorknob");
                item.setKeyId(this.checkKeyId());
                this.square.AddWorldInventoryItem(item, 0.0F, 0.0F, 0.0F);
                int NumHinges = Rand.Next(3);

                for (int i = 0; i < NumHinges; i++) {
                    this.square.AddWorldInventoryItem("Base.Hinge", 0.0F, 0.0F, 0.0F);
                }

                if (this.hasCurtain) {
                    this.square.AddWorldInventoryItem("Base.Sheet", 0.0F, 0.0F, 0.0F);
                }

                this.destroyed = true;
                this.square.transmitRemoveItemFromSquare(this);
            }
        }
    }

    public IsoGridSquare getOtherSideOfDoor(IsoGameCharacter chr) {
        if (this.north) {
            return chr.getCurrentSquare().getRoom() == this.square.getRoom()
                ? IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ())
                : IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ());
        } else {
            return chr.getCurrentSquare().getRoom() == this.square.getRoom()
                ? IsoWorld.instance.currentCell.getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ())
                : IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ());
        }
    }

    @Deprecated
    public boolean isExteriorDoor(IsoGameCharacter chr) {
        return this.isExterior();
    }

    public boolean isExterior() {
        IsoGridSquare sq = this.getSquare();
        IsoGridSquare sqOtherSide = this.getOppositeSquare();
        if (sqOtherSide == null) {
            return false;
        } else {
            return sq.has(IsoFlagType.exterior) && sqOtherSide.getBuilding() != null && sqOtherSide.getBuilding().getDef() != null
                ? true
                : sq.getBuilding() != null && sq.getBuilding().getDef() != null && sqOtherSide.has(IsoFlagType.exterior);
        }
    }

    @Override
    public boolean isHoppable() {
        if (this.IsOpen()) {
            return false;
        } else if (this.closedSprite == null) {
            return false;
        } else {
            PropertyContainer props = this.closedSprite.getProperties();
            return props.has(IsoFlagType.HoppableN) || props.has(IsoFlagType.HoppableW);
        }
    }

    @Override
    public boolean canClimbOver(IsoGameCharacter chr) {
        if (this.square == null) {
            return false;
        } else {
            return !this.isHoppable() ? false : chr == null || IsoWindow.canClimbThroughHelper(chr, this.getSquare(), this.getOppositeSquare(), this.north);
        }
    }

    public boolean couldBeOpen(IsoGameCharacter chr) {
        if (chr instanceof IsoAnimal) {
            return false;
        } else if (this.isBarricaded()) {
            return false;
        } else if (this.isLockedByKey()
            && chr instanceof IsoPlayer
            && (
                chr.getCurrentSquare().has(IsoFlagType.exterior)
                    || this.getProperties().has("forceLocked")
                    || this.getModData().rawget("CustomLock") != null
                        && this.getModData().rawget("CustomLock") instanceof Boolean
                        && (Boolean)this.getModData().rawget("CustomLock")
            )
            && chr.getInventory().haveThisKeyId(this.getKeyId()) == null) {
            return false;
        } else {
            return this.isObstructed() ? false : !"Tutorial".equals(Core.getInstance().getGameMode()) || !this.isLockedByKey();
        }
    }

    public void ToggleDoorActual(IsoGameCharacter chr) {
        IsoPlayer player = Type.tryCastTo(chr, IsoPlayer.class);
        if (!(chr instanceof IsoAnimal)) {
            this.lastPlayerOnlineId = chr != null ? chr.getOnlineID() : -1;
            if (GameServer.server && chr instanceof IsoPlayer && player.getRole().hasCapability(Capability.CanOpenLockedDoors)) {
                this.locked = false;
                this.setLockedByKey(false);
            }

            if (Core.debug && DebugOptions.instance.cheat.door.unlock.getValue()) {
                this.locked = false;
                this.setLockedByKey(false);
            }

            if (this.isHoppable()) {
                this.locked = false;
                this.setLockedByKey(false);
            }

            this.wasTryingToggleLockedDoor = false;
            this.wasTryingToggleBarricadedDoor = false;
            if (this.isBarricaded()) {
                if (chr != null) {
                    this.TriggerBarricadedDoor(chr);
                }

                this.wasTryingToggleBarricadedDoor = true;
                this.syncIsoObject(false, (byte)(this.open ? 1 : 0), null, null);
            } else {
                this.checkKeyId();
                if (this.locked && !this.lockedByKey && this.getKeyId() != -1) {
                    this.lockedByKey = true;
                }

                if (chr instanceof IsoPlayer isoPlayer) {
                    if (!this.open) {
                        isoPlayer.timeSinceOpenDoor = 0.0F;
                    } else {
                        isoPlayer.timeSinceCloseDoor = 0.0F;
                    }
                }

                this.DirtySlice();
                IsoGridSquare.recalcLightTime = -1.0F;
                Core.dirtyGlobalLightsCount++;
                GameTime.instance.lightSourceUpdate = 100.0F;
                this.square.InvalidateSpecialObjectPaths();
                if (this.isLockedByKey()
                    && chr != null
                    && chr instanceof IsoPlayer
                    && (
                        chr.getCurrentSquare().has(IsoFlagType.exterior)
                            || this.getProperties().has("forceLocked")
                            || this.getModData().rawget("CustomLock") != null
                                && this.getModData().rawget("CustomLock") instanceof Boolean
                                && (Boolean)this.getModData().rawget("CustomLock")
                    )
                    && !this.open) {
                    if (chr.getInventory().haveThisKeyId(this.getKeyId()) == null) {
                        this.TriggerLockedDoor(chr);
                        this.wasTryingToggleLockedDoor = true;
                        this.syncIsoObject(false, (byte)(this.open ? 1 : 0), null, null);
                        return;
                    }

                    this.playDoorSound(chr.getEmitter(), "Unlock");
                    this.playDoorSound(chr.getEmitter(), "Open");
                    this.locked = false;
                    this.setLockedByKey(false);
                }

                boolean bUnlock = chr instanceof IsoPlayer && !chr.getCurrentSquare().isOutside();
                if ("Tutorial".equals(Core.getInstance().getGameMode()) && this.isLockedByKey()) {
                    bUnlock = false;
                }

                if (chr instanceof IsoPlayer && this.getSprite().getProperties().has("GarageDoor")) {
                    boolean bInteriorSide = this.getSprite().getProperties().has("InteriorSide");
                    if (bInteriorSide) {
                        bUnlock = this.north ? chr.getY() >= this.getY() : chr.getX() >= this.getX();
                    } else {
                        bUnlock = this.north ? chr.getY() < this.getY() : chr.getX() < this.getX();
                    }
                }

                if (this.locked && !bUnlock && !this.open) {
                    this.playDoorSound(chr.getEmitter(), "Locked");
                    this.setRenderEffect(RenderEffectType.Hit_Door, true);
                } else if (this.getSprite().getProperties().has("DoubleDoor")) {
                    if (isDoubleDoorObstructed(this)) {
                        if (chr != null) {
                            this.playDoorSound(chr.getEmitter(), "Blocked");
                            chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0F);
                        }
                    } else {
                        boolean wasOpen = this.open;
                        toggleDoubleDoor(this, true);
                        if (wasOpen != this.open) {
                            this.playDoorSound(chr.getEmitter(), this.open ? "Open" : "Close");
                            if (player != null && player.isLocalPlayer()) {
                                player.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                            }
                        }
                    }
                } else if (this.getSprite().getProperties().has("GarageDoor")) {
                    if (isGarageDoorObstructed(this)) {
                        if (chr != null) {
                            this.playDoorSound(chr.getEmitter(), "Blocked");
                            chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0F);
                        }
                    } else {
                        boolean wasOpen = this.open;
                        toggleGarageDoor(this, true);
                        if (wasOpen != this.open) {
                            this.playDoorSound(chr.getEmitter(), this.open ? "Open" : "Close");
                        }

                        if (player != null && player.isLocalPlayer()) {
                            player.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                        }
                    }
                } else if (this.isObstructed()) {
                    if (chr != null) {
                        this.playDoorSound(chr.getEmitter(), "Blocked");
                        chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0F);
                    }
                } else {
                    this.locked = false;
                    this.setLockedByKey(false);
                    if (chr instanceof IsoPlayer) {
                        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                            LosUtil.cachecleared[pn] = true;
                        }

                        IsoGridSquare.setRecalcLightTime(-1.0F);
                    }

                    this.open = !this.open;
                    WeatherFxMask.forceMaskUpdateAll();
                    this.sprite = this.closedSprite;
                    if (this.open) {
                        if (chr != null) {
                            this.playDoorSound(chr.getEmitter(), "Open");
                        }

                        this.sprite = this.openSprite;
                    } else if (chr != null) {
                        this.playDoorSound(chr.getEmitter(), "Close");
                    }

                    this.square.RecalcAllWithNeighbours(true);
                    this.syncIsoObject(false, (byte)(this.open ? 1 : 0), null, null);
                    PolygonalMap2.instance.squareChanged(this.square);
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                    this.invalidateRenderChunkLevel(256L);
                    if (player != null && player.isLocalPlayer()) {
                        player.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                    }

                    this.PlayAnimation();
                }
            }
        }
    }

    private void PlayAnimation() {
        SpriteModel spriteModel1 = this.getSpriteModel();
        if (spriteModel1 != null && spriteModel1.animationName != null) {
            IsoObjectAnimations.getInstance().addObject(this, spriteModel1, this.open ? "Open" : "Close");
        }

        this.setAnimating(true);
    }

    private void TriggerLockedDoor(IsoGameCharacter chr) {
        this.playDoorSound(chr.getEmitter(), "Locked");
        this.setRenderEffect(RenderEffectType.Hit_Door, true);
    }

    private void TriggerBarricadedDoor(IsoGameCharacter chr) {
        this.playDoorSound(chr.getEmitter(), "Blocked");
        if (chr instanceof IsoPlayer player && player.isLocalPlayer()) {
            chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBarricaded"), 255, 255, 255, 256.0F);
        }

        this.setRenderEffect(RenderEffectType.Hit_Door, true);
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        byte i = (byte)this.square.getObjects().indexOf(this);
        b.putByte(i);
        b.putByte((byte)1);
        b.putByte((byte)0);
        b.putBoolean(this.open);
        b.putBoolean(this.locked);
        b.putBoolean(this.lockedByKey);
        b.putBoolean(this.wasTryingToggleLockedDoor);
        b.putBoolean(this.wasTryingToggleBarricadedDoor);
        b.putInt(this.health);
        b.putShort(this.lastPlayerOnlineId);
        if (this.getSprite().getProperties().has("GarageDoor")) {
            b.putBoolean(true);
            ArrayList<NetObject> doorParts = new ArrayList<>();
            doorParts.add(new NetObject().setObject(this));

            for (IsoObject prev = getGarageDoorPrev(this); prev != null; prev = getGarageDoorPrev(prev)) {
                doorParts.add(new NetObject().setObject(prev));
            }

            for (IsoObject next = getGarageDoorNext(this); next != null; next = getGarageDoorNext(next)) {
                doorParts.add(new NetObject().setObject(next));
            }

            b.putByte((byte)doorParts.size());

            for (NetObject doorPart : doorParts) {
                doorPart.write(b);
            }
        } else {
            b.putBoolean(false);
        }
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source, ByteBuffer bb) {
        if (GameClient.client || GameServer.server) {
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
            } else if (!bRemote) {
                if (GameClient.client) {
                    this.lastPlayerOnlineId = IsoPlayer.getInstance().getOnlineID();
                    ByteBufferWriter b = GameClient.connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
                } else {
                    for (UdpConnection connection : GameServer.udpEngine.connections) {
                        if (connection.isFullyConnected() && connection.RelevantTo(this.getX(), this.getY())) {
                            ByteBufferWriter b = connection.startPacket();
                            PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                            this.syncIsoObjectSend(b);
                            PacketTypes.PacketType.SyncIsoObject.send(connection);
                        }
                    }
                }
            } else if (bb == null) {
                DebugLog.Multiplayer.error("ERROR: " + this.getClass().getSimpleName() + " ByteBuffer is null");
            } else {
                boolean bOpen = bb.get() == 1;
                boolean bLocked = bb.get() == 1;
                boolean bLockedByKey = bb.get() == 1;
                boolean bWasTryingToggleLockedDoor = bb.get() == 1;
                boolean bWasTryingToggleBarricadedDoor = bb.get() == 1;
                int health = bb.getInt();
                this.lastPlayerOnlineId = bb.getShort();
                boolean bParts = bb.get() == 1;
                ArrayList<NetObject> doorParts = new ArrayList<>();
                if (bParts) {
                    byte size = bb.get();

                    for (byte i = 0; i < size; i++) {
                        NetObject doorPart = new NetObject();
                        doorPart.parse(bb, null);
                        doorParts.add(doorPart);
                    }
                }

                if (bRemote) {
                    boolean wasLocked = this.locked && !bLocked;
                    this.locked = bLocked;
                    this.lockedByKey = bLockedByKey;
                    this.health = health;
                    IsoPlayer player = null;
                    if (GameClient.client && this.lastPlayerOnlineId != -1) {
                        player = GameClient.IDToPlayerMap.get(this.lastPlayerOnlineId);
                        if (player != null) {
                            player.networkAi.setNoCollision(1000L);
                        }
                    }

                    if (wasLocked && player != null) {
                        this.playDoorSound(player.getEmitter(), "Unlock");
                    }

                    if (getDoubleDoorIndex(this) != -1) {
                        if (bOpen != this.open) {
                            toggleDoubleDoor(this, false);
                        }
                    } else if (bOpen != this.open) {
                        this.open = bOpen;
                        this.sprite = this.open ? this.openSprite : this.closedSprite;
                        if (player != null) {
                            this.playDoorSound(player.getEmitter(), this.open ? "Open" : "Close");
                            if (player.isLocalPlayer()) {
                                player.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                            }
                        }

                        this.PlayAnimation();
                    }

                    for (NetObject doorPart : doorParts) {
                        IsoObject obj = doorPart.getObject();
                        if (obj == null || obj.getSquare() == null) {
                            DebugLog.General.error("expected IsoDoor index is invalid " + doorPart.getDescription());
                            return;
                        }

                        IsoDoor door = obj instanceof IsoDoor isoDoor ? isoDoor : null;
                        IsoThumpable thumpable = obj instanceof IsoThumpable isoThumpable ? isoThumpable : null;
                        if (door != null) {
                            door.open = this.open;
                            door.setLockedByKey(bLockedByKey);
                            door.changeSprite(door);
                        }

                        if (thumpable != null) {
                            thumpable.open = this.open;
                            thumpable.setLockedByKey(bLockedByKey);
                            thumpable.changeSprite(thumpable);
                        }

                        obj.getSquare().RecalcAllWithNeighbours(true);
                        obj.invalidateRenderChunkLevel(256L);
                        PolygonalMap2.instance.squareChanged(obj.getSquare());
                    }

                    if (bWasTryingToggleLockedDoor) {
                        if (player != null) {
                            this.TriggerLockedDoor(player);
                        }
                    } else if (bWasTryingToggleBarricadedDoor && player != null) {
                        this.TriggerBarricadedDoor(player);
                    }

                    if (GameServer.server) {
                        for (UdpConnection connectionx : GameServer.udpEngine.connections) {
                            if (connectionx.isFullyConnected()
                                && connectionx.RelevantTo(this.getX(), this.getY())
                                && (source == null || connectionx.getConnectedGUID() != source.getConnectedGUID())) {
                                ByteBufferWriter b = connectionx.startPacket();
                                PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                                this.syncIsoObjectSend(b);
                                PacketTypes.PacketType.SyncIsoObject.send(connectionx);
                            }
                        }
                    }
                }

                this.square.InvalidateSpecialObjectPaths();
                this.square.RecalcProperties();
                this.square.RecalcAllWithNeighbours(true);

                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                IsoGridSquare.setRecalcLightTime(-1.0F);
                GameTime.instance.lightSourceUpdate = 100.0F;
                LuaEventManager.triggerEvent("OnContainerUpdate");
                WeatherFxMask.forceMaskUpdateAll();
                this.flagForHotSave();
            }
        }
    }

    public void ToggleDoor(IsoGameCharacter chr) {
        this.ToggleDoorActual(chr);
    }

    public void ToggleDoorSilent() {
        if (!this.isBarricaded()) {
            this.square.InvalidateSpecialObjectPaths();

            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                LosUtil.cachecleared[pn] = true;
            }

            IsoGridSquare.setRecalcLightTime(-1.0F);
            this.open = !this.open;
            this.sprite = this.closedSprite;
            if (this.open) {
                this.sprite = this.openSprite;
            }
        }
    }

    void Damage(int amount) {
        this.DirtySlice();
        this.health -= amount;
        if (GameServer.server) {
            this.sync();
        }
    }

    @Override
    public IsoBarricade getBarricadeOnSameSquare() {
        return IsoBarricade.GetBarricadeOnSquare(this.square, this.north ? IsoDirections.N : IsoDirections.W);
    }

    @Override
    public IsoBarricade getBarricadeOnOppositeSquare() {
        return IsoBarricade.GetBarricadeOnSquare(this.getOppositeSquare(), this.north ? IsoDirections.S : IsoDirections.E);
    }

    @Override
    public boolean isBarricaded() {
        IsoBarricade barricade = this.getBarricadeOnSameSquare();
        if (barricade == null) {
            barricade = this.getBarricadeOnOppositeSquare();
        }

        return barricade != null;
    }

    @Override
    public boolean isBarricadeAllowed() {
        return this.getSprite() != null && !this.getSprite().getProperties().has("DoubleDoor") && !this.getSprite().getProperties().has("GarageDoor");
    }

    @Override
    public IsoBarricade getBarricadeForCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeForCharacter(this, chr);
    }

    @Override
    public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeOppositeCharacter(this, chr);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean bLocked) {
        this.locked = bLocked;
    }

    @Override
    public boolean getNorth() {
        return this.north;
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0F, 0.0F);
        } else {
            return this.north ? pos.set(this.getX() + 0.5F, this.getY()) : pos.set(this.getX(), this.getY() + 0.5F);
        }
    }

    @Override
    public Vector2 getFacingPositionAlt(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0F, 0.0F);
        } else {
            switch (this.getSpriteEdge(false)) {
                case N:
                    return pos.set(this.getX() + 0.5F, this.getY());
                case S:
                    return pos.set(this.getX() + 0.5F, this.getY() + 1.0F);
                case W:
                    return pos.set(this.getX(), this.getY() + 0.5F);
                case E:
                    return pos.set(this.getX() + 1.0F, this.getY() + 0.5F);
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public void setIsLocked(boolean lock) {
        this.locked = lock;
    }

    public IsoSprite getOpenSprite() {
        return this.openSprite;
    }

    public void setOpenSprite(IsoSprite sprite) {
        this.openSprite = sprite;
    }

    @Override
    public int getKeyId() {
        return this.keyId;
    }

    public void syncDoorKey() {
        ByteBufferWriter b = GameClient.connection.startPacket();
        PacketTypes.PacketType.SyncDoorKey.doPacket(b);
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        byte i = (byte)this.square.getObjects().indexOf(this);
        if (i == -1) {
            System.out.println("ERROR: Door not found on square " + this.square.getX() + ", " + this.square.getY() + ", " + this.square.getZ());
            GameClient.connection.cancelPacket();
        } else {
            b.putByte(i);
            b.putInt(this.getKeyId());
            PacketTypes.PacketType.SyncDoorKey.send(GameClient.connection);
        }
    }

    @Override
    public void setKeyId(int keyId) {
        if (this.keyId != keyId && GameClient.client) {
            this.keyId = keyId;
            this.syncDoorKey();
        } else {
            this.keyId = keyId;
        }
    }

    @Override
    public boolean isLockedByKey() {
        return this.lockedByKey;
    }

    @Override
    public void setLockedByKey(boolean lockedByKey) {
        boolean changed = lockedByKey != this.lockedByKey;
        this.lockedByKey = lockedByKey;
        this.locked = lockedByKey;
        if (changed) {
            if (lockedByKey) {
                this.syncIsoObject(false, (byte)3, null, null);
            } else {
                this.syncIsoObject(false, (byte)4, null, null);
            }
        }
    }

    public boolean haveKey() {
        return this.haveKey;
    }

    public void setHaveKey(boolean haveKey) {
        this.haveKey = haveKey;
        if (!GameServer.server) {
            if (haveKey) {
                this.syncIsoObject(false, (byte)-1, null, null);
            } else {
                this.syncIsoObject(false, (byte)-2, null, null);
            }
        }
    }

    @Override
    public IsoGridSquare getOppositeSquare() {
        return this.getNorth()
            ? this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ())
            : this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ());
    }

    public boolean isAdjacentToSquare(IsoGridSquare square2) {
        IsoGridSquare square1 = this.getSquare();
        if (square1 != null && square2 != null) {
            boolean bClosed = !this.IsOpen();
            IsoGridSquare nw = square1.getAdjacentSquare(IsoDirections.NW);
            IsoGridSquare n = square1.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare ne = square1.getAdjacentSquare(IsoDirections.NE);
            IsoGridSquare w = square1.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare e = square1.getAdjacentSquare(IsoDirections.E);
            IsoGridSquare sw = square1.getAdjacentSquare(IsoDirections.SW);
            IsoGridSquare s = square1.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare se = square1.getAdjacentSquare(IsoDirections.SE);
            switch (this.getSpriteEdge(false)) {
                case N:
                    if (square2 == nw) {
                        if (!nw.isWallTo(n) && !nw.isWindowTo(n) && !nw.hasDoorOnEdge(IsoDirections.E, false) && !n.hasDoorOnEdge(IsoDirections.W, false)) {
                            if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                                return false;
                            }

                            if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.N)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == n) {
                        if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                            return false;
                        }

                        if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.N)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == ne) {
                        if (!ne.isWallTo(n) && !ne.isWindowTo(n) && !ne.hasDoorOnEdge(IsoDirections.W, false) && !n.hasDoorOnEdge(IsoDirections.E, false)) {
                            if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                                return false;
                            }

                            if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.N)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == w) {
                        if (!w.isWallTo(square1)
                            && !w.isWindowTo(square1)
                            && !w.hasDoorOnEdge(IsoDirections.E, false)
                            && !square1.hasDoorOnEdge(IsoDirections.W, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.N)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == square1) {
                        if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.N)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == e) {
                        if (!e.isWallTo(square1)
                            && !e.isWindowTo(square1)
                            && !e.hasDoorOnEdge(IsoDirections.W, false)
                            && !square1.hasDoorOnEdge(IsoDirections.E, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.N)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }
                    break;
                case S:
                    if (square2 == w) {
                        if (!w.isWallTo(square1)
                            && !w.isWindowTo(square1)
                            && !w.hasDoorOnEdge(IsoDirections.E, false)
                            && !square1.hasDoorOnEdge(IsoDirections.W, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.S)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == square1) {
                        if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.S)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == e) {
                        if (!e.isWallTo(square1)
                            && !e.isWindowTo(square1)
                            && !e.hasDoorOnEdge(IsoDirections.W, false)
                            && !square1.hasDoorOnEdge(IsoDirections.E, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.S)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == sw) {
                        if (!sw.isWallTo(s) && !sw.isWindowTo(s) && !sw.hasDoorOnEdge(IsoDirections.E, false) && !s.hasDoorOnEdge(IsoDirections.W, false)) {
                            if (s.hasDoorOnEdge(IsoDirections.N, false)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == s) {
                        if (s.hasDoorOnEdge(IsoDirections.N, false)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == se) {
                        if (!se.isWallTo(s) && !se.isWindowTo(s) && !se.hasDoorOnEdge(IsoDirections.W, false) && !s.hasDoorOnEdge(IsoDirections.E, false)) {
                            if (s.hasDoorOnEdge(IsoDirections.N, false)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }
                    break;
                case W:
                    if (square2 == nw) {
                        if (!nw.isWallTo(w) && !nw.isWindowTo(w) && !nw.hasDoorOnEdge(IsoDirections.S, false) && !w.hasDoorOnEdge(IsoDirections.N, false)) {
                            if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                                return false;
                            }

                            if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.W)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == w) {
                        if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                            return false;
                        }

                        if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.W)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == sw) {
                        if (!sw.isWallTo(w) && !sw.isWindowTo(w) && !sw.hasDoorOnEdge(IsoDirections.N, false) && !w.hasDoorOnEdge(IsoDirections.S, false)) {
                            if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                                return false;
                            }

                            if (this.IsOpen() && square1.hasClosedDoorOnEdge(IsoDirections.W)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == n) {
                        if (!n.isWallTo(square1)
                            && !n.isWindowTo(square1)
                            && !n.hasDoorOnEdge(IsoDirections.S, false)
                            && !square1.hasDoorOnEdge(IsoDirections.N, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.W)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == square1) {
                        if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.W)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == s) {
                        if (!s.isWallTo(square1)
                            && !s.isWindowTo(square1)
                            && !s.hasDoorOnEdge(IsoDirections.N, false)
                            && !square1.hasDoorOnEdge(IsoDirections.S, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.W)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }
                    break;
                case E:
                    if (square2 == n) {
                        if (!n.isWallTo(square1)
                            && !n.isWindowTo(square1)
                            && !n.hasDoorOnEdge(IsoDirections.S, false)
                            && !square1.hasDoorOnEdge(IsoDirections.N, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.E)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == square1) {
                        if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.E)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == s) {
                        if (!s.isWallTo(square1)
                            && !s.isWindowTo(square1)
                            && !s.hasDoorOnEdge(IsoDirections.N, false)
                            && !square1.hasDoorOnEdge(IsoDirections.S, false)) {
                            if (bClosed && square1.hasOpenDoorOnEdge(IsoDirections.E)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == ne) {
                        if (!ne.isWallTo(e) && !ne.isWindowTo(e) && !ne.hasDoorOnEdge(IsoDirections.S, false) && !w.hasDoorOnEdge(IsoDirections.N, false)) {
                            if (e.hasDoorOnEdge(IsoDirections.W, false)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }

                    if (square2 == e) {
                        if (e.hasDoorOnEdge(IsoDirections.W, false)) {
                            return false;
                        }

                        return true;
                    }

                    if (square2 == se) {
                        if (!se.isWallTo(e) && !se.isWindowTo(e) && !se.hasDoorOnEdge(IsoDirections.N, false) && !e.hasDoorOnEdge(IsoDirections.S, false)) {
                            if (e.hasDoorOnEdge(IsoDirections.E, false)) {
                                return false;
                            }

                            return true;
                        }

                        return false;
                    }
                    break;
                default:
                    return false;
            }

            return false;
        } else {
            return false;
        }
    }

    public int checkKeyId() {
        if (this.getKeyId() != -1) {
            return this.getKeyId();
        } else {
            IsoGridSquare sq = this.getSquare();
            IsoGridSquare sqOtherSide = this.getOppositeSquare();
            if (sq != null && sqOtherSide != null) {
                BuildingDef sqBuilding = sq.getBuilding() == null ? null : sq.getBuilding().getDef();
                BuildingDef sqOtherSideBuilding = sqOtherSide.getBuilding() == null ? null : sqOtherSide.getBuilding().getDef();
                if (sqBuilding == null && sqOtherSideBuilding != null) {
                    this.setKeyId(sqOtherSideBuilding.getKeyId());
                } else if (sqBuilding != null && sqOtherSideBuilding == null) {
                    this.setKeyId(sqBuilding.getKeyId());
                } else if (this.getProperties().has("forceLocked") && sqBuilding != null) {
                    this.setKeyId(sqBuilding.getKeyId());
                }

                if (this.locked && !this.lockedByKey && this.getKeyId() != -1) {
                    this.lockedByKey = true;
                }

                return this.getKeyId();
            } else {
                return -1;
            }
        }
    }

    @Override
    public void setHealth(int Health) {
        this.health = Health;
    }

    private void initCurtainSprites() {
        if (this.curtainN == null) {
            this.curtainW = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_16").newInstance();
            this.curtainW.setScale(0.8F, 0.8F);
            this.curtainWopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_20").newInstance();
            this.curtainWopen.setScale(0.8F, 0.8F);
            this.curtainE = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_17").newInstance();
            this.curtainE.setScale(0.8F, 0.8F);
            this.curtainEopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_21").newInstance();
            this.curtainEopen.setScale(0.8F, 0.8F);
            this.curtainN = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_18").newInstance();
            this.curtainN.setScale(0.8F, 0.8F);
            this.curtainNopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_22").newInstance();
            this.curtainNopen.setScale(0.8F, 0.8F);
            this.curtainS = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_19").newInstance();
            this.curtainS.setScale(0.8F, 0.8F);
            this.curtainSopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_23").newInstance();
            this.curtainSopen.setScale(0.8F, 0.8F);
        }
    }

    @Override
    public boolean canAddCurtain() {
        PropertyContainer props = this.getProperties();
        if (props == null) {
            return false;
        } else if (props.has("GarageDoor")) {
            return false;
        } else {
            return "SlidingGlassDoor".equalsIgnoreCase(this.getSoundPrefix()) ? false : props.has("doorTrans");
        }
    }

    public IsoDoor HasCurtains() {
        return this.hasCurtain ? this : null;
    }

    @Override
    public boolean isCurtainOpen() {
        return this.hasCurtain && this.curtainOpen;
    }

    public void setCurtainOpen(boolean open) {
        if (this.hasCurtain) {
            this.curtainOpen = open;
            if (!GameServer.server) {
                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                GameTime.instance.lightSourceUpdate = 100.0F;
                IsoGridSquare.setRecalcLightTime(-1.0F);
                if (this.square != null) {
                    this.square.RecalcProperties();
                    this.square.RecalcAllWithNeighbours(true);
                }
            }
        }
    }

    public void transmitSetCurtainOpen(boolean open) {
        if (this.hasCurtain) {
            if (GameServer.server) {
                this.sendObjectChange("setCurtainOpen", "open", open);
            }

            if (GameClient.client) {
                GameClient.instance
                    .sendClientCommandV(
                        null,
                        "object",
                        "openCloseCurtain",
                        "x",
                        this.getX(),
                        "y",
                        this.getY(),
                        "z",
                        this.getZ(),
                        "index",
                        this.getObjectIndex(),
                        "open",
                        !this.curtainOpen
                    );
            }
        }
    }

    public void toggleCurtain() {
        if (this.hasCurtain) {
            if (GameClient.client) {
                this.transmitSetCurtainOpen(!this.isCurtainOpen());
            } else {
                this.setCurtainOpen(!this.isCurtainOpen());
                if (GameServer.server) {
                    this.transmitSetCurtainOpen(this.isCurtainOpen());
                }
            }
        }
    }

    public void addSheet(IsoGameCharacter chr) {
        if (!this.hasCurtain && chr != null && chr.getCurrentSquare() != null) {
            IsoGridSquare sqChr = chr.getCurrentSquare();
            IsoGridSquare sqInside = this.getSquare();

            this.addSheet(switch (this.getSpriteEdge(false)) {
                case N -> this.north == sqChr.getY() >= sqInside.getY();
                case S -> sqChr.getY() > sqInside.getY();
                case W -> this.north == sqChr.getX() < sqInside.getX();
                case E -> sqChr.getX() > sqInside.getX();
                default -> throw new IllegalStateException();
            }, chr);
        }
    }

    public void addSheet(boolean inside, IsoGameCharacter chr) {
        if (!this.hasCurtain) {
            this.hasCurtain = true;
            this.curtainInside = inside;
            this.curtainOpen = true;
            if (!GameClient.client) {
                InventoryItem item = chr.getInventory().FindAndReturn("Sheet");
                chr.getInventory().Remove(item);
                if (GameServer.server) {
                    GameServer.sendRemoveItemFromContainer(chr.getInventory(), item);
                }
            }

            if (GameServer.server) {
                this.sendObjectChange("addSheet", "inside", inside);
            } else if (chr != null) {
                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                GameTime.instance.lightSourceUpdate = 100.0F;
                IsoGridSquare.setRecalcLightTime(-1.0F);
                if (this.square != null) {
                    this.square.RecalcProperties();
                }
            }
        }
    }

    public void removeSheet(IsoGameCharacter chr) {
        if (this.hasCurtain) {
            this.hasCurtain = false;
            InventoryItem item = chr.getInventory().AddItem("Base.Sheet");
            if (GameServer.server) {
                GameServer.sendAddItemToContainer(chr.getInventory(), item);
            }

            if (GameServer.server) {
                this.sendObjectChange("removeSheet");
            } else if (chr != null) {
                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                GameTime.instance.lightSourceUpdate = 100.0F;
                IsoGridSquare.setRecalcLightTime(-1.0F);
                if (this.square != null) {
                    this.square.RecalcProperties();
                }
            }
        }
    }

    /**
     * Returns the square the player should stand on to add a sheet.
     */
    public IsoGridSquare getAddSheetSquare(IsoGameCharacter chr) {
        if (chr != null && chr.getCurrentSquare() != null) {
            IsoGridSquare sqChr = chr.getCurrentSquare();
            IsoGridSquare sqInside = this.getSquare();
            switch (this.getSpriteEdge(false)) {
                case N:
                    return sqChr.getY() >= sqInside.getY() ? sqInside : this.getCell().getGridSquare(sqInside.x, sqInside.y - 1, sqInside.z);
                case S:
                    return sqChr.getY() <= sqInside.getY() ? sqInside : this.getCell().getGridSquare(sqInside.x, sqInside.y + 1, sqInside.z);
                case W:
                    return sqChr.getX() >= sqInside.getX() ? sqInside : this.getCell().getGridSquare(sqInside.x - 1, sqInside.y, sqInside.z);
                case E:
                    return sqChr.getX() <= sqInside.getX() ? sqInside : this.getCell().getGridSquare(sqInside.x + 1, sqInside.y, sqInside.z);
                default:
                    throw new IllegalStateException();
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the square the player should stand on to open/close/remove a sheet.
     */
    public IsoGridSquare getSheetSquare() {
        if (!this.hasCurtain) {
            return null;
        } else {
            switch (this.getSpriteEdge(false)) {
                case N:
                    if (this.open) {
                        return this.curtainInside
                            ? this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ())
                            : this.getSquare();
                    }

                    return this.curtainInside
                        ? this.getSquare()
                        : this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ());
                case S:
                    return this.curtainInside
                        ? this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() + 1.0F), (double)this.getZ())
                        : this.getSquare();
                case W:
                    if (this.open) {
                        return this.curtainInside
                            ? this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ())
                            : this.getSquare();
                    }

                    return this.curtainInside
                        ? this.getSquare()
                        : this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ());
                case E:
                    return this.curtainInside
                        ? this.getCell().getGridSquare((double)(this.getX() + 1.0F), (double)this.getY(), (double)this.getZ())
                        : this.getSquare();
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Override
    public int getHealth() {
        return this.health;
    }

    @Override
    public int getMaxHealth() {
        return this.maxHealth;
    }

    public boolean isFacingSheet(IsoGameCharacter chr) {
        if (this.hasCurtain && chr != null && chr.getCurrentSquare() == this.getSheetSquare()) {
            IsoDirections dir;
            if (this.curtainInside) {
                if (this.open) {
                    if (this.north) {
                        dir = IsoDirections.E;
                    } else {
                        dir = IsoDirections.S;
                    }
                } else if (this.north) {
                    dir = IsoDirections.N;
                } else {
                    dir = IsoDirections.W;
                }
            } else if (this.open) {
                if (this.north) {
                    dir = IsoDirections.W;
                } else {
                    dir = IsoDirections.N;
                }
            } else if (this.north) {
                dir = IsoDirections.S;
            } else {
                dir = IsoDirections.E;
            }

            IsoDirections edge = this.getSpriteEdge(false);
            if (edge == IsoDirections.E) {
                dir = this.curtainInside ? IsoDirections.W : IsoDirections.E;
            }

            if (edge == IsoDirections.S) {
                dir = this.curtainInside ? IsoDirections.N : IsoDirections.S;
            }

            return chr.getDir() == dir || chr.getDir() == IsoDirections.RotLeft(dir) || chr.getDir() == IsoDirections.RotRight(dir);
        } else {
            return false;
        }
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("addSheet".equals(change)) {
            if (tbl != null && tbl.rawget("inside") instanceof Boolean) {
                bb.put((byte)(tbl.rawget("inside") ? 1 : 0));
            }
        } else if (!"removeSheet".equals(change)) {
            if ("setCurtainOpen".equals(change)) {
                if (tbl != null && tbl.rawget("open") instanceof Boolean) {
                    bb.put((byte)(tbl.rawget("open") ? 1 : 0));
                }
            } else {
                super.saveChange(change, tbl, bb);
            }
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        if ("addSheet".equals(change)) {
            this.addSheet(bb.get() == 1, null);
        } else if ("removeSheet".equals(change)) {
            this.removeSheet(null);
        } else if ("setCurtainOpen".equals(change)) {
            this.setCurtainOpen(bb.get() == 1);
        } else {
            super.loadChange(change, bb);
        }
    }

    public void addRandomBarricades() {
        IsoGridSquare outside = this.square.getRoom() == null ? this.square : this.getOppositeSquare();
        if (outside != null && outside.getRoom() == null) {
            boolean addOpposite = outside != this.square;
            IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(this, addOpposite);
            if (barricade != null) {
                int numPlanks = Rand.Next(1, 4);

                for (int b = 0; b < numPlanks; b++) {
                    barricade.addPlank(null, null);
                }
            }
        }
    }

    public boolean isObstructed() {
        return isDoorObstructed(this);
    }

    public static boolean isDoorObstructed(IsoObject object) {
        IsoDoor door = object instanceof IsoDoor isoDoor ? isoDoor : null;
        IsoThumpable thump = object instanceof IsoThumpable isoThumpable ? isoThumpable : null;
        if (door == null && thump == null) {
            return false;
        } else {
            IsoGridSquare sq = object.getSquare();
            if (sq == null) {
                return false;
            } else if (!sq.isSolid() && !sq.isSolidTrans() && !sq.has(IsoObjectType.tree)) {
                int chunkMinX = (sq.x - 1) / 8;
                int chunkMinY = (sq.y - 1) / 8;
                int chunkMaxX = (int)Math.ceil((sq.x + 1.0F) / 8.0F);
                int chunkMaxY = (int)Math.ceil((sq.y + 1.0F) / 8.0F);

                for (int cy = chunkMinY; cy <= chunkMaxY; cy++) {
                    for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
                        IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                        if (chunk != null) {
                            for (int i = 0; i < chunk.vehicles.size(); i++) {
                                BaseVehicle vehicle = chunk.vehicles.get(i);
                                if (vehicle.isIntersectingSquareWithShadow(sq.x, sq.y, sq.z)) {
                                    return true;
                                }
                            }
                        }
                    }
                }

                return false;
            } else {
                return true;
            }
        }
    }

    public static void toggleDoubleDoor(IsoObject oneOfFour, boolean doSync) {
        int oneOfFourIndex = getDoubleDoorIndex(oneOfFour);
        if (oneOfFourIndex != -1) {
            IsoDoor door = oneOfFour instanceof IsoDoor isoDoor ? isoDoor : null;
            IsoThumpable thumpable = oneOfFour instanceof IsoThumpable isoThumpable ? isoThumpable : null;
            if (door != null) {
                ;
            }

            boolean open = door == null ? thumpable.open : door.open;
            IsoObject object1 = getDoubleDoorObject(oneOfFour, 1);
            IsoObject object2 = getDoubleDoorObject(oneOfFour, 2);
            IsoObject object3 = getDoubleDoorObject(oneOfFour, 3);
            IsoObject object4 = getDoubleDoorObject(oneOfFour, 4);
            if (object1 != null) {
                toggleDoubleDoorObject(object1);
            }

            if (object2 != null) {
                toggleDoubleDoorObject(object2);
            }

            if (object3 != null) {
                toggleDoubleDoorObject(object3);
            }

            if (object4 != null) {
                toggleDoubleDoorObject(object4);
            }

            LuaEventManager.triggerEvent("OnContainerUpdate");
            if (doSync) {
                if (object1 != null) {
                    object1.syncIsoObject(false, (byte)(open ? 1 : 0), null, null);
                } else if (object4 != null) {
                    object4.syncIsoObject(false, (byte)(open ? 1 : 0), null, null);
                }
            }

            if (object1 != null) {
                SpriteModel spriteModel1 = object1.getSpriteModel();
                if (spriteModel1 != null && spriteModel1.animationName != null) {
                    IsoObjectAnimations.getInstance().addObject(object1, spriteModel1, !open ? "Open" : "Close");
                    object1.setAnimating(true);
                }
            }

            if (object4 != null) {
                SpriteModel spriteModel1 = object4.getSpriteModel();
                if (spriteModel1 != null && spriteModel1.animationName != null) {
                    IsoObjectAnimations.getInstance().addObject(object4, spriteModel1, !open ? "Open" : "Close");
                    object4.setAnimating(true);
                }
            }
        }
    }

    private static void toggleDoubleDoorObject(IsoObject oneOfFour) {
        int oneOfFourIndex = getDoubleDoorIndex(oneOfFour);
        if (oneOfFourIndex != -1) {
            IsoDoor door = oneOfFour instanceof IsoDoor isoDoor ? isoDoor : null;
            IsoThumpable thumpable = oneOfFour instanceof IsoThumpable isoThumpable ? isoThumpable : null;
            boolean north = door == null ? thumpable.north : door.north;
            boolean open = door == null ? thumpable.open : door.open;
            int keyId = -1;
            if (door != null) {
                door.open = !open;
                door.setLockedByKey(false);
                keyId = door.checkKeyId();
            }

            if (thumpable != null) {
                thumpable.open = !open;
                thumpable.setLockedByKey(false);
                keyId = thumpable.getKeyId();
            }

            IsoSprite sprite = oneOfFour.getSprite();
            int offset = north ? DoubleDoorNorthSpriteOffset[oneOfFourIndex - 1] : DoubleDoorWestSpriteOffset[oneOfFourIndex - 1];
            if (open) {
                offset *= -1;
            }

            oneOfFour.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, sprite.getName(), offset);
            oneOfFour.getSquare().RecalcAllWithNeighbours(true);
            if (oneOfFourIndex != 2 && oneOfFourIndex != 3) {
                PolygonalMap2.instance.squareChanged(oneOfFour.getSquare());
                oneOfFour.invalidateRenderChunkLevel(256L);
            } else {
                IsoGridSquare sq = oneOfFour.getSquare();
                int[] xOffsetCur;
                int[] yOffsetCur;
                int[] xOffsetNew;
                int[] yOffsetNew;
                if (north) {
                    if (open) {
                        xOffsetCur = DoubleDoorNorthOpenXOffset;
                        yOffsetCur = DoubleDoorNorthOpenYOffset;
                        xOffsetNew = DoubleDoorNorthClosedXOffset;
                        yOffsetNew = DoubleDoorNorthClosedYOffset;
                    } else {
                        xOffsetCur = DoubleDoorNorthClosedXOffset;
                        yOffsetCur = DoubleDoorNorthClosedYOffset;
                        xOffsetNew = DoubleDoorNorthOpenXOffset;
                        yOffsetNew = DoubleDoorNorthOpenYOffset;
                    }
                } else if (open) {
                    xOffsetCur = DoubleDoorWestOpenXOffset;
                    yOffsetCur = DoubleDoorWestOpenYOffset;
                    xOffsetNew = DoubleDoorWestClosedXOffset;
                    yOffsetNew = DoubleDoorWestClosedYOffset;
                } else {
                    xOffsetCur = DoubleDoorWestClosedXOffset;
                    yOffsetCur = DoubleDoorWestClosedYOffset;
                    xOffsetNew = DoubleDoorWestOpenXOffset;
                    yOffsetNew = DoubleDoorWestOpenYOffset;
                }

                int firstX = sq.getX() - xOffsetCur[oneOfFourIndex - 1];
                int firstY = sq.getY() - yOffsetCur[oneOfFourIndex - 1];
                int indexX = firstX + xOffsetNew[oneOfFourIndex - 1];
                int indexY = firstY + yOffsetNew[oneOfFourIndex - 1];
                sq.RemoveTileObject(oneOfFour, false);
                PolygonalMap2.instance.squareChanged(sq);
                sq = IsoWorld.instance.currentCell.getGridSquare(indexX, indexY, sq.getZ());
                if (sq == null) {
                    return;
                }

                if (thumpable != null) {
                    IsoThumpable newDoorB = new IsoThumpable(sq.getCell(), sq, oneOfFour.getSprite().getName(), north, thumpable.getTable());
                    newDoorB.setModData(thumpable.getModData());
                    newDoorB.setCanBeLockByPadlock(thumpable.canBeLockByPadlock());
                    newDoorB.setCanBePlastered(thumpable.canBePlastered());
                    newDoorB.setIsHoppable(thumpable.isHoppable());
                    newDoorB.setIsDismantable(thumpable.isDismantable());
                    newDoorB.setName(thumpable.getName());
                    newDoorB.setIsDoor(true);
                    newDoorB.setIsThumpable(thumpable.isThumpable());
                    newDoorB.setThumpDmg(thumpable.getThumpDmg());
                    newDoorB.setThumpSound(thumpable.getThumpSound());
                    newDoorB.open = !open;
                    newDoorB.keyId = keyId;
                    sq.AddSpecialObject(newDoorB);
                } else {
                    IsoDoor newDoorB = new IsoDoor(sq.getCell(), sq, oneOfFour.getSprite().getName(), north);
                    newDoorB.open = !open;
                    newDoorB.keyId = keyId;
                    sq.getObjects().add(newDoorB);
                    sq.getSpecialObjects().add(newDoorB);
                    sq.RecalcProperties();
                    sq.invalidateRenderChunkLevel(64L);
                }

                if (!GameClient.client) {
                    sq.restackSheetRope();
                }

                PolygonalMap2.instance.squareChanged(sq);
            }
        }
    }

    public static int getDoubleDoorIndex(IsoObject oneOfFour) {
        if (oneOfFour != null && oneOfFour.getSquare() != null) {
            PropertyContainer props = oneOfFour.getProperties();
            if (props != null && props.has("DoubleDoor")) {
                int ddIndex = Integer.parseInt(props.get("DoubleDoor"));
                if (ddIndex >= 1 && ddIndex <= 8) {
                    IsoDoor door = oneOfFour instanceof IsoDoor isoDoor ? isoDoor : null;
                    IsoThumpable thump = oneOfFour instanceof IsoThumpable isoThumpable ? isoThumpable : null;
                    if (door == null && thump == null) {
                        return -1;
                    } else {
                        boolean open = door == null ? thump.open : door.open;
                        if (!open) {
                            return ddIndex;
                        } else {
                            return ddIndex >= 5 ? ddIndex - 4 : -1;
                        }
                    }
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static IsoObject getDoubleDoorObject(IsoObject oneOfFour, int index) {
        int oneOfFourIndex = getDoubleDoorIndex(oneOfFour);
        if (oneOfFourIndex != -1 && index > 0) {
            IsoDoor door = oneOfFour instanceof IsoDoor isoDoor ? isoDoor : null;
            IsoThumpable thump = oneOfFour instanceof IsoThumpable thumpable ? thumpable : null;
            boolean north = door == null ? thump.north : door.north;
            boolean open = door == null ? thump.open : door.open;
            IsoGridSquare sq = oneOfFour.getSquare();
            int[] xOffset;
            int[] yOffset;
            if (north) {
                if (open) {
                    xOffset = DoubleDoorNorthOpenXOffset;
                    yOffset = DoubleDoorNorthOpenYOffset;
                } else {
                    xOffset = DoubleDoorNorthClosedXOffset;
                    yOffset = DoubleDoorNorthClosedYOffset;
                }
            } else if (open) {
                xOffset = DoubleDoorWestOpenXOffset;
                yOffset = DoubleDoorWestOpenYOffset;
            } else {
                xOffset = DoubleDoorWestClosedXOffset;
                yOffset = DoubleDoorWestClosedYOffset;
            }

            int firstX = sq.getX() - xOffset[oneOfFourIndex - 1];
            int firstY = sq.getY() - yOffset[oneOfFourIndex - 1];
            int indexX = firstX + xOffset[index - 1];
            int indexY = firstY + yOffset[index - 1];
            sq = IsoWorld.instance.currentCell.getGridSquare(indexX, indexY, sq.getZ());
            if (sq == null) {
                return null;
            } else {
                ArrayList<IsoObject> SpecialObjects = sq.getSpecialObjects();
                if (door != null) {
                    for (int i = 0; i < SpecialObjects.size(); i++) {
                        IsoObject obj = SpecialObjects.get(i);
                        if (obj instanceof IsoDoor isoDoorx && isoDoorx.north == north && getDoubleDoorIndex(obj) == index) {
                            return obj;
                        }
                    }
                }

                if (thump != null) {
                    for (int ix = 0; ix < SpecialObjects.size(); ix++) {
                        IsoObject obj = SpecialObjects.get(ix);
                        if (obj instanceof IsoThumpable isoThumpable && isoThumpable.north == north && getDoubleDoorIndex(obj) == index) {
                            return obj;
                        }
                    }
                }

                return null;
            }
        } else {
            return null;
        }
    }

    public static int getDoubleDoorPartnerIndex(int ddIndex) {
        if (ddIndex >= 5) {
            ddIndex -= 4;
        }
        return switch (ddIndex) {
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 4;
            case 4 -> 3;
            default -> -1;
        };
    }

    public static boolean isDoubleDoorObstructed(IsoObject oneOfFour) {
        int oneOfFourIndex = getDoubleDoorIndex(oneOfFour);
        if (oneOfFourIndex == -1) {
            return false;
        } else {
            IsoDoor door = oneOfFour instanceof IsoDoor isoDoor ? isoDoor : null;
            IsoThumpable thump = oneOfFour instanceof IsoThumpable isoThumpable ? isoThumpable : null;
            boolean north = door == null ? thump.north : door.north;
            boolean open = door == null ? thump.open : door.open;
            IsoGridSquare sq = oneOfFour.getSquare();
            int[] xOffset;
            int[] yOffset;
            if (north) {
                if (open) {
                    xOffset = DoubleDoorNorthOpenXOffset;
                    yOffset = DoubleDoorNorthOpenYOffset;
                } else {
                    xOffset = DoubleDoorNorthClosedXOffset;
                    yOffset = DoubleDoorNorthClosedYOffset;
                }
            } else if (open) {
                xOffset = DoubleDoorWestOpenXOffset;
                yOffset = DoubleDoorWestOpenYOffset;
            } else {
                xOffset = DoubleDoorWestClosedXOffset;
                yOffset = DoubleDoorWestClosedYOffset;
            }

            boolean bHasLeft = getDoubleDoorObject(oneOfFour, 1) != null;
            boolean bHasRight = getDoubleDoorObject(oneOfFour, 4) != null;
            int firstX = sq.getX() - xOffset[oneOfFourIndex - 1];
            int firstY = sq.getY() - yOffset[oneOfFourIndex - 1];
            int minX = firstX;
            int minY = firstY;
            int z = sq.getZ();
            int maxX;
            int maxY;
            if (north) {
                maxX = firstX + 4;
                maxY = firstY + 2;
                if (bHasLeft && hasSomething4x4(firstX, firstY, firstX + 1, firstY + 1, z)) {
                    return true;
                }

                if (bHasRight && hasSomething4x4(firstX + 2, firstY, firstX + 3, firstY + 1, z)) {
                    return true;
                }

                if (!bHasLeft) {
                    minX = firstX + 2;
                }

                if (!bHasRight) {
                    maxX -= 2;
                }
            } else {
                minY = firstY - 3;
                maxX = firstX + 2;
                maxY = minY + 4;
                if (bHasLeft && hasSomething4x4(firstX, firstY - 1, firstX + 1, firstY, z)) {
                    return true;
                }

                if (bHasRight && hasSomething4x4(firstX, firstY - 3, firstX + 1, firstY - 2, z)) {
                    return true;
                }

                if (!bHasLeft) {
                    maxY -= 2;
                }

                if (!bHasRight) {
                    minY += 2;
                }
            }

            int chunkMinX = PZMath.fastfloor((minX - 4.0F) / 8.0F);
            int chunkMinY = PZMath.fastfloor((minY - 4.0F) / 8.0F);
            int chunkMaxX = (int)Math.ceil((maxX + 4.0F) / 8.0F);
            int chunkMaxY = (int)Math.ceil((maxY + 4.0F) / 8.0F);

            for (int cy = chunkMinY; cy <= chunkMaxY; cy++) {
                for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
                    IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);

                            for (int y = minY; y < maxY; y++) {
                                for (int x = minX; x < maxX; x++) {
                                    if (vehicle.isIntersectingSquare(x, y, z)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    private static boolean hasSolidObjects(IsoGridSquare square1) {
        return square1 == null ? false : square1.isSolid() || square1.isSolidTrans() || square1.has(IsoObjectType.tree);
    }

    private static boolean isSomethingTo(IsoGridSquare square1, IsoGridSquare square2) {
        if (square1 != null && square2 != null) {
            if (square2 != square1.getAdjacentSquare(IsoDirections.E)
                || !square1.hasDoorOnEdge(IsoDirections.E, false)
                    && !square2.hasDoorOnEdge(IsoDirections.W, false)
                    && !square2.getProperties().has(IsoFlagType.DoorWallW)) {
                if (square2 != square1.getAdjacentSquare(IsoDirections.SE)
                    || !square1.hasDoorOnEdge(IsoDirections.E, false)
                        && !square1.hasDoorOnEdge(IsoDirections.S, false)
                        && !square2.hasDoorOnEdge(IsoDirections.W, false)
                        && !square2.hasDoorOnEdge(IsoDirections.N, false)
                        && !square2.getProperties().has(IsoFlagType.DoorWallN)
                        && !square2.getProperties().has(IsoFlagType.DoorWallW)) {
                    return square2 != square1.getAdjacentSquare(IsoDirections.S)
                            || !square1.hasDoorOnEdge(IsoDirections.S, false)
                                && !square2.hasDoorOnEdge(IsoDirections.N, false)
                                && !square2.getProperties().has(IsoFlagType.DoorWallN)
                        ? square1.isWallTo(square2) || square1.isWindowTo(square2)
                        : true;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean hasSomething4x4(int x1, int y1, int x2, int y2, int z) {
        IsoGridSquare square1 = IsoWorld.instance.currentCell.getGridSquare(x1, y1, z);
        IsoGridSquare square2 = IsoWorld.instance.currentCell.getGridSquare(x2, y1, z);
        IsoGridSquare square3 = IsoWorld.instance.currentCell.getGridSquare(x2, y2, z);
        IsoGridSquare square4 = IsoWorld.instance.currentCell.getGridSquare(x1, y2, z);
        if (hasSolidObjects(square1) || hasSolidObjects(square2) || hasSolidObjects(square3) || hasSolidObjects(square4)) {
            return true;
        } else if (isSomethingTo(square1, square2)) {
            return true;
        } else {
            return isSomethingTo(square1, square3) ? true : isSomethingTo(square1, square4);
        }
    }

    public static boolean destroyDoubleDoor(IsoObject oneOfFour) {
        int ddIndex = getDoubleDoorIndex(oneOfFour);
        if (ddIndex == -1) {
            return false;
        } else {
            if (ddIndex == 1 || ddIndex == 4) {
                IsoObject attached = getDoubleDoorObject(oneOfFour, ddIndex == 1 ? 2 : 3);
                if (attached instanceof IsoDoor isoDoor) {
                    isoDoor.destroy();
                } else if (attached instanceof IsoThumpable isoThumpable) {
                    isoThumpable.destroy();
                }
            }

            if (oneOfFour instanceof IsoDoor isoDoor) {
                isoDoor.destroy();
            } else if (oneOfFour instanceof IsoThumpable isoThumpable) {
                isoThumpable.destroy();
            }

            LuaEventManager.triggerEvent("OnContainerUpdate");
            return true;
        }
    }

    public static int getGarageDoorIndex(IsoObject oneOfThree) {
        if (oneOfThree != null && oneOfThree.getSquare() != null) {
            PropertyContainer props = oneOfThree.getProperties();
            if (props != null && props.has("GarageDoor")) {
                int ddIndex = Integer.parseInt(props.get("GarageDoor"));
                if (ddIndex >= 1 && ddIndex <= 6) {
                    IsoDoor door = oneOfThree instanceof IsoDoor isoDoor ? isoDoor : null;
                    IsoThumpable thump = oneOfThree instanceof IsoThumpable isoThumpable ? isoThumpable : null;
                    if (door == null && thump == null) {
                        return -1;
                    } else {
                        boolean open = door == null ? thump.open : door.open;
                        if (!open) {
                            return ddIndex;
                        } else {
                            return ddIndex >= 4 ? ddIndex - 3 : -1;
                        }
                    }
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static IsoObject getGarageDoorPrev(IsoObject oneOfThree) {
        int oneOfThreeIndex = getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return null;
        } else if (oneOfThreeIndex == 1) {
            return null;
        } else {
            IsoDoor door = oneOfThree instanceof IsoDoor isoDoor ? isoDoor : null;
            IsoThumpable thump = oneOfThree instanceof IsoThumpable isoThumpable ? isoThumpable : null;
            boolean north = door == null ? thump.north : door.north;
            IsoGridSquare sq = oneOfThree.getSquare();
            int prevX = sq.x - (north ? 1 : 0);
            int prevY = sq.y + (north ? 0 : 1);
            sq = IsoWorld.instance.currentCell.getGridSquare(prevX, prevY, sq.getZ());
            if (sq == null) {
                return null;
            } else {
                ArrayList<IsoObject> SpecialObjects = sq.getSpecialObjects();
                if (door != null) {
                    for (int i = 0; i < SpecialObjects.size(); i++) {
                        IsoObject obj = SpecialObjects.get(i);
                        if (obj instanceof IsoDoor isoDoorx && isoDoorx.north == north && getGarageDoorIndex(obj) <= oneOfThreeIndex) {
                            return obj;
                        }
                    }
                }

                if (thump != null) {
                    for (int ix = 0; ix < SpecialObjects.size(); ix++) {
                        IsoObject obj = SpecialObjects.get(ix);
                        if (obj instanceof IsoThumpable isoThumpablex && isoThumpablex.north == north && getGarageDoorIndex(obj) <= oneOfThreeIndex) {
                            return obj;
                        }
                    }
                }

                return null;
            }
        }
    }

    public static IsoObject getGarageDoorNext(IsoObject oneOfThree) {
        int oneOfThreeIndex = getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return null;
        } else if (oneOfThreeIndex == 3) {
            return null;
        } else {
            IsoDoor door = oneOfThree instanceof IsoDoor isoDoor ? isoDoor : null;
            IsoThumpable thump = oneOfThree instanceof IsoThumpable isoThumpable ? isoThumpable : null;
            boolean north = door == null ? thump.north : door.north;
            IsoGridSquare sq = oneOfThree.getSquare();
            int nextX = sq.x + (north ? 1 : 0);
            int nextY = sq.y - (north ? 0 : 1);
            sq = IsoWorld.instance.currentCell.getGridSquare(nextX, nextY, sq.getZ());
            if (sq == null) {
                return null;
            } else {
                ArrayList<IsoObject> SpecialObjects = sq.getSpecialObjects();
                if (door != null) {
                    for (int i = 0; i < SpecialObjects.size(); i++) {
                        IsoObject obj = SpecialObjects.get(i);
                        if (obj instanceof IsoDoor isoDoorx && isoDoorx.north == north && getGarageDoorIndex(obj) >= oneOfThreeIndex) {
                            return obj;
                        }
                    }
                }

                if (thump != null) {
                    for (int ix = 0; ix < SpecialObjects.size(); ix++) {
                        IsoObject obj = SpecialObjects.get(ix);
                        if (obj instanceof IsoThumpable isoThumpablex && isoThumpablex.north == north && getGarageDoorIndex(obj) >= oneOfThreeIndex) {
                            return obj;
                        }
                    }
                }

                return null;
            }
        }
    }

    public static IsoObject getGarageDoorFirst(IsoObject oneOfThree) {
        int oneOfThreeIndex = getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return null;
        } else if (oneOfThreeIndex == 1) {
            return oneOfThree;
        } else {
            for (IsoObject prev = getGarageDoorPrev(oneOfThree); prev != null; prev = getGarageDoorPrev(prev)) {
                if (getGarageDoorIndex(prev) == 1) {
                    return prev;
                }
            }

            return oneOfThree;
        }
    }

    public void changeSprite(IsoDoor door) {
        door.sprite = door.open ? door.openSprite : door.closedSprite;
    }

    private static void toggleGarageDoorObject(IsoObject oneOfThree) {
        int oneOfThreeIndex = getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex != -1) {
            IsoDoor door = oneOfThree instanceof IsoDoor isoDoor ? isoDoor : null;
            IsoThumpable thumpable = oneOfThree instanceof IsoThumpable isoThumpable ? isoThumpable : null;
            boolean open = door == null ? thumpable.open : door.open;
            if (door != null) {
                door.open = !open;
                door.setLockedByKey(false);
                door.sprite = door.open ? door.openSprite : door.closedSprite;
            }

            if (thumpable != null) {
                thumpable.open = !open;
                thumpable.setLockedByKey(false);
                thumpable.sprite = thumpable.open ? thumpable.openSprite : thumpable.closedSprite;
            }

            oneOfThree.getSquare().RecalcAllWithNeighbours(true);
            PolygonalMap2.instance.squareChanged(oneOfThree.getSquare());
            oneOfThree.invalidateRenderChunkLevel(256L);
        }
    }

    public static void toggleGarageDoor(IsoObject oneOfThree, boolean doSync) {
        int oneOfThreeIndex = getGarageDoorIndex(oneOfThree);
        ArrayList<IsoObject> doorPartsIndexes = new ArrayList<>();
        doorPartsIndexes.add(oneOfThree);
        IsoDoor door = oneOfThree instanceof IsoDoor isoDoor ? isoDoor : null;
        if (!(oneOfThree instanceof IsoThumpable isoThumpable)) {
            Object var10 = null;
        }

        toggleGarageDoorObject(oneOfThree);

        for (IsoObject prev = getGarageDoorPrev(oneOfThree); prev != null; prev = getGarageDoorPrev(prev)) {
            toggleGarageDoorObject(prev);
            doorPartsIndexes.add(prev);
        }

        for (IsoObject next = getGarageDoorNext(oneOfThree); next != null; next = getGarageDoorNext(next)) {
            toggleGarageDoorObject(next);
            doorPartsIndexes.add(next);
        }

        if (GameClient.client || GameServer.server) {
            oneOfThree.syncIsoObject(false, (byte)(door.open ? 1 : 0), null, null);
        }

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            LosUtil.cachecleared[pn] = true;
        }

        IsoGridSquare.setRecalcLightTime(-1.0F);
        LuaEventManager.triggerEvent("OnContainerUpdate");
    }

    private static boolean isGarageDoorObstructed(IsoObject oneOfThree) {
        int oneOfThreeIndex = getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return false;
        } else {
            IsoDoor door = oneOfThree instanceof IsoDoor isoDoor ? isoDoor : null;
            IsoThumpable thumpable = oneOfThree instanceof IsoThumpable isoThumpable ? isoThumpable : null;
            boolean north = door == null ? thumpable.north : door.north;
            boolean open = door == null ? thumpable.open : door.open;
            if (!open) {
                return false;
            } else {
                int minX = oneOfThree.square.x;
                int minY = oneOfThree.square.y;
                int maxX = minX;
                int maxY = minY;
                if (north) {
                    for (IsoObject prev = getGarageDoorPrev(oneOfThree); prev != null; prev = getGarageDoorPrev(prev)) {
                        minX--;
                    }

                    for (IsoObject next = getGarageDoorNext(oneOfThree); next != null; next = getGarageDoorNext(next)) {
                        maxX++;
                    }
                } else {
                    for (IsoObject prev = getGarageDoorPrev(oneOfThree); prev != null; prev = getGarageDoorPrev(prev)) {
                        maxY++;
                    }

                    for (IsoObject next = getGarageDoorNext(oneOfThree); next != null; next = getGarageDoorNext(next)) {
                        minY--;
                    }
                }

                int chunkMinX = PZMath.fastfloor((minX - 4.0F) / 8.0F);
                int chunkMinY = PZMath.fastfloor((minY - 4.0F) / 8.0F);
                int chunkMaxX = (int)Math.ceil((maxX + 4.0F) / 8.0F);
                int chunkMaxY = (int)Math.ceil((maxY + 4.0F) / 8.0F);
                int z = oneOfThree.square.z;

                for (int cy = chunkMinY; cy <= chunkMaxY; cy++) {
                    for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
                        IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                        if (chunk != null) {
                            for (int i = 0; i < chunk.vehicles.size(); i++) {
                                BaseVehicle vehicle = chunk.vehicles.get(i);

                                for (int y = minY; y <= maxY; y++) {
                                    for (int x = minX; x <= maxX; x++) {
                                        if (vehicle.isIntersectingSquare(x, y, z) && vehicle.isIntersectingSquare(x - (north ? 0 : 1), y - (north ? 1 : 0), z)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                return false;
            }
        }
    }

    public static boolean destroyGarageDoor(IsoObject oneOfThree) {
        int gdIndex = getGarageDoorIndex(oneOfThree);
        if (gdIndex == -1) {
            return false;
        } else {
            IsoObject prev = getGarageDoorPrev(oneOfThree);

            while (prev != null) {
                IsoObject prev2 = getGarageDoorPrev(prev);
                if (prev instanceof IsoDoor isoDoor) {
                    isoDoor.destroy();
                } else if (prev instanceof IsoThumpable isoThumpable) {
                    isoThumpable.destroy();
                }

                prev = prev2;
            }

            IsoObject next = getGarageDoorNext(oneOfThree);

            while (next != null) {
                IsoObject next2 = getGarageDoorNext(next);
                if (next instanceof IsoDoor isoDoor) {
                    isoDoor.destroy();
                } else if (next instanceof IsoThumpable isoThumpable) {
                    isoThumpable.destroy();
                }

                next = next2;
            }

            if (oneOfThree instanceof IsoDoor isoDoor) {
                isoDoor.destroy();
            } else if (oneOfThree instanceof IsoThumpable isoThumpable) {
                isoThumpable.destroy();
            }

            LuaEventManager.triggerEvent("OnContainerUpdate");
            return true;
        }
    }

    @Override
    public IsoObject getRenderEffectMaster() {
        int ddIndex = getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            IsoObject first = null;
            if (ddIndex == 2) {
                first = getDoubleDoorObject(this, 1);
            } else if (ddIndex == 3) {
                first = getDoubleDoorObject(this, 4);
            }

            if (first != null) {
                return first;
            }
        } else {
            IsoObject firstx = getGarageDoorFirst(this);
            if (firstx != null) {
                return firstx;
            }
        }

        return this;
    }

    @Override
    public int getRenderEffectObjectCount() {
        int ddIndex = getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            return 2;
        } else {
            int gdIndex = getGarageDoorIndex(this);
            if (gdIndex == -1) {
                return 1;
            } else {
                IsoObject object = this;
                int count = 1;

                while ((object = getGarageDoorPrev(object)) != null) {
                    count++;
                }

                object = this;

                while ((object = getGarageDoorNext(object)) != null) {
                    count++;
                }

                return count;
            }
        }
    }

    @Override
    public IsoObject getRenderEffectObjectByIndex(int index) {
        int ddIndex = getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            return getDoubleDoorObject(this, index);
        } else {
            int gdIndex = getGarageDoorIndex(this);
            if (gdIndex == -1) {
                return this;
            } else {
                IsoObject object = getGarageDoorFirst(this);

                for (int count = 0; object != null && count != index; count++) {
                    object = getGarageDoorNext(object);
                }

                return object;
            }
        }
    }

    public String getThumpSound() {
        if (this.sprite != null && this.sprite.getProperties().has("ThumpSound")) {
            String soundName = this.sprite.getProperties().get("ThumpSound");
            if (!StringUtils.isNullOrWhitespace(soundName)) {
                return soundName;
            }
        }

        String var3 = this.getSoundPrefix();
        switch (var3) {
            case "MetalGate":
                return "ZombieThumpChainlinkFence";
            case "MetalPoleGate":
            case "MetalPoleGateDouble":
                return "ZombieThumpMetalPoleGate";
            case "GarageDoor":
                return "ZombieThumpGarageDoor";
            case "MetalDoor":
            case "PrisonMetalDoor":
                return "ZombieThumpMetal";
            case "SlidingGlassDoor":
                return "ZombieThumpWindow";
            default:
                return "ZombieThumpGeneric";
        }
    }

    public String getSoundPrefix() {
        if (this.closedSprite == null) {
            return "WoodDoor";
        } else {
            PropertyContainer props = this.closedSprite.getProperties();
            return props.has("DoorSound") ? props.get("DoorSound") : "WoodDoor";
        }
    }

    private void playDoorSound(BaseCharacterSoundEmitter emitter, String suffix) {
        emitter.playSoundImpl(this.getSoundPrefix() + suffix, this);
    }

    @Override
    public SpriteModel getSpriteModel() {
        int ddIndex = getDoubleDoorIndex(this);
        return (ddIndex == 1 || ddIndex == 4) && getDoubleDoorObject(this, ddIndex == 1 ? 2 : 3) == null ? null : super.getSpriteModel();
    }

    public static enum DoorType {
        WeakWooden,
        StrongWooden;
    }
}
