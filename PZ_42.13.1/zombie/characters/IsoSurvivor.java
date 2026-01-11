// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.core.Color;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;
import zombie.iso.IsoPushableObject;

@UsedFromLua
public final class IsoSurvivor extends IsoLivingCharacter {
    public boolean noGoreDeath;
    public boolean draggable;
    public IsoGameCharacter following;
    public boolean dragging;
    private final int repathDelay = 0;
    public int nightsSurvived;
    private int ping;
    public IsoPushableObject collidePushable;
    private final boolean tryToTeamUp = true;
    private int neightbourUpdate = 20;
    private final int neightbourUpdateMax = 20;

    @Override
    public void Despawn() {
        if (this.descriptor != null) {
            this.descriptor.setInstance(null);
        }
    }

    @Override
    public String getObjectName() {
        return "Survivor";
    }

    public IsoSurvivor(IsoCell cell) {
        super(cell, 0.0F, 0.0F, 0.0F);
        this.outlineOnMouseover = true;
        this.getCell().getSurvivorList().add(this);
        LuaEventManager.triggerEvent("OnCreateSurvivor", this);
        this.initWornItems("Human");
        this.initAttachedItems("Human");
    }

    public IsoSurvivor(SurvivorDesc desc, IsoCell cell, int x, int y, int z) {
        super(cell, x, y, z);
        this.setFemale(desc.isFemale());
        this.descriptor = desc;
        desc.setInstance(this);
        this.outlineOnMouseover = true;
        String str = "Zombie_palette";
        str = str + "01";
        this.InitSpriteParts(desc);
        this.speakColour = new Color(Rand.Next(200) + 55, Rand.Next(200) + 55, Rand.Next(200) + 55, 255);
        this.finder.maxSearchDistance = 120;
        this.neightbourUpdate = Rand.Next(20);
        this.Dressup(desc);
        LuaEventManager.triggerEventGarbage("OnCreateSurvivor", this);
        LuaEventManager.triggerEventGarbage("OnCreateLivingCharacter", this, this.descriptor);
        this.initWornItems("Human");
        this.initAttachedItems("Human");
    }

    public void reloadSpritePart() {
    }

    public IsoSurvivor(SurvivorDesc desc, IsoCell cell, int x, int y, int z, boolean bSetInstance) {
        super(cell, x, y, z);
        this.setFemale(desc.isFemale());
        this.descriptor = desc;
        if (bSetInstance) {
            desc.setInstance(this);
        }

        this.outlineOnMouseover = true;
        this.InitSpriteParts(desc);
        this.speakColour = new Color(Rand.Next(200) + 55, Rand.Next(200) + 55, Rand.Next(200) + 55, 255);
        this.finder.maxSearchDistance = 120;
        this.neightbourUpdate = Rand.Next(20);
        this.Dressup(desc);
        LuaEventManager.triggerEvent("OnCreateSurvivor", this);
    }
}
