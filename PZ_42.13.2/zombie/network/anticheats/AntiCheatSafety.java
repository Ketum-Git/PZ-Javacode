// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SafetySystemManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.iso.areas.NonPvpZone;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

public class AntiCheatSafety extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatSafety.IAntiCheat field = (AntiCheatSafety.IAntiCheat)packet;
        boolean updateDelay = SafetySystemManager.checkUpdateDelay(field.getWielder(), field.getTarget());
        return updateDelay ? result : checkPVP(field.getWielder(), field.getTarget());
    }

    private static String checkPVP(IsoGameCharacter owner, IsoMovingObject obj) {
        IsoPlayer wielder = Type.tryCastTo(owner, IsoPlayer.class);
        IsoPlayer target = Type.tryCastTo(obj, IsoPlayer.class);
        if (wielder == null) {
            return "wielder not found";
        } else if (target == null) {
            return "target not found";
        } else if (target.isGodMod()) {
            return "target is in god-mode";
        } else if (!ServerOptions.instance.pvp.getValue()) {
            return "PVP is disabled";
        } else if (ServerOptions.instance.safetySystem.getValue() && owner.getSafety().isEnabled() && target.getSafety().isEnabled()) {
            return "safety is enabled";
        } else if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(wielder.getX()), PZMath.fastfloor(wielder.getY())) != null) {
            long safetyTimestamp = SafetySystemManager.getSafetyTimestamp(wielder.getUsername());
            return System.currentTimeMillis() - safetyTimestamp < SafetySystemManager.getSafetyDelay() ? "" : "wiedler is in non-pvp zone";
        } else if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(target.getX()), PZMath.fastfloor(target.getY())) != null) {
            return "target is in non-pvp zone";
        } else {
            return !wielder.isFactionPvp() && !target.isFactionPvp() && Faction.isInSameFaction(wielder, target) ? "faction pvp is disabled" : null;
        }
    }

    public interface IAntiCheat {
        IsoGameCharacter getTarget();

        IsoPlayer getWielder();
    }
}
