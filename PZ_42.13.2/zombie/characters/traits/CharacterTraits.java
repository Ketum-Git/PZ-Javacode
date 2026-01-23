// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.traits;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferWriter;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public final class CharacterTraits {
    private final Map<CharacterTrait, Boolean> traits = new LinkedHashMap<>();
    private final List<CharacterTrait> knownTraits = new ArrayList<>();
    private static final float EmaciatedDamageDealtReductionModifier = 0.4F;
    private static final float VeryUnderweightDamageDealtReductionModifier = 0.6F;
    private static final float UnderweightDamageDealtReductionModifier = 0.8F;
    private static final float TraitDamageDealtReductionDefaultModifier = 1.0F;
    public static final int ObeseStrengthPenalty = 2;
    public static final int OverweightStrengthPenalty = 1;
    public static final int AllThumbsStrengthPenalty = 1;
    public static final int DextrousStrengthBonus = 1;
    public static final int BurglarStrengthBonus = 1;
    public static final int GymnastStrengthBonus = 1;
    private static final float AsthmaticEnduranceLossModifier = 1.2F;
    private static final float TraitEnduranceLossDefaultModifier = 1.0F;
    private static final float OutdoorsmanWeatherPenaltyModifier = 1.0F;
    private static final float TraitWeatherPenaltyDefaultModifier = 1.5F;
    public static final float ObeseClimbingPenalty = -25.0F;
    public static final float OverweightClimbingPenalty = -15.0F;
    public static final float ClumsyClimbingPenaltyDivisor = 2.0F;
    public static final float AwkwardGlovesClimbingPenaltyDivisor = 2.0F;
    public static final float RegularGlovesClimbingBonus = 4.0F;
    public static final float PerkClimbingBonusMultiplier = 2.0F;
    public static final float EnduranceClimbingPenaltyMultiplier = -5.0F;
    public static final float DrunkClimbingPenaltyMultiplier = -8.0F;
    public static final float HeavyLoadClimbingPenaltyMultiplier = -8.0F;
    public static final float PainClimbingPenaltyMultiplier = -5.0F;
    public static final float AllThumbsClimbingPenalty = -4.0F;
    public static final float DextrousClimbingBonus = 4.0F;
    public static final float BurglarClimbingBonus = 4.0F;
    public static final float GymnastClimbingBonus = 4.0F;
    public static final float HealthReductionMultiplierModerate = 1.2F;
    public static final float HealthReductionMultiplierSevere = 1.4F;

    public CharacterTraits() {
        for (ResourceLocation key : Registries.CHARACTER_TRAIT.keys()) {
            this.traits.put(Registries.CHARACTER_TRAIT.get(key), false);
        }
    }

    public boolean get(CharacterTrait characterTrait) {
        return this.traits.get(characterTrait);
    }

    public boolean set(CharacterTrait characterTrait, boolean value) {
        boolean oldValue = this.get(characterTrait);
        this.traits.put(characterTrait, value);
        if (value) {
            this.knownTraits.add(characterTrait);
        } else {
            this.knownTraits.remove(characterTrait);
        }

        return value != oldValue;
    }

    public void add(CharacterTrait characterTrait) {
        this.set(characterTrait, true);
    }

    public void remove(CharacterTrait characterTrait) {
        this.set(characterTrait, false);
    }

    public void load(ByteBuffer input) throws IOException {
        this.reset();
        int numberOfKnownTraits = input.getInt();

        for (int i = 0; i < numberOfKnownTraits; i++) {
            CharacterTrait characterTrait = CharacterTrait.get(ResourceLocation.of(GameWindow.ReadString(input)));
            this.add(characterTrait);
        }
    }

    public void save(ByteBuffer output) throws IOException {
        int numberOfKnownTraits = this.knownTraits.size();
        output.putInt(numberOfKnownTraits);

        for (CharacterTrait characterTrait : this.knownTraits) {
            GameWindow.WriteString(output, Registries.CHARACTER_TRAIT.getLocation(characterTrait).toString());
        }
    }

    public void write(ByteBufferWriter output) {
        int numberOfKnownTraits = this.knownTraits.size();
        output.putInt(numberOfKnownTraits);

        for (CharacterTrait characterTrait : this.knownTraits) {
            output.putUTF(Registries.CHARACTER_TRAIT.getLocation(characterTrait).toString());
        }
    }

    public void read(ByteBuffer input) {
        int numberOfKnownTraits = input.getInt();

        for (int i = 0; i < numberOfKnownTraits; i++) {
            CharacterTrait characterTrait = CharacterTrait.get(ResourceLocation.of(GameWindow.ReadString(input)));
            this.add(characterTrait);
        }
    }

    public Map<CharacterTrait, Boolean> getTraits() {
        return this.traits;
    }

    public List<CharacterTrait> getKnownTraits() {
        return new ArrayList<>(this.knownTraits);
    }

    public float getTraitDamageDealtReductionModifier() {
        float damageReductionModifier = 1.0F;
        if (this.traits.get(CharacterTrait.UNDERWEIGHT)) {
            damageReductionModifier *= 0.8F;
        }

        if (this.traits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            damageReductionModifier *= 0.6F;
        }

        if (this.traits.get(CharacterTrait.EMACIATED)) {
            damageReductionModifier *= 0.4F;
        }

        return damageReductionModifier;
    }

    public float getTraitEnduranceLossModifier() {
        return this.traits.get(CharacterTrait.ASTHMATIC) ? 1.2F : 1.0F;
    }

    public float getTraitWeatherPenaltyModifier() {
        return this.traits.get(CharacterTrait.OUTDOORSMAN) ? 1.0F : 1.5F;
    }

    private void reset() {
        this.knownTraits.clear();

        for (Entry<CharacterTrait, Boolean> entry : this.traits.entrySet()) {
            entry.setValue(false);
        }
    }
}
