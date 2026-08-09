// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.Moodles;

import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.Thermoregulator;
import zombie.core.Color;
import zombie.iso.weather.Temperature;
import zombie.scripting.objects.MoodleType;
import zombie.ui.MoodlesUI;

@UsedFromLua
public final class Moodle {
    private static final int MinChevrons = 0;
    private static final int MaxChevrons = 3;
    private static final int CantSprintTimerDuration = 300;
    private static final int PainTimerDuration = 120;
    private final MoodleType moodleType;
    private int moodleLevel = Moodle.MoodleLevel.MinMoodleLevel.ordinal();
    private final IsoGameCharacter isoGameCharacter;
    private int painTimer;
    private Color chevronColor = Color.white;
    private boolean chevronIsUp = true;
    private int chevronCount = 0;
    private static final Color colorNeg = new Color(0.88235295F, 0.15686275F, 0.15686275F);
    private static final Color colorPos = new Color(0.15686275F, 0.88235295F, 0.15686275F);
    private int cantSprintTimer = 300;

    public Moodle(MoodleType moodleType, IsoGameCharacter isoGameCharacter) {
        this.isoGameCharacter = isoGameCharacter;
        this.moodleType = moodleType;
    }

    public MoodleType getMoodleType() {
        return this.moodleType;
    }

    public int getChevronCount() {
        return this.chevronCount;
    }

    public boolean isChevronIsUp() {
        return this.chevronIsUp;
    }

    public Color getChevronColor() {
        return this.chevronColor;
    }

    public boolean chevronDifference(int count, boolean isUp, Color col) {
        return count != this.chevronCount || isUp != this.chevronIsUp || col != this.chevronColor;
    }

    public void setChevron(int count, boolean isUp, Color col) {
        if (count < 0) {
            count = 0;
        }

        if (count > 3) {
            count = 3;
        }

        this.chevronCount = count;
        this.chevronIsUp = isUp;
        this.chevronColor = col != null ? col : Color.white;
    }

    public int getLevel() {
        return this.moodleLevel;
    }

    private boolean updateMoodleLevel(int moodleLevel) {
        if (moodleLevel != this.moodleLevel) {
            this.moodleLevel = Math.max(Moodle.MoodleLevel.MinMoodleLevel.ordinal(), Math.min(moodleLevel, Moodle.MoodleLevel.MaxMoodleLevel.ordinal()));
            return true;
        } else {
            return false;
        }
    }

    public boolean Update() {
        int moodleLevelToSet = Moodle.MoodleLevel.MinMoodleLevel.ordinal();
        if (this.isoGameCharacter.isDead() && this.moodleType != MoodleType.DEAD && this.moodleType != MoodleType.ZOMBIE) {
            return this.updateMoodleLevel(moodleLevelToSet);
        } else {
            if (this.moodleType == MoodleType.CANT_SPRINT && ((IsoPlayer)this.isoGameCharacter).moodleCantSprint) {
                moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                this.cantSprintTimer--;
                MoodlesUI.getInstance().wiggle(MoodleType.CANT_SPRINT);
                if (this.cantSprintTimer == 0) {
                    moodleLevelToSet = Moodle.MoodleLevel.MinMoodleLevel.ordinal();
                    this.cantSprintTimer = 300;
                    ((IsoPlayer)this.isoGameCharacter).moodleCantSprint = false;
                }
            }

            if (this.moodleType == MoodleType.ENDURANCE && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float endurance = this.isoGameCharacter.getStats().get(CharacterStat.ENDURANCE);
                if (endurance > MoodleStat.ENDURANCE.getMinimumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MinMoodleLevel.ordinal();
                } else if (endurance > MoodleStat.ENDURANCE.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                } else if (endurance > MoodleStat.ENDURANCE.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                } else if (endurance > MoodleStat.ENDURANCE.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                } else {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.ANGRY) {
                float anger = this.isoGameCharacter.getStats().get(CharacterStat.ANGER);
                if (anger > MoodleStat.ANGRY.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                } else if (anger > MoodleStat.ANGRY.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                } else if (anger > MoodleStat.ANGRY.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                } else if (anger > MoodleStat.ANGRY.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.TIRED && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float fatigue = this.isoGameCharacter.getStats().get(CharacterStat.FATIGUE);
                if (fatigue > MoodleStat.TIRED.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (fatigue > MoodleStat.TIRED.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (fatigue > MoodleStat.TIRED.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (fatigue > MoodleStat.TIRED.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.HUNGRY && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float hunger = this.isoGameCharacter.getStats().get(CharacterStat.HUNGER);
                if (hunger > MoodleStat.HUNGRY.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (hunger > MoodleStat.HUNGRY.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (hunger > MoodleStat.HUNGRY.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (hunger > MoodleStat.HUNGRY.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.PANIC && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float panic = this.isoGameCharacter.getStats().get(CharacterStat.PANIC);
                if (panic > MoodleStat.PANIC.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (panic > MoodleStat.PANIC.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (panic > MoodleStat.PANIC.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (panic > MoodleStat.PANIC.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.SICK && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float sickness = this.isoGameCharacter.getBodyDamage().getApparentInfectionLevel() / 100.0F
                    + this.isoGameCharacter.getStats().get(CharacterStat.SICKNESS);
                if (sickness > MoodleStat.SICK.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (sickness > MoodleStat.SICK.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (sickness > MoodleStat.SICK.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (sickness > MoodleStat.SICK.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.BORED && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float boredom = this.isoGameCharacter.getStats().get(CharacterStat.BOREDOM);
                if (boredom > MoodleStat.BORED.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (boredom > MoodleStat.BORED.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (boredom > MoodleStat.BORED.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (boredom > MoodleStat.BORED.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.UNHAPPY && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float unhappinessLevel = this.isoGameCharacter.getStats().get(CharacterStat.UNHAPPINESS);
                if (unhappinessLevel > MoodleStat.UNHAPPY.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (unhappinessLevel > MoodleStat.UNHAPPY.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (unhappinessLevel > MoodleStat.UNHAPPY.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (unhappinessLevel > MoodleStat.UNHAPPY.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.STRESS) {
                float effectiveStress = this.isoGameCharacter.getStats().getNicotineStress();
                if (effectiveStress > MoodleStat.STRESS.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                } else if (effectiveStress > MoodleStat.STRESS.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                } else if (effectiveStress > MoodleStat.STRESS.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                } else if (effectiveStress > MoodleStat.STRESS.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.THIRST) {
                if (this.isoGameCharacter.getStats().get(CharacterStat.THIRST) > MoodleStat.THIRST.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getStats().get(CharacterStat.THIRST) > MoodleStat.THIRST.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getStats().get(CharacterStat.THIRST) > MoodleStat.THIRST.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getStats().get(CharacterStat.THIRST) > MoodleStat.THIRST.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.BLEEDING && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                moodleLevelToSet = this.isoGameCharacter.getBodyDamage().getNumPartsBleeding();
                if (this.isoGameCharacter.getBodyDamage().isNeckBleeding()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }

                if (moodleLevelToSet > Moodle.MoodleLevel.MaxMoodleLevel.ordinal()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.WET && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float wetness = this.isoGameCharacter.getStats().get(CharacterStat.WETNESS);
                if (wetness > MoodleStat.WET.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (wetness > MoodleStat.WET.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (wetness > MoodleStat.WET.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (wetness > MoodleStat.WET.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.HAS_A_COLD && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                if (this.isoGameCharacter.getBodyDamage().getColdStrength() > MoodleStat.HAS_A_COLD.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getBodyDamage().getColdStrength() > MoodleStat.HAS_A_COLD.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getBodyDamage().getColdStrength() > MoodleStat.HAS_A_COLD.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getBodyDamage().getColdStrength() > MoodleStat.HAS_A_COLD.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.INJURED && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                if (100.0F - this.isoGameCharacter.getBodyDamage().getHealth() > MoodleStat.INJURED.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (100.0F - this.isoGameCharacter.getBodyDamage().getHealth() > MoodleStat.INJURED.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (100.0F - this.isoGameCharacter.getBodyDamage().getHealth() > MoodleStat.INJURED.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (100.0F - this.isoGameCharacter.getBodyDamage().getHealth() > MoodleStat.INJURED.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.PAIN) {
                this.painTimer++;
                if (this.painTimer < 120) {
                    return false;
                }

                this.painTimer = 0;
                if (this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                    float pain = this.isoGameCharacter.getStats().get(CharacterStat.PAIN);
                    if (pain > MoodleStat.PAIN.getLowestThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                    }

                    if (pain > MoodleStat.PAIN.getModerateThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                    }

                    if (pain > MoodleStat.PAIN.getHighestThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                    }

                    if (pain > MoodleStat.PAIN.getMaximumThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                    }
                }

                if (moodleLevelToSet != this.getLevel()
                    && moodleLevelToSet >= Moodle.MoodleLevel.HighMoodleLevel.ordinal()
                    && this.getLevel() < Moodle.MoodleLevel.HighMoodleLevel.ordinal()
                    && this.isoGameCharacter instanceof IsoPlayer player
                    && player.isLocalPlayer()) {
                    player.playerVoiceSound("PainMoodle");
                }
            }

            if (this.moodleType == MoodleType.HEAVY_LOAD) {
                float weight = this.isoGameCharacter.getInventory().getCapacityWeight();
                float maxWeight = this.isoGameCharacter.getMaxWeight();
                float ratio = weight / maxWeight;
                if (this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                    if (ratio >= MoodleStat.HEAVY_LOAD.getMaximumThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                    } else if (ratio >= MoodleStat.HEAVY_LOAD.getHighestThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                    } else if (ratio >= MoodleStat.HEAVY_LOAD.getModerateThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                    } else if (ratio > MoodleStat.HEAVY_LOAD.getLowestThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                    }
                }
            }

            if (this.moodleType == MoodleType.DRUNK && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float intoxication = this.isoGameCharacter.getStats().get(CharacterStat.INTOXICATION);
                if (intoxication > MoodleStat.DRUNK.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (intoxication > MoodleStat.DRUNK.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (intoxication > MoodleStat.DRUNK.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (intoxication > MoodleStat.DRUNK.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.DEAD && this.isoGameCharacter.isDead()) {
                moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                if (!this.isoGameCharacter.getBodyDamage().IsFakeInfected() && this.isoGameCharacter.getStats().get(CharacterStat.ZOMBIE_INFECTION) >= 0.001F) {
                    moodleLevelToSet = Moodle.MoodleLevel.MinMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.ZOMBIE
                && this.isoGameCharacter.isDead()
                && !this.isoGameCharacter.getBodyDamage().IsFakeInfected()
                && this.isoGameCharacter.getStats().get(CharacterStat.ZOMBIE_INFECTION) >= 0.001F) {
                moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
            }

            if (this.moodleType == MoodleType.FOOD_EATEN && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                if (this.isoGameCharacter.getBodyDamage().getHealthFromFoodTimer() > 0.0F) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getBodyDamage().getHealthFromFoodTimer() > this.isoGameCharacter.getBodyDamage().getStandardHealthFromFoodTime()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getBodyDamage().getHealthFromFoodTimer()
                    > this.isoGameCharacter.getBodyDamage().getStandardHealthFromFoodTime() * 2.0F) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (this.isoGameCharacter.getBodyDamage().getHealthFromFoodTimer()
                    > this.isoGameCharacter.getBodyDamage().getStandardHealthFromFoodTime() * 3.0F) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            int chevCount = this.chevronCount;
            boolean chevIsUp = this.chevronIsUp;
            Color chevCol = this.chevronColor;
            float temperature = this.isoGameCharacter.getStats().get(CharacterStat.TEMPERATURE);
            if ((this.moodleType == MoodleType.HYPERTHERMIA || this.moodleType == MoodleType.HYPOTHERMIA) && this.isoGameCharacter instanceof IsoPlayer) {
                if (!(temperature < 36.5F) && !(temperature > 37.5F)) {
                    chevCount = 0;
                } else {
                    Thermoregulator thermos = this.isoGameCharacter.getBodyDamage().getThermoregulator();
                    if (thermos == null) {
                        chevCount = 0;
                    } else {
                        chevIsUp = thermos.thermalChevronUp();
                        chevCount = thermos.thermalChevronCount();
                    }
                }
            }

            if (this.moodleType == MoodleType.HYPERTHERMIA) {
                if (chevCount > 0) {
                    chevCol = chevIsUp ? colorNeg : colorPos;
                }

                if (temperature != 0.0F) {
                    if (temperature > 37.5F) {
                        moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                    }

                    if (temperature > 39.0F) {
                        moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                    }

                    if (temperature > 40.0F) {
                        moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                    }

                    if (temperature > 41.0F) {
                        moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                    }
                }

                if (moodleLevelToSet != this.getLevel()
                    || moodleLevelToSet > Moodle.MoodleLevel.MinMoodleLevel.ordinal() && this.chevronDifference(chevCount, chevIsUp, chevCol)) {
                    this.setChevron(chevCount, chevIsUp, chevCol);
                }
            }

            if (this.moodleType == MoodleType.HYPOTHERMIA) {
                if (chevCount > 0) {
                    chevCol = chevIsUp ? colorPos : colorNeg;
                }

                if (temperature != 0.0F) {
                    if (temperature < 36.5F && this.isoGameCharacter.getStats().get(CharacterStat.INTOXICATION) <= MoodleStat.HYPOTHERMIA.getLowestThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                    }

                    if (temperature < 35.0F
                        && this.isoGameCharacter.getStats().get(CharacterStat.INTOXICATION) <= MoodleStat.HYPOTHERMIA.getModerateThreshold()) {
                        moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                    }

                    if (temperature < 30.0F) {
                        moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                    }

                    if (temperature < 25.0F) {
                        moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                    }
                }

                if (moodleLevelToSet != this.getLevel()
                    || moodleLevelToSet > Moodle.MoodleLevel.MinMoodleLevel.ordinal() && this.chevronDifference(chevCount, chevIsUp, chevCol)) {
                    this.setChevron(chevCount, chevIsUp, chevCol);
                }
            }

            if (this.moodleType == MoodleType.WINDCHILL && this.isoGameCharacter instanceof IsoPlayer isoPlayer) {
                float windChillAmount = Temperature.getWindChillAmountForPlayer(isoPlayer);
                if (windChillAmount > MoodleStat.WINDCHILL.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }

                if (windChillAmount > MoodleStat.WINDCHILL.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                }

                if (windChillAmount > MoodleStat.WINDCHILL.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                }

                if (windChillAmount > MoodleStat.WINDCHILL.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.UNCOMFORTABLE && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                float discomfort = this.isoGameCharacter.getStats().get(CharacterStat.DISCOMFORT);
                if (discomfort >= MoodleStat.UNCOMFORTABLE.getMaximumThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                } else if (discomfort >= MoodleStat.UNCOMFORTABLE.getHighestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                } else if (discomfort >= MoodleStat.UNCOMFORTABLE.getModerateThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                } else if (discomfort >= MoodleStat.UNCOMFORTABLE.getLowestThreshold()) {
                    moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                }
            }

            if (this.moodleType == MoodleType.NOXIOUS_SMELL
                && this.isoGameCharacter.getBodyDamage() != null
                && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0F) {
                if (this.isoGameCharacter.getCurrentBuilding() != null
                    && this.isoGameCharacter.getCurrentBuilding().isToxic()
                    && !this.isoGameCharacter.isProtectedFromToxic(false)) {
                    moodleLevelToSet = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
                } else if (SandboxOptions.instance.decayingCorpseHealthImpact.getValue() != 1) {
                    float rate = this.isoGameCharacter.getCorpseSicknessRate();
                    if (rate > 0.0F) {
                        if (rate > MoodleStat.NOXIOUS_SMELL.getHighestThreshold()) {
                            moodleLevelToSet = Moodle.MoodleLevel.HighMoodleLevel.ordinal();
                        } else if (rate > MoodleStat.NOXIOUS_SMELL.getModerateThreshold()) {
                            moodleLevelToSet = Moodle.MoodleLevel.ModerateMoodleLevel.ordinal();
                        } else {
                            moodleLevelToSet = Moodle.MoodleLevel.LowMoodleLevel.ordinal();
                        }
                    }
                }
            }

            return this.updateMoodleLevel(moodleLevelToSet);
        }
    }

    public static enum MoodleLevel {
        MinMoodleLevel,
        LowMoodleLevel,
        ModerateMoodleLevel,
        HighMoodleLevel,
        MaxMoodleLevel;
    }
}
