// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.SandboxOptions;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraits;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.scripting.objects.CharacterTrait;

public class AntiCheatXPUpdate extends AbstractAntiCheat {
    private static final float MIN_XP_MULTIPLIER = 1.0F;
    private static final int XP_BOOST_OPTION_1 = 1;
    private static final int XP_BOOST_OPTION_2 = 2;
    private static final int XP_BOOST_OPTION_3 = 3;
    private static final float XP_BOOST_VALUE_1 = 1.0F;
    private static final float XP_BOOST_VALUE_2 = 1.33F;
    private static final float XP_BOOST_VALUE_3 = 1.66F;
    private static final float XP_BOOST_DEFAULT_VALUE = 0.25F;
    private static final float XP_BOOST_TRAIT = 1.3F;

    private static float getMaxPerkXpBoostMultiplier(IsoPlayer player, PerkFactory.Perk perk) {
        int newXpBoost = player.getXp().getPerkBoost(perk);
        int oldXpBoost = player.getNetworkCharacterAI().setXpBoost(perk, newXpBoost);
        int maxXpBoost = Math.max(newXpBoost, oldXpBoost);

        float maxXpBoostMultiplier = switch (maxXpBoost) {
            case 1 -> 1.0F;
            case 2 -> 1.33F;
            case 3 -> 1.66F;
            default -> 0.25F;
        };
        CharacterTraits characterTraits = player.getCharacterTraits();
        if (characterTraits.get(CharacterTrait.FAST_LEARNER) && perk.getParent() != PerkFactory.Perks.PhysicalCategory) {
            maxXpBoostMultiplier *= 1.3F;
        }

        if (characterTraits.get(CharacterTrait.CRAFTY) && perk.getParent() == PerkFactory.Perks.Crafting) {
            maxXpBoostMultiplier *= 1.3F;
        }

        return maxXpBoostMultiplier;
    }

    private static double getMaxPerkXpMultiplier(IsoPlayer player, PerkFactory.Perk perk) {
        float newXpMultiplier = Math.max(1.0F, player.getXp().getMultiplier(perk));
        float oldXpMultiplier = Math.max(1.0F, player.getNetworkCharacterAI().setXpMultiplier(perk, newXpMultiplier));
        float maxXpMultiplier = Math.max(newXpMultiplier, oldXpMultiplier);
        if (SandboxOptions.instance.multipliersConfig.xpMultiplierGlobalToggle.getValue()) {
            return maxXpMultiplier * Math.max(1.0, SandboxOptions.instance.multipliersConfig.xpMultiplierGlobal.getValue());
        } else {
            SandboxOptions.SandboxOption multiplierSandboxOption = SandboxOptions.instance.getOptionByName("MultiplierConfig." + perk.getName());
            return multiplierSandboxOption != null
                ? maxXpMultiplier * Math.max(1.0, Double.parseDouble(multiplierSandboxOption.asConfigOption().getValueAsString()))
                : maxXpMultiplier;
        }
    }

    private static boolean isPerkXpGrowthRateTooHigh(IsoPlayer player, PerkFactory.Perk perk) {
        float newXp = player.getXp().getXP(perk);
        float oldXp = player.getNetworkCharacterAI().setXp(perk, newXp);
        float xpDelta = newXp - oldXp;
        double maxPerkXpMultiplier = getMaxPerkXpMultiplier(player, perk);
        float maxPerkXpBoostMultiplier = getMaxPerkXpBoostMultiplier(player, perk);
        double maxXpDelta = 1000.0 * maxPerkXpMultiplier * maxPerkXpBoostMultiplier;
        if (xpDelta > maxXpDelta) {
            DebugType.Multiplayer
                .error(
                    "perk %s xp growth is too high: xp=%f max-xp=%f multiplier=%f boost=%f",
                    perk.getName(),
                    xpDelta,
                    maxXpDelta,
                    maxPerkXpMultiplier,
                    maxPerkXpBoostMultiplier
                );
            return true;
        } else {
            return false;
        }
    }

    private static boolean isXpGrowthRateTooHigh(IsoPlayer player) {
        if (!player.getNetworkCharacterAI().isXpCheckerIntervalPassed()) {
            return false;
        } else {
            for (PerkFactory.Perk perk : PerkFactory.PerkList) {
                if (isPerkXpGrowthRateTooHigh(player, perk)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean update(UdpConnection connection) {
        super.update(connection);
        if (!this.antiCheat.isEnabled()) {
            return true;
        } else {
            for (IsoPlayer player : connection.players) {
                if (player != null && isXpGrowthRateTooHigh(player)) {
                    return false;
                }
            }

            return true;
        }
    }
}
