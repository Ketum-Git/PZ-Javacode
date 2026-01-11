// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.ai.states.PathFindState;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.pathfind.PathFindBehavior2;

public class MPDebugAI {
    private static final Vector2 tempo = new Vector2();
    private static final Vector2 tempo2 = new Vector2();

    public static IsoPlayer getNearestPlayer(IsoPlayer target) {
        IsoPlayer player = null;

        for (IsoPlayer p : GameClient.IDToPlayerMap.values()) {
            if (p != target && (player == null || player.getDistanceSq(target) > p.getDistanceSq(target))) {
                player = p;
            }
        }

        return player;
    }

    public static boolean updateMovementFromInput(IsoPlayer player, IsoPlayer.MoveVars moveVars) {
        if (GameClient.client
            && player.isLocalPlayer()
            && (DebugOptions.instance.multiplayer.debug.attackPlayer.getValue() || DebugOptions.instance.multiplayer.debug.followPlayer.getValue())) {
            IsoPlayer p = getNearestPlayer(player);
            if (p != null) {
                Vector2 move = new Vector2(p.getX() - player.getX(), player.getY() - p.getY());
                move.rotate((float) (-Math.PI / 4));
                move.normalize();
                moveVars.moveX = move.x;
                moveVars.moveY = move.y;
                moveVars.newFacing = IsoDirections.fromAngle(move);
                if (p.getDistanceSq(player) > 100.0F) {
                    player.removeFromSquare();
                    player.setX(p.realx);
                    player.setY(p.realy);
                    player.setZ(p.realz);
                    player.setLastX(p.realx);
                    player.setLastY(p.realy);
                    player.setLastZ(p.realz);
                    player.ensureOnTile();
                } else if (p.getDistanceSq(player) > 50.0F) {
                    player.setRunning(true);
                    player.setSprinting(true);
                } else if (p.getDistanceSq(player) > 5.0F) {
                    player.setRunning(true);
                } else if (p.getDistanceSq(player) < 1.25F) {
                    moveVars.moveX = 0.0F;
                    moveVars.moveY = 0.0F;
                }
            }

            PathFindBehavior2 pfb2 = player.getPathFindBehavior2();
            if (moveVars.moveX == 0.0F && moveVars.moveY == 0.0F && player.getPath2() != null && pfb2.isStrafing() && !pfb2.stopping) {
                Vector2 v = tempo.set(pfb2.getTargetX() - player.getX(), pfb2.getTargetY() - player.getY());
                Vector2 u = tempo2.set(-1.0F, 0.0F);
                float uLen = 1.0F;
                float vDotU = v.dot(u);
                float forwardComponent = vDotU / 1.0F;
                u = tempo2.set(0.0F, -1.0F);
                vDotU = v.dot(u);
                float rightComponent = vDotU / 1.0F;
                tempo.set(rightComponent, forwardComponent);
                tempo.normalize();
                tempo.rotate((float) (Math.PI / 4));
                moveVars.moveX = tempo.x;
                moveVars.moveY = tempo.y;
            }

            if (moveVars.moveX != 0.0F || moveVars.moveY != 0.0F) {
                if (player.stateMachine.getCurrent() == PathFindState.instance()) {
                    player.setDefaultState();
                }

                player.setJustMoved(true);
                player.setMoveDelta(1.0F);
                if (player.isStrafing()) {
                    tempo.set(moveVars.moveX, moveVars.moveY);
                    tempo.normalize();
                    float angle = player.legsSprite.modelSlot.model.animPlayer.getRenderedAngle();
                    angle = (float)(angle + (Math.PI / 4));
                    if (angle > Math.PI * 2) {
                        angle = (float)(angle - (Math.PI * 2));
                    }

                    if (angle < 0.0F) {
                        angle = (float)(angle + (Math.PI * 2));
                    }

                    tempo.rotate(angle);
                    moveVars.strafeX = tempo.x;
                    moveVars.strafeY = tempo.y;
                } else {
                    tempo.set(moveVars.moveX, -moveVars.moveY);
                    tempo.normalize();
                    tempo.rotate((float) (-Math.PI / 4));
                    player.setForwardDirection(tempo);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static boolean updateInputState(IsoPlayer player, IsoPlayer.InputState state) {
        if (GameClient.client && player.isLocalPlayer()) {
            if (DebugOptions.instance.multiplayer.debug.attackPlayer.getValue()) {
                state.reset();
                IsoPlayer p = getNearestPlayer(player);
                if (p != null) {
                    state.isCharging = true;
                    if (p.getDistanceSq(player) < 0.5F) {
                        state.melee = true;
                        state.isAttacking = true;
                    }
                }

                return true;
            }

            if (DebugOptions.instance.multiplayer.debug.followPlayer.getValue()) {
                state.reset();
                return true;
            }
        }

        return false;
    }
}
