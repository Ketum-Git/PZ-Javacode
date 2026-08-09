// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.Map;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

@UsedFromLua
public final class SmashWindowState extends State {
    private static final SmashWindowState INSTANCE = new SmashWindowState();
    public static final State.Param<Boolean> SCRATCHED = State.Param.ofBool("scratched", false);
    public static final State.Param<IsoWindow> ISO_WINDOW = State.Param.of("iso_window", IsoWindow.class);
    public static final State.Param<VehicleWindow> VEHICLE_WINDOW = State.Param.of("vehicle_window", VehicleWindow.class);
    public static final State.Param<BaseVehicle> VEHICLE = State.Param.of("vehicle", BaseVehicle.class);
    public static final State.Param<VehiclePart> VEHICLE_PART = State.Param.of("vehicle_part", VehiclePart.class);
    public static final State.Param<Boolean> CLIMB_THROUGH_WINDOW = State.Param.ofBool("climb_through_window", false);

    public static SmashWindowState instance() {
        return INSTANCE;
    }

    private SmashWindowState() {
        super(true, false, true, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setVariable("bSmashWindow", true);
        HandWeapon weapon = Type.tryCastTo(owner.getPrimaryHandItem(), HandWeapon.class);
        if (weapon != null && weapon.isRanged()) {
            owner.playSound("AttackShove");
        } else if (weapon != null && !StringUtils.isNullOrWhitespace(weapon.getSwingSound())) {
            owner.playSound(weapon.getSwingSound());
        }

        if (GameClient.client) {
            boolean scratch = !(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon);
            owner.set(SCRATCHED, scratch);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (!(owner.get(ISO_WINDOW) instanceof IsoWindow) && !(owner.get(VEHICLE_WINDOW) instanceof VehicleWindow)) {
            owner.setVariable("bSmashWindow", false);
        } else {
            IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
            if (player.pressedMovement(false) || player.pressedCancelAction()) {
                owner.setVariable("bSmashWindow", false);
            } else if (owner.getVariableBoolean("bSmashWindow")) {
                if (owner.get(ISO_WINDOW) instanceof IsoWindow window) {
                    if (window.getObjectIndex() == -1 || window.isDestroyed() && !owner.getVariableBoolean("OwnerSmashedIt")) {
                        owner.setVariable("bSmashWindow", false);
                        return;
                    }

                    if (window.isNorth()) {
                        if (window.getSquare().getY() < owner.getY()) {
                            owner.setDir(IsoDirections.N);
                        } else {
                            owner.setDir(IsoDirections.S);
                        }
                    } else if (window.getSquare().getX() < owner.getX()) {
                        owner.setDir(IsoDirections.W);
                    } else {
                        owner.setDir(IsoDirections.E);
                    }
                } else if (owner.get(VEHICLE_WINDOW) instanceof VehicleWindow window) {
                    BaseVehicle vehicle = owner.get(VEHICLE);
                    owner.faceThisObject(vehicle);
                    if (window.isDestroyed() && !owner.getVariableBoolean("OwnerSmashedIt")) {
                        owner.setVariable("bSmashWindow", false);
                    }
                }
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.clearVariable("bSmashWindow");
        owner.clearVariable("OwnerSmashedIt");
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (owner.get(ISO_WINDOW) instanceof IsoWindow window) {
            if (event.eventName.equalsIgnoreCase("AttackCollisionCheck")) {
                owner.setVariable("OwnerSmashedIt", true);
                IsoPlayer.getInstance().contextPanic = 0.0F;
                if (!GameClient.client) {
                    window.WeaponHit(owner, null);
                    if (!(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon)) {
                        owner.getBodyDamage().setScratchedWindow();
                        IsoPlayer player = (IsoPlayer)owner;
                        player.playerVoiceSound("PainFromGlassCut");
                    }
                }
            } else if (event.eventName.equalsIgnoreCase("ActiveAnimFinishing")) {
                owner.setVariable("bSmashWindow", false);
                if (owner.get(CLIMB_THROUGH_WINDOW)) {
                    owner.climbThroughWindow(window);
                }
            }
        } else if (owner.get(VEHICLE_WINDOW) instanceof VehicleWindow windowx) {
            if (event.eventName.equalsIgnoreCase("AttackCollisionCheck")) {
                owner.setVariable("OwnerSmashedIt", true);
                IsoPlayer.getInstance().contextPanic = 0.0F;
                if (((IsoPlayer)owner).isLocalPlayer() && !GameClient.client) {
                    windowx.hit(owner);
                    if (!(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon)) {
                        owner.getBodyDamage().setScratchedWindow();
                        IsoPlayer player = (IsoPlayer)owner;
                        player.playerVoiceSound("PainFromGlassCut");
                    }
                }
            } else if (event.eventName.equalsIgnoreCase("ActiveAnimFinishing")) {
                owner.setVariable("bSmashWindow", false);
            }
        }
    }

    /**
     * @return TRUE if this state handles the "Cancel Action" key or the B controller button.
     */
    @Override
    public boolean isDoingActionThatCanBeCancelled() {
        return true;
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        if (SCRATCHED.fromDelegate(delegate)) {
            owner.getBodyDamage().setScratchedWindow();
        }
    }
}
