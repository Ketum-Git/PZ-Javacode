// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
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
}
