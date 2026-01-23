// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.BodyDamage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.random.Rand;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.SyncPlayerStatsPacket;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.MoodleType;

@UsedFromLua
public final class Fitness {
    private IsoGameCharacter parent;
    private HashMap<String, Float> regularityMap = new HashMap<>();
    private int fitnessLvl;
    private int strLvl;
    private final HashMap<String, Integer> stiffnessTimerMap = new HashMap<>();
    private final HashMap<String, Float> stiffnessIncMap = new HashMap<>();
    private final ArrayList<String> bodypartToIncStiffness = new ArrayList<>();
    private final HashMap<String, Fitness.FitnessExercise> exercises = new HashMap<>();
    private final HashMap<String, Long> exeTimer = new HashMap<>();
    private int lastUpdate = -1;
    private Fitness.FitnessExercise currentExe;
    private static final int HOURS_FOR_STIFFNESS = 12;
    private static final float BASE_STIFFNESS_INC = 0.5F;
    private static final float BASE_ENDURANCE_RED = 0.015F;
    private static final float BASE_REGULARITY_INC = 0.08F;
    private static final float BASE_REGULARITY_DEC = 0.002F;
    private static final float BASE_PAIN_INC = 2.5F;

    public Fitness(IsoGameCharacter parent) {
        this.setParent(parent);
    }

    /**
     * We update every 10 in game minutes to facilitate calculs
     */
    public void update() {
        int currentMin = GameTime.getInstance().getMinutes() / 10;
        if (this.lastUpdate == -1) {
            this.lastUpdate = currentMin;
        }

        if (currentMin != this.lastUpdate) {
            this.lastUpdate = currentMin;
            ArrayList<String> toremove = new ArrayList<>();
            this.decreaseRegularity();

            for (String currentBodypart : this.stiffnessTimerMap.keySet()) {
                Integer timer = this.stiffnessTimerMap.get(currentBodypart);
                timer = timer - 1;
                if (timer <= 0) {
                    toremove.add(currentBodypart);
                    this.bodypartToIncStiffness.add(currentBodypart);
                } else {
                    this.stiffnessTimerMap.put(currentBodypart, timer);
                }
            }

            for (int i = 0; i < toremove.size(); i++) {
                this.stiffnessTimerMap.remove(toremove.get(i));
            }

            for (int i = 0; i < this.bodypartToIncStiffness.size(); i++) {
                String bodypartType = this.bodypartToIncStiffness.get(i);
                Float stiffnessLeft = this.stiffnessIncMap.get(bodypartType);
                if (stiffnessLeft == null) {
                    return;
                }

                stiffnessLeft = stiffnessLeft - 1.0F;
                this.increasePain(bodypartType);
                if (stiffnessLeft <= 0.0F) {
                    this.bodypartToIncStiffness.remove(i);
                    this.stiffnessIncMap.remove(bodypartType);
                    i--;
                } else {
                    this.stiffnessIncMap.put(bodypartType, stiffnessLeft);
                }
            }
        }
    }

    private void decreaseRegularity() {
        for (String currentExe : this.regularityMap.keySet()) {
            if (this.exeTimer.containsKey(currentExe) && GameTime.getInstance().getCalender().getTimeInMillis() - this.exeTimer.get(currentExe) > 86400000L) {
                float currentValue = this.regularityMap.get(currentExe);
                currentValue -= 0.002F;
                this.regularityMap.put(currentExe, currentValue);
            }
        }
    }

    private void increasePain(String bodypartType) {
        if ("arms".equals(bodypartType)) {
            for (int i = BodyPartType.ForeArm_L.index(); i < BodyPartType.UpperArm_R.index() + 1; i++) {
                BodyPart part = this.parent.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i));
                part.setStiffness(part.getStiffness() + 2.5F);
            }
        }

        if ("legs".equals(bodypartType)) {
            for (int i = BodyPartType.UpperLeg_L.index(); i < BodyPartType.LowerLeg_R.index() + 1; i++) {
                BodyPart part = this.parent.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i));
                part.setStiffness(part.getStiffness() + 2.5F);
            }
        }

        if ("chest".equals(bodypartType)) {
            BodyPart part = this.parent.getBodyDamage().getBodyPart(BodyPartType.Torso_Upper);
            part.setStiffness(part.getStiffness() + 2.5F);
        }

        if ("abs".equals(bodypartType)) {
            BodyPart part = this.parent.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
            part.setStiffness(part.getStiffness() + 2.5F);
        }
    }

    public void setCurrentExercise(String type) {
        this.currentExe = this.exercises.get(type);
    }

    public void exerciseRepeat() {
        this.fitnessLvl = this.parent.getPerkLevel(PerkFactory.Perks.Fitness);
        this.strLvl = this.parent.getPerkLevel(PerkFactory.Perks.Strength);
        this.incRegularity();
        this.reduceEndurance();
        this.incFutureStiffness();
        this.incStats();
        this.updateExeTimer();
    }

    private void updateExeTimer() {
        this.exeTimer.put(this.currentExe.type, GameTime.getInstance().getCalender().getTimeInMillis());
    }

    /**
     * Increase the regularity when you've done a repeat of an exercice
     *  Depend on fitness (using logarithm), the more fitness, the LESS regularity you get
     *  Regularity will influence on the stiffness you get once you've finished an exercise
     */
    public void incRegularity() {
        float baseInc = 0.08F;
        int logMod = 4;
        double incLog = Math.log(this.fitnessLvl / 5.0F + 4.0F);
        baseInc = (float)(baseInc * (Math.log(5.0) / incLog));
        Float result = this.regularityMap.get(this.currentExe.type);
        if (result == null) {
            result = 0.0F;
        }

        result = result + baseInc;
        result = Math.min(Math.max(result, 0.0F), 100.0F);
        this.regularityMap.put(this.currentExe.type, result);
    }

    /**
     * Reduce endurance, using metabolics (to know what kind of exercise it is, some are more exhausting than others), regularity, current carrying weight.
     */
    public void reduceEndurance() {
        float baseRed = 0.015F;
        Float reg = this.regularityMap.get(this.currentExe.type);
        if (reg == null) {
            reg = 0.0F;
        }

        int logMod = 50;
        double incLog = Math.log(reg / 50.0F + 50.0F);
        baseRed = (float)(baseRed * (incLog / Math.log(51.0)));
        if (this.currentExe.metabolics == Metabolics.FitnessHeavy) {
            baseRed *= 1.3F;
        }

        baseRed *= 1 + this.parent.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) / 3;
        if (!GameClient.client) {
            this.parent.getStats().remove(CharacterStat.ENDURANCE, baseRed);
        }

        if (GameServer.server && this.parent instanceof IsoPlayer isoPlayer) {
            INetworkPacket.send(
                isoPlayer, PacketTypes.PacketType.SyncPlayerStats, this.parent, SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.ENDURANCE)
            );
        }
    }

    /**
     * We setup a timer after finishing an exercice, 12h after, we gonna start to increase stiffness (add pains in muscles)
     *  When adding the stiffness, we decrease slowly our vars while increasing pain, untill no more stiffness is to be added.
     *  Stiffness induced will depend on regularity, fatigue.
     *  Numbers approx: At 0 regularity, 60min exercises should gives almost 4h of stiffness (gets additional pain)
     */
    public void incFutureStiffness() {
        Float reg = this.regularityMap.get(this.currentExe.type);
        if (reg == null) {
            reg = 0.0F;
        }

        for (int i = 0; i < this.currentExe.stiffnessInc.size(); i++) {
            float baseRed = 0.5F;
            String bodyPart = this.currentExe.stiffnessInc.get(i);
            if (!this.stiffnessTimerMap.containsKey(bodyPart) && !this.bodypartToIncStiffness.contains(bodyPart)) {
                this.stiffnessTimerMap.put(bodyPart, 72);
            }

            Float currentStiffnessInc = this.stiffnessIncMap.get(bodyPart);
            if (currentStiffnessInc == null) {
                currentStiffnessInc = 0.0F;
            }

            baseRed *= (120.0F - reg) / 170.0F;
            if (this.currentExe.metabolics == Metabolics.FitnessHeavy) {
                baseRed *= 1.3F;
            }

            baseRed *= 1 + this.parent.getMoodles().getMoodleLevel(MoodleType.TIRED) / 3;
            currentStiffnessInc = currentStiffnessInc + baseRed;
            currentStiffnessInc = Math.min(currentStiffnessInc, 150.0F);
            this.stiffnessIncMap.put(bodyPart, currentStiffnessInc);
        }
    }

    public void incStats() {
        float str = 0.0F;
        float fit = 0.0F;

        for (int i = 0; i < this.currentExe.stiffnessInc.size(); i++) {
            String bodypart = this.currentExe.stiffnessInc.get(i);
            if ("arms".equals(bodypart)) {
                str += 4.0F;
            }

            if ("chest".equals(bodypart)) {
                str += 2.0F;
            }

            if ("legs".equals(bodypart)) {
                fit += 4.0F;
            }

            if ("abs".equals(bodypart)) {
                fit += 2.0F;
            }
        }

        if (this.strLvl > 5) {
            str *= 1 + (this.strLvl - 5) / 10;
        }

        if (this.fitnessLvl > 5) {
            fit *= 1 + (this.fitnessLvl - 5) / 10;
        }

        str *= this.currentExe.xpModifier;
        fit *= this.currentExe.xpModifier;
        if (GameServer.server) {
            if (this.parent instanceof IsoPlayer isoPlayer) {
                GameServer.addXp(isoPlayer, PerkFactory.Perks.Strength, (int)str);
                GameServer.addXp(isoPlayer, PerkFactory.Perks.Fitness, (int)fit);
            }
        } else if (!GameClient.client) {
            this.parent.getXp().AddXP(PerkFactory.Perks.Strength, str);
            this.parent.getXp().AddXP(PerkFactory.Perks.Fitness, fit);
        }
    }

    public void resetValues() {
        this.stiffnessIncMap.clear();
        this.stiffnessTimerMap.clear();
        this.regularityMap.clear();
    }

    public void removeStiffnessValue(String type) {
        this.stiffnessIncMap.remove(type);
        this.stiffnessTimerMap.remove(type);
    }

    public void save(ByteBuffer output) {
        output.putInt(this.stiffnessIncMap.size());

        for (String str : this.stiffnessIncMap.keySet()) {
            GameWindow.WriteString(output, str);
            output.putFloat(this.stiffnessIncMap.get(str));
        }

        output.putInt(this.stiffnessTimerMap.size());

        for (String str : this.stiffnessTimerMap.keySet()) {
            GameWindow.WriteString(output, str);
            output.putInt(this.stiffnessTimerMap.get(str));
        }

        output.putInt(this.regularityMap.size());

        for (String str : this.regularityMap.keySet()) {
            GameWindow.WriteString(output, str);
            output.putFloat(this.regularityMap.get(str));
        }

        output.putInt(this.bodypartToIncStiffness.size());

        for (int i = 0; i < this.bodypartToIncStiffness.size(); i++) {
            GameWindow.WriteString(output, this.bodypartToIncStiffness.get(i));
        }

        output.putInt(this.exeTimer.size());

        for (String str : this.exeTimer.keySet()) {
            GameWindow.WriteString(output, str);
            output.putLong(this.exeTimer.get(str));
        }
    }

    public void load(ByteBuffer input, int WorldVersion) {
        int size = input.getInt();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                this.stiffnessIncMap.put(GameWindow.ReadString(input), input.getFloat());
            }
        }

        size = input.getInt();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                this.stiffnessTimerMap.put(GameWindow.ReadString(input), input.getInt());
            }
        }

        size = input.getInt();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                this.regularityMap.put(GameWindow.ReadString(input), input.getFloat());
            }
        }

        size = input.getInt();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                this.bodypartToIncStiffness.add(GameWindow.ReadString(input));
            }
        }

        size = input.getInt();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                this.exeTimer.put(GameWindow.ReadString(input), input.getLong());
            }
        }
    }

    public boolean onGoingStiffness() {
        return !this.bodypartToIncStiffness.isEmpty();
    }

    public int getCurrentExeStiffnessTimer(String type) {
        type = type.split(",")[0];
        return this.stiffnessTimerMap.get(type) != null ? this.stiffnessTimerMap.get(type) : 0;
    }

    public float getCurrentExeStiffnessInc(String type) {
        type = type.split(",")[0];
        return this.stiffnessIncMap.get(type) != null ? this.stiffnessIncMap.get(type) : 0.0F;
    }

    public IsoGameCharacter getParent() {
        return this.parent;
    }

    public void setParent(IsoGameCharacter parent) {
        this.parent = parent;
    }

    public float getRegularity(String type) {
        Float result = this.regularityMap.get(type);
        if (result == null) {
            result = 0.0F;
        }

        return result;
    }

    public HashMap<String, Float> getRegularityMap() {
        return this.regularityMap;
    }

    public void setRegularityMap(HashMap<String, Float> regularityMap) {
        this.regularityMap = regularityMap;
    }

    public void init() {
        if (this.exercises.isEmpty()) {
            KahluaTableImpl exercisesType = (KahluaTableImpl)LuaManager.env.rawget("FitnessExercises");
            if (exercisesType != null) {
                KahluaTableImpl exercisesList = (KahluaTableImpl)exercisesType.rawget("exercisesType");
                if (exercisesList != null) {
                    for (Entry<Object, Object> objectObjectEntry : exercisesList.delegate.entrySet()) {
                        this.exercises.put((String)objectObjectEntry.getKey(), new Fitness.FitnessExercise((KahluaTableImpl)objectObjectEntry.getValue()));
                    }

                    this.initRegularityMapProfession();
                }
            }
        }
    }

    public void initRegularityMapProfession() {
        if (this.regularityMap.isEmpty()) {
            boolean fireman = false;
            boolean fitnessinstructor = false;
            boolean securityguard = false;
            if (this.parent.getDescriptor().isCharacterProfession(CharacterProfession.FITNESS_INSTRUCTOR)) {
                fitnessinstructor = true;
            }

            if (this.parent.getDescriptor().isCharacterProfession(CharacterProfession.FIRE_OFFICER)) {
                fireman = true;
            }

            if (this.parent.getDescriptor().isCharacterProfession(CharacterProfession.SECURITY_GUARD)) {
                securityguard = true;
            }

            if (fireman || fitnessinstructor || securityguard) {
                for (String s : this.exercises.keySet()) {
                    float reg = Rand.Next(7, 12);
                    if (fireman) {
                        reg = Rand.Next(10, 20);
                    } else if (fitnessinstructor) {
                        reg = Rand.Next(40, 60);
                    }

                    this.regularityMap.put(s, reg);
                }
            }
        }
    }

    public static final class FitnessExercise {
        String type;
        Metabolics metabolics;
        ArrayList<String> stiffnessInc;
        float xpModifier = 1.0F;

        public FitnessExercise(KahluaTableImpl exeDatas) {
            this.type = exeDatas.rawgetStr("type");
            this.metabolics = (Metabolics)exeDatas.rawget("metabolics");
            this.stiffnessInc = new ArrayList<>(Arrays.asList(exeDatas.rawgetStr("stiffness").split(",")));
            if (exeDatas.rawgetFloat("xpMod") > 0.0F) {
                this.xpModifier = exeDatas.rawgetFloat("xpMod");
            }
        }
    }
}
