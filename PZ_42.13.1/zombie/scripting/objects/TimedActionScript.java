// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.skills.PerkFactory;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

@DebugClassFields
@UsedFromLua
public class TimedActionScript extends BaseScriptObject {
    private Metabolics metabolics = Metabolics.Default;
    private int time = -1;
    private boolean faceObject;
    private String prop1;
    private String prop2;
    private String actionAnim;
    private String animVarKey;
    private String animVarVal;
    private String sound;
    private ActionSoundTime soundTime = ActionSoundTime.ACTION_START;
    private String completionSound;
    private float muscleStrainFactor;
    private ArrayList<BodyPartType> muscleStrainParts = new ArrayList<>();
    private PerkFactory.Perk muscleStrainSkill;
    private boolean cantSit;

    public TimedActionScript() {
        super(ScriptType.TimedAction);
    }

    public String getName() {
        return this.getScriptObjectName();
    }

    public String getFullType() {
        return this.getScriptObjectFullType();
    }

    public Metabolics getMetabolics() {
        return this.metabolics;
    }

    public int getTime() {
        return this.time;
    }

    public boolean isFaceObject() {
        return this.faceObject;
    }

    public boolean isCantSit() {
        return this.cantSit;
    }

    public String getProp1() {
        return this.prop1;
    }

    public String getProp2() {
        return this.prop2;
    }

    public String getActionAnim() {
        return this.actionAnim;
    }

    public String getAnimVarKey() {
        return this.animVarKey;
    }

    public String getAnimVarVal() {
        return this.animVarVal;
    }

    public String getSound() {
        return this.sound;
    }

    public ActionSoundTime getSoundTime() {
        return this.soundTime;
    }

    public String getCompletionSound() {
        return this.completionSound;
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("metabolics")) {
                    this.metabolics = Metabolics.valueOf(val);
                } else if (key.equalsIgnoreCase("time")) {
                    this.time = Integer.parseInt(val);
                } else if (key.equalsIgnoreCase("faceObject")) {
                    this.faceObject = Boolean.parseBoolean(val);
                } else if (key.equalsIgnoreCase("cantSit")) {
                    this.cantSit = Boolean.parseBoolean(val);
                } else if (key.equalsIgnoreCase("prop1")) {
                    this.prop1 = val;
                } else if (key.equalsIgnoreCase("prop2")) {
                    this.prop2 = val;
                } else if (key.equalsIgnoreCase("actionAnim")) {
                    this.actionAnim = val;
                } else if (key.equalsIgnoreCase("animVarKey")) {
                    this.animVarKey = val;
                } else if (key.equalsIgnoreCase("animVarVal")) {
                    this.animVarVal = val;
                } else if (key.equalsIgnoreCase("sound")) {
                    this.sound = val;
                } else if (key.equalsIgnoreCase("soundTime")) {
                    this.soundTime = ActionSoundTime.fromValue(val);
                } else if (key.equalsIgnoreCase("completionSound")) {
                    this.completionSound = val;
                } else if (key.equalsIgnoreCase("muscleStrainFactor")) {
                    this.muscleStrainFactor = Float.parseFloat(val);
                } else if (key.equalsIgnoreCase("muscleStrainSkill")) {
                    PerkFactory.Perk skill = PerkFactory.Perks.FromString(val);
                    if (skill == PerkFactory.Perks.MAX) {
                        DebugLog.Recipe.warn("Unknown skill \"%s\" in timedaction script \"%s\"", val, this);
                    } else {
                        this.muscleStrainSkill = skill;
                    }
                } else if (key.equalsIgnoreCase("muscleStrainParts")) {
                    this.muscleStrainParts = new ArrayList<>();
                    String[] split = val.split(";");

                    for (int i = 0; i < split.length; i++) {
                        this.muscleStrainParts.add(BodyPartType.FromString(split[i].trim()));
                    }
                }
            }
        }
    }

    @Override
    public void PreReload() {
        this.metabolics = Metabolics.Default;
        this.time = -1;
        this.faceObject = false;
        this.cantSit = false;
        this.prop1 = null;
        this.prop2 = null;
        this.actionAnim = null;
        this.animVarKey = null;
        this.animVarVal = null;
        this.sound = null;
        this.soundTime = ActionSoundTime.ACTION_START;
        this.completionSound = null;
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
    }

    public boolean hasMuscleStrain() {
        return !this.muscleStrainParts.isEmpty() && this.muscleStrainFactor > 0.0F;
    }

    public void applyMuscleStrain(IsoGameCharacter player) {
        if (this.hasMuscleStrain()) {
            float strain = GameTime.instance.getMultiplier() * this.muscleStrainFactor;
            if (this.muscleStrainSkill != null) {
                int skillLvl = player.getPerkLevel(this.muscleStrainSkill);
                strain *= (float)(1.0 - skillLvl * 0.5);
            }

            for (int i = 0; i < this.muscleStrainParts.size(); i++) {
                player.addStiffness(this.muscleStrainParts.get(i), strain);
            }
        }
    }
}
