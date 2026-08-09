// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SafetySystemManager;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.areas.NonPvpZone;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

public class AntiCheatSafety extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (packet instanceof AntiCheatSafety.IAntiCheat field) {
            IsoPlayer wielder = Type.tryCastTo(field.getWielder(), IsoPlayer.class);
            if (wielder == null) {
                return "wielder not found";
            } else {
                IsoPlayer target = Type.tryCastTo(field.getTarget(), IsoPlayer.class);
                if (target == null) {
                    return "target not found";
                } else if (target.isGodMod()) {
                    return "target is in god-mode";
                } else if (!ServerOptions.instance.pvp.getValue()) {
                    return "server pvp is disabled";
                } else {
                    boolean isInSameFaction = Faction.isInSameFaction(wielder, target);
                    if (isInSameFaction && !field.isMelee()) {
                        return "only melee pvp is allowed for faction members";
                    } else if (isInSameFaction && isFactionPvPBlocked(wielder, target)) {
                        return "faction melee pvp is disabled by players";
                    } else if (ServerOptions.instance.safetySystem.getValue() && isPvPBlocked(wielder, target)) {
                        return "pvp is disabled by players";
                    } else {
                        boolean isWielderInNonPvpZone = NonPvpZone.isInNonPvpZone(wielder);
                        if (isWielderInNonPvpZone && isInNonPvPBlockedState(wielder)) {
                            return "wielder is in non-pvp zone";
                        } else if (isWielderInNonPvpZone) {
                            return "";
                        } else {
                            boolean isTargetInNonPvpZone = NonPvpZone.isInNonPvpZone(target);
                            if (isTargetInNonPvpZone && isInNonPvPBlockedState(target)) {
                                return "target is in non-pvp zone";
                            } else {
                                return isTargetInNonPvpZone ? "" : result;
                            }
                        }
                    }
                }
            }
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    private static boolean isPvPBlocked(IsoPlayer wielder, IsoPlayer target) {
        return wielder.getSafety().isEnabled() && target.getSafety().isEnabled();
    }

    private static boolean isFactionPvPBlocked(IsoPlayer wielder, IsoPlayer target) {
        return !wielder.isFactionPvp() && !target.isFactionPvp();
    }

    private static boolean isInNonPvPBlockedState(IsoPlayer player) {
        return SafetySystemManager.isPlayerSafetyChangeDelayed(player);
    }

    public interface IAntiCheat {
        IsoGameCharacter getTarget();

        IsoPlayer getWielder();

        default boolean isMelee() {
            return false;
        }
    }
}
