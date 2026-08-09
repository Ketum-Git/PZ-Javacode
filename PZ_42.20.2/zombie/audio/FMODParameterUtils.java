// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;
import zombie.scripting.objects.CharacterTrait;

public final class FMODParameterUtils {
    public static IsoGameCharacter getFirstListener() {
        IsoGameCharacter character = null;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player != null
                && (
                    character == null
                        || character.isDead() && player.isAlive()
                        || character.hasTrait(CharacterTrait.DEAF) && !player.hasTrait(CharacterTrait.DEAF)
                )) {
                character = player;
            }
        }

        return character;
    }

    public static IsoPlayer getClosestListener(float soundX, float soundY, float soundZ) {
        if (IsoPlayer.numPlayers == 1) {
            IsoPlayer chr = IsoPlayer.players[0];
            return chr != null && chr.getCurrentSquare() != null && !chr.hasTrait(CharacterTrait.DEAF) ? chr : null;
        } else {
            float minDist = Float.MAX_VALUE;
            IsoPlayer closest = null;

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer chr = IsoPlayer.players[i];
                if (chr != null && chr.getCurrentSquare() != null && !chr.hasTrait(CharacterTrait.DEAF)) {
                    float px = chr.getX();
                    float py = chr.getY();
                    float pz = chr.getZ();
                    float distSq = IsoUtils.DistanceToSquared(px, py, pz * 3.0F, soundX, soundY, soundZ * 3.0F);
                    distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0F);
                    if (distSq < minDist) {
                        minDist = distSq;
                        closest = chr;
                    }
                }
            }

            return closest;
        }
    }

    public static float getClosestListenerDistanceSquared(float soundX, float soundY, float soundZ) {
        IsoGameCharacter closest = getClosestListener(soundX, soundY, soundZ);
        return closest == null
            ? Float.MAX_VALUE
            : IsoUtils.DistanceToSquared(closest.getX(), closest.getY(), closest.getZ() * 3.0F, soundX, soundY, soundZ * 3.0F);
    }
}
