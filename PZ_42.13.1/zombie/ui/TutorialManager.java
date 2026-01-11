// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.network.GameServer;

public final class TutorialManager {
    public static boolean debug;
    public boolean active;
    public boolean activeControlZombies;
    public float targetZombies;
    public TutorialManager.Stage stage = TutorialManager.Stage.getBelt;
    public IsoSurvivor wife;
    private IsoZombie zombie;
    public IsoStove tutorialStove;
    public IsoBuilding tutBuilding;
    public boolean doorsLocked = true;
    public int barricadeCount;
    public IsoSurvivor gunnut;
    public boolean stealControl;
    public int alarmTime;
    public boolean profanityFilter;
    public int timer;
    public int alarmTickTime = 160;
    public boolean doneFirstSleep;
    public boolean wifeKilledByEarl;
    public boolean warnedHammer;
    public boolean triggerFire;
    public boolean canDragWife;
    public boolean allowSleep;
    public boolean skipped;
    private final boolean doneDeath = false;
    boolean doGunnutDeadTalk = true;
    public String millingTune = "tune1.ogg";
    IsoRadio radio;
    public static TutorialManager instance = new TutorialManager();

    public boolean AllowUse(IsoObject Object) {
        return true;
    }

    public void CheckWake() {
    }

    public void CreateQuests() {
        try {
            for (int n = 0; n < IsoWorld.instance.currentCell.getStaticUpdaterObjectList().size(); n++) {
                IsoObject obj = IsoWorld.instance.currentCell.getStaticUpdaterObjectList().get(n);
                if (obj instanceof IsoRadio isoRadio) {
                    this.radio = isoRadio;
                }
            }
        } catch (Exception var4) {
            var4.printStackTrace();
            this.radio = null;
        }
    }

    public void init() {
        if (!GameServer.server) {
            if (this.active) {
                ;
            }
        }
    }

    public void update() {
    }

    private void ForceKillZombies() {
        IsoWorld.instance.ForceKillAllZombies();
    }

    public static enum Stage {
        getBelt,
        RipSheet,
        Apply,
        FindShed,
        getShedItems,
        EquipHammer,
        BoardUpHouse,
        FindFood,
        InHouseFood,
        KillZombie,
        StockUp,
        ExploreHouse,
        BreakBarricade,
        getSoupIngredients,
        MakeSoupPot,
        LightStove,
        Distraction,
        InvestigateSound,
        Alarm,
        Mouseover,
        Escape,
        ShouldBeOk;
    }
}
