// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.ZombieThumpManager;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class ThumpState extends State {
    private static final ThumpState _instance = new ThumpState();

    public static ThumpState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (!GameClient.client || owner.isLocal()) {
            switch (Rand.Next(3)) {
                case 0:
                    owner.setVariable("ThumpType", "DoorClaw");
                    break;
                case 1:
                    owner.setVariable("ThumpType", "Door");
                    break;
                case 2:
                    owner.setVariable("ThumpType", "DoorBang");
            }
        }

        if (GameClient.client && owner.isLocal()) {
            INetworkPacket.send(PacketTypes.PacketType.Thump, owner);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        Thumpable thump = owner.getThumpTarget();
        if (thump == null) {
            this.exit(owner);
        } else {
            if (thump instanceof IsoObject isoObject) {
                if (isoObject.getSquare() == null) {
                    this.exit(owner);
                    return;
                }

                owner.faceThisObject(isoObject);
            }

            this.slideAwayFromEdge(owner, thump);
            boolean fastForward = GameServer.server && GameServer.fastForward || !GameServer.server && IsoPlayer.allPlayersAsleep();
            if (fastForward || owner.getActionContext().hasEventOccurred("thumpframe")) {
                owner.getActionContext().clearEvent("thumpframe");
                owner.setTimeThumping(owner.getTimeThumping() + 1);
                if (zombie.timeSinceSeenFlesh < 5.0F) {
                    owner.setTimeThumping(0);
                }

                int count = 1;
                if (owner.getCurrentSquare() != null) {
                    count = owner.getCurrentSquare().getMovingObjects().size();
                }

                for (int n = 0; n < count && this.isThumpTargetValid(owner, owner.getThumpTarget()); n++) {
                    owner.getThumpTarget().Thump(owner);
                }

                Thumpable thumpableFor = owner.getThumpTarget() == null ? null : owner.getThumpTarget().getThumpableFor(owner);
                boolean listener = GameServer.server || SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 20.0F);
                if (listener && !IsoPlayer.allPlayersAsleep()) {
                    if (thumpableFor instanceof IsoWindow) {
                        zombie.setThumpFlag(Rand.Next(3) == 0 ? 2 : 3);
                        zombie.setThumpCondition(thumpableFor.getThumpCondition());
                        if (!GameServer.server) {
                            ZombieThumpManager.instance.addCharacter(zombie);
                        }
                    } else if (thumpableFor != null) {
                        String thumpSound = "ZombieThumpGeneric";
                        IsoBarricade barricade = Type.tryCastTo(thumpableFor, IsoBarricade.class);
                        if (barricade == null || !barricade.isMetal() && !barricade.isMetalBar()) {
                            if (barricade != null && barricade.getNumPlanks() > 0) {
                                if (owner.isVariable("ThumpType", "DoorClaw")) {
                                    thumpSound = "ZombieThumpWood";
                                }
                            } else if (thumpableFor instanceof IsoDoor door) {
                                thumpSound = door.getThumpSound();
                                if ("WoodDoor".equalsIgnoreCase(door.getSoundPrefix()) && owner.isVariable("ThumpType", "DoorClaw")) {
                                    thumpSound = "ZombieThumpWood";
                                }
                            } else if (thumpableFor instanceof IsoThumpable thumpable) {
                                thumpSound = thumpable.getThumpSound();
                                if (thumpable.isDoor() && "WoodDoor".equalsIgnoreCase(thumpable.getSoundPrefix()) && owner.isVariable("ThumpType", "DoorClaw")) {
                                    thumpSound = "ZombieThumpWood";
                                }
                            } else if (thumpableFor instanceof IsoObject object && object.sprite != null && object.sprite.getProperties().has("ThumpSound")) {
                                String soundName = object.sprite.getProperties().get("ThumpSound");
                                if (!StringUtils.isNullOrWhitespace(soundName)) {
                                    thumpSound = soundName;
                                }
                            }
                        } else {
                            thumpSound = "ZombieThumpMetal";
                        }

                        if ("ZombieThumpGeneric".equals(thumpSound)) {
                            zombie.setThumpFlag(1);
                        } else if ("ZombieThumpWindow".equals(thumpSound)) {
                            zombie.setThumpFlag(3);
                        } else if ("ZombieThumpWindowExtra".equals(thumpSound)) {
                            zombie.setThumpFlag(2);
                        } else if ("ZombieThumpMetal".equals(thumpSound)) {
                            zombie.setThumpFlag(4);
                        } else if ("ZombieThumpGarageDoor".equals(thumpSound)) {
                            zombie.setThumpFlag(5);
                        } else if ("ZombieThumpChainlinkFence".equals(thumpSound)) {
                            zombie.setThumpFlag(6);
                        } else if ("ZombieThumpMetalPoleGate".equals(thumpSound)) {
                            zombie.setThumpFlag(7);
                        } else if ("ZombieThumpWood".equals(thumpSound)) {
                            zombie.setThumpFlag(8);
                        } else {
                            zombie.setThumpFlag(1);
                        }

                        zombie.setThumpCondition(thumpableFor.getThumpCondition());
                        if (!GameServer.server) {
                            ZombieThumpManager.instance.addCharacter(zombie);
                        }
                    }
                }
            }

            if (!this.isThumpTargetValid(owner, owner.getThumpTarget())) {
                owner.setThumpTarget(null);
                owner.setTimeThumping(0);
                if (thump instanceof IsoWindow isoWindow && isoWindow.canClimbThrough(owner)) {
                    owner.climbThroughWindow(isoWindow);
                } else {
                    if (thump instanceof IsoDoor doorx && (doorx.open || thump.isDestroyed())) {
                        IsoGridSquare sq = doorx.getSquare();
                        IsoGridSquare sq2 = doorx.getOppositeSquare();
                        if (this.lungeThroughDoor(zombie, sq, sq2)) {
                            return;
                        }
                    }

                    if (thump instanceof IsoThumpable doorxx && doorxx.isDoor() && (doorxx.open || thump.isDestroyed())) {
                        IsoGridSquare sq = doorxx.getSquare();
                        IsoGridSquare sq2 = doorxx.getInsideSquare();
                        if (this.lungeThroughDoor(zombie, sq, sq2)) {
                            return;
                        }
                    }

                    if (zombie.lastTargetSeenX != -1) {
                        owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
                    }
                }
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setThumpTarget(null);
        ((IsoZombie)owner).setThumpTimer(200);
        if (GameClient.client && owner.isLocal()) {
            INetworkPacket.send(PacketTypes.PacketType.Thump, owner);
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("ThumpFrame")) {
        }
    }

    private void slideAwayFromEdge(IsoGameCharacter owner, Thumpable target) {
        if (target == null) {
            this.exit(owner);
        } else {
            boolean bSolid = false;
            boolean bNorth = false;
            if (!(target instanceof BaseVehicle)) {
                if (target instanceof IsoObject object) {
                    IsoGridSquare square = object.getSquare();
                    if (!(target instanceof IsoBarricade barricade)) {
                        if (target instanceof IsoDoor door) {
                            bNorth = door.getNorth();
                        } else if (target instanceof IsoThumpable thumpable) {
                            bSolid = thumpable.isBlockAllTheSquare();
                            bNorth = thumpable.getNorth();
                        } else if (target instanceof IsoWindow window) {
                            bNorth = window.getNorth();
                        } else if (target instanceof IsoWindowFrame windowFrame) {
                            bNorth = windowFrame.getNorth();
                        }
                    } else {
                        bNorth = barricade.getDir() == IsoDirections.N || barricade.getDir() == IsoDirections.S;
                    }

                    float dist = 0.4F;
                    Thumpable thumpable = target.getThumpableFor(owner);
                    if (thumpable instanceof IsoBarricade barricadex
                        && target instanceof BarricadeAble barricadeAble
                        && IsoBarricade.GetBarricadeForCharacter(barricadeAble, owner) == thumpable) {
                        dist = 0.47F;
                    }

                    if (square == null) {
                        this.exit(owner);
                        return;
                    }

                    if (bSolid) {
                        if (owner.getY() < square.y) {
                            this.slideAwayFromEdgeN(owner, square.y, dist);
                        } else if (owner.getY() > square.y + 1) {
                            this.slideAwayFromEdgeS(owner, square.y + 1, dist);
                        }

                        if (owner.getX() < square.x) {
                            this.slideAwayFromEdgeW(owner, square.x, dist);
                        } else if (owner.getX() > square.x + 1) {
                            this.slideAwayFromEdgeE(owner, square.x + 1, dist);
                        }
                    } else if (bNorth) {
                        if (owner.getY() < square.y) {
                            this.slideAwayFromEdgeN(owner, square.y, dist);
                        } else {
                            this.slideAwayFromEdgeS(owner, square.y, dist);
                        }
                    } else if (owner.getX() < square.x) {
                        this.slideAwayFromEdgeW(owner, square.x, dist);
                    } else {
                        this.slideAwayFromEdgeE(owner, square.x, dist);
                    }
                }
            }
        }
    }

    private void slideAwayFromEdgeN(IsoGameCharacter owner, int squareY, float dist) {
        if (owner.getY() > squareY - dist) {
            owner.setNextY(squareY - dist);
        }
    }

    private void slideAwayFromEdgeS(IsoGameCharacter owner, int squareY, float dist) {
        if (owner.getY() < squareY + dist) {
            owner.setNextY(squareY + dist);
        }
    }

    private void slideAwayFromEdgeW(IsoGameCharacter owner, int squareX, float dist) {
        if (owner.getX() > squareX - dist) {
            owner.setNextX(squareX - dist);
        }
    }

    private void slideAwayFromEdgeE(IsoGameCharacter owner, int squareX, float dist) {
        if (owner.getX() < squareX + dist) {
            owner.setNextX(squareX + dist);
        }
    }

    private IsoPlayer findPlayer(int x1, int x2, int y1, int y2, int z) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                if (sq != null) {
                    for (int i = 0; i < sq.getMovingObjects().size(); i++) {
                        IsoMovingObject o = sq.getMovingObjects().get(i);
                        if (o instanceof IsoPlayer isoPlayer && !isoPlayer.isGhostMode()) {
                            return isoPlayer;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean lungeThroughDoor(IsoZombie z, IsoGridSquare sq, IsoGridSquare sq2) {
        if (sq != null && sq2 != null) {
            boolean north = sq.getY() > sq2.getY();
            IsoGridSquare sq3 = null;
            IsoPlayer player = null;
            if (z.getCurrentSquare() == sq) {
                sq3 = sq2;
                if (north) {
                    player = this.findPlayer(sq2.getX() - 1, sq2.getX() + 1, sq2.getY() - 1, sq2.getY(), sq2.getZ());
                } else {
                    player = this.findPlayer(sq2.getX() - 1, sq2.getX(), sq2.getY() - 1, sq2.getY() + 1, sq2.getZ());
                }
            } else if (z.getCurrentSquare() == sq2) {
                sq3 = sq;
                if (north) {
                    player = this.findPlayer(sq.getX() - 1, sq.getX() + 1, sq.getY(), sq.getY() + 1, sq.getZ());
                } else {
                    player = this.findPlayer(sq.getX(), sq.getX() + 1, sq.getY() - 1, sq.getY() + 1, sq.getZ());
                }
            }

            if (player != null
                && !LosUtil.lineClearCollide(
                    sq3.getX(),
                    sq3.getY(),
                    sq3.getZ(),
                    PZMath.fastfloor(player.getX()),
                    PZMath.fastfloor(player.getY()),
                    PZMath.fastfloor(player.getZ()),
                    false
                )) {
                z.setTarget(player);
                z.vectorToTarget.x = player.getX();
                z.vectorToTarget.y = player.getY();
                z.vectorToTarget.x = z.vectorToTarget.x - z.getX();
                z.vectorToTarget.y = z.vectorToTarget.y - z.getY();
                z.timeSinceSeenFlesh = 0.0F;
                z.setThumpTarget(null);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static int getFastForwardDamageMultiplier() {
        GameTime gt = GameTime.getInstance();
        if (GameServer.server) {
            return (int)(GameServer.fastForward ? ServerOptions.instance.fastForwardMultiplier.getValue() / gt.getDeltaMinutesPerDay() : 1.0);
        } else if (GameClient.client) {
            return (int)(GameClient.fastForward ? ServerOptions.instance.fastForwardMultiplier.getValue() / gt.getDeltaMinutesPerDay() : 1.0);
        } else {
            return IsoPlayer.allPlayersAsleep() ? (int)(200.0F * (30.0F / PerformanceSettings.getLockFPS()) / 1.6F) : (int)gt.getTrueMultiplier();
        }
    }

    private boolean isThumpTargetValid(IsoGameCharacter owner, Thumpable thumpable) {
        if (thumpable == null) {
            return false;
        } else if (thumpable.isDestroyed()) {
            return false;
        } else if (thumpable instanceof IsoObject obj) {
            if (thumpable instanceof BaseVehicle) {
                return obj.getMovingObjectIndex() != -1;
            } else if (obj.getObjectIndex() == -1) {
                return false;
            } else {
                int wx = obj.getSquare().getX() / 8;
                int wy = obj.getSquare().getY() / 8;
                IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(wx, wy) : IsoWorld.instance.currentCell.getChunk(wx, wy);
                return chunk == null ? false : thumpable.getThumpableFor(owner) != null;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isSyncOnEnter() {
        return false;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }
}
