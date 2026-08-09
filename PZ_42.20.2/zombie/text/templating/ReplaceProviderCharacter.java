// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.text.templating;

import java.util.Objects;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;

/**
 * TurboTuTone.
 *  Example of ReplaceProvider that registers firstname and lastname keys for the supplied character.
 */
@UsedFromLua
public class ReplaceProviderCharacter extends ReplaceProvider {
    public ReplaceProviderCharacter(final IsoGameCharacter character) {
        this.addReplacer(
            "firstname",
            new IReplace() {
                {
                    Objects.requireNonNull(ReplaceProviderCharacter.this);
                }

                @Override
                public String getString() {
                    return character != null && character.getDescriptor() != null && character.getDescriptor().getForename() != null
                        ? character.getDescriptor().getForename()
                        : "Bob";
                }
            }
        );
        this.addReplacer(
            "lastname",
            new IReplace() {
                {
                    Objects.requireNonNull(ReplaceProviderCharacter.this);
                }

                @Override
                public String getString() {
                    return character != null && character.getDescriptor() != null && character.getDescriptor().getSurname() != null
                        ? character.getDescriptor().getSurname()
                        : "Smith";
                }
            }
        );
    }
}
