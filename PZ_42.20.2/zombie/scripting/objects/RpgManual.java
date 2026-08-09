// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class RpgManual {
    public static final RpgManual ADVENTURE_MANAGER_IMAGINATION_RULEBOOK = registerBase("AdventureManagerImaginationRulebook");
    public static final RpgManual ADVERSARY_COMPENDIUM = registerBase("AdversaryCompendium");
    public static final RpgManual CRATER_WORLD = registerBase("CraterWorld");
    public static final RpgManual CREATURE_DIRECTORY = registerBase("CreatureDirectory");
    public static final RpgManual DAMSELS_DANGERS = registerBase("DamselsDangers");
    public static final RpgManual FANTASIES_OF_POWER = registerBase("FantasiesofPower");
    public static final RpgManual JOHN_SPIRAL = registerBase("JohnSpiral");
    public static final RpgManual LEGENDS_OF_IRONSAND_BOG = registerBase("LegendsofIronsandBog");
    public static final RpgManual LOOT_LORDS = registerBase("LootLords");
    public static final RpgManual METALSPEAR_600_K = registerBase("Metalspear600K");
    public static final RpgManual PLANET_MASTERS = registerBase("PlanetMasters");
    public static final RpgManual PLANET_MASTERS_ALIENS_REFERENCE = registerBase("PlanetMastersAliensReference");
    public static final RpgManual RECORD_SHEETS_FOR_ENCHANTMENTS_AND_BATTLE = registerBase("RecordSheetsforEnchantmentsAndBattle");
    public static final RpgManual SWORDS_FIREBALLS = registerBase("SwordsFireballs");
    public static final RpgManual TRIALS_OF_TANGLEWOOD = registerBase("TrialsofTanglewood");
    public static final RpgManual VAMPIRE_HACKERS = registerBase("VampireHackers");
    private final String translationKey;

    private RpgManual(String id) {
        this.translationKey = "IGUI_RPG_" + id;
    }

    public static RpgManual get(ResourceLocation id) {
        return Registries.RPG_MANUAL.get(id);
    }

    @Override
    public String toString() {
        return Registries.RPG_MANUAL.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static RpgManual register(String id) {
        return register(false, id);
    }

    private static RpgManual registerBase(String id) {
        return register(true, id);
    }

    private static RpgManual register(boolean allowDefaultNamespace, String id) {
        return Registries.RPG_MANUAL.register(RegistryReset.createLocation(id, allowDefaultNamespace), new RpgManual(id));
    }

    static {
        if (Core.IS_DEV) {
            for (RpgManual rpgMagazine : Registries.RPG_MANUAL) {
                TranslationKeyValidator.of(rpgMagazine.translationKey);
            }
        }
    }
}
