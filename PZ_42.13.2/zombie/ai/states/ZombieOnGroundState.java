// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.Model;
import zombie.iso.Vector3;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.NetworkVariables;

@UsedFromLua
public final class ZombieOnGroundState extends State {
    private static final ZombieOnGroundState _instance = new ZombieOnGroundState();
    static Vector3 tempVector = new Vector3();
    static Vector3 tempVectorBonePos = new Vector3();

    public static ZombieOnGroundState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie ownerZombie = (IsoZombie)owner;
        ownerZombie.setCollidable(false);
        if (!ownerZombie.isDead()) {
            ownerZombie.setOnFloor(true);
        }

        if (!ownerZombie.isDead() && !ownerZombie.isFakeDead()) {
            ownerZombie.setStaggerBack(false);
            ownerZombie.setKnockedDown(false);
            if (ownerZombie.isAlive()) {
                ownerZombie.setHitReaction("");
            }

            ownerZombie.setEatBodyTarget(null, false);
            ownerZombie.setSitAgainstWall(false);
            if (!ownerZombie.isBecomeCrawler()) {
                if (!"Tutorial".equals(Core.gameMode)) {
                    startReanimateTimer(ownerZombie);
                }

                if (GameClient.client && ownerZombie.isReanimatedPlayer()) {
                    IsoDeadBody.removeDeadBody(ownerZombie.networkAi.reanimatedBodyId);
                }

                ownerZombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
            }
        } else {
            if (GameClient.client) {
                ownerZombie.networkAi.extraUpdate();
            }

            ownerZombie.die();
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        if (!zombie.isDead() && !zombie.isFakeDead()) {
            if (zombie.isReanimatedForGrappleOnly() && !zombie.isBeingGrappled()) {
                zombie.die();
            } else if (zombie.isBecomeCrawler()) {
                if (!zombie.isBeingSteppedOn() && !zombie.isUnderVehicle()) {
                    zombie.setCrawler(true);
                    zombie.setCanWalk(false);
                    zombie.setReanimate(true);
                    zombie.setBecomeCrawler(false);
                }
            } else {
                if (zombie.hasAnimationPlayer()) {
                    zombie.getAnimationPlayer().setTargetToAngle();
                }

                zombie.setReanimateTimer(zombie.getReanimateTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
                if (zombie.getReanimateTimer() <= 2.0F) {
                    if (zombie.isBeingSteppedOn() && zombie.getReanimatedPlayer() == null) {
                        startReanimateTimer(zombie);
                    }

                    if (!zombie.isLocal()
                        && NetworkVariables.ZombieState.FallDown != zombie.realState
                        && NetworkVariables.ZombieState.OnGround != zombie.realState
                        && NetworkVariables.ZombieState.HitReaction != zombie.realState
                        && NetworkVariables.ZombieState.StaggerBack != zombie.realState) {
                        zombie.setReanimateTimer(0.0F);
                    }
                }

                if (GameClient.client && zombie.isReanimatedPlayer()) {
                    IsoDeadBody.removeDeadBody(zombie.networkAi.reanimatedBodyId);
                }
            }
        } else if (!zombie.isRagdollSimulationActive()) {
            zombie.die();
        }
    }

    public static boolean isCharacterStandingOnOther(IsoGameCharacter chrStanding, IsoGameCharacter chrProne) {
        AnimationPlayer animPlayer = chrProne.getAnimationPlayer();
        int bone = DoCollisionBoneCheck(chrStanding, chrProne, animPlayer.getSkinningBoneIndex("Bip01_Spine", -1), 0.32F);
        if (bone == -1) {
            bone = DoCollisionBoneCheck(chrStanding, chrProne, animPlayer.getSkinningBoneIndex("Bip01_L_Calf", -1), 0.18F);
        }

        if (bone == -1) {
            bone = DoCollisionBoneCheck(chrStanding, chrProne, animPlayer.getSkinningBoneIndex("Bip01_R_Calf", -1), 0.18F);
        }

        if (bone == -1) {
            bone = DoCollisionBoneCheck(chrStanding, chrProne, animPlayer.getSkinningBoneIndex("Bip01_Head", -1), 0.28F);
        }

        return bone > -1;
    }

    private static int DoCollisionBoneCheck(IsoGameCharacter chrStanding, IsoGameCharacter chrProne, int bone, float tempoLengthTest) {
        float weaponLength = 0.3F;
        Model.BoneToWorldCoords(chrProne, bone, tempVectorBonePos);

        for (int x = 1; x <= 10; x++) {
            float delta = x / 10.0F;
            tempVector.x = chrStanding.getX();
            tempVector.y = chrStanding.getY();
            tempVector.z = chrStanding.getZ();
            tempVector.x = tempVector.x + chrStanding.getForwardDirectionX() * 0.3F * delta;
            tempVector.y = tempVector.y + chrStanding.getForwardDirectionY() * 0.3F * delta;
            tempVector.x = tempVectorBonePos.x - tempVector.x;
            tempVector.y = tempVectorBonePos.y - tempVector.y;
            tempVector.z = 0.0F;
            boolean hit = tempVector.getLength() < tempoLengthTest;
            if (hit) {
                return bone;
            }
        }

        return -1;
    }

    public static void startReanimateTimer(IsoZombie ownerZombie) {
        ownerZombie.setReanimateTimer(Rand.Next(60) + 30);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
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
