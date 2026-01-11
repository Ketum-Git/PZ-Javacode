// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.core.opengl.RenderSettings;
import zombie.core.random.Rand;
import zombie.inventory.ItemContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.scripting.objects.ItemType;

@UsedFromLua
public final class IsoBuilding {
    public Rectangle bounds;
    public final Vector<IsoRoomExit> exits = new Vector<>();
    public boolean isResidence = true;
    public final ArrayList<ItemContainer> container = new ArrayList<>();
    public final Vector<IsoRoom> rooms = new Vector<>();
    public final Vector<IsoWindow> windows = new Vector<>();
    public int id;
    public static int idCount;
    public int safety;
    public int transparentWalls;
    private boolean isToxic;
    public static float poorBuildingScore = 10.0F;
    public static float goodBuildingScore = 100.0F;
    public int scoreUpdate = -1;
    public BuildingDef def;
    public boolean seenInside;
    public ArrayList<IsoLightSource> lights = new ArrayList<>();
    static ArrayList<IsoRoom> tempo = new ArrayList<>();
    static ArrayList<ItemContainer> tempContainer = new ArrayList<>();
    static ArrayList<String> randomContainerChoices = new ArrayList<>();
    static ArrayList<IsoWindow> windowchoices = new ArrayList<>();

    public int getRoomsNumber() {
        return this.rooms.size();
    }

    public IsoBuilding() {
        this.id = idCount++;
        this.scoreUpdate = -120 + Rand.Next(120);
    }

    public int getID() {
        return this.id;
    }

    public void TriggerAlarm() {
    }

    public IsoBuilding(IsoCell cell) {
        this.id = idCount++;
        this.scoreUpdate = -120 + Rand.Next(120);
    }

    public boolean ContainsAllItems(Stack<String> items) {
        return false;
    }

    public float ScoreBuildingPersonSpecific(SurvivorDesc desc, boolean bFarGood) {
        float score = 0.0F;
        score += this.rooms.size() * 5;
        score += this.exits.size() * 15;
        score -= this.transparentWalls * 10;

        for (int n = 0; n < this.container.size(); n++) {
            ItemContainer con = this.container.get(n);
            score += con.items.size() * 3;
        }

        if (!IsoWorld.instance.currentCell.getBuildingScores().containsKey(this.id)) {
            BuildingScore s = new BuildingScore(this);
            s.building = this;
            IsoWorld.instance.currentCell.getBuildingScores().put(this.id, s);
            this.ScoreBuildingGeneral(s);
        }

        BuildingScore s = IsoWorld.instance.currentCell.getBuildingScores().get(this.id);
        score += (s.defense + s.food + s.size + s.weapons + s.wood) * 10.0F;
        int dx = -10000;
        int dy = -10000;
        if (!this.exits.isEmpty()) {
            IsoRoomExit exit = this.exits.get(0);
            dx = exit.x;
            dy = exit.y;
        }

        float dist = IsoUtils.DistanceManhatten(desc.getInstance().getX(), desc.getInstance().getY(), dx, dy);
        if (dist > 0.0F) {
            if (bFarGood) {
                score *= dist * 0.5F;
            } else {
                score /= dist * 0.5F;
            }
        }

        return score;
    }

    public BuildingDef getDef() {
        return this.def;
    }

    public void update() {
        if (!this.exits.isEmpty()) {
            int safescore = 0;
            int tilecount = 0;

            for (int n = 0; n < this.rooms.size(); n++) {
                IsoRoom room = this.rooms.get(n);
                if (room.layer == 0) {
                    for (int z = 0; z < room.tileList.size(); z++) {
                        tilecount++;
                        IsoGridSquare var6 = room.tileList.get(z);
                    }
                }
            }

            if (tilecount == 0) {
                tilecount++;
            }

            safescore = (int)((float)safescore / tilecount);
            this.scoreUpdate--;
            if (this.scoreUpdate <= 0) {
                this.scoreUpdate += 120;
                BuildingScore score = null;
                if (IsoWorld.instance.currentCell.getBuildingScores().containsKey(this.id)) {
                    score = IsoWorld.instance.currentCell.getBuildingScores().get(this.id);
                } else {
                    score = new BuildingScore(this);
                    score.building = this;
                }

                score = this.ScoreBuildingGeneral(score);
                score.defense += safescore * 10;
                this.safety = safescore;
                IsoWorld.instance.currentCell.getBuildingScores().put(this.id, score);
            }
        }
    }

    public void AddRoom(IsoRoom room) {
        this.rooms.add(room);
        if (this.bounds == null) {
            this.bounds = (Rectangle)room.bounds.clone();
        }

        if (room != null && room.bounds != null) {
            this.bounds.add(room.bounds);
        }
    }

    public void CalculateExits() {
        for (IsoRoom room : this.rooms) {
            for (IsoRoomExit exit : room.exits) {
                if (exit.to.from == null && room.layer == 0) {
                    this.exits.add(exit);
                }
            }
        }
    }

    public void CalculateWindows() {
        for (IsoRoom room : this.rooms) {
            for (IsoGridSquare sq : room.tileList) {
                IsoGridSquare s = sq.getCell().getGridSquare(sq.getX(), sq.getY() + 1, sq.getZ());
                IsoGridSquare e = sq.getCell().getGridSquare(sq.getX() + 1, sq.getY(), sq.getZ());
                if (sq.getProperties().has(IsoFlagType.collideN) && sq.getProperties().has(IsoFlagType.transparentN)) {
                    room.transparentWalls++;
                    this.transparentWalls++;
                }

                if (sq.getProperties().has(IsoFlagType.collideW) && sq.getProperties().has(IsoFlagType.transparentW)) {
                    room.transparentWalls++;
                    this.transparentWalls++;
                }

                if (s != null) {
                    boolean bSameBuilding = s.getRoom() != null;
                    if (s.getRoom() != null && s.getRoom().building != room.building) {
                        bSameBuilding = false;
                    }

                    if (s.getProperties().has(IsoFlagType.collideN) && s.getProperties().has(IsoFlagType.transparentN) && !bSameBuilding) {
                        room.transparentWalls++;
                        this.transparentWalls++;
                    }
                }

                if (e != null) {
                    boolean bSameBuildingx = e.getRoom() != null;
                    if (e.getRoom() != null && e.getRoom().building != room.building) {
                        bSameBuildingx = false;
                    }

                    if (e.getProperties().has(IsoFlagType.collideW) && e.getProperties().has(IsoFlagType.transparentW) && !bSameBuildingx) {
                        room.transparentWalls++;
                        this.transparentWalls++;
                    }
                }

                for (int n = 0; n < sq.getSpecialObjects().size(); n++) {
                    IsoObject spec = sq.getSpecialObjects().get(n);
                    if (spec instanceof IsoWindow isoWindow) {
                        this.windows.add(isoWindow);
                    }
                }

                if (s != null) {
                    for (int nx = 0; nx < s.getSpecialObjects().size(); nx++) {
                        IsoObject spec = s.getSpecialObjects().get(nx);
                        if (spec instanceof IsoWindow isoWindow) {
                            this.windows.add(isoWindow);
                        }
                    }
                }

                if (e != null) {
                    for (int nxx = 0; nxx < e.getSpecialObjects().size(); nxx++) {
                        IsoObject spec = e.getSpecialObjects().get(nxx);
                        if (spec instanceof IsoWindow isoWindow) {
                            this.windows.add(isoWindow);
                        }
                    }
                }
            }
        }
    }

    public void FillContainers() {
        boolean bIsTutHouse = false;

        for (IsoRoom room : this.rooms) {
            if (room.roomDef != null && room.roomDef.contains("tutorial")) {
                bIsTutHouse = true;
            }

            if (!room.tileList.isEmpty()) {
                IsoGridSquare sq2 = room.tileList.get(0);
                if (sq2.getX() < 74 && sq2.getY() < 32) {
                    bIsTutHouse = true;
                }
            }

            if (room.roomDef.contains("shop")) {
                this.isResidence = false;
            }

            for (IsoGridSquare sq : room.tileList) {
                for (int n = 0; n < sq.getObjects().size(); n++) {
                    IsoObject obj = sq.getObjects().get(n);
                    if (obj.hasWater()) {
                        room.getWaterSources().add(obj);
                    }

                    if (obj.container != null) {
                        this.container.add(obj.container);
                        room.containers.add(obj.container);
                    }
                }

                if (sq.getProperties().has(IsoFlagType.bed)) {
                    room.beds.add(sq);
                }
            }
        }
    }

    public ItemContainer getContainerWith(ItemType itemType) {
        for (IsoRoom room : this.rooms) {
            for (ItemContainer container : room.containers) {
                if (container.HasType(itemType)) {
                    return container;
                }
            }
        }

        return null;
    }

    public IsoRoom getRandomRoom() {
        return this.rooms.isEmpty() ? null : this.rooms.get(Rand.Next(this.rooms.size()));
    }

    private BuildingScore ScoreBuildingGeneral(BuildingScore score) {
        score.food = 0.0F;
        score.defense = 0.0F;
        score.weapons = 0.0F;
        score.wood = 0.0F;
        score.building = this;
        score.size = 0;
        score.defense = score.defense + (this.exits.size() - 1) * 140;
        score.defense = score.defense - this.transparentWalls * 40;
        score.size = this.rooms.size() * 10;
        score.size = score.size + this.container.size() * 10;
        return score;
    }

    public IsoGridSquare getFreeTile() {
        IsoGridSquare sq = null;

        do {
            IsoRoom room = this.rooms.get(Rand.Next(this.rooms.size()));
            sq = room.getFreeTile();
        } while (sq == null);

        return sq;
    }

    public boolean hasWater() {
        Iterator<IsoRoom> it = this.rooms.iterator();

        while (it != null && it.hasNext()) {
            IsoRoom r = it.next();
            if (!r.waterSources.isEmpty()) {
                IsoObject waterSource = null;
                int i = 0;

                while (true) {
                    if (i < r.waterSources.size()) {
                        if (!r.waterSources.get(i).hasWater()) {
                            i++;
                            continue;
                        }

                        waterSource = r.waterSources.get(i);
                    }

                    if (waterSource != null) {
                        return true;
                    }
                    break;
                }
            }
        }

        return false;
    }

    public void CreateFrom(BuildingDef building, IsoMetaCell metaCell) {
        for (int n = 0; n < building.rooms.size(); n++) {
            RoomDef roomDef = building.rooms.get(n);
            IsoRoom r = IsoWorld.instance.getMetaGrid().getRoomByID(roomDef.id);
            if (r != null) {
                r.building = this;
                if (!this.rooms.contains(r)) {
                    this.rooms.add(r);
                }
            }
        }
    }

    public void setAllExplored(boolean b) {
        this.def.alarmed = false;

        for (int n = 0; n < this.rooms.size(); n++) {
            IsoRoom r = this.rooms.get(n);
            r.def.setExplored(b);

            for (int x = r.def.getX(); x <= r.def.getX2(); x++) {
                for (int y = r.def.getY(); y <= r.def.getY2(); y++) {
                    IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare(x, y, r.def.level);
                    if (g != null) {
                        g.setHourSeenToCurrent();
                    }
                }
            }
        }
    }

    public boolean isAllExplored() {
        for (int n = 0; n < this.rooms.size(); n++) {
            IsoRoom r = this.rooms.get(n);
            if (!r.def.explored) {
                return false;
            }
        }

        return true;
    }

    public void addWindow(IsoWindow obj, boolean bOtherTile, IsoGridSquare from, IsoBuilding building) {
        this.windows.add(obj);
        IsoGridSquare squareToAdd = null;
        if (bOtherTile) {
            squareToAdd = obj.square;
        } else {
            squareToAdd = from;
        }

        if (squareToAdd != null) {
            if (squareToAdd.getRoom() == null) {
                float r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                float g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                float b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                int radius = 7;
                IsoLightSource ls = new IsoLightSource(squareToAdd.getX(), squareToAdd.getY(), squareToAdd.getZ(), r, g, b, 7, building);
                this.lights.add(ls);
                IsoWorld.instance.currentCell.getLamppostPositions().add(ls);
            }
        }
    }

    public void addWindow(IsoWindow obj, boolean bOtherTile) {
        this.addWindow(obj, bOtherTile, obj.square, null);
    }

    public void addDoor(IsoDoor obj, boolean bOtherTile, IsoGridSquare from, IsoBuilding building) {
        IsoGridSquare squareToAdd = null;
        if (bOtherTile) {
            squareToAdd = obj.square;
        } else {
            squareToAdd = from;
        }

        if (squareToAdd != null) {
            if (squareToAdd.getRoom() == null) {
                float r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                float g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                float b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7F;
                int radius = 7;
                IsoLightSource ls = new IsoLightSource(squareToAdd.getX(), squareToAdd.getY(), squareToAdd.getZ(), r, g, b, 7, building);
                this.lights.add(ls);
                IsoWorld.instance.currentCell.getLamppostPositions().add(ls);
            }
        }
    }

    public void addDoor(IsoDoor obj, boolean bOtherTile) {
        this.addDoor(obj, bOtherTile, obj.square, null);
    }

    public boolean isResidential() {
        return this.getDef().isResidential();
    }

    public boolean containsRoom(String room) {
        for (int n = 0; n < this.rooms.size(); n++) {
            if (room.equals(this.rooms.get(n).getName())) {
                return true;
            }
        }

        return false;
    }

    public IsoRoom getRandomRoom(String room) {
        tempo.clear();

        for (int n = 0; n < this.rooms.size(); n++) {
            if (room.equals(this.rooms.get(n).getName())) {
                tempo.add(this.rooms.get(n));
            }
        }

        return tempo.isEmpty() ? null : tempo.get(Rand.Next(tempo.size()));
    }

    public boolean hasRoom(String room) {
        for (int n = 0; n < this.rooms.size(); n++) {
            if (room.equals(this.rooms.get(n).getName())) {
                return true;
            }
        }

        return false;
    }

    public ItemContainer getRandomContainer(String type) {
        randomContainerChoices.clear();
        String[] choices = null;
        if (type != null) {
            choices = type.split(",");
        }

        if (choices != null) {
            for (int n = 0; n < choices.length; n++) {
                randomContainerChoices.add(choices[n]);
            }
        }

        tempContainer.clear();

        for (int n = 0; n < this.rooms.size(); n++) {
            IsoRoom room = this.rooms.get(n);

            for (int m = 0; m < room.containers.size(); m++) {
                ItemContainer c = room.containers.get(m);
                if (type == null || randomContainerChoices.contains(c.getType())) {
                    tempContainer.add(c);
                }
            }
        }

        return tempContainer.isEmpty() ? null : tempContainer.get(Rand.Next(tempContainer.size()));
    }

    public ItemContainer getRandomContainerSingle(String type) {
        tempContainer.clear();

        for (int n = 0; n < this.rooms.size(); n++) {
            IsoRoom room = this.rooms.get(n);

            for (int m = 0; m < room.containers.size(); m++) {
                ItemContainer c = room.containers.get(m);
                if (type == null || type.equals(c.getType())) {
                    tempContainer.add(c);
                }
            }
        }

        return tempContainer.isEmpty() ? null : tempContainer.get(Rand.Next(tempContainer.size()));
    }

    public IsoWindow getRandomFirstFloorWindow() {
        windowchoices.clear();
        windowchoices.addAll(this.windows);

        for (int n = 0; n < windowchoices.size(); n++) {
            if (windowchoices.get(n).getZ() > 0.0F) {
                windowchoices.remove(n);
            }
        }

        return !windowchoices.isEmpty() ? windowchoices.get(Rand.Next(windowchoices.size())) : null;
    }

    public boolean isToxic() {
        return this.isToxic;
    }

    public void setToxic(boolean isToxic) {
        this.isToxic = isToxic;
        if (GameServer.server) {
            GameServer.sendToxicBuilding(this.getDef().getX() + this.getDef().getW() / 2, this.getDef().getY() + this.getDef().getH() / 2, isToxic);
        }
    }

    /**
     * Check for player inside the house and awake them all
     */
    public void forceAwake() {
        for (int x = this.def.getX(); x <= this.def.getX2(); x++) {
            for (int y = this.def.getY(); y <= this.def.getY2(); y++) {
                for (int z = 0; z <= 4; z++) {
                    IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    if (g != null) {
                        for (int i = 0; i < g.getMovingObjects().size(); i++) {
                            if (g.getMovingObjects().get(i) instanceof IsoGameCharacter) {
                                ((IsoGameCharacter)g.getMovingObjects().get(i)).forceAwake();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean hasBasement() {
        return false;
    }

    public boolean isEntirelyEmptyOutside() {
        return this.getDef() != null && this.getDef().isEntirelyEmptyOutside();
    }
}
